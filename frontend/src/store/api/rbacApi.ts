/**
 * RBAC API Service
 *
 * RTK Query API service for Role-Based Access Control operations:
 * - Role and permission management
 * - User role assignments
 * - Permission evaluation
 * - Audit trails and compliance
 * - Access reviews and approval workflows
 */

import { createApi } from '@reduxjs/toolkit/query/react'
import { z } from 'zod'

import { createValidatedBaseQuery, createValidatedEndpoint, wrapSuccessResponse } from '@/lib/api/validation'
import {
  Role,
  Permission,
  UserRoleAssignment,
  RoleTemplate,
  PermissionGroup,
  RBACAnalytics,
  RBACauditEvent,
  PermissionEvaluationResult,
  PermissionRequest,
  AccessReviewCampaign,
  RBACConfiguration,
  CreateRoleForm,
  AssignRoleForm,
  PermissionRequestForm,
  PermissionContext,
  UserWithRoles,
} from '@/types/rbac'

// Validation schemas
const PermissionSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string(),
  resource: z.enum(['organization', 'user', 'payment', 'subscription', 'billing', 'analytics', 'audit', 'settings', 'mfa', 'api_keys', 'webhooks', 'integrations']),
  action: z.enum(['create', 'read', 'update', 'delete', 'manage', 'approve', 'export', 'invite', 'revoke']),
  conditions: z.array(z.object({
    field: z.string(),
    operator: z.enum(['equals', 'not_equals', 'in', 'not_in', 'greater_than', 'less_than', 'contains']),
    value: z.union([z.string(), z.array(z.string()), z.number(), z.boolean()]),
    description: z.string().optional(),
  })).optional(),
  scope: z.enum(['organization', 'system', 'personal']),
  category: z.string(),
  isSystemPermission: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

const RoleSchema = z.object({
  id: z.string(),
  name: z.string(),
  description: z.string(),
  slug: z.string(),
  permissions: z.array(PermissionSchema),
  isSystemRole: z.boolean(),
  isCustomRole: z.boolean(),
  organizationId: z.string().optional(),
  hierarchy: z.number(),
  inheritsFrom: z.array(z.string()).optional(),
  color: z.string(),
  icon: z.string().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
  createdBy: z.string(),
  statistics: z.object({
    userCount: z.number(),
    lastAssigned: z.string().optional(),
  }),
})

const UserRoleAssignmentSchema = z.object({
  id: z.string(),
  userId: z.string(),
  roleId: z.string(),
  organizationId: z.string().optional(),
  assignedBy: z.string(),
  assignedAt: z.string(),
  expiresAt: z.string().optional(),
  conditions: z.array(z.object({
    type: z.enum(['time_based', 'ip_restricted', 'device_restricted', 'location_restricted']),
    parameters: z.record(z.any()),
    description: z.string(),
  })).optional(),
  isActive: z.boolean(),
  metadata: z.record(z.any()).optional(),
})

const CreateRoleSchema = z.object({
  name: z.string().min(1, 'Role name is required').max(50, 'Role name must be 50 characters or less'),
  description: z.string().max(200, 'Description must be 200 characters or less'),
  permissions: z.array(z.string()),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Color must be a valid hex color'),
  inheritsFrom: z.array(z.string()).optional(),
})

const AssignRoleSchema = z.object({
  userId: z.string(),
  roleIds: z.array(z.string()).min(1, 'At least one role must be assigned'),
  expiresAt: z.string().optional(),
  justification: z.string().optional(),
})

