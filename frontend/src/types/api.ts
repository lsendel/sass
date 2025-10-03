import { z } from 'zod'

/**
 * Comprehensive Zod schemas for runtime API type validation
 * These schemas ensure type safety and provide validation for all API responses
 */

// Base types
export const UserRoleSchema = z.enum(['USER', 'ORGANIZATION_ADMIN', 'ADMIN'])
export const OrganizationPlanSchema = z.enum(['FREE', 'PRO', 'ENTERPRISE'])
export const PaymentStatusSchema = z.enum([
  'PENDING',
  'COMPLETED',
  'FAILED',
  'REFUNDED',
])
export const SubscriptionStatusSchema = z.enum([
  'ACTIVE',
  'PAUSED',
  'CANCELED',
  'PAST_DUE',
])

// User related schemas
export const UserSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  firstName: z.string().optional(),
  lastName: z.string().optional(),
  role: UserRoleSchema,
  emailVerified: z.boolean(),
  organizationId: z.string().uuid().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  lastLoginAt: z.string().datetime().optional(),
})

export const OrganizationSchema = z.object({
  id: z.string().uuid(),
  name: z.string().min(1),
  plan: OrganizationPlanSchema,
  status: z.enum(['ACTIVE', 'SUSPENDED', 'DELETED']),
  billingEmail: z.string().email().optional(),
  maxUsers: z.number().int().positive(),
  currentUsers: z.number().int().nonnegative(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

// Authentication schemas
export const OAuth2ProviderSchema = z.object({
  name: z.string(),
  displayName: z.string(),
  iconUrl: z.string().url().optional(),
  authUrl: z.string().url(),
})

export const SessionInfoSchema = z.object({
  user: UserSchema,
  session: z.object({
    activeTokens: z.number().int().nonnegative(),
    lastActiveAt: z.string().datetime(),
    createdAt: z.string().datetime(),
  }),
})

export const AuthMethodsResponseSchema = z.object({
  methods: z.array(z.string()),
  passwordAuthEnabled: z.boolean(),
  oauth2Providers: z.array(z.string()),
})

export const LoginResponseSchema = z.object({
  user: UserSchema,
  token: z.string().min(1),
})

export const AuthUrlResponseSchema = z.object({
  authUrl: z.string().url(),
})

// Request schemas
export const LoginRequestSchema = z.object({
  provider: z.string().min(1),
  redirectUri: z.string().url(),
})

export const PasswordLoginRequestSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  organizationId: z.string().uuid(),
})

export const PasswordRegisterRequestSchema = z.object({
  email: z.string().email(),
  password: z
    .string()
    .min(12)
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
      'Password must contain uppercase, lowercase, digit, and special character'
    ),
  name: z.string().min(1),
})

export const CallbackRequestSchema = z.object({
  code: z.string().min(1),
  state: z.string().optional(),
})

// Payment schemas
export const PaymentMethodSchema = z.object({
  id: z.string(),
  type: z.enum(['CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET']),
  last4: z.string().length(4).optional(),
  brand: z.string().optional(),
  expiryMonth: z.number().int().min(1).max(12).optional(),
  expiryYear: z.number().int().min(2024).optional(),
  isDefault: z.boolean(),
  createdAt: z.string().datetime(),
})

export const PaymentSchema = z.object({
  id: z.string().uuid(),
  amount: z.number().positive(),
  currency: z.string().length(3),
  status: PaymentStatusSchema,
  paymentMethodId: z.string(),
  customerId: z.string(),
  organizationId: z.string().uuid(),
  subscriptionId: z.string().uuid().optional(),
  description: z.string().optional(),
  metadata: z.record(z.string(), z.string()).optional(),
  stripePaymentIntentId: z.string().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  paidAt: z.string().datetime().optional(),
})

export const RefundSchema = z.object({
  id: z.string().uuid(),
  paymentId: z.string().uuid(),
  amount: z.number().positive(),
  currency: z.string().length(3),
  reason: z.enum([
    'REQUESTED_BY_CUSTOMER',
    'DUPLICATE',
    'FRAUDULENT',
    'SUBSCRIPTION_CANCELED',
  ]),
  status: z.enum(['PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED']),
  createdAt: z.string().datetime(),
  processedAt: z.string().datetime().optional(),
})

