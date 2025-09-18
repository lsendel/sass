import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import CreateOrganizationModal from '../../components/organizations/CreateOrganizationModal'
import {
  BuildingOfficeIcon,
  PlusIcon,
  UserGroupIcon,
  CalendarDaysIcon,
  SparklesIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'

const OrganizationsPage: React.FC = () => {
  const {
    data: organizations,
    isLoading,
    error,
  } = useGetUserOrganizationsQuery()
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="backdrop-blur-xl bg-white/10 border border-white/20 rounded-3xl p-8 shadow-2xl">
          <LoadingSpinner size="lg" />
          <p className="mt-4 text-white/80 text-center">Loading organizations...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="backdrop-blur-xl bg-red-500/20 border border-red-300/30 rounded-3xl p-6 shadow-2xl">
        <div className="text-sm text-red-100 text-center">
          Failed to load organizations. Please try again later.
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-tr from-blue-500 to-cyan-600 rounded-3xl shadow-lg mb-6">
          <BuildingOfficeIcon className="w-8 h-8 text-white" />
        </div>
        <h2 className="text-4xl font-bold text-white mb-3">
          Organizations
        </h2>
        <p className="text-xl text-white/80 max-w-2xl mx-auto mb-8">
          Manage your organization memberships and create new organizations.
        </p>
        <button
          type="button"
          onClick={() => setIsCreateModalOpen(true)}
          className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-blue-600 to-cyan-600 text-white font-medium rounded-2xl shadow-lg hover:from-blue-700 hover:to-cyan-700 transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-white/20"
        >
          <PlusIcon className="-ml-1 mr-2 h-5 w-5" />
          New Organization
        </button>
      </div>

      {/* Organizations List */}
      {organizations && organizations.length > 0 ? (
        <div className="space-y-4">
          {organizations.map(organization => (
            <Link
              key={organization.id}
              to={`/organizations/${organization.slug}`}
              className="block group"
            >
              <div className="backdrop-blur-xl bg-white/10 border border-white/20 rounded-3xl p-6 shadow-2xl hover:bg-white/15 hover:scale-105 transition-all duration-300">
                <div className="flex items-center justify-between">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className="h-12 w-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-2xl flex items-center justify-center shadow-lg">
                        <BuildingOfficeIcon className="h-6 w-6 text-white" />
                      </div>
                    </div>
                    <div className="ml-4">
                      <div className="flex items-center">
                        <p className="text-lg font-bold text-white truncate">
                          {organization.name}
                        </p>
                        <span className="ml-3 inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-white/20 text-white/90 backdrop-blur-sm">
                          /{organization.slug}
                        </span>
                      </div>
                      <div className="mt-1 flex items-center text-sm text-white/70">
                        <CalendarDaysIcon className="flex-shrink-0 mr-1.5 h-4 w-4" />
                        <p>
                          Created{' '}
                          {format(
                            new Date(organization.createdAt),
                            'MMM d, yyyy'
                          )}
                        </p>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-4">
                    <div className="flex items-center text-sm text-white/70">
                      <UserGroupIcon className="flex-shrink-0 mr-1.5 h-4 w-4" />
                      <span>Members</span>
                    </div>
                    <div className="text-white/60 group-hover:text-white/80 group-hover:translate-x-1 transition-all duration-300">
                      <svg
                        className="h-5 w-5"
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
        <div className="text-center py-12 backdrop-blur-xl bg-white/10 border border-white/20 rounded-3xl shadow-2xl">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-gray-500 to-gray-600 rounded-3xl shadow-lg mb-6">
            <BuildingOfficeIcon className="w-10 h-10 text-white" />
          </div>
          <h3 className="text-2xl font-bold text-white mb-2">
            No organizations yet
          </h3>
          <p className="text-white/70 mb-8 max-w-md mx-auto">
            Get started by creating your first organization and begin managing your team.
          </p>
          <button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-blue-600 to-cyan-600 text-white font-medium rounded-2xl shadow-lg hover:from-blue-700 hover:to-cyan-700 transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-white/20"
          >
            <PlusIcon className="-ml-1 mr-2 h-5 w-5" />
            Create Your First Organization
          </button>
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
