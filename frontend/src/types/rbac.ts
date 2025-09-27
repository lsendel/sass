/**
 * Role-Based Access Control (RBAC) Types
 *
 * Comprehensive RBAC system for enterprise-grade authorization:
 * - Hierarchical roles and permissions
 * - Resource-based access control
 * - Dynamic permission evaluation
 * - Audit trails and compliance
 * - Organization-level and system-level permissions
 */

// Base permission and resource types
export type ResourceType =
  | 'organization'
  | 'user'
  | 'payment'
  | 'subscription'
  | 'billing'
  | 'analytics'
  | 'audit'
  | 'settings'
  | 'mfa'
  | 'api_keys'
  | 'webhooks'
  | 'integrations'

export type ActionType =
  | 'create'
  | 'read'
  | 'update'
  | 'delete'
  | 'manage'
  | 'approve'
  | 'export'
  | 'invite'
  | 'revoke'

// Permission structure
export interface Permission {
  id: string
  name: string
  description: string
  resource: ResourceType
  action: ActionType
  conditions?: PermissionCondition[]
  scope: 'organization' | 'system' | 'personal'
  category: string
  isSystemPermission: boolean
  createdAt: string
  updatedAt: string
}

// Dynamic permission conditions
export interface PermissionCondition {
  field: string
  operator: 'equals' | 'not_equals' | 'in' | 'not_in' | 'greater_than' | 'less_than' | 'contains'
  value: string | string[] | number | boolean
  description?: string
}

// Role structure
export interface Role {
  id: string
  name: string
  description: string
  slug: string
  permissions: Permission[]
  isSystemRole: boolean
  isCustomRole: boolean
  organizationId?: string
  hierarchy: number // Higher number = more privileged
  inheritsFrom?: string[] // Role inheritance
  color: string
  icon?: string
  createdAt: string
  updatedAt: string
  createdBy: string
  statistics: {
    userCount: number
    lastAssigned?: string
  }
}

// User role assignment
export interface UserRoleAssignment {
  id: string
  userId: string
  roleId: string
  organizationId?: string
  assignedBy: string
  assignedAt: string
  expiresAt?: string
  conditions?: RoleCondition[]
  isActive: boolean
  metadata?: Record<string, any>
}

// Role conditions (temporary assignments, time-based, etc.)
export interface RoleCondition {
  type: 'time_based' | 'ip_restricted' | 'device_restricted' | 'location_restricted'
  parameters: Record<string, any>
  description: string
}

// System-defined roles (cannot be modified)
export interface SystemRole extends Omit<Role, 'isSystemRole' | 'isCustomRole'> {
  isSystemRole: true
  isCustomRole: false
  systemRoleType: 'super_admin' | 'org_owner' | 'org_admin' | 'org_member' | 'billing_manager' | 'read_only'
}

// Permission evaluation context
export interface PermissionContext {
  userId: string
  organizationId?: string
  resource: ResourceType
  action: ActionType
  resourceId?: string
  additionalContext?: Record<string, any>
}

// Permission evaluation result
export interface PermissionEvaluationResult {
  allowed: boolean
  reason: string
  matchedPermissions: Permission[]
  deniedBy?: Permission[]
  conditions?: PermissionCondition[]
  requiresApproval?: boolean
  approvalWorkflow?: ApprovalWorkflow
}

// Approval workflow for sensitive actions
export interface ApprovalWorkflow {
  id: string
  name: string
  description: string
  requiredApprovals: number
  approvers: UserReference[]
  timeoutHours: number
  escalationRules: EscalationRule[]
  isActive: boolean
}

export interface EscalationRule {
  afterHours: number
  escalateTo: UserReference[]
  action: 'notify' | 'auto_approve' | 'auto_deny'
}

export interface UserReference {
  id: string
  name: string
  email: string
  role?: string
}

// Role templates for common use cases
export interface RoleTemplate {
  id: string
  name: string
  description: string
  category: 'management' | 'financial' | 'technical' | 'support' | 'custom'
  permissions: string[] // Permission IDs
  recommendedFor: string[]
  tags: string[]
  isPopular: boolean
}

// Permission groups for better organization
export interface PermissionGroup {
  id: string
  name: string
  description: string
  category: string
  permissions: Permission[]
  icon?: string
  order: number
}

// RBAC analytics and reporting
export interface RBACAnalytics {
  totalRoles: number
  totalPermissions: number
  totalAssignments: number
  roleDistribution: Record<string, number>
  permissionUsage: Record<string, number>
  riskScore: number
  recommendations: RBACRecommendation[]
  complianceStatus: ComplianceStatus
}

export interface RBACRecommendation {
  type: 'security' | 'efficiency' | 'compliance'
  severity: 'low' | 'medium' | 'high' | 'critical'
  title: string
  description: string
  action: string
  impact: string
}

export interface ComplianceStatus {
  soxCompliant: boolean
  gdprCompliant: boolean
  iso27001Compliant: boolean
  pciCompliant: boolean
  customCompliance: Record<string, boolean>
  lastAuditDate?: string
  nextAuditDate?: string
}

// Audit trail for RBAC changes
export interface RBACauditEvent {
  id: string
  eventType: 'role_created' | 'role_updated' | 'role_deleted' | 'permission_granted' | 'permission_revoked' | 'assignment_created' | 'assignment_expired'
  entityType: 'role' | 'permission' | 'assignment'
  entityId: string
  actorId: string
  actorName: string
  targetUserId?: string
  targetUserName?: string
  organizationId?: string
  changes: Record<string, { from: any; to: any }>
  reason?: string
  ipAddress: string
  userAgent: string
  timestamp: string
  metadata?: Record<string, any>
}

