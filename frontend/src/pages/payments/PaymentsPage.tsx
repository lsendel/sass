import React, { useState, useMemo } from 'react'
import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
import {
  useGetOrganizationPaymentsQuery,
  useGetPaymentStatisticsQuery,
} from '../../store/api/paymentApi'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { LoadingCard, TableSkeleton, LoadingButton } from '../../components/ui/LoadingStates'
import { ApiErrorDisplay, EmptyState } from '../../components/ui/ErrorStates'
import { useCrossComponentSync } from '../../hooks/useDataSync'
import {
  SearchAndFilterBar,
  FilterDropdown,
  DateRangeFilter,
  ResultsCounter
} from '../../components/ui/SearchAndFilter'
import PaymentMethodsModal from '../../components/payments/PaymentMethodsModal'
import {
  CreditCardIcon,
  ClockIcon,
  CheckCircleIcon,
  XCircleIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { clsx } from 'clsx'

const PaymentsPage: React.FC = () => {
  const [showPaymentMethods, setShowPaymentMethods] = useState(false)

  // Search and filter state
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<string[]>([])
  const [dateRange, setDateRange] = useState({ start: '', end: '' })

  const { data: organizations, isLoading: orgsLoading } =
    useGetUserOrganizationsQuery()

  // Get the primary organization
  const primaryOrg = organizations?.[0]

  const {
    data: payments,
    isLoading: paymentsLoading,
    error: paymentsError,
    refetch: refetchPayments
  } = useGetOrganizationPaymentsQuery(primaryOrg?.id || '', {
      skip: !primaryOrg?.id,
    })

  const {
    data: statistics,
    error: statisticsError,
    refetch: refetchStatistics
  } = useGetPaymentStatisticsQuery(primaryOrg?.id || '', {
      skip: !primaryOrg?.id,
    })

  const { syncPaymentData } = useCrossComponentSync()

  // Filter and search payments
  const filteredPayments = useMemo(() => {
    if (!payments) return []

    return payments.filter(payment => {
      // Search filter
      if (searchQuery) {
        const searchLower = searchQuery.toLowerCase()
        const matchesSearch =
          payment.description?.toLowerCase().includes(searchLower) ||
          payment.stripePaymentIntentId.toLowerCase().includes(searchLower) ||
          payment.amount.toString().includes(searchQuery)

        if (!matchesSearch) return false
      }

      // Status filter
      if (statusFilter.length > 0 && !statusFilter.includes(payment.status)) {
        return false
      }

      // Date range filter
      if (dateRange.start || dateRange.end) {
        const paymentDate = new Date(payment.createdAt)
        const startDate = dateRange.start ? new Date(dateRange.start) : null
        const endDate = dateRange.end ? new Date(dateRange.end) : null

        if (startDate && paymentDate < startDate) return false
        if (endDate && paymentDate > endDate) return false
      }

      return true
    })
  }, [payments, searchQuery, statusFilter, dateRange])

  // Status options for filter
  const statusOptions = useMemo(() => {
    if (!payments) return []

    const statusCounts = payments.reduce((acc, payment) => {
      acc[payment.status] = (acc[payment.status] || 0) + 1
      return acc
    }, {} as Record<string, number>)

    return Object.entries(statusCounts).map(([status, count]) => ({
      label: status,
      value: status,
      count,
    }))
  }, [payments])

  if (orgsLoading) {
    return (
      <div className="space-y-6">
        <LoadingCard
          title="Loading Organizations"
          message="Fetching your organization data..."
        />
      </div>
    )
  }

  if (paymentsLoading && !payments) {
    return (
      <div className="space-y-6">
        {/* Header Skeleton */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="flex-1 min-w-0 space-y-2">
            <div className="h-8 bg-gray-200 rounded animate-pulse w-48" />
            <div className="h-4 bg-gray-200 rounded animate-pulse w-64" />
          </div>
          <div className="h-10 bg-gray-200 rounded animate-pulse w-32" />
        </div>

        {/* Statistics Skeleton */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="bg-white overflow-hidden shadow rounded-lg animate-pulse">
              <div className="px-4 py-5 sm:p-6">
                <div className="h-4 bg-gray-200 rounded w-24 mb-2" />
                <div className="h-8 bg-gray-200 rounded w-16" />
              </div>
            </div>
          ))}
        </div>

        {/* Table Skeleton */}
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <div className="px-4 py-5 sm:px-6 border-b border-gray-200">
            <div className="h-6 bg-gray-200 rounded animate-pulse w-32" />
          </div>
          <TableSkeleton rows={5} columns={5} />
        </div>
      </div>
    )
  }

  // Error handling for payments data
  if (paymentsError) {
    return (
      <div className="space-y-6">
        {/* Header */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="flex-1 min-w-0">
            <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
              Payments
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              View your payment history and manage payment methods.
            </p>
          </div>
        </div>

        <ApiErrorDisplay
          error={paymentsError}
          onRetry={async () => {
            await refetchPayments()
            await refetchStatistics()
            await syncPaymentData()
          }}
          fallbackMessage="Failed to load payment data. Please try again."
        />
      </div>
    )
  }

  if (!primaryOrg) {
    return (
      <div className="space-y-6">
        {/* Header */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="flex-1 min-w-0">
            <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
              Payments
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              View your payment history and manage payment methods.
            </p>
          </div>
        </div>

        <EmptyState
          title="No organization found"
          message="Please create an organization first to manage payments."
          action={{
            label: "Go to Organizations",
            onClick: () => window.location.href = '/organizations'
          }}
        />
      </div>
    )
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCEEDED':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />
      case 'FAILED':
        return <XCircleIcon className="h-5 w-5 text-red-500" />
      case 'PENDING':
      case 'PROCESSING':
        return <ClockIcon className="h-5 w-5 text-yellow-500" />
      default:
        return <ClockIcon className="h-5 w-5 text-gray-400" />
    }
  }

  const getStatusBadge = (status: string) => {
    const statusStyles = {
      SUCCEEDED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
      PROCESSING: 'bg-blue-100 text-blue-800',
      CANCELED: 'bg-gray-100 text-gray-800',
    }

    return (
      <span
        className={clsx(
          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
          statusStyles[status as keyof typeof statusStyles] ||
            'bg-gray-100 text-gray-800'
        )}
      >
        {status}
      </span>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="md:flex md:items-center md:justify-between">
        <div className="flex-1 min-w-0">
          <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
            Payments
          </h2>
          <p className="mt-1 text-sm text-gray-500">
            View your payment history and manage payment methods.
          </p>
        </div>
        <div className="mt-4 flex md:mt-0 md:ml-4">
          <LoadingButton
            onClick={() => setShowPaymentMethods(true)}
            variant="primary"
            size="md"
          >
            <CreditCardIcon className="-ml-1 mr-2 h-5 w-5" />
            Payment Methods
          </LoadingButton>
        </div>
      </div>

      {/* Search and Filter Bar */}
      <SearchAndFilterBar
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
        searchPlaceholder="Search payments by description, amount, or ID..."
        filters={
          <>
            <FilterDropdown
              label="Status"
              options={statusOptions}
              selectedValues={statusFilter}
              onChange={setStatusFilter}
            />
            <DateRangeFilter
              startDate={dateRange.start}
              endDate={dateRange.end}
              onChange={(start, end) => setDateRange({ start, end })}
            />
          </>
        }
        resultsCount={
          <ResultsCounter
            total={payments?.length || 0}
            filtered={searchQuery || statusFilter.length > 0 || dateRange.start || dateRange.end ? filteredPayments.length : undefined}
            query={searchQuery}
          />
        }
      />

      {/* Statistics */}
      {statistics && (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">
                Total Payments
              </dt>
              <dd className="mt-1 text-3xl font-semibold text-gray-900">
                {statistics.totalSuccessfulPayments}
              </dd>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">
                Total Amount
              </dt>
              <dd className="mt-1 text-3xl font-semibold text-gray-900">
                ${statistics.totalAmount.toFixed(2)}
              </dd>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <dt className="text-sm font-medium text-gray-500 truncate">
                Recent (30 days)
              </dt>
              <dd className="mt-1 text-3xl font-semibold text-gray-900">
                ${statistics.recentAmount.toFixed(2)}
              </dd>
            </div>
          </div>
        </div>
      )}

      {/* Payments Table */}
      <div className="bg-white shadow overflow-hidden sm:rounded-md">
        <div className="px-4 py-5 sm:px-6 border-b border-gray-200">
          <h3 className="text-lg leading-6 font-medium text-gray-900">
            Payment History
          </h3>
        </div>

        {filteredPayments && filteredPayments.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Description
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Amount
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Payment ID
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredPayments.map(payment => (
                  <tr key={payment.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {format(new Date(payment.createdAt), 'MMM d, yyyy')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {payment.description || 'Payment'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      ${payment.amount.toFixed(2)}{' '}
                      {payment.currency.toUpperCase()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        {getStatusIcon(payment.status)}
                        <span className="ml-2">
                          {getStatusBadge(payment.status)}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 font-mono">
                      {payment.stripePaymentIntentId.substring(0, 20)}...
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12">
            <EmptyState
              title="No payments found"
              message={
                searchQuery || statusFilter.length > 0 || dateRange.start || dateRange.end
                  ? "No payments match your current filters. Try adjusting your search criteria."
                  : "Your payment history will appear here once you make a payment."
              }
              action={{
                label: "Set up Payment Methods",
                onClick: () => setShowPaymentMethods(true)
              }}
              showRetry={false}
            />
          </div>
        )}
      </div>

      {/* Payment Methods Modal */}
      <PaymentMethodsModal
        isOpen={showPaymentMethods}
        onClose={() => setShowPaymentMethods(false)}
        organizationId={primaryOrg.id}
      />
    </div>
  )
}

export default PaymentsPage
