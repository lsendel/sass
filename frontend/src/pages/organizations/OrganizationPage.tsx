import React, { useState } from 'react'
import { useParams, Navigate } from 'react-router-dom'
import { UserPlusIcon, UsersIcon, TrashIcon } from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { toast } from 'react-hot-toast'
import { clsx } from 'clsx'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

import CreateOrganizationModal from '../../components/organizations/CreateOrganizationModal'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { useGetOrganizationSubscriptionQuery } from '../../store/api/subscriptionApi'
import {
  useGetUserOrganizationsQuery,
  useGetOrganizationMembersQuery,
  useInviteUserMutation,
  useRemoveMemberMutation,
} from '../../store/api/organizationApi'
import { logger } from '../../utils/logger'
import { parseApiError } from '../../utils/apiError'
import PageHeader from '../../components/ui/PageHeader'

const inviteMemberSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  role: z.enum(['OWNER', 'ADMIN', 'MEMBER'], {
    message: 'Please select a role',
  }),
})

type InviteMemberForm = z.infer<typeof inviteMemberSchema>

const OrganizationPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>()
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showInviteForm, setShowInviteForm] = useState(false)
  // const [editingOrg, setEditingOrg] = useState(false)

  const { data: organizations, isLoading: orgsLoading } =
    useGetUserOrganizationsQuery()
  const organization = organizations?.find(org => org.slug === slug)

  const {
    data: members,
    isLoading: membersLoading,
    refetch: refetchMembers,
  } = useGetOrganizationMembersQuery(organization?.id ?? '', {
    skip: !organization?.id,
  })

  const { data: subscription } = useGetOrganizationSubscriptionQuery(
    organization?.id ?? '',
    {
      skip: !organization?.id,
    }
  )

  // const [updateOrganization, { isLoading: isUpdating }] = useUpdateOrganizationMutation()
  const [inviteUser, { isLoading: isInviting }] = useInviteUserMutation()
  const [removeMember] = useRemoveMemberMutation()

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<InviteMemberForm>({
    resolver: zodResolver(inviteMemberSchema),
  })

  if (orgsLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!organization && !orgsLoading) {
    return <Navigate to="/organizations" replace />
  }

  if (!organization) {
    return null
  }

  const handleInviteMember = async (data: InviteMemberForm) => {
    try {
      await inviteUser({
        organizationId: organization.id,
        email: data.email,
        role: data.role,
      }).unwrap()

      toast.success(`Invitation sent to ${data.email}`)
      reset()
      setShowInviteForm(false)
      void refetchMembers()
    } catch (err) {
      const parsed = parseApiError(err)
      logger.error('Failed to invite member:', parsed)
      toast.error(parsed.message ?? 'Failed to send invitation')
    }
  }

  const handleRemoveMember = async (userId: string, memberEmail: string) => {
    if (
      !confirm(
        `Are you sure you want to remove ${memberEmail} from this organization?`
      )
    ) {
      return
    }

    try {
      await removeMember({
        organizationId: organization.id,
        userId,
      }).unwrap()

      toast.success('Member removed successfully')
      void refetchMembers()
    } catch (err) {
      const parsed = parseApiError(err)
      logger.error('Failed to remove member:', parsed)
      toast.error(parsed.message ?? 'Failed to remove member')
    }
  }

  const getRoleBadge = (role: string) => {
    const roleStyles = {
      OWNER: 'bg-purple-100 text-purple-800',
      ADMIN: 'bg-blue-100 text-blue-800',
      MEMBER: 'bg-gray-100 text-gray-800',
    }

    return (
      <span
        className={clsx(
          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
          roleStyles[role as keyof typeof roleStyles] ??
            'bg-gray-100 text-gray-800'
        )}
      >
        {role}
      </span>
    )
  }

  // Status badge omitted; OrganizationMemberInfo does not include status

  return (
    <div className="space-y-6">
      <PageHeader
        title={organization.name}
        description="Organization settings and member management"
        actions={
          <button
            type="button"
            onClick={() => setShowInviteForm(true)}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
          >
            <UserPlusIcon className="-ml-1 mr-2 h-5 w-5" />
            Invite Member
          </button>
        }
      />

      {/* Organization Details */}
      <div className="bg-white shadow overflow-hidden sm:rounded-lg">
        <div className="px-4 py-5 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">
            Organization Information
          </h3>
          <p className="mt-1 max-w-2xl text-sm text-gray-500">
            Basic details and settings for this organization.
          </p>
        </div>
        <div className="border-t border-gray-200">
          <dl>
            <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Name</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                {organization.name}
              </dd>
            </div>
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Slug</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                {organization.slug}
              </dd>
            </div>
            {/* Status not available on Organization type; omit display or derive if available */}
            <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
              <dt className="text-sm font-medium text-gray-500">Created</dt>
              <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                {format(new Date(organization.createdAt), 'MMM d, yyyy')}
              </dd>
            </div>
            {(organization.settings as { description?: string } | undefined)
              ?.description && (
              <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">
                  Description
                </dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  {
                    (organization.settings as { description?: string })
                      .description!
                  }
                </dd>
              </div>
            )}
            {subscription && (
              <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">
                  Subscription
                </dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                    Active
                  </span>
                  <span className="ml-2 text-sm text-gray-500">
                    Next billing:{' '}
                    {format(
                      new Date(subscription.currentPeriodEnd),
                      'MMM d, yyyy'
                    )}
                  </span>
                </dd>
              </div>
            )}
          </dl>
        </div>
      </div>

      {/* Members */}
      <div className="bg-white shadow overflow-hidden sm:rounded-md">
        <div className="px-4 py-5 sm:px-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg leading-6 font-medium text-gray-900">
                Members
              </h3>
              <p className="mt-1 text-sm text-gray-500">
                People who have access to this organization
              </p>
            </div>
            <UsersIcon className="h-5 w-5 text-gray-400" />
          </div>
        </div>

        {membersLoading ? (
          <div className="flex justify-center py-8">
            <LoadingSpinner size="lg" />
          </div>
        ) : members && members.length > 0 ? (
          <ul className="divide-y divide-gray-200">
            {members.map(member => (
              <li key={member.userId} className="px-4 py-4 sm:px-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="h-10 w-10 rounded-full bg-gray-300 flex items-center justify-center">
                        <span className="text-sm font-medium text-gray-700">
                          {(member.userName ?? member.userEmail)
                            .charAt(0)
                            .toUpperCase()}
                        </span>
                      </div>
                    </div>
                    <div className="ml-4">
                      <div className="flex items-center">
                        <p className="text-sm font-medium text-gray-900">
                          {member.userName ?? member.userEmail}
                        </p>
                        <div className="ml-2">{getRoleBadge(member.role)}</div>
                      </div>
                      <div className="flex items-center mt-1">
                        <p className="text-sm text-gray-500">
                          {member.userEmail}
                        </p>
                      </div>
                      {/* No status/invitedAt on OrganizationMemberInfo; omit */}
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    {member.role !== 'OWNER' && (
                      <button
                        onClick={() =>
                          void handleRemoveMember(
                            member.userId,
                            member.userEmail
                          )
                        }
                        className="text-red-600 hover:text-red-500"
                      >
                        <TrashIcon className="h-5 w-5" />
                      </button>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <div className="text-center py-12">
            <UsersIcon className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">
              No members
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              Start by inviting team members to this organization.
            </p>
          </div>
        )}
      </div>

      {/* Invite Member Form */}
      {showInviteForm && (
        <div className="bg-white shadow sm:rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              Invite New Member
            </h3>
            <div className="mt-2 max-w-xl text-sm text-gray-500">
              <p>Send an invitation to join this organization.</p>
            </div>
            <form
              onSubmit={e => void handleSubmit(handleInviteMember)(e)}
              className="mt-5 space-y-4"
            >
              <div>
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-gray-700"
                >
                  Email address
                </label>
                <div className="mt-1">
                  <input
                    {...register('email')}
                    type="email"
                    className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                    placeholder="member@example.com"
                  />
                </div>
                {errors.email && (
                  <p className="mt-2 text-sm text-red-600">
                    {errors.email.message}
                  </p>
                )}
              </div>

              <div>
                <label
                  htmlFor="role"
                  className="block text-sm font-medium text-gray-700"
                >
                  Role
                </label>
                <div className="mt-1">
                  <select
                    {...register('role')}
                    className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                  >
                    <option value="">Select a role</option>
                    <option value="MEMBER">Member</option>
                    <option value="ADMIN">Admin</option>
                    <option value="OWNER">Owner</option>
                  </select>
                </div>
                {errors.role && (
                  <p className="mt-2 text-sm text-red-600">
                    {errors.role.message}
                  </p>
                )}
              </div>

              <div className="flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowInviteForm(false)
                    reset()
                  }}
                  className="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isInviting}
                  className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isInviting ? 'Sending...' : 'Send Invitation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create Organization Modal */}
      <CreateOrganizationModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
      />
    </div>
  )
}

export default OrganizationPage