// Role hierarchy and inheritance
export interface RoleHierarchy {
  parentRoleId: string
  childRoleId: string
  inheritanceType: 'full' | 'partial'
  excludedPermissions?: string[]
  addedPermissions?: string[]
  conditions?: RoleCondition[]
}

// Resource ownership and delegation
export interface ResourceOwnership {
  resourceType: ResourceType
  resourceId: string
  ownerId: string
  ownerType: 'user' | 'role'
  delegatedUsers: UserReference[]
  permissions: Permission[]
  createdAt: string
  expiresAt?: string
}

// Dynamic role evaluation
export interface DynamicRoleRule {
  id: string
  name: string
  description: string
  conditions: RoleCondition[]
  assignedRoles: string[]
  isActive: boolean
  priority: number
  evaluationFrequency: 'realtime' | 'hourly' | 'daily'
  lastEvaluated?: string
}

// Permission request and approval system
export interface PermissionRequest {
  id: string
  requesterId: string
  requesterName: string
  targetUserId?: string
  targetUserName?: string
  organizationId: string
  requestType: 'role_assignment' | 'permission_grant' | 'resource_access'
  requestedRoles?: string[]
  requestedPermissions?: string[]
  resourceDetails?: {
    type: ResourceType
    id: string
    actions: ActionType[]
  }
  justification: string
  urgency: 'low' | 'medium' | 'high'
  status: 'pending' | 'approved' | 'denied' | 'expired'
  reviewers: PermissionReviewer[]
  createdAt: string
  expiresAt: string
  approvedAt?: string
  deniedAt?: string
  metadata?: Record<string, any>
}

export interface PermissionReviewer {
  userId: string
  userName: string
  status: 'pending' | 'approved' | 'denied'
  comment?: string
  reviewedAt?: string
}

// RBAC configuration and settings
export interface RBACConfiguration {
  organizationId: string
  settings: {
    enableRoleInheritance: boolean
    enableDynamicRoles: boolean
    enableApprovalWorkflows: boolean
    defaultRoleForNewUsers?: string
    maxRolesPerUser: number
    roleAssignmentRequiresApproval: boolean
    sensitivePermissionsRequireApproval: boolean[]
    auditRetentionDays: number
    enableResourceOwnership: boolean
  }
  complianceSettings: {
    enableSoxCompliance: boolean
    enableGdprCompliance: boolean
    enableAuditLogging: boolean
    requireJustificationForRoleChanges: boolean
    enablePeriodicAccessReview: boolean
    accessReviewFrequencyDays: number
  }
  notifications: {
    notifyOnRoleChanges: boolean
    notifyOnPermissionChanges: boolean
    notifyOnAccessRequests: boolean
    notifyOnComplianceViolations: boolean
  }
}

// Access review and certification
export interface AccessReviewCampaign {
  id: string
  name: string
  description: string
  organizationId: string
  reviewType: 'periodic' | 'targeted' | 'compliance'
  status: 'draft' | 'active' | 'completed' | 'cancelled'
  scope: {
    userIds?: string[]
    roleIds?: string[]
    departmentIds?: string[]
    includeSystemRoles: boolean
  }
  reviewers: UserReference[]
  startDate: string
  endDate: string
  completionPercentage: number
  findings: AccessReviewFinding[]
  createdBy: string
  createdAt: string
}

export interface AccessReviewFinding {
  id: string
  userId: string
  userName: string
  finding: 'appropriate' | 'excessive' | 'insufficient' | 'violated_policy'
  recommendedAction: 'keep' | 'modify' | 'revoke'
  reviewerComment?: string
  riskLevel: 'low' | 'medium' | 'high'
  reviewedBy: string
  reviewedAt: string
}

// Form and UI types
export interface CreateRoleForm {
  name: string
  description: string
  permissions: string[]
  color: string
  inheritsFrom?: string[]
}

export interface AssignRoleForm {
  userId: string
  roleIds: string[]
  expiresAt?: string
  justification?: string
}

export interface PermissionRequestForm {
  targetUserId?: string
  requestType: 'role_assignment' | 'permission_grant' | 'resource_access'
  requestedRoles?: string[]
  requestedPermissions?: string[]
  justification: string
  urgency: 'low' | 'medium' | 'high'
}

// React component props
export interface RoleManagerProps {
  organizationId: string
  onRoleUpdated: (role: Role) => void
}

export interface PermissionCheckerProps {
  resource: ResourceType
  action: ActionType
  children: React.ReactNode
  fallback?: React.ReactNode
  resourceId?: string
}

export interface RoleAssignmentProps {
  userId: string
  availableRoles: Role[]
  currentAssignments: UserRoleAssignment[]
  onAssignmentChange: (assignments: UserRoleAssignment[]) => void
}

// Utility types
export type RoleWithPermissions = Role & {
  effectivePermissions: Permission[]
  inheritedPermissions: Permission[]
}

export interface UserWithRoles {
  id: string
  name: string
  email: string
  assignments: UserRoleAssignment[]
  effectiveRoles: Role[]
  effectivePermissions: Permission[]
}

export type PermissionMatrix = Record<ResourceType, Record<ActionType, boolean>>

// Error types
export interface RBACError {
  code: string
  message: string
  details?: Record<string, any>
  suggestions?: string[]
}