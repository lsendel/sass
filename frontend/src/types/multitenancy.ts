/**
 * Multi-Tenancy Type Definitions
 *
 * Comprehensive type system for multi-tenant architecture supporting:
 * - Tenant isolation and data segregation
 * - Hierarchical tenant relationships
 * - Tenant-aware routing and middleware
 * - Cross-tenant analytics and reporting
 * - Resource quotas and billing per tenant
 */

import { z } from 'zod'

// Tenant identification and branding
export type TenantId = string & { readonly __brand: unique symbol }
export type TenantSlug = string & { readonly __brand: unique symbol }

export const createTenantId = (id: string): TenantId => id as TenantId
export const createTenantSlug = (slug: string): TenantSlug => slug as TenantSlug

// Tenant isolation levels
export type TenantIsolationLevel = 'shared' | 'dedicated' | 'hybrid'

// Tenant subscription tiers
export type TenantTier = 'starter' | 'professional' | 'enterprise' | 'white_label'

// Tenant status types
export type TenantStatus = 'active' | 'suspended' | 'trial' | 'pending_setup' | 'archived'

// Core tenant interface
export interface Tenant {
  id: TenantId
  slug: TenantSlug
  name: string
  displayName: string
  description?: string
  domain?: string
  customDomain?: string
  status: TenantStatus
  tier: TenantTier
  isolationLevel: TenantIsolationLevel

  // Tenant hierarchy
  parentTenantId?: TenantId
  isRootTenant: boolean

  // Branding and customization
  branding: TenantBranding

  // Billing and quotas
  billing: TenantBilling
  quotas: TenantQuotas

  // Compliance and security
  compliance: TenantCompliance

  // Metadata
  createdAt: string
  updatedAt: string
  createdBy: string
  lastActiveAt?: string

  // Feature flags
  features: TenantFeatureFlags

  // Regional settings
  region: string
  timezone: string
  locale: string
}

// Tenant branding configuration
export interface TenantBranding {
  primaryColor: string
  secondaryColor: string
  logoUrl?: string
  faviconUrl?: string
  customCss?: string

  // White-label settings
  hideProviderBranding: boolean
  customFooter?: string
  customEmailTemplate?: string

  // Theme configuration
  theme: 'light' | 'dark' | 'auto'
  fontFamily?: string
}

// Tenant billing configuration
export interface TenantBilling {
  currency: string
  billingCycle: 'monthly' | 'annual'
  billingContact: TenantContact
  paymentMethodId?: string

  // Pricing model
  pricingModel: 'fixed' | 'usage_based' | 'per_seat' | 'tiered'
  basePrice: number
  usageRates: Record<string, number>

  // Billing status
  billingStatus: 'current' | 'past_due' | 'suspended' | 'cancelled'
  nextBillingDate?: string
  currentBalance: number

  // Invoice preferences
  invoiceEmail: string
  invoiceFrequency: 'immediate' | 'daily' | 'weekly' | 'monthly'
  autoPayEnabled: boolean
}

// Tenant resource quotas
export interface TenantQuotas {
  users: QuotaLimit
  organizations: QuotaLimit
  storage: QuotaLimit // in GB
  apiCalls: QuotaLimit // per month
  bandwidth: QuotaLimit // in GB per month

  // Feature-specific limits
  customDomains: QuotaLimit
  ssoConnections: QuotaLimit
  webhooks: QuotaLimit
  auditRetention: number // days

  // Performance limits
  concurrentSessions: QuotaLimit
  rateLimit: number // requests per minute
}

interface QuotaLimit {
  current: number
  limit: number // -1 for unlimited
  softLimit?: number // warning threshold
}

// Tenant compliance settings
export interface TenantCompliance {
  gdprEnabled: boolean
  dataResidency: string[] // allowed regions
  auditLevel: 'basic' | 'standard' | 'comprehensive'
  dataRetentionDays: number

  // Security requirements
  requireMFA: boolean
  requireSSO: boolean
  allowedAuthMethods: string[]
  sessionTimeoutMinutes: number

  // Compliance certifications
  certifications: string[] // SOC2, HIPAA, etc.
  complianceContact?: TenantContact
}

