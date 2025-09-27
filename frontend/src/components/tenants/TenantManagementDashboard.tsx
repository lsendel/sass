/**
 * Tenant Management Dashboard
 *
 * Comprehensive multi-tenant administration interface supporting:
 * - Tenant creation, configuration, and lifecycle management
 * - Hierarchical tenant relationships and organization
 * - Real-time tenant analytics and performance monitoring
 * - Cross-tenant reporting and administrative operations
 * - Tenant-specific branding and customization settings
 */

import React, { useState, useMemo } from 'react'
import {
  PlusIcon,
  MagnifyingGlassIcon,
  BuildingOfficeIcon,
  UsersIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  XCircleIcon,
  PauseIcon,
  PlayIcon,
  TrashIcon,
  PencilIcon,
  EyeIcon,
} from '@heroicons/react/24/outline'

import {
  useListTenantsQuery,
  useDeleteTenantMutation,
  useSuspendTenantMutation,
  useActivateTenantMutation,
  useGetTenantAnalyticsQuery,
  useGetTenantHierarchyQuery,
} from '@/store/api/tenantApi'
import { useMultiTenantAdmin } from '@/hooks/useTenantContext'
import type {
  Tenant,
  TenantId,
  TenantStatus,
  TenantTier,
} from '@/types/multitenancy'

// Icons for better UI

interface TenantManagementDashboardProps {
  className?: string
}

