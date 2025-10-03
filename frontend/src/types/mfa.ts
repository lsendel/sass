/**
 * Multi-Factor Authentication (MFA) Types
 *
 * Comprehensive type definitions for MFA system including:
 * - TOTP (Time-based One-Time Password)
 * - SMS verification
 * - Email verification
 * - Backup codes
 * - Biometric authentication
 * - Hardware security keys (WebAuthn)
 */

// MFA Method Types
export type MFAMethodType =
  | 'totp'
  | 'sms'
  | 'email'
  | 'backup_codes'
  | 'webauthn'
  | 'biometric'

export interface MFAMethod {
  id: string
  type: MFAMethodType
  name: string
  isEnabled: boolean
  isPrimary: boolean
  createdAt: string
  lastUsed?: string
  metadata?: Record<string, any>
}

// TOTP-specific interfaces
export interface TOTPMethod extends MFAMethod {
  type: 'totp'
  metadata: {
    issuer: string
    accountName: string
    algorithm: 'SHA1' | 'SHA256' | 'SHA512'
    digits: 6 | 8
    period: number
    secretKey?: string // Only present during setup
    qrCodeUrl?: string // Only present during setup
    backupCodes?: string[] // Generated with TOTP setup
  }
}

// SMS-specific interfaces
export interface SMSMethod extends MFAMethod {
  type: 'sms'
  metadata: {
    phoneNumber: string
    countryCode: string
    isVerified: boolean
    lastVerificationAttempt?: string
  }
}

// Email-specific interfaces
export interface EmailMethod extends MFAMethod {
  type: 'email'
  metadata: {
    emailAddress: string
    isVerified: boolean
    lastVerificationAttempt?: string
  }
}

// Backup codes interfaces
export interface BackupCodesMethod extends MFAMethod {
  type: 'backup_codes'
  metadata: {
    totalCodes: number
    usedCodes: number
    remainingCodes: number
    lastUsed?: string
  }
}

// WebAuthn/FIDO2 interfaces
export interface WebAuthnMethod extends MFAMethod {
  type: 'webauthn'
  metadata: {
    credentialId: string
    deviceName: string
    deviceType: 'security_key' | 'platform' | 'cross_platform'
    attestationType: string
    counter: number
    userAgent: string
  }
}

// Biometric interfaces
export interface BiometricMethod extends MFAMethod {
  type: 'biometric'
  metadata: {
    biometricType: 'fingerprint' | 'face' | 'voice' | 'iris'
    deviceId: string
    platformAuthenticator: boolean
  }
}

// Union type for all MFA methods
export type AnyMFAMethod =
  | TOTPMethod
  | SMSMethod
  | EmailMethod
  | BackupCodesMethod
  | WebAuthnMethod
  | BiometricMethod

// MFA Setup interfaces
export interface MFASetupRequest {
  type: MFAMethodType
  name: string
  phoneNumber?: string
  emailAddress?: string
  deviceName?: string
}

export interface MFASetupResponse {
  method: AnyMFAMethod
  setupData?: {
    qrCode?: string
    secretKey?: string
    backupCodes?: string[]
    verificationCode?: string
    challengeOptions?: PublicKeyCredentialCreationOptions // For WebAuthn
  }
  nextStep: 'verify' | 'complete' | 'backup_codes'
}

// MFA Verification interfaces
export interface MFAVerificationRequest {
  methodId: string
  code?: string
  backupCode?: string
  webauthnResponse?: PublicKeyCredential
  biometricResponse?: any
}

export interface MFAVerificationResponse {
  success: boolean
  methodId: string
  remainingAttempts?: number
  lockoutUntil?: string
  backupCodesRemaining?: number
  error?: {
    code: string
    message: string
    details?: Record<string, any>
  }
}

// MFA Challenge interfaces (for login)
export interface MFAChallenge {
  challengeId: string
  requiredMethods: MFAMethodType[]
  availableMethods: Array<{
    id: string
    type: MFAMethodType
    name: string
    metadata: Record<string, any>
  }>
  expiresAt: string
  attemptCount: number
  maxAttempts: number
}