// Tenant feature flags
export interface TenantFeatureFlags {
  // Authentication features
  mfaRequired: boolean
  ssoEnabled: boolean
  socialLogin: boolean

  // Payment features
  advancedBilling: boolean
  multiCurrency: boolean
  invoiceCustomization: boolean

  // Analytics features
  advancedAnalytics: boolean
  customReports: boolean
  dataExport: boolean

  // Integration features
  webhooks: boolean
  apiAccess: boolean
  zapierIntegration: boolean

  // White-label features
  customDomain: boolean
  brandingCustomization: boolean
  customEmailTemplates: boolean
}

// Tenant contact information
export interface TenantContact {
  name: string
  email: string
  phone?: string
  role?: string
}

// Tenant settings for different contexts
export interface TenantSettings {
  tenantId: TenantId

  // General settings
  general: {
    allowUserRegistration: boolean
    defaultUserRole: string
    emailVerificationRequired: boolean
    userInviteExpirationDays: number
  }

  // Security settings
  security: {
    passwordPolicy: PasswordPolicy
    sessionSettings: SessionSettings
    auditSettings: AuditSettings
  }

  // Integration settings
  integrations: {
    stripe: StripeSettings
    oauth: OAuthSettings
    smtp: SMTPSettings
  }

  // Notification settings
  notifications: {
    emailNotifications: EmailNotificationSettings
    webhookNotifications: WebhookNotificationSettings
  }
}

interface PasswordPolicy {
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireNumbers: boolean
  requireSpecialChars: boolean
  preventReuse: number
  expirationDays?: number
}

interface SessionSettings {
  timeoutMinutes: number
  maxConcurrentSessions: number
  requireReauthForSensitive: boolean
}

interface AuditSettings {
  enabled: boolean
  retentionDays: number
  logLevel: 'basic' | 'detailed' | 'comprehensive'
  realTimeAlerts: boolean
}

interface StripeSettings {
  publishableKey: string
  webhookSecret: string
  enabledPaymentMethods: string[]
  currency: string
}

interface OAuthSettings {
  providers: OAuthProvider[]
  allowedDomains?: string[]
  autoProvision: boolean
}

interface OAuthProvider {
  name: string
  clientId: string
  enabled: boolean
  scopes: string[]
}

interface SMTPSettings {
  host: string
  port: number
  username: string
  fromAddress: string
  encryption: 'none' | 'tls' | 'ssl'
}

interface EmailNotificationSettings {
  welcomeEmails: boolean
  billingNotifications: boolean
  securityAlerts: boolean
  systemUpdates: boolean
}

interface WebhookNotificationSettings {
  endpoints: WebhookEndpoint[]
  retryPolicy: WebhookRetryPolicy
}

interface WebhookEndpoint {
  url: string
  events: string[]
  active: boolean
  secret: string
}

interface WebhookRetryPolicy {
  maxRetries: number
  retryDelayMs: number
  backoffMultiplier: number
}

// Tenant context for requests
export interface TenantContext {
  tenantId: TenantId
  tenantSlug: TenantSlug
  tier: TenantTier
  features: TenantFeatureFlags
  quotas: TenantQuotas
  userId?: string
  organizationId?: string
}

// Multi-tenant routing
export interface TenantRoute {
  path: string
  tenantId: TenantId
  isCustomDomain: boolean
  subdomain?: string
}

// Tenant analytics
export interface TenantAnalytics {
  tenantId: TenantId
  period: AnalyticsPeriod

  // Usage metrics
  usage: {
    activeUsers: number
    totalSessions: number
    apiCalls: number
    storageUsed: number
    bandwidthUsed: number
  }

  // Business metrics
  business: {
    revenue: number
    subscriptions: number
    churnRate: number
    growthRate: number
  }

  // Performance metrics
  performance: {
    avgResponseTime: number
    errorRate: number
    uptime: number
  }

  // Security metrics
  security: {
    failedLogins: number
    suspiciousActivity: number
    mfaAdoption: number
  }
}

export type AnalyticsPeriod = 'day' | 'week' | 'month' | 'quarter' | 'year'