const TenantManagementDashboard: React.FC<TenantManagementDashboardProps> = ({
  className = '',
}) => {
  const { isMultiTenantAdmin } = useMultiTenantAdmin()
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null)
  const [, setShowCreateModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [filters, setFilters] = useState({
    status: '' as TenantStatus | '',
    tier: '' as TenantTier | '',
    search: '',
  })
  const [currentPage, setCurrentPage] = useState(1)
  const [selectedTenants, setSelectedTenants] = useState<Set<TenantId>>(new Set())

  // API hooks
  const {
    data: tenantsResponse,
    isLoading: isLoadingTenants,
    error: tenantsError,
    refetch: refetchTenants,
  } = useListTenantsQuery({
    page: currentPage,
    pageSize: 20,
    ...(filters.status && { status: filters.status }),
    ...(filters.tier && { tier: filters.tier }),
    ...(filters.search && { search: filters.search }),
    sortBy: 'createdAt',
    sortOrder: 'desc',
  })

  const [deleteTenant] = useDeleteTenantMutation()
  const [suspendTenant] = useSuspendTenantMutation()
  const [activateTenant] = useActivateTenantMutation()

  // Analytics for selected tenant
  const { data: tenantAnalytics } = useGetTenantAnalyticsQuery(
    {
      tenantId: selectedTenant?.id!,
      period: 'month',
    },
    { skip: !selectedTenant }
  )

  // Tenant hierarchy for selected tenant
  const { data: tenantHierarchy } = useGetTenantHierarchyQuery(
    { tenantId: selectedTenant?.id! },
    { skip: !selectedTenant }
  )

  if (!isMultiTenantAdmin) {
    return (
      <div className="text-center py-12">
        <ExclamationTriangleIcon className="mx-auto h-12 w-12 text-gray-400" />
        <h3 className="mt-2 text-sm font-semibold text-gray-900">Access Restricted</h3>
        <p className="mt-1 text-sm text-gray-500">
          You don't have permission to access tenant management.
        </p>
      </div>
    )
  }

  const tenants = tenantsResponse?.tenants || []
  const totalTenants = tenantsResponse?.total || 0

  // Tenant statistics
  const tenantStats = useMemo(() => {
    const stats = {
      total: totalTenants,
      active: 0,
      suspended: 0,
      trial: 0,
      pending: 0,
      byTier: {
        starter: 0,
        professional: 0,
        enterprise: 0,
        white_label: 0,
      },
    }

    tenants.forEach(tenant => {
      switch (tenant.status) {
        case 'active':
          stats.active++
          break
        case 'suspended':
          stats.suspended++
          break
        case 'trial':
          stats.trial++
          break
        case 'pending_setup':
          stats.pending++
          break
      }

      stats.byTier[tenant.tier]++
    })

    return stats
  }, [tenants, totalTenants])

  const handleDeleteTenant = async (tenantId: TenantId) => {
    if (!confirm('Are you sure you want to delete this tenant? This action cannot be undone.')) {
      return
    }

    try {
      await deleteTenant({ tenantId }).unwrap()
      refetchTenants()
    } catch (error) {
      console.error('Failed to delete tenant:', error)
    }
  }

  const handleSuspendTenant = async (tenantId: TenantId) => {
    const reason = prompt('Please provide a reason for suspending this tenant:')
    if (!reason) return

    try {
      await suspendTenant({ tenantId, reason }).unwrap()
      refetchTenants()
    } catch (error) {
      console.error('Failed to suspend tenant:', error)
    }
  }

  const handleActivateTenant = async (tenantId: TenantId) => {
    try {
      await activateTenant({ tenantId }).unwrap()
      refetchTenants()
    } catch (error) {
      console.error('Failed to activate tenant:', error)
    }
  }

  const getStatusColor = (status: TenantStatus) => {
    switch (status) {
      case 'active':
        return 'bg-green-100 text-green-800'
      case 'suspended':
        return 'bg-red-100 text-red-800'
      case 'trial':
        return 'bg-blue-100 text-blue-800'
      case 'pending_setup':
        return 'bg-yellow-100 text-yellow-800'
      case 'archived':
        return 'bg-gray-100 text-gray-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getTierBadge = (tier: TenantTier) => {
    const colors = {
      starter: 'bg-gray-100 text-gray-800',
      professional: 'bg-blue-100 text-blue-800',
      enterprise: 'bg-purple-100 text-purple-800',
      white_label: 'bg-indigo-100 text-indigo-800',
    }

    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${colors[tier]}`}>
        {tier.replace('_', ' ')}
      </span>
    )
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="sm:flex sm:items-center">
        <div className="sm:flex-auto">
          <h1 className="text-2xl font-semibold leading-6 text-gray-900">
            Tenant Management
          </h1>
          <p className="mt-2 text-sm text-gray-700">
            Manage all tenants, their settings, and monitor their performance across the platform.
          </p>
        </div>
        <div className="mt-4 sm:ml-16 sm:mt-0 sm:flex-none">
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="block rounded-md bg-indigo-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
          >
            <PlusIcon className="h-5 w-5 inline mr-2" />
            Create Tenant
          </button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <BuildingOfficeIcon className="h-6 w-6 text-gray-400" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Total Tenants</dt>
                  <dd className="text-lg font-medium text-gray-900">{tenantStats.total}</dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <CheckCircleIcon className="h-6 w-6 text-green-400" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Active Tenants</dt>
                  <dd className="text-lg font-medium text-gray-900">{tenantStats.active}</dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <XCircleIcon className="h-6 w-6 text-red-400" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Suspended</dt>
                  <dd className="text-lg font-medium text-gray-900">{tenantStats.suspended}</dd>
                </dl>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white overflow-hidden shadow rounded-lg">
          <div className="p-5">
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <UsersIcon className="h-6 w-6 text-blue-400" />
              </div>
              <div className="ml-5 w-0 flex-1">
                <dl>
                  <dt className="text-sm font-medium text-gray-500 truncate">Trial Tenants</dt>
                  <dd className="text-lg font-medium text-gray-900">{tenantStats.trial}</dd>
                </dl>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Filters and Search */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-4">
            <div>
              <label htmlFor="search" className="block text-sm font-medium text-gray-700">
                Search
              </label>
              <div className="mt-1 relative">
                <input
                  type="text"
                  name="search"
                  id="search"
                  value={filters.search}
                  onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                  className="shadow-sm focus:ring-indigo-500 focus:border-indigo-500 block w-full sm:text-sm border-gray-300 rounded-md"
                  placeholder="Search tenants..."
                />
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <MagnifyingGlassIcon className="h-5 w-5 text-gray-400" />
                </div>
              </div>
            </div>

            <div>
              <label htmlFor="status-filter" className="block text-sm font-medium text-gray-700">
                Status
              </label>
              <select
                id="status-filter"
                name="status-filter"
                value={filters.status}
                onChange={(e) => setFilters({ ...filters, status: e.target.value as TenantStatus | '' })}
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              >
                <option value="">All Statuses</option>
                <option value="active">Active</option>
                <option value="suspended">Suspended</option>
                <option value="trial">Trial</option>
                <option value="pending_setup">Pending Setup</option>
                <option value="archived">Archived</option>
              </select>
            </div>

            <div>
              <label htmlFor="tier-filter" className="block text-sm font-medium text-gray-700">
                Tier
              </label>
              <select
                id="tier-filter"
                name="tier-filter"
                value={filters.tier}
                onChange={(e) => setFilters({ ...filters, tier: e.target.value as TenantTier | '' })}
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              >
                <option value="">All Tiers</option>
                <option value="starter">Starter</option>
                <option value="professional">Professional</option>
                <option value="enterprise">Enterprise</option>
                <option value="white_label">White Label</option>
              </select>
            </div>

            <div className="flex items-end">
              <button
                type="button"
                onClick={() => setFilters({ status: '', tier: '', search: '' })}
                className="w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Tenants Table */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="px-4 py-5 sm:p-6">
          <div className="flow-root">
            <div className="-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
              <div className="inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8">
                {isLoadingTenants ? (
                  <div className="text-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
                    <p className="mt-4 text-sm text-gray-500">Loading tenants...</p>
                  </div>
                ) : tenantsError ? (
                  <div className="text-center py-12">
                    <ExclamationTriangleIcon className="mx-auto h-12 w-12 text-red-400" />
                    <h3 className="mt-2 text-sm font-semibold text-gray-900">Error Loading Tenants</h3>
                    <p className="mt-1 text-sm text-gray-500">
                      There was an error loading the tenants list.
                    </p>
                    <div className="mt-6">
                      <button
                        type="button"
                        onClick={() => refetchTenants()}
                        className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
                      >
                        Try Again
                      </button>
                    </div>
                  </div>
                ) : (
                  <table className="min-w-full divide-y divide-gray-300">
                    <thead className="bg-gray-50">
                      <tr>
                        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          <input
                            type="checkbox"
                            className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                            checked={selectedTenants.size === tenants.length}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedTenants(new Set(tenants.map(t => t.id)))
                              } else {
                                setSelectedTenants(new Set())
                              }
                            }}
                          />
                        </th>
                        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Tenant
                        </th>
                        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Status
                        </th>
                        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Tier
                        </th>
                        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Created
                        </th>
                        <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Last Active
                        </th>
                        <th scope="col" className="relative px-6 py-3">
                          <span className="sr-only">Actions</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {tenants.map((tenant) => (
                        <tr key={tenant.id} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap">
                            <input
                              type="checkbox"
                              className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                              checked={selectedTenants.has(tenant.id)}
                              onChange={(e) => {
                                const newSelected = new Set(selectedTenants)
                                if (e.target.checked) {
                                  newSelected.add(tenant.id)
                                } else {
                                  newSelected.delete(tenant.id)
                                }
                                setSelectedTenants(newSelected)
                              }}
                            />
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center">
                              <div className="flex-shrink-0 h-10 w-10">
                                {tenant.branding?.logoUrl ? (
                                  <img
                                    className="h-10 w-10 rounded-lg object-cover"
                                    src={tenant.branding.logoUrl}
                                    alt={`${tenant.name} logo`}
                                  />
                                ) : (
                                  <div className="h-10 w-10 rounded-lg bg-gray-200 flex items-center justify-center">
                                    <BuildingOfficeIcon className="h-6 w-6 text-gray-400" />
                                  </div>
                                )}
                              </div>
                              <div className="ml-4">
                                <div className="text-sm font-medium text-gray-900">
                                  {tenant.displayName}
                                </div>
                                <div className="text-sm text-gray-500">
                                  {tenant.slug}
                                  {tenant.customDomain && (
                                    <span className="ml-2 text-blue-600">
                                      • {tenant.customDomain}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(tenant.status)}`}>
                              {tenant.status.replace('_', ' ')}
                            </span>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            {getTierBadge(tenant.tier)}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {new Date(tenant.createdAt).toLocaleDateString()}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {tenant.lastActiveAt ? new Date(tenant.lastActiveAt).toLocaleDateString() : 'Never'}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                            <div className="flex items-center justify-end space-x-2">
                              <button
                                onClick={() => setSelectedTenant(tenant)}
                                className="text-indigo-600 hover:text-indigo-900"
                                title="View Details"
                              >
                                <EyeIcon className="h-5 w-5" />
                              </button>
                              <button
                                onClick={() => {
                                  setSelectedTenant(tenant)
                                  setShowEditModal(true)
                                }}
                                className="text-gray-600 hover:text-gray-900"
                                title="Edit Tenant"
                              >
                                <PencilIcon className="h-5 w-5" />
                              </button>
                              {tenant.status === 'suspended' ? (
                                <button
                                  onClick={() => handleActivateTenant(tenant.id)}
                                  className="text-green-600 hover:text-green-900"
                                  title="Activate Tenant"
                                >
                                  <PlayIcon className="h-5 w-5" />
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleSuspendTenant(tenant.id)}
                                  className="text-yellow-600 hover:text-yellow-900"
                                  title="Suspend Tenant"
                                >
                                  <PauseIcon className="h-5 w-5" />
                                </button>
                              )}
                              <button
                                onClick={() => handleDeleteTenant(tenant.id)}
                                className="text-red-600 hover:text-red-900"
                                title="Delete Tenant"
                              >
                                <TrashIcon className="h-5 w-5" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          </div>

          {/* Pagination */}
          {tenantsResponse && tenantsResponse.total > 20 && (
            <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
              <div className="flex-1 flex justify-between sm:hidden">
                <button
                  onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                  disabled={currentPage === 1}
                  className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={!tenantsResponse.hasNext}
                  className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                >
                  Next
                </button>
              </div>
              <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm text-gray-700">
                    Showing{' '}
                    <span className="font-medium">{(currentPage - 1) * 20 + 1}</span> to{' '}
                    <span className="font-medium">
                      {Math.min(currentPage * 20, tenantsResponse.total)}
                    </span>{' '}
                    of <span className="font-medium">{tenantsResponse.total}</span> results
                  </p>
                </div>
                <div>
                  <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                    <button
                      onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}
                      disabled={currentPage === 1}
                      className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      Previous
                    </button>
                    <button
                      onClick={() => setCurrentPage(currentPage + 1)}
                      disabled={!tenantsResponse.hasNext}
                      className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                    >
                      Next
                    </button>
                  </nav>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Tenant Details Panel */}
      {selectedTenant && !showEditModal && (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <div className="px-4 py-5 sm:p-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg leading-6 font-medium text-gray-900">
                {selectedTenant.displayName} Details
              </h3>
              <button
                onClick={() => setSelectedTenant(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <XCircleIcon className="h-6 w-6" />
              </button>
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              {/* Basic Information */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-4">Basic Information</h4>
                <dl className="space-y-2">
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">ID:</dt>
                    <dd className="text-sm text-gray-900">{selectedTenant.id}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Slug:</dt>
                    <dd className="text-sm text-gray-900">{selectedTenant.slug}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Status:</dt>
                    <dd>
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(selectedTenant.status)}`}>
                        {selectedTenant.status.replace('_', ' ')}
                      </span>
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Tier:</dt>
                    <dd>{getTierBadge(selectedTenant.tier)}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Region:</dt>
                    <dd className="text-sm text-gray-900">{selectedTenant.region}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-sm text-gray-500">Timezone:</dt>
                    <dd className="text-sm text-gray-900">{selectedTenant.timezone}</dd>
                  </div>
                </dl>
              </div>

              {/* Resource Usage */}
              {tenantAnalytics && (
                <div>
                  <h4 className="text-sm font-medium text-gray-900 mb-4">Usage Statistics</h4>
                  <dl className="space-y-2">
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Active Users:</dt>
                      <dd className="text-sm text-gray-900">{tenantAnalytics.usage.activeUsers}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Total Sessions:</dt>
                      <dd className="text-sm text-gray-900">{tenantAnalytics.usage.totalSessions.toLocaleString()}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">API Calls:</dt>
                      <dd className="text-sm text-gray-900">{tenantAnalytics.usage.apiCalls.toLocaleString()}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Storage Used:</dt>
                      <dd className="text-sm text-gray-900">{(tenantAnalytics.usage.storageUsed / 1024).toFixed(2)} GB</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-sm text-gray-500">Monthly Revenue:</dt>
                      <dd className="text-sm text-gray-900">${tenantAnalytics.business.revenue.toFixed(2)}</dd>
                    </div>
                  </dl>
                </div>
              )}
            </div>

            {/* Tenant Hierarchy */}
            {tenantHierarchy && (
              <div className="mt-6">
                <h4 className="text-sm font-medium text-gray-900 mb-4">Tenant Hierarchy</h4>
                <div className="space-y-2">
                  {tenantHierarchy.ancestors.length > 0 && (
                    <div>
                      <p className="text-sm text-gray-500">Parent Tenants:</p>
                      <div className="flex flex-wrap gap-2 mt-1">
                        {tenantHierarchy.ancestors.map(ancestor => (
                          <span
                            key={ancestor.id}
                            className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800"
                          >
                            {ancestor.displayName}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                  {tenantHierarchy.children.length > 0 && (
                    <div>
                      <p className="text-sm text-gray-500">Child Tenants:</p>
                      <div className="flex flex-wrap gap-2 mt-1">
                        {tenantHierarchy.children.map(child => (
                          <span
                            key={child.id}
                            className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                          >
                            {child.displayName}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default TenantManagementDashboard