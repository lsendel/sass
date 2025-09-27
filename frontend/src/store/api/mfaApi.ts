/**
 * MFA API Service
 *
 * RTK Query API service for Multi-Factor Authentication operations:
 * - MFA method setup and management
 * - Verification and challenges
 * - Recovery and backup codes
 * - WebAuthn integration
 * - Security analytics
 */

import { createApi } from '@reduxjs/toolkit/query/react'
import { z } from 'zod'

import { createValidatedBaseQuery, createValidatedEndpoint, wrapSuccessResponse } from '@/lib/api/validation'
import {
  AnyMFAMethod,
  MFASettings,
  MFASetupRequest,
  MFASetupResponse,
  MFAVerificationRequest,
  MFAVerificationResponse,
  MFAChallenge,
  MFAChallengeResponse,
  MFARecoveryRequest,
  MFARecoveryResponse,
  MFAAnalytics,
  MFAAuditEvent,
  MFACapabilities,
  WebAuthnMethod,
} from '@/types/mfa'

// Validation schemas
const MFAMethodSchema = z.object({
  id: z.string(),
  type: z.enum(['totp', 'sms', 'email', 'backup_codes', 'webauthn', 'biometric']),
  name: z.string(),
  isEnabled: z.boolean(),
  isPrimary: z.boolean(),
  createdAt: z.string(),
  lastUsed: z.string().optional(),
  metadata: z.record(z.any()).optional(),
})

const MFASettingsSchema = z.object({
  isEnabled: z.boolean(),
  requiredForLogin: z.boolean(),
  requiredForSensitiveActions: z.boolean(),
  backupCodesEnabled: z.boolean(),
  trustedDevices: z.array(z.object({
    id: z.string(),
    name: z.string(),
    deviceFingerprint: z.string(),
    userAgent: z.string(),
    ipAddress: z.string(),
    location: z.string().optional(),
    createdAt: z.string(),
    lastUsed: z.string(),
    expiresAt: z.string(),
  })),
  securityNotifications: z.object({
    emailOnMethodChange: z.boolean(),
    emailOnSuccessfulAuth: z.boolean(),
    emailOnFailedAttempts: z.boolean(),
  }),
  lockoutPolicy: z.object({
    maxAttempts: z.number(),
    lockoutDurationMinutes: z.number(),
    progressiveLockout: z.boolean(),
  }),
})

const MFASetupRequestSchema = z.object({
  type: z.enum(['totp', 'sms', 'email', 'backup_codes', 'webauthn', 'biometric']),
  name: z.string().min(1, 'Method name is required'),
  phoneNumber: z.string().optional(),
  emailAddress: z.string().email().optional(),
  deviceName: z.string().optional(),
})

const MFAVerificationRequestSchema = z.object({
  methodId: z.string(),
  code: z.string().optional(),
  backupCode: z.string().optional(),
  webauthnResponse: z.any().optional(),
  biometricResponse: z.any().optional(),
})

const MFAChallengeResponseSchema = z.object({
  challengeId: z.string(),
  methodId: z.string(),
  code: z.string().optional(),
  backupCode: z.string().optional(),
  webauthnResponse: z.any().optional(),
  biometricResponse: z.any().optional(),
})

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const mfaApi = createApi({
  reducerPath: 'mfaApi',
  baseQuery: createValidatedBaseQuery(`${API_BASE_URL}/mfa`),
  tagTypes: ['MFAMethod', 'MFASettings', 'MFAAnalytics', 'MFACapabilities'],
  endpoints: (builder) => ({

    // Get user's MFA methods
    getMFAMethods: builder.query<AnyMFAMethod[], void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(MFAMethodSchema)), {
        query: () => '/methods',
      }),
      providesTags: ['MFAMethod'],
    }),

    // Get MFA capabilities (what the browser/device supports)
    getMFACapabilities: builder.query<MFACapabilities, void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        supportedMethods: z.array(z.enum(['totp', 'sms', 'email', 'backup_codes', 'webauthn', 'biometric'])),
        webauthnSupported: z.boolean(),
        biometricSupported: z.boolean(),
        platformAuthenticatorAvailable: z.boolean(),
        securityKeySupported: z.boolean(),
      })), {
        query: () => '/capabilities',
      }),
      providesTags: ['MFACapabilities'],
    }),

    // Setup new MFA method
    setupMFAMethod: builder.mutation<MFASetupResponse, MFASetupRequest>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        method: MFAMethodSchema,
        setupData: z.object({
          qrCode: z.string().optional(),
          secretKey: z.string().optional(),
          backupCodes: z.array(z.string()).optional(),
          verificationCode: z.string().optional(),
          challengeOptions: z.any().optional(), // PublicKeyCredentialCreationOptions
        }).optional(),
        nextStep: z.enum(['verify', 'complete', 'backup_codes']),
      })), {
        query: (setupRequest) => {
          MFASetupRequestSchema.parse(setupRequest)
          return {
            url: '/methods/setup',
            method: 'POST',
            body: setupRequest,
          }
        },
      }),
      invalidatesTags: ['MFAMethod'],
    }),

    // Verify MFA method during setup
    verifyMFASetup: builder.mutation<{ success: boolean; method: AnyMFAMethod }, MFAVerificationRequest>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        success: z.boolean(),
        method: MFAMethodSchema,
      })), {
        query: (verificationRequest) => {
          MFAVerificationRequestSchema.parse(verificationRequest)
          return {
            url: '/methods/verify-setup',
            method: 'POST',
            body: verificationRequest,
          }
        },
      }),
      invalidatesTags: ['MFAMethod'],
    }),

    // Update MFA method (enable/disable, rename, etc.)
    updateMFAMethod: builder.mutation<AnyMFAMethod, { methodId: string; updates: Partial<Pick<AnyMFAMethod, 'name' | 'isEnabled' | 'isPrimary'>> }>({
      ...createValidatedEndpoint(wrapSuccessResponse(MFAMethodSchema), {
        query: ({ methodId, updates }) => ({
          url: `/methods/${methodId}`,
          method: 'PATCH',
          body: updates,
        }),
      }),
      invalidatesTags: ['MFAMethod'],
    }),

    // Delete MFA method
    deleteMFAMethod: builder.mutation<{ success: boolean }, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({ success: z.boolean() })), {
        query: (methodId) => ({
          url: `/methods/${methodId}`,
          method: 'DELETE',
        }),
      }),
      invalidatesTags: ['MFAMethod'],
    }),

    // Get backup codes
    getBackupCodes: builder.query<string[], void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.string())), {
        query: () => '/backup-codes',
      }),
    }),

    // Generate new backup codes
    generateBackupCodes: builder.mutation<string[], void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.string())), {
        query: () => ({
          url: '/backup-codes/generate',
          method: 'POST',
        }),
      }),
      invalidatesTags: ['MFAMethod'],
    }),

    // Get MFA settings
    getMFASettings: builder.query<MFASettings, void>({
      ...createValidatedEndpoint(wrapSuccessResponse(MFASettingsSchema), {
        query: () => '/settings',
      }),
      providesTags: ['MFASettings'],
    }),

    // Update MFA settings
    updateMFASettings: builder.mutation<MFASettings, Partial<MFASettings>>({
      ...createValidatedEndpoint(wrapSuccessResponse(MFASettingsSchema), {
        query: (settings) => ({
          url: '/settings',
          method: 'PATCH',
          body: settings,
        }),
      }),
      invalidatesTags: ['MFASettings'],
    }),

    // Challenge-based verification (for login and sensitive actions)
    createMFAChallenge: builder.mutation<MFAChallenge, { action: 'login' | 'sensitive_action'; context?: Record<string, any> }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        challengeId: z.string(),
        requiredMethods: z.array(z.enum(['totp', 'sms', 'email', 'backup_codes', 'webauthn', 'biometric'])),
        availableMethods: z.array(z.object({
          id: z.string(),
          type: z.enum(['totp', 'sms', 'email', 'backup_codes', 'webauthn', 'biometric']),
          name: z.string(),
          metadata: z.record(z.any()),
        })),
        expiresAt: z.string(),
        attemptCount: z.number(),
        maxAttempts: z.number(),
      })), {
        query: (challengeRequest) => ({
          url: '/challenges',
          method: 'POST',
          body: challengeRequest,
        }),
      }),
    }),

    // Respond to MFA challenge
    respondToMFAChallenge: builder.mutation<MFAVerificationResponse, MFAChallengeResponse>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        success: z.boolean(),
        methodId: z.string(),
        remainingAttempts: z.number().optional(),
        lockoutUntil: z.string().optional(),
        backupCodesRemaining: z.number().optional(),
        error: z.object({
          code: z.string(),
          message: z.string(),
          details: z.record(z.any()).optional(),
        }).optional(),
      })), {
        query: (challengeResponse) => {
          MFAChallengeResponseSchema.parse(challengeResponse)
          return {
            url: '/challenges/respond',
            method: 'POST',
            body: challengeResponse,
          }
        },
      }),
    }),

    // MFA recovery
    initiateMFARecovery: builder.mutation<MFARecoveryResponse, MFARecoveryRequest>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        recoveryId: z.string(),
        method: z.string(),
        nextStep: z.enum(['verify_identity', 'wait_approval', 'reset_available']),
        estimatedTime: z.string().optional(),
      })), {
        query: (recoveryRequest) => ({
          url: '/recovery/initiate',
          method: 'POST',
          body: recoveryRequest,
        }),
      }),
    }),

    // Check recovery status
    checkMFARecoveryStatus: builder.query<{ status: string; canReset: boolean; nextStep?: string }, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        status: z.string(),
        canReset: z.boolean(),
        nextStep: z.string().optional(),
      })), {
        query: (recoveryId) => `/recovery/${recoveryId}/status`,
      }),
    }),

    // Complete MFA recovery
    completeMFARecovery: builder.mutation<{ success: boolean; temporaryToken?: string }, { recoveryId: string; verificationData: Record<string, any> }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        success: z.boolean(),
        temporaryToken: z.string().optional(),
      })), {
        query: ({ recoveryId, verificationData }) => ({
          url: `/recovery/${recoveryId}/complete`,
          method: 'POST',
          body: verificationData,
        }),
      }),
      invalidatesTags: ['MFAMethod', 'MFASettings'],
    }),

    // Get MFA analytics
    getMFAAnalytics: builder.query<MFAAnalytics, void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        userId: z.string(),
        totalMethods: z.number(),
        methodBreakdown: z.record(z.number()),
        loginAttempts: z.object({
          successful: z.number(),
          failed: z.number(),
          locked: z.number(),
        }),
        securityScore: z.number(),
        recommendations: z.array(z.string()),
        lastActivity: z.string(),
      })), {
        query: () => '/analytics',
      }),
      providesTags: ['MFAAnalytics'],
    }),

    // Get MFA audit log
    getMFAAuditLog: builder.query<MFAAuditEvent[], { limit?: number; offset?: number }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        userId: z.string(),
        action: z.enum(['setup', 'verify', 'disable', 'recover', 'login_attempt']),
        methodType: z.enum(['totp', 'sms', 'email', 'backup_codes', 'webauthn', 'biometric']).optional(),
        methodId: z.string().optional(),
        success: z.boolean(),
        ipAddress: z.string(),
        userAgent: z.string(),
        location: z.string().optional(),
        timestamp: z.string(),
        details: z.record(z.any()).optional(),
      }))), {
        query: ({ limit = 50, offset = 0 }) => ({
          url: '/audit',
          params: { limit, offset },
        }),
      }),
    }),

    // WebAuthn specific endpoints
    beginWebAuthnRegistration: builder.mutation<PublicKeyCredentialCreationOptions, { deviceName: string }>({
      query: ({ deviceName }) => ({
        url: '/webauthn/register/begin',
        method: 'POST',
        body: { deviceName },
      }),
    }),

    completeWebAuthnRegistration: builder.mutation<WebAuthnMethod, {
      deviceName: string;
      credential: PublicKeyCredential
    }>({
      query: ({ deviceName, credential }) => ({
        url: '/webauthn/register/complete',
        method: 'POST',
        body: { deviceName, credential },
      }),
      invalidatesTags: ['MFAMethod'],
    }),

    beginWebAuthnAuthentication: builder.mutation<PublicKeyCredentialRequestOptions, { challengeId: string }>({
      query: ({ challengeId }) => ({
        url: '/webauthn/authenticate/begin',
        method: 'POST',
        body: { challengeId },
      }),
    }),

    // Trusted device management
    getTrustedDevices: builder.query<MFASettings['trustedDevices'], void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        name: z.string(),
        deviceFingerprint: z.string(),
        userAgent: z.string(),
        ipAddress: z.string(),
        location: z.string().optional(),
        createdAt: z.string(),
        lastUsed: z.string(),
        expiresAt: z.string(),
      }))), {
        query: () => '/trusted-devices',
      }),
      providesTags: ['MFASettings'],
    }),

    revokeTrustedDevice: builder.mutation<{ success: boolean }, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({ success: z.boolean() })), {
        query: (deviceId) => ({
          url: `/trusted-devices/${deviceId}`,
          method: 'DELETE',
        }),
      }),
      invalidatesTags: ['MFASettings'],
    }),
  }),
})

// Export hooks
export const {
  useGetMFAMethodsQuery,
  useGetMFACapabilitiesQuery,
  useSetupMFAMethodMutation,
  useVerifyMFASetupMutation,
  useUpdateMFAMethodMutation,
  useDeleteMFAMethodMutation,
  useGetBackupCodesQuery,
  useGenerateBackupCodesMutation,
  useGetMFASettingsQuery,
  useUpdateMFASettingsMutation,
  useCreateMFAChallengeMutation,
  useRespondToMFAChallengeMutation,
  useInitiateMFARecoveryMutation,
  useCheckMFARecoveryStatusQuery,
  useCompleteMFARecoveryMutation,
  useGetMFAAnalyticsQuery,
  useGetMFAAuditLogQuery,
  useBeginWebAuthnRegistrationMutation,
  useCompleteWebAuthnRegistrationMutation,
  useBeginWebAuthnAuthenticationMutation,
  useGetTrustedDevicesQuery,
  useRevokeTrustedDeviceMutation,
} = mfaApi