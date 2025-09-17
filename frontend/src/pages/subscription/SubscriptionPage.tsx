import React, { useState } from 'react'
import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
import {
import { logger } from '../../utils/logger'
  useGetOrganizationSubscriptionQuery,
  useGetAvailablePlansQuery,
  useCancelSubscriptionMutation,
  useReactivateSubscriptionMutation,
  useGetOrganizationInvoicesQuery,
} from '../../store/api/subscriptionApi'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import UpgradePlanModal from '../../components/subscription/UpgradePlanModal'
import {
  DocumentTextIcon,
  ExclamationTriangleIcon,
  ArrowUpIcon,
  ReceiptRefundIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { toast } from 'react-hot-toast'
import { clsx } from 'clsx'

const SubscriptionPage: React.FC = () => {
  const [showUpgradeModal, setShowUpgradeModal] = useState(false)
  const { data: organizations, isLoading: orgsLoading } =
    useGetUserOrganizationsQuery()
  const { data: availablePlans } = useGetAvailablePlansQuery()

  const primaryOrg = organizations?.[0]

  const {
    data: subscription,
    isLoading: subLoading,
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

  if (orgsLoading || subLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!primaryOrg) {
    return (
      <div className="text-center py-12">
        <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400" />
        <h3 className="mt-2 text-sm font-medium text-gray-900">
          No organization found
        </h3>
        <p className="mt-1 text-sm text-gray-500">
          Please create an organization first to manage subscriptions.
        </p>
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

      toast.success('Subscription scheduled for cancellation')
      refetchSubscription()
    } catch (error) {
      logger.error('Failed to cancel subscription:', error)
      toast.error('Failed to cancel subscription')
    }
  }

  const handleReactivateSubscription = async () => {
    if (!subscription) {return}

    try {
      await reactivateSubscription({
        subscriptionId: subscription.id,
        organizationId: primaryOrg.id,
      }).unwrap()

      toast.success('Subscription reactivated')
      refetchSubscription()
    } catch (error) {
      logger.error('Failed to reactivate subscription:', error)
      toast.error('Failed to reactivate subscription')
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
      {/* Header */}
      <div className="md:flex md:items-center md:justify-between">
        <div className="flex-1 min-w-0">
          <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
            Subscription
          </h2>
          <p className="mt-1 text-sm text-gray-500">
            Manage your subscription plan and view invoices.
          </p>
        </div>
      </div>

      {/* Current Subscription */}
      {subscription ? (
        <div className="bg-white shadow overflow-hidden sm:rounded-lg">
          <div className="px-4 py-5 sm:px-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              Current Subscription
            </h3>
            <p className="mt-1 max-w-2xl text-sm text-gray-500">
              Your subscription details and billing information.
            </p>
          </div>
          <div className="border-t border-gray-200">
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

          <div className="px-4 py-4 sm:px-6 bg-gray-50 flex justify-end space-x-3">
            {subscription.status === 'ACTIVE' && !subscription.cancelAt && (
              <>
                <button
                  onClick={() => setShowUpgradeModal(true)}
                  className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                >
                  <ArrowUpIcon className="-ml-1 mr-2 h-4 w-4" />
                  Change Plan
                </button>
                <button
                  onClick={handleCancelSubscription}
                  className="inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                >
                  Cancel Subscription
                </button>
              </>
            )}
            {subscription.cancelAt && (
              <button
                onClick={handleReactivateSubscription}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
              >
                Reactivate Subscription
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="bg-white shadow sm:rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              No Active Subscription
            </h3>
            <div className="mt-2 max-w-xl text-sm text-gray-500">
              <p>
                You don&apos;t have an active subscription. Choose a plan to get
                started.
              </p>
            </div>
            <div className="mt-5">
              <button
                type="button"
                onClick={() => setShowUpgradeModal(true)}
                className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:text-sm"
              >
                Choose a Plan
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Invoices */}
      {invoices && invoices.length > 0 && (
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <div className="px-4 py-5 sm:px-6 border-b border-gray-200">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              Invoice History
            </h3>
          </div>
          <div className="overflow-x-auto">
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
        </div>
      )}

      {/* Upgrade Plan Modal */}
      <UpgradePlanModal
        isOpen={showUpgradeModal}
        onClose={() => setShowUpgradeModal(false)}
        currentPlanId={subscription?.planId}
        organizationId={primaryOrg?.id}
        subscriptionId={subscription?.id}
      />
    </div>
  )
}

export default SubscriptionPage
