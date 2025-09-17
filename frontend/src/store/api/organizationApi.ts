import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../index'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api/v1'

export type Organization = {
  id: string
  name: string
  slug: string
  ownerId: string
  settings: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export type OrganizationMemberRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export type OrganizationMember = {
  id: string
  userId: string
  organizationId: string
  role: OrganizationMemberRole
  joinedAt: string
}

export type OrganizationMemberInfo = {
  userId: string
  userEmail: string
  userName: string
  role: OrganizationMemberRole
  joinedAt: string
}

export type Invitation = {
  id: string
  organizationId: string
  invitedBy: string
  email: string
  role: OrganizationMemberRole
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED' | 'REVOKED'
  token: string
  expiresAt: string
  createdAt: string
}

export type CreateOrganizationRequest = {
  name: string
  slug: string
  settings?: Record<string, unknown>
}

export type UpdateOrganizationRequest = {
  name: string
  settings?: Record<string, unknown>
}

export type UpdateSettingsRequest = {
  settings: Record<string, unknown>
}

export type InviteUserRequest = {
  email: string
  role: OrganizationMemberRole
}

export type UpdateMemberRoleRequest = {
  role: OrganizationMemberRole
}

export const organizationApi = createApi({
  reducerPath: 'organizationApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}/organizations`,
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  tagTypes: ['Organization', 'OrganizationMember', 'Invitation'],
  endpoints: builder => ({
    createOrganization: builder.mutation<
      Organization,
      CreateOrganizationRequest
    >({
      query: body => ({
        url: '',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Organization'],
    }),

    getOrganization: builder.query<Organization, string>({
      query: organizationId => `/${organizationId}`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'Organization', id: organizationId },
      ],
    }),

    getOrganizationBySlug: builder.query<Organization, string>({
      query: slug => `/slug/${slug}`,
      providesTags: result =>
        result ? [{ type: 'Organization', id: result.id }] : [],
    }),

    getUserOrganizations: builder.query<Organization[], void>({
      query: () => '',
      providesTags: ['Organization'],
    }),

    updateOrganization: builder.mutation<
      Organization,
      {
        organizationId: string
      } & UpdateOrganizationRequest
    >({
      query: ({ organizationId, ...body }) => ({
        url: `/${organizationId}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { organizationId }) => [
        { type: 'Organization', id: organizationId },
      ],
    }),

    updateSettings: builder.mutation<
      Organization,
      {
        organizationId: string
      } & UpdateSettingsRequest
    >({
      query: ({ organizationId, ...body }) => ({
        url: `/${organizationId}/settings`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { organizationId }) => [
        { type: 'Organization', id: organizationId },
      ],
    }),

    deleteOrganization: builder.mutation<void, string>({
      query: organizationId => ({
        url: `/${organizationId}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Organization'],
    }),

    getOrganizationMembers: builder.query<OrganizationMemberInfo[], string>({
      query: organizationId => `/${organizationId}/members`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'OrganizationMember', id: organizationId },
      ],
    }),

    inviteUser: builder.mutation<
      Invitation,
      {
        organizationId: string
      } & InviteUserRequest
    >({
      query: ({ organizationId, ...body }) => ({
        url: `/${organizationId}/invitations`,
        method: 'POST',
        body,
      }),
      invalidatesTags: (_result, _error, { organizationId }) => [
        { type: 'Invitation', id: organizationId },
      ],
    }),

    acceptInvitation: builder.mutation<OrganizationMember, string>({
      query: token => ({
        url: `/invitations/${token}/accept`,
        method: 'POST',
      }),
      invalidatesTags: ['Organization', 'OrganizationMember'],
    }),

    declineInvitation: builder.mutation<void, string>({
      query: token => ({
        url: `/invitations/${token}/decline`,
        method: 'POST',
      }),
      invalidatesTags: ['Invitation'],
    }),

    getPendingInvitations: builder.query<Invitation[], string>({
      query: organizationId => `/${organizationId}/invitations`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'Invitation', id: organizationId },
      ],
    }),

    revokeInvitation: builder.mutation<void, string>({
      query: invitationId => ({
        url: `/invitations/${invitationId}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Invitation'],
    }),

    removeMember: builder.mutation<
      void,
      {
        organizationId: string
        userId: string
      }
    >({
      query: ({ organizationId, userId }) => ({
        url: `/${organizationId}/members/${userId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_result, _error, { organizationId }) => [
        { type: 'OrganizationMember', id: organizationId },
      ],
    }),

    updateMemberRole: builder.mutation<
      OrganizationMember,
      {
        organizationId: string
        userId: string
      } & UpdateMemberRoleRequest
    >({
      query: ({ organizationId, userId, ...body }) => ({
        url: `/${organizationId}/members/${userId}/role`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { organizationId }) => [
        { type: 'OrganizationMember', id: organizationId },
      ],
    }),
  }),
})

export const {
  useCreateOrganizationMutation,
  useGetOrganizationQuery,
  useGetOrganizationBySlugQuery,
  useGetUserOrganizationsQuery,
  useUpdateOrganizationMutation,
  useUpdateSettingsMutation,
  useDeleteOrganizationMutation,
  useGetOrganizationMembersQuery,
  useInviteUserMutation,
  useAcceptInvitationMutation,
  useDeclineInvitationMutation,
  useGetPendingInvitationsQuery,
  useRevokeInvitationMutation,
  useRemoveMemberMutation,
  useUpdateMemberRoleMutation,
  // Export for lazy queries
  useLazyGetOrganizationQuery,
  useLazyGetOrganizationBySlugQuery,
} = organizationApi
