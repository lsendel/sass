import React, { useState } from 'react'
import {
  ExclamationTriangleIcon,
  ArrowUpIcon,
  ReceiptRefundIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { clsx } from 'clsx'

import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
import {
  useGetOrganizationSubscriptionQuery,
  useGetAvailablePlansQuery,
  useCancelSubscriptionMutation,
  useReactivateSubscriptionMutation,
  useGetOrganizationInvoicesQuery,
} from '../../store/api/subscriptionApi'
import { LoadingCard, ListSkeleton, LoadingButton } from '../../components/ui/LoadingStates'
import { ApiErrorDisplay, EmptyState } from '../../components/ui/ErrorStates'
import UpgradePlanModal from '../../components/subscription/UpgradePlanModal'
import { useCrossComponentSync } from '../../hooks/useDataSync'
import { useNotifications } from '../../components/ui/FeedbackSystem'
import PageHeader from '../../components/ui/PageHeader'
import StatsCard from '../../components/ui/StatsCard'

const SubscriptionPage: React.FC = () => {
  const [showUpgradeModal, setShowUpgradeModal] = useState(false)
  const { data: organizations, isLoading: orgsLoading, error: orgsError } =
    useGetUserOrganizationsQuery()
  const { data: availablePlans } = useGetAvailablePlansQuery()
  const { syncSubscriptionData, syncPaymentData } = useCrossComponentSync()

  const primaryOrg = organizations?.[0]

  const headerDescription = 'Manage your subscription plan and view invoices.'
  const renderHeader = (actions?: React.ReactNode) => (
    <PageHeader title="Subscription" description={headerDescription} actions={actions} />
  )

  const {
    data: subscription,
    isLoading: subLoading,
    error: subError,
    refetch: refetchSubscription,
  } = useGetOrganizationSubscriptionQuery(primaryOrg?.id || '', {
    skip: !primaryOrg?.id,
  })

  const { data: invoices } = useGetOrganizationInvoicesQuery(
    primaryOrg?.id || '',
    {
      skip: !primaryOrg?.id,
    }
  )

  const [cancelSubscription] = useCancelSubscriptionMutation()
  const [reactivateSubscription] = useReactivateSubscriptionMutation()

  const currentPlan = availablePlans?.find(p => p.id === subscription?.planId)

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

  if (subLoading && !subscription) {
    return (
      <div className="space-y-6">
        {/* Header Skeleton */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="flex-1 min-w-0 space-y-2">
            <div className="h-8 bg-gray-200 rounded animate-pulse w-48" />
            <div className="h-4 bg-gray-200 rounded animate-pulse w-64" />
          </div>
        </div>

        {/* Subscription Card Skeleton */}
        <div className="bg-white shadow sm:rounded-lg animate-pulse">
          <div className="px-4 py-5 sm:px-6">
            <div className="h-6 bg-gray-200 rounded w-48 mb-2" />
            <div className="h-4 bg-gray-200 rounded w-64" />
          </div>
          <div className="border-t border-gray-200">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="px-4 py-5 sm:px-6 border-b border-gray-100 last:border-b-0">
                <div className="h-4 bg-gray-200 rounded w-24 mb-2" />
                <div className="h-5 bg-gray-200 rounded w-32" />
              </div>
            ))}
          </div>
        </div>

        {/* Invoices Skeleton */}
        <div className="bg-white shadow sm:rounded-lg">
          <div className="px-4 py-5 sm:px-6 border-b border-gray-200">
            <div className="h-6 bg-gray-200 rounded animate-pulse w-32" />
          </div>
          <ListSkeleton items={3} />
        </div>
      </div>
    )
  }

  // Error handling for organizations data
  if (orgsError) {
    return (
      <div className="space-y-6">
        {renderHeader()}

        <ApiErrorDisplay
          error={orgsError}
          onRetry={() => {
            window.location.reload()
          }}
          fallbackMessage="Failed to load organization data. Please try again."
        />
      </div>
    )
  }

  // Error handling for subscription data
  if (subError) {
    return (
      <div className="space-y-6">
        {renderHeader()}

        <ApiErrorDisplay
          error={subError}
          onRetry={() => {
            void refetchSubscription()
            void syncSubscriptionData()
          }}
          fallbackMessage="Failed to load subscription data. Please try again."
        />
      </div>
    )
  }

  if (!primaryOrg) {
    return (
      <div className="space-y-6">
        {renderHeader()}

        <EmptyState
          title="No organization found"
          message="Please create an organization first to manage subscriptions."
          action={{
            label: "Go to Organizations",
            onClick: () => window.location.href = '/organizations'
          }}
        />
      </div>
    )
  }

  const handleCancelSubscription = async () => {
    if (!subscription) {return}

    if (
      !confirm(
        'Are you sure you want to cancel your subscription? It will remain active until the end of the current billing period.'
      )
    ) {
      return
    }

    try {
      await cancelSubscription({
        subscriptionId: subscription.id,
        organizationId: primaryOrg.id,
        immediate: false,
      }).unwrap()

      showSuccess('Subscription Canceled', 'Your subscription has been scheduled for cancellation at the end of the current billing period.')
      await refetchSubscription()
      await syncSubscriptionData()
    } catch (error) {
      console.error('Failed to cancel subscription:', error)
      // showError('Failed to Cancel', 'Unable to cancel your subscription. Please try again.')
    }
  }

  const handleReactivateSubscription = async () => {
    if (!subscription) {return}

    try {
      await reactivateSubscription({
        subscriptionId: subscription.id,
        organizationId: primaryOrg.id,
      }).unwrap()

      showSuccess('Subscription Reactivated', 'Your subscription has been successfully reactivated.')
      await refetchSubscription()
      await syncSubscriptionData()
      await syncPaymentData()
    } catch (error) {
      console.error('Failed to reactivate subscription:', error)
      // showError('Reactivation Failed', 'Unable to reactivate your subscription. Please try again.')
    }
  }

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      ACTIVE: { color: 'bg-green-100 text-green-800', label: 'Active' },
      TRIALING: { color: 'bg-blue-100 text-blue-800', label: 'Trial' },
      PAST_DUE: { color: 'bg-red-100 text-red-800', label: 'Past Due' },
      CANCELED: { color: 'bg-gray-100 text-gray-800', label: 'Canceled' },
      UNPAID: { color: 'bg-red-100 text-red-800', label: 'Unpaid' },
    }

    const config = statusConfig[status as keyof typeof statusConfig] || {
      color: 'bg-gray-100 text-gray-800',
      label: status,
    }

    return (
      <span
        className={clsx(
          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
          config.color
        )}
      >
        {config.label}
      </span>
    )
  }

  return (
    <div className="space-y-6">
      {renderHeader()}

      {/* Current Subscription */}
      {subscription ? (
        <StatsCard
          title="Current Subscription"
          description="Your subscription details and billing information."
          className="shadow overflow-hidden sm:rounded-lg"
          contentClassName="px-4 py-5 sm:px-6"
          footer={(
            <div className="flex justify-end space-x-3">
              {subscription.status === 'ACTIVE' && !subscription.cancelAt && (
                <>
                  <LoadingButton
                    onClick={() => setShowUpgradeModal(true)}
                    variant="primary"
                    size="md"
                  >
                    <ArrowUpIcon className="-ml-1 mr-2 h-4 w-4" />
                    Change Plan
                  </LoadingButton>
                  <LoadingButton
                    onClick={() => void handleCancelSubscription()}
                    variant="secondary"
                    size="md"
                  >
                    Cancel Subscription
                  </LoadingButton>
                </>
              )}
              {subscription.cancelAt && (
                <LoadingButton
                  onClick={() => void handleReactivateSubscription()}
                  variant="success"
                  size="md"
                >
                  Reactivate Subscription
                </LoadingButton>
              )}
            </div>
          )}
        >
          <div className="mt-4 border-t border-gray-200">
            <dl>
              <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">Plan</dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  {currentPlan?.name || 'Unknown Plan'}
                </dd>
              </div>
              <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">Status</dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  {getStatusBadge(subscription.status)}
                </dd>
              </div>
              <div className="bg-gray-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">Price</dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  ${currentPlan?.amount.toFixed(2)} /{' '}
                  {currentPlan?.interval.toLowerCase()}
                </dd>
              </div>
              <div className="bg-white px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                <dt className="text-sm font-medium text-gray-500">
                  Current Period
                </dt>
                <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                  {format(
                    new Date(subscription.currentPeriodStart),
                    'MMM d, yyyy'
                  )}{' '}
                  -{' '}
                  {format(
                    new Date(subscription.currentPeriodEnd),
                    'MMM d, yyyy'
                  )}
                </dd>
              </div>
              {subscription.cancelAt && (
                <div className="bg-yellow-50 px-4 py-5 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                  <dt className="text-sm font-medium text-gray-500">
                    <ExclamationTriangleIcon className="inline h-4 w-4 text-yellow-600 mr-1" />
                    Cancels On
                  </dt>
                  <dd className="mt-1 text-sm text-gray-900 sm:mt-0 sm:col-span-2">
                    {format(new Date(subscription.cancelAt), 'MMM d, yyyy')}
                  </dd>
                </div>
              )}
            </dl>
          </div>
        </StatsCard>
      ) : (
        <EmptyState
          title="No Active Subscription"
          message="You don't have an active subscription. Choose a plan to get started and unlock all features."
          action={{
            label: "Choose a Plan",
            onClick: () => setShowUpgradeModal(true)
          }}
          showRetry={false}
        />
      )}

      {/* Invoices */}
      {invoices && invoices.length > 0 && (
        <StatsCard
          title="Invoice History"
          className="shadow overflow-hidden sm:rounded-md"
          contentClassName="px-4 py-5 sm:px-6"
        >
          <div className="mt-4 overflow-x-auto border-t border-gray-200">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Invoice Number
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Amount
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {invoices.map(invoice => (
                  <tr key={invoice.id}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {invoice.invoiceNumber}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {format(new Date(invoice.createdAt), 'MMM d, yyyy')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      ${invoice.totalAmount.toFixed(2)}{' '}
                      {invoice.currency.toUpperCase()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={clsx(
                          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                          invoice.status === 'PAID'
                            ? 'bg-green-100 text-green-800'
                            : invoice.status === 'OPEN'
                              ? 'bg-yellow-100 text-yellow-800'
                              : 'bg-gray-100 text-gray-800'
                        )}
                      >
                        {invoice.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <button className="text-primary-600 hover:text-primary-900">
                        <ReceiptRefundIcon className="h-5 w-5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </StatsCard>
      )}

      {/* Upgrade Plan Modal */}
      <UpgradePlanModal
        isOpen={showUpgradeModal}
        onClose={() => setShowUpgradeModal(false)}
        organizationId={primaryOrg?.id || ''}
        {...(subscription?.planId && { currentPlanId: subscription.planId })}
        {...(subscription?.id && { subscriptionId: subscription.id })}
      />
    </div>
  )
}

export default SubscriptionPage