// Subscription schemas
export const PlanSchema = z.object({
  id: z.string().uuid(),
  name: z.string().min(1),
  description: z.string().optional(),
  amount: z.number().nonnegative(),
  currency: z.string().length(3),
  interval: z.enum(['MONTHLY', 'YEARLY']),
  features: z.array(z.string()),
  maxUsers: z.number().int().positive(),
  isActive: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export const SubscriptionSchema = z.object({
  id: z.string().uuid(),
  organizationId: z.string().uuid(),
  planId: z.string().uuid(),
  status: SubscriptionStatusSchema,
  currentPeriodStart: z.string().datetime(),
  currentPeriodEnd: z.string().datetime(),
  cancelAtPeriodEnd: z.boolean(),
  canceledAt: z.string().datetime().optional(),
  trialStart: z.string().datetime().optional(),
  trialEnd: z.string().datetime().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

export const InvoiceSchema = z.object({
  id: z.string().uuid(),
  subscriptionId: z.string().uuid(),
  organizationId: z.string().uuid(),
  amount: z.number().positive(),
  currency: z.string().length(3),
  status: z.enum(['DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE']),
  dueDate: z.string().datetime(),
  paidAt: z.string().datetime().optional(),
  invoiceNumber: z.string(),
  downloadUrl: z.string().url().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
})

// API Response wrappers
export const ApiSuccessResponseSchema = <T extends z.ZodTypeAny>(
  dataSchema: T
) =>
  z.object({
    success: z.literal(true),
    data: dataSchema,
    message: z.string().optional(),
    timestamp: z.string().datetime(),
  })

export const ApiErrorResponseSchema = z.object({
  success: z.literal(false),
  error: z.object({
    code: z.string(),
    message: z.string(),
    details: z
      .record(z.string(), z.union([z.string(), z.number(), z.boolean()]))
      .optional(),
  }),
  correlationId: z.string().uuid().optional(),
  timestamp: z.string().datetime(),
})

export const PaginatedResponseSchema = <T extends z.ZodTypeAny>(
  itemSchema: T
) =>
  ApiSuccessResponseSchema(
    z.object({
      items: z.array(itemSchema),
      pagination: z.object({
        page: z.number().int().nonnegative(),
        size: z.number().int().positive(),
        totalElements: z.number().int().nonnegative(),
        totalPages: z.number().int().nonnegative(),
        hasNext: z.boolean(),
        hasPrevious: z.boolean(),
      }),
    })
  )

// Audit and compliance schemas
export const AuditEventSchema = z.object({
  id: z.string().uuid(),
  eventType: z.string(),
  entityId: z.string().uuid(),
  entityType: z.string(),
  userId: z.string().uuid().optional(),
  organizationId: z.string().uuid().optional(),
  action: z.string(),
  details: z.record(z.string(), z.union([z.string(), z.number(), z.boolean()])),
  // Zod v4 does not include a built-in .ip() validator. Use a conservative IPv4/IPv6 regex.
  // This keeps validation strict without adding extra deps.
  ipAddress: z
    .string()
    .regex(
      // Basic IPv4 or IPv6 pattern (not exhaustive but reasonable for client-side validation)
      /^(?:(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}|\[?[A-Fa-f0-9:]+\]?)$/,
      'Invalid IP address'
    )
    .optional(),
  userAgent: z.string().optional(),
  correlationId: z.string().uuid().optional(),
  timestamp: z.string().datetime(),
})

/**
 * Partial organization types for different contexts
 * Avoid type mismatches when different components expect different subsets of organization properties
 */

export type OrganizationBasic = Omit<
  z.infer<typeof OrganizationSchema>,
  'plan' | 'status' | 'maxUsers' | 'currentUsers' | 'billingEmail'
> & {
  slug: string // Organizations page expects slug for routing
}
export const OrganizationBasicSchema = OrganizationSchema.omit({
  plan: true,
  status: true,
  maxUsers: true,
  currentUsers: true,
  billingEmail: true,
}).extend({
  slug: z.string(),
})

export type OrganizationFull = z.infer<typeof OrganizationSchema>

// Keep Organization as alias for OrganizationFull for backward compatibility
export type Organization = OrganizationFull
export type User = z.infer<typeof UserSchema>
export type OAuth2Provider = z.infer<typeof OAuth2ProviderSchema>
export type SessionInfo = z.infer<typeof SessionInfoSchema>
export type AuthMethodsResponse = z.infer<typeof AuthMethodsResponseSchema>
export type LoginResponse = z.infer<typeof LoginResponseSchema>
export type AuthUrlResponse = z.infer<typeof AuthUrlResponseSchema>

export type LoginRequest = z.infer<typeof LoginRequestSchema>
export type PasswordLoginRequest = z.infer<typeof PasswordLoginRequestSchema>
export type PasswordRegisterRequest = z.infer<
  typeof PasswordRegisterRequestSchema
>
export type CallbackRequest = z.infer<typeof CallbackRequestSchema>

export type PaymentMethod = z.infer<typeof PaymentMethodSchema>
export type Payment = z.infer<typeof PaymentSchema>
export type Refund = z.infer<typeof RefundSchema>

export type Plan = z.infer<typeof PlanSchema>
export type Subscription = z.infer<typeof SubscriptionSchema>
export type Invoice = z.infer<typeof InvoiceSchema>

export type AuditEvent = z.infer<typeof AuditEventSchema>

export interface ApiSuccessResponse<T> {
  success: true
  data: T
  message?: string
  timestamp: string
}

export type ApiErrorResponse = z.infer<typeof ApiErrorResponseSchema>

export type PaginatedResponse<T> = ApiSuccessResponse<{
  items: T[]
  pagination: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    hasNext: boolean
    hasPrevious: boolean
  }
}>