const PermissionRequestSchema = z.object({
  targetUserId: z.string().optional(),
  requestType: z.enum(['role_assignment', 'permission_grant', 'resource_access']),
  requestedRoles: z.array(z.string()).optional(),
  requestedPermissions: z.array(z.string()).optional(),
  justification: z.string().min(10, 'Justification must be at least 10 characters'),
  urgency: z.enum(['low', 'medium', 'high']),
})

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const rbacApi = createApi({
  reducerPath: 'rbacApi',
  baseQuery: createValidatedBaseQuery(`${API_BASE_URL}/rbac`),
  tagTypes: [
    'Role',
    'Permission',
    'UserRoleAssignment',
    'RoleTemplate',
    'PermissionGroup',
    'RBACAnalytics',
    'PermissionRequest',
    'AccessReview',
    'RBACConfiguration',
    'AuditEvent',
  ],
  endpoints: (builder) => ({

    // ==== ROLES MANAGEMENT ====

    // Get all roles for organization
    getRoles: builder.query<Role[], { organizationId?: string; includeSystem?: boolean }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(RoleSchema)), {
        query: ({ organizationId, includeSystem = true }) => ({
          url: '/roles',
          params: { organizationId, includeSystem },
        }),
      }),
      providesTags: ['Role'],
    }),

    // Get specific role by ID
    getRole: builder.query<Role, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(RoleSchema), {
        query: (roleId) => `/roles/${roleId}`,
      }),
      providesTags: (result, error, roleId) => [{ type: 'Role', id: roleId }],
    }),

    // Create new role
    createRole: builder.mutation<Role, CreateRoleForm & { organizationId: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(RoleSchema), {
        query: (roleData) => {
          CreateRoleSchema.parse(roleData)
          return {
            url: '/roles',
            method: 'POST',
            body: roleData,
          }
        },
      }),
      invalidatesTags: ['Role', 'RBACAnalytics'],
    }),

    // Update existing role
    updateRole: builder.mutation<Role, { roleId: string; updates: Partial<CreateRoleForm> }>({
      ...createValidatedEndpoint(wrapSuccessResponse(RoleSchema), {
        query: ({ roleId, updates }) => ({
          url: `/roles/${roleId}`,
          method: 'PATCH',
          body: updates,
        }),
      }),
      invalidatesTags: (result, error, { roleId }) => [
        { type: 'Role', id: roleId },
        'Role',
        'UserRoleAssignment',
        'RBACAnalytics',
      ],
    }),

    // Delete role
    deleteRole: builder.mutation<{ success: boolean }, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({ success: z.boolean() })), {
        query: (roleId) => ({
          url: `/roles/${roleId}`,
          method: 'DELETE',
        }),
      }),
      invalidatesTags: ['Role', 'UserRoleAssignment', 'RBACAnalytics'],
    }),

    // ==== PERMISSIONS MANAGEMENT ====

    // Get all permissions
    getPermissions: builder.query<Permission[], { category?: string; resource?: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(PermissionSchema)), {
        query: ({ category, resource }) => ({
          url: '/permissions',
          params: { category, resource },
        }),
      }),
      providesTags: ['Permission'],
    }),

    // Get permission groups for UI organization
    getPermissionGroups: builder.query<PermissionGroup[], void>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        name: z.string(),
        description: z.string(),
        category: z.string(),
        permissions: z.array(PermissionSchema),
        icon: z.string().optional(),
        order: z.number(),
      }))), {
        query: () => '/permissions/groups',
      }),
      providesTags: ['PermissionGroup'],
    }),

    // ==== USER ROLE ASSIGNMENTS ====

    // Get user's role assignments
    getUserRoleAssignments: builder.query<UserRoleAssignment[], string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(UserRoleAssignmentSchema)), {
        query: (userId) => `/users/${userId}/role-assignments`,
      }),
      providesTags: (result, error, userId) => [
        { type: 'UserRoleAssignment', id: userId },
      ],
    }),

    // Get organization's role assignments
    getOrganizationRoleAssignments: builder.query<UserRoleAssignment[], string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(UserRoleAssignmentSchema)), {
        query: (organizationId) => `/organizations/${organizationId}/role-assignments`,
      }),
      providesTags: ['UserRoleAssignment'],
    }),

    // Get users with their roles (for organization overview)
    getUsersWithRoles: builder.query<UserWithRoles[], string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        name: z.string(),
        email: z.string(),
        assignments: z.array(UserRoleAssignmentSchema),
        effectiveRoles: z.array(RoleSchema),
        effectivePermissions: z.array(PermissionSchema),
      }))), {
        query: (organizationId) => `/organizations/${organizationId}/users-with-roles`,
      }),
      providesTags: ['UserRoleAssignment'],
    }),

    // Assign roles to user
    assignRoles: builder.mutation<UserRoleAssignment[], AssignRoleForm & { organizationId: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(UserRoleAssignmentSchema)), {
        query: (assignmentData) => {
          AssignRoleSchema.parse(assignmentData)
          return {
            url: '/role-assignments',
            method: 'POST',
            body: assignmentData,
          }
        },
      }),
      invalidatesTags: (result, error, { userId, organizationId }) => [
        { type: 'UserRoleAssignment', id: userId },
        'UserRoleAssignment',
        'RBACAnalytics',
      ],
    }),

    // Revoke role assignment
    revokeRoleAssignment: builder.mutation<{ success: boolean }, { assignmentId: string; reason?: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({ success: z.boolean() })), {
        query: ({ assignmentId, reason }) => ({
          url: `/role-assignments/${assignmentId}`,
          method: 'DELETE',
          body: { reason },
        }),
      }),
      invalidatesTags: ['UserRoleAssignment', 'RBACAnalytics'],
    }),

    // ==== PERMISSION EVALUATION ====

    // Check if user has permission for specific action
    checkPermission: builder.query<PermissionEvaluationResult, PermissionContext>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        allowed: z.boolean(),
        reason: z.string(),
        matchedPermissions: z.array(PermissionSchema),
        deniedBy: z.array(PermissionSchema).optional(),
        conditions: z.array(z.object({
          field: z.string(),
          operator: z.enum(['equals', 'not_equals', 'in', 'not_in', 'greater_than', 'less_than', 'contains']),
          value: z.union([z.string(), z.array(z.string()), z.number(), z.boolean()]),
          description: z.string().optional(),
        })).optional(),
        requiresApproval: z.boolean().optional(),
      })), {
        query: (context) => ({
          url: '/permissions/check',
          method: 'POST',
          body: context,
        }),
      }),
    }),

    // Get user's effective permissions (all permissions from all roles)
    getUserEffectivePermissions: builder.query<Permission[], { userId: string; organizationId?: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(PermissionSchema)), {
        query: ({ userId, organizationId }) => ({
          url: `/users/${userId}/effective-permissions`,
          params: { organizationId },
        }),
      }),
    }),

    // ==== ROLE TEMPLATES ====

    // Get available role templates
    getRoleTemplates: builder.query<RoleTemplate[], { category?: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        name: z.string(),
        description: z.string(),
        category: z.enum(['management', 'financial', 'technical', 'support', 'custom']),
        permissions: z.array(z.string()),
        recommendedFor: z.array(z.string()),
        tags: z.array(z.string()),
        isPopular: z.boolean(),
      }))), {
        query: ({ category }) => ({
          url: '/role-templates',
          params: { category },
        }),
      }),
      providesTags: ['RoleTemplate'],
    }),

    // Create role from template
    createRoleFromTemplate: builder.mutation<Role, {
      templateId: string;
      organizationId: string;
      customizations?: Partial<CreateRoleForm>;
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(RoleSchema), {
        query: ({ templateId, organizationId, customizations }) => ({
          url: `/role-templates/${templateId}/create-role`,
          method: 'POST',
          body: { organizationId, customizations },
        }),
      }),
      invalidatesTags: ['Role', 'RBACAnalytics'],
    }),

    // ==== PERMISSION REQUESTS & APPROVAL ====

    // Get permission requests (for approval)
    getPermissionRequests: builder.query<PermissionRequest[], {
      status?: 'pending' | 'approved' | 'denied' | 'expired';
      organizationId: string;
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        requesterId: z.string(),
        requesterName: z.string(),
        targetUserId: z.string().optional(),
        targetUserName: z.string().optional(),
        organizationId: z.string(),
        requestType: z.enum(['role_assignment', 'permission_grant', 'resource_access']),
        requestedRoles: z.array(z.string()).optional(),
        requestedPermissions: z.array(z.string()).optional(),
        resourceDetails: z.object({
          type: z.enum(['organization', 'user', 'payment', 'subscription', 'billing', 'analytics', 'audit', 'settings', 'mfa', 'api_keys', 'webhooks', 'integrations']),
          id: z.string(),
          actions: z.array(z.enum(['create', 'read', 'update', 'delete', 'manage', 'approve', 'export', 'invite', 'revoke'])),
        }).optional(),
        justification: z.string(),
        urgency: z.enum(['low', 'medium', 'high']),
        status: z.enum(['pending', 'approved', 'denied', 'expired']),
        reviewers: z.array(z.object({
          userId: z.string(),
          userName: z.string(),
          status: z.enum(['pending', 'approved', 'denied']),
          comment: z.string().optional(),
          reviewedAt: z.string().optional(),
        })),
        createdAt: z.string(),
        expiresAt: z.string(),
        approvedAt: z.string().optional(),
        deniedAt: z.string().optional(),
        metadata: z.record(z.any()).optional(),
      }))), {
        query: ({ status, organizationId }) => ({
          url: '/permission-requests',
          params: { status, organizationId },
        }),
      }),
      providesTags: ['PermissionRequest'],
    }),

    // Create permission request
    createPermissionRequest: builder.mutation<PermissionRequest, PermissionRequestForm & { organizationId: string }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        id: z.string(),
        requesterId: z.string(),
        requesterName: z.string(),
        organizationId: z.string(),
        requestType: z.enum(['role_assignment', 'permission_grant', 'resource_access']),
        status: z.enum(['pending', 'approved', 'denied', 'expired']),
        createdAt: z.string(),
        expiresAt: z.string(),
      })), {
        query: (requestData) => {
          PermissionRequestSchema.parse(requestData)
          return {
            url: '/permission-requests',
            method: 'POST',
            body: requestData,
          }
        },
      }),
      invalidatesTags: ['PermissionRequest'],
    }),

    // Review permission request (approve/deny)
    reviewPermissionRequest: builder.mutation<PermissionRequest, {
      requestId: string;
      action: 'approve' | 'deny';
      comment?: string;
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        id: z.string(),
        status: z.enum(['pending', 'approved', 'denied', 'expired']),
        reviewedAt: z.string(),
      })), {
        query: ({ requestId, action, comment }) => ({
          url: `/permission-requests/${requestId}/review`,
          method: 'POST',
          body: { action, comment },
        }),
      }),
      invalidatesTags: ['PermissionRequest', 'UserRoleAssignment'],
    }),

    // ==== ANALYTICS & REPORTING ====

    // Get RBAC analytics
    getRBACAnalytics: builder.query<RBACAnalytics, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        totalRoles: z.number(),
        totalPermissions: z.number(),
        totalAssignments: z.number(),
        roleDistribution: z.record(z.number()),
        permissionUsage: z.record(z.number()),
        riskScore: z.number(),
        recommendations: z.array(z.object({
          type: z.enum(['security', 'efficiency', 'compliance']),
          severity: z.enum(['low', 'medium', 'high', 'critical']),
          title: z.string(),
          description: z.string(),
          action: z.string(),
          impact: z.string(),
        })),
        complianceStatus: z.object({
          soxCompliant: z.boolean(),
          gdprCompliant: z.boolean(),
          iso27001Compliant: z.boolean(),
          pciCompliant: z.boolean(),
          customCompliance: z.record(z.boolean()),
          lastAuditDate: z.string().optional(),
          nextAuditDate: z.string().optional(),
        }),
      })), {
        query: (organizationId) => `/organizations/${organizationId}/analytics`,
      }),
      providesTags: ['RBACAnalytics'],
    }),

    // Get RBAC audit events
    getRBACAuditLog: builder.query<RBACauditEvent[], {
      organizationId: string;
      eventType?: string;
      limit?: number;
      offset?: number;
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(z.object({
        id: z.string(),
        eventType: z.enum(['role_created', 'role_updated', 'role_deleted', 'permission_granted', 'permission_revoked', 'assignment_created', 'assignment_expired']),
        entityType: z.enum(['role', 'permission', 'assignment']),
        entityId: z.string(),
        actorId: z.string(),
        actorName: z.string(),
        targetUserId: z.string().optional(),
        targetUserName: z.string().optional(),
        organizationId: z.string().optional(),
        changes: z.record(z.object({
          from: z.any(),
          to: z.any(),
        })),
        reason: z.string().optional(),
        ipAddress: z.string(),
        userAgent: z.string(),
        timestamp: z.string(),
        metadata: z.record(z.any()).optional(),
      }))), {
        query: ({ organizationId, eventType, limit = 50, offset = 0 }) => ({
          url: `/organizations/${organizationId}/audit`,
          params: { eventType, limit, offset },
        }),
      }),
      providesTags: ['AuditEvent'],
    }),

    // ==== CONFIGURATION ====

    // Get RBAC configuration
    getRBACConfiguration: builder.query<RBACConfiguration, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        organizationId: z.string(),
        settings: z.object({
          enableRoleInheritance: z.boolean(),
          enableDynamicRoles: z.boolean(),
          enableApprovalWorkflows: z.boolean(),
          defaultRoleForNewUsers: z.string().optional(),
          maxRolesPerUser: z.number(),
          roleAssignmentRequiresApproval: z.boolean(),
          sensitivePermissionsRequireApproval: z.array(z.string()),
          auditRetentionDays: z.number(),
          enableResourceOwnership: z.boolean(),
        }),
        complianceSettings: z.object({
          enableSoxCompliance: z.boolean(),
          enableGdprCompliance: z.boolean(),
          enableAuditLogging: z.boolean(),
          requireJustificationForRoleChanges: z.boolean(),
          enablePeriodicAccessReview: z.boolean(),
          accessReviewFrequencyDays: z.number(),
        }),
        notifications: z.object({
          notifyOnRoleChanges: z.boolean(),
          notifyOnPermissionChanges: z.boolean(),
          notifyOnAccessRequests: z.boolean(),
          notifyOnComplianceViolations: z.boolean(),
        }),
      })), {
        query: (organizationId) => `/organizations/${organizationId}/configuration`,
      }),
      providesTags: ['RBACConfiguration'],
    }),

    // Update RBAC configuration
    updateRBACConfiguration: builder.mutation<RBACConfiguration, {
      organizationId: string;
      updates: Partial<RBACConfiguration>;
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        organizationId: z.string(),
        settings: z.record(z.any()),
        complianceSettings: z.record(z.any()),
        notifications: z.record(z.any()),
      })), {
        query: ({ organizationId, updates }) => ({
          url: `/organizations/${organizationId}/configuration`,
          method: 'PATCH',
          body: updates,
        }),
      }),
      invalidatesTags: ['RBACConfiguration'],
    }),

  }),
})

// Export hooks
export const {
  useGetRolesQuery,
  useGetRoleQuery,
  useCreateRoleMutation,
  useUpdateRoleMutation,
  useDeleteRoleMutation,
  useGetPermissionsQuery,
  useGetPermissionGroupsQuery,
  useGetUserRoleAssignmentsQuery,
  useGetOrganizationRoleAssignmentsQuery,
  useGetUsersWithRolesQuery,
  useAssignRolesMutation,
  useRevokeRoleAssignmentMutation,
  useCheckPermissionQuery,
  useGetUserEffectivePermissionsQuery,
  useGetRoleTemplatesQuery,
  useCreateRoleFromTemplateMutation,
  useGetPermissionRequestsQuery,
  useCreatePermissionRequestMutation,
  useReviewPermissionRequestMutation,
  useGetRBACAnalyticsQuery,
  useGetRBACAuditLogQuery,
  useGetRBACConfigurationQuery,
  useUpdateRBACConfigurationMutation,
} = rbacApi