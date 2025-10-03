import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  BuildingOfficeIcon,
  PlusIcon,
  UserGroupIcon,
  CalendarDaysIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'

import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
import { ListSkeleton, LoadingButton } from '../../components/ui/LoadingStates'
import { ApiErrorDisplay } from '../../components/ui/ErrorStates'
import CreateOrganizationModal from '../../components/organizations/CreateOrganizationModal'
import { getIconClasses, getCardClasses } from '../../lib/theme'

const OrganizationsPage: React.FC = () => {
  const {
    data: organizations,
    isLoading,
    error,
    refetch,
  } = useGetUserOrganizationsQuery()
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  if (isLoading) {
    return (
      <div className="space-y-6">
        {/* Header Skeleton */}
        <div>
          <div className="flex items-center justify-between mb-6">
            <div>
              <div className="h-8 bg-gray-200 rounded animate-pulse w-48 mb-2" />
              <div className="h-4 bg-gray-200 rounded animate-pulse w-64" />
            </div>
            <div className="h-10 bg-gray-200 rounded animate-pulse w-36" />
          </div>
        </div>

        {/* Organizations List Skeleton */}
        <ListSkeleton items={3} />
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-6">
        {/* Header */}
        <div>
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 mb-2">
                <span className="gradient-brand bg-clip-text text-transparent">
                  Organizations
                </span>
              </h1>
              <p className="text-sm text-gray-600">
                Manage your organization memberships and create new
                organizations.
              </p>
            </div>
          </div>
        </div>

        <ApiErrorDisplay
          error={error}
          onRetry={() => refetch()}
          fallbackMessage="Failed to load organizations. Please try again."
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 mb-2">
              <span className="gradient-brand bg-clip-text text-transparent">
                Organizations
              </span>
            </h1>
            <p className="text-sm text-gray-600">
              Manage your organization memberships and create new organizations.
            </p>
          </div>
          <LoadingButton
            onClick={() => setIsCreateModalOpen(true)}
            variant="primary"
            size="md"
          >
            <PlusIcon className={getIconClasses('sm')} />
            New Organization
          </LoadingButton>
        </div>
      </div>

      {/* Organizations List */}
      {organizations && organizations.length > 0 ? (
        <div className="grid gap-4">
          {organizations.map((organization: any) => (
            <Link
              key={organization.id}
              to={`/organizations/${organization.slug}`}
              className="block group transition-transform hover:scale-[1.02]"
            >
              <div
                className={`${getCardClasses('elevated')} group-hover:shadow-xl transition-all duration-200`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex-shrink-0">
                      <div className="w-8 h-8 gradient-brand bg-[#2563eb] rounded-lg flex items-center justify-center">
                        <BuildingOfficeIcon
                          className={`${getIconClasses('sm')} text-white`}
                        />
                      </div>
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-3">
                        <h3 className="text-base font-semibold text-gray-900 truncate">
                          {organization.name}
                        </h3>
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                          /{organization.slug}
                        </span>
                      </div>
                      <div className="mt-1 flex items-center gap-1.5 text-xs text-gray-500">
                        <CalendarDaysIcon className={getIconClasses('xs')} />
                        <span>
                          Created{' '}
                          {format(
                            new Date(organization.createdAt),
                            'MMM d, yyyy'
                          )}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-1.5 text-xs text-gray-500">
                      <UserGroupIcon className={getIconClasses('xs')} />
                      <span>Members</span>
                    </div>
                    <div className="text-gray-400 group-hover:text-gray-600 transition-colors">
                      <svg
                        className={getIconClasses('sm')}
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M13 7l5 5m0 0l-5 5m5-5H6"
                        />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className={`${getCardClasses('subtle')} text-center py-8`}>
          <div className="inline-flex items-center justify-center w-12 h-12 bg-gray-200 rounded-lg mb-4">
            <BuildingOfficeIcon
              className={`${getIconClasses('lg')} text-gray-500`}
            />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 mb-2">
            No organizations yet
          </h3>
          <p className="text-sm text-gray-600 mb-4 max-w-md mx-auto">
            Get started by creating your first organization and begin managing
            your team.
          </p>
          <LoadingButton
            onClick={() => setIsCreateModalOpen(true)}
            variant="primary"
            size="md"
          >
            <PlusIcon className={getIconClasses('sm')} />
            Create Your First Organization
          </LoadingButton>
        </div>
      )}

      {/* Create Organization Modal */}
      <CreateOrganizationModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />
    </div>
  )
}

export default OrganizationsPage