// Cross-tenant operations
export interface CrossTenantQuery {
  tenantIds: TenantId[]
  aggregation: 'sum' | 'avg' | 'count' | 'max' | 'min'
  groupBy?: string[]
  filters?: Record<string, string | number | boolean | string[]>
}

export interface CrossTenantResult {
  tenantId: TenantId
  data: Record<string, unknown>
  metadata: {
    executionTime: number
    recordCount: number
    hasMore: boolean
  }
}

// Validation schemas
export const TenantBrandingSchema = z.object({
  primaryColor: z.string().regex(/^#[0-9A-F]{6}$/i),
  secondaryColor: z.string().regex(/^#[0-9A-F]{6}$/i),
  logoUrl: z.string().url().optional(),
  faviconUrl: z.string().url().optional(),
  customCss: z.string().optional(),
  hideProviderBranding: z.boolean(),
  customFooter: z.string().optional(),
  customEmailTemplate: z.string().optional(),
  theme: z.enum(['light', 'dark', 'auto']),
  fontFamily: z.string().optional(),
})

export const TenantQuotasSchema = z.object({
  users: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  organizations: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  storage: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  apiCalls: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  bandwidth: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  customDomains: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  ssoConnections: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  webhooks: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  auditRetention: z.number().min(1),
  concurrentSessions: z.object({
    current: z.number().min(0),
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  rateLimit: z.number().min(1),
})

export const TenantSchema = z.object({
  id: z.string(),
  slug: z.string().min(2).max(50).regex(/^[a-z0-9-]+$/),
  name: z.string().min(1).max(100),
  displayName: z.string().min(1).max(100),
  description: z.string().optional(),
  domain: z.string().optional(),
  customDomain: z.string().optional(),
  status: z.enum(['active', 'suspended', 'trial', 'pending_setup', 'archived']),
  tier: z.enum(['starter', 'professional', 'enterprise', 'white_label']),
  isolationLevel: z.enum(['shared', 'dedicated', 'hybrid']),
  parentTenantId: z.string().optional(),
  isRootTenant: z.boolean(),
  branding: TenantBrandingSchema,
  quotas: TenantQuotasSchema,
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  createdBy: z.string(),
  lastActiveAt: z.string().datetime().optional(),
  region: z.string(),
  timezone: z.string(),
  locale: z.string(),
})

// Request/Response schemas
export const CreateTenantRequestSchema = z.object({
  slug: z.string().min(2).max(50).regex(/^[a-z0-9-]+$/),
  name: z.string().min(1).max(100),
  displayName: z.string().min(1).max(100),
  description: z.string().optional(),
  tier: z.enum(['starter', 'professional', 'enterprise', 'white_label']),
  parentTenantId: z.string().optional(),
  region: z.string().default('us-east-1'),
  timezone: z.string().default('UTC'),
  locale: z.string().default('en-US'),
  branding: TenantBrandingSchema.partial().optional(),
})

export const UpdateTenantRequestSchema = z.object({
  name: z.string().min(1).max(100).optional(),
  displayName: z.string().min(1).max(100).optional(),
  description: z.string().optional(),
  status: z.enum(['active', 'suspended', 'trial', 'pending_setup', 'archived']).optional(),
  branding: TenantBrandingSchema.partial().optional(),
  tier: z.enum(['starter', 'professional', 'enterprise', 'white_label']).optional(),
})

export const TenantAnalyticsRequestSchema = z.object({
  tenantIds: z.array(z.string()).optional(),
  period: z.enum(['day', 'week', 'month', 'quarter', 'year']),
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional(),
  metrics: z.array(z.string()).optional(),
})

// Type inference helpers
export type CreateTenantRequest = z.infer<typeof CreateTenantRequestSchema>
export type UpdateTenantRequest = z.infer<typeof UpdateTenantRequestSchema>
export type TenantAnalyticsRequest = z.infer<typeof TenantAnalyticsRequestSchema>

// Utility types
export interface TenantListResponse {
  tenants: Tenant[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
}

export interface TenantCreationResponse {
  tenant: Tenant
  setupUrl: string
  adminCredentials?: {
    username: string
    temporaryPassword: string
  }
}

// Multi-tenant aware component props
export interface WithTenantProps {
  tenantContext?: TenantContext
}