export interface MFAChallengeResponse {
  challengeId: string
  methodId: string
  code?: string
  backupCode?: string
  webauthnResponse?: PublicKeyCredential
  biometricResponse?: any
}

// MFA Settings and Preferences
export interface MFASettings {
  isEnabled: boolean
  requiredForLogin: boolean
  requiredForSensitiveActions: boolean
  backupCodesEnabled: boolean
  trustedDevices: TrustedDevice[]
  securityNotifications: {
    emailOnMethodChange: boolean
    emailOnSuccessfulAuth: boolean
    emailOnFailedAttempts: boolean
  }
  lockoutPolicy: {
    maxAttempts: number
    lockoutDurationMinutes: number
    progressiveLockout: boolean
  }
}

export interface TrustedDevice {
  id: string
  name: string
  deviceFingerprint: string
  userAgent: string
  ipAddress: string
  location?: string
  createdAt: string
  lastUsed: string
  expiresAt: string
}

// MFA Recovery interfaces
export interface MFARecoveryRequest {
  email: string
  recoveryMethod: 'email' | 'admin' | 'identity_verification'
  identityProof?: {
    documentType: string
    documentNumber: string
    additionalInfo: Record<string, any>
  }
}

export interface MFARecoveryResponse {
  recoveryId: string
  method: string
  nextStep: 'verify_identity' | 'wait_approval' | 'reset_available'
  estimatedTime?: string
}

// Analytics and Audit interfaces
export interface MFAAnalytics {
  userId: string
  totalMethods: number
  methodBreakdown: Record<MFAMethodType, number>
  loginAttempts: {
    successful: number
    failed: number
    locked: number
  }
  securityScore: number
  recommendations: string[]
  lastActivity: string
}

export interface MFAAuditEvent {
  id: string
  userId: string
  action: 'setup' | 'verify' | 'disable' | 'recover' | 'login_attempt'
  methodType?: MFAMethodType
  methodId?: string
  success: boolean
  ipAddress: string
  userAgent: string
  location?: string
  timestamp: string
  details?: Record<string, any>
}

// Error types
export interface MFAError {
  code: string
  message: string
  field?: string
  retryAfter?: number
  supportContact?: string
}

// Status enums
export enum MFAStatus {
  NOT_SETUP = 'not_setup',
  PARTIALLY_SETUP = 'partially_setup',
  FULLY_SETUP = 'fully_setup',
  DISABLED = 'disabled',
  LOCKED = 'locked',
  RECOVERY_MODE = 'recovery_mode',
}

export enum MFASecurityLevel {
  BASIC = 'basic', // Single factor (password only)
  STANDARD = 'standard', // Two factors (password + one MFA)
  ENHANCED = 'enhanced', // Multiple MFA methods available
  MAXIMUM = 'maximum', // Hardware security key + biometrics
}

// Utility types
export type MFAMethodMap = {
  [K in MFAMethodType]: Extract<AnyMFAMethod, { type: K }>
}

export interface MFACapabilities {
  supportedMethods: MFAMethodType[]
  webauthnSupported: boolean
  biometricSupported: boolean
  platformAuthenticatorAvailable: boolean
  securityKeySupported: boolean
}

// React component props types
export interface MFASetupProps {
  onComplete: (method: AnyMFAMethod) => void
  onCancel: () => void
  allowedMethods?: MFAMethodType[]
}

export interface MFAVerificationProps {
  challenge: MFAChallenge
  onVerify: (response: MFAChallengeResponse) => void
  onCancel?: () => void
  showBackupOptions?: boolean
}

export interface MFAManagementProps {
  methods: AnyMFAMethod[]
  settings: MFASettings
  onMethodUpdate: (method: AnyMFAMethod) => void
  onSettingsUpdate: (settings: Partial<MFASettings>) => void
}

// Form validation schemas (to be used with Zod)
export interface TOTPSetupForm {
  name: string
  verificationCode: string
}

export interface SMSSetupForm {
  name: string
  phoneNumber: string
  countryCode: string
  verificationCode: string
}

export interface EmailSetupForm {
  name: string
  emailAddress: string
  verificationCode: string
}

export interface WebAuthnSetupForm {
  name: string
  deviceName: string
}
