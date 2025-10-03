import React from 'react'
import {
  BuildingOfficeIcon,
  CreditCardIcon,
  DocumentTextIcon,
  UserGroupIcon,
  ArrowTrendingUpIcon,
  CalendarDaysIcon,
  ChartBarIcon,
  BellIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import clsx from 'clsx'

import { useAppSelector } from '../../store/hooks'
import { selectCurrentUser } from '../../store/slices/authSlice'
import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
// import { useGetPaymentStatisticsQuery } from '../../store/api/paymentApi'
import { useGetSubscriptionStatisticsQuery } from '../../store/api/subscriptionApi'
import { useRealTimeUpdates } from '../../hooks/useRealTimeUpdates'
import { LoadingCard, InlineLoading } from '../../components/ui/LoadingStates'
import StatsCard from '../../components/ui/StatsCard'
import { getCardClasses } from '../../lib/theme'
// import { usePagePerformance, usePerformanceTracking } from '../../utils/performance'

const DashboardPage: React.FC = () => {
  const user = useAppSelector(selectCurrentUser)

  // Performance tracking for dashboard
  // usePagePerformance('Dashboard')
  // usePerformanceTracking()
  const { data: organizations, isLoading: orgsLoading } =
    useGetUserOrganizationsQuery()

  // Get statistics for the first organization (primary org)
  const primaryOrg = organizations?.[0]
  // Payment stats temporarily disabled
  const paymentStats = { totalSuccessfulPayments: 0, totalAmount: 0 }
  const paymentStatsLoading = false

  const {
    data: subscriptionStats,
    isLoading: subscriptionStatsLoading,
    refetch: refetchSubscriptionStats,
  } = useGetSubscriptionStatisticsQuery(primaryOrg?.id || '', {
    skip: !primaryOrg?.id,
  })

  // Set up real-time updates for dashboard data
  const realTimeUpdates = useRealTimeUpdates(
    async () => {
      // Measure refresh performance
      // Refresh subscription statistics
      const subscriptionData = await refetchSubscriptionStats()
      return { subscriptionData }
    },
    {
      interval: 30000, // Update every 30 seconds
      enabled: !!primaryOrg?.id, // Only update when we have an organization
      pauseWhenInactive: true,
      pauseAfterInactivity: 300000, // Pause after 5 minutes of inactivity
    }
  )

  if (orgsLoading) {
    return (
      <div className="space-y-6">
        <LoadingCard
          title="Loading Dashboard"
          message="Preparing your personalized dashboard experience..."
        />
      </div>
    )
  }

  const stats = [
    {
      name: 'Organizations',
      value: organizations?.length || 0,
      icon: BuildingOfficeIcon,
      bgColor: 'bg-primary-50',
      iconColor: 'text-primary-600',
      trend: '+12%',
    },
    {
      name: 'Total Payments',
      value: paymentStats?.totalSuccessfulPayments || 0,
      icon: CreditCardIcon,
      bgColor: 'bg-success-50',
      iconColor: 'text-success-600',
      trend: '+18%',
    },
    {
      name: 'Revenue',
      value:
        paymentStats && typeof paymentStats.totalAmount === 'number'
          ? `$${paymentStats.totalAmount.toFixed(2)}`
          : '$0.00',
      icon: ArrowTrendingUpIcon,
      bgColor: 'bg-accent-50',
      iconColor: 'text-accent-600',
      trend: '+32%',
    },
    {
      name: 'Subscription',
      value: String(subscriptionStats?.status ?? 'Active'),
      icon: DocumentTextIcon,
      bgColor: 'bg-gray-50',
      iconColor: 'text-gray-600',
      trend: 'Current',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Welcome Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Welcome back, <span className="brand-element">{user?.email}</span>!
            👋
          </h1>
          <p className="text-sm text-gray-600">
            Here's your dashboard overview with the latest insights and metrics.
          </p>
        </div>

        {/* Real-time update status */}
        <div className="text-right">
          <div className="text-xs text-gray-500 mb-1">
            {realTimeUpdates.isActive ? (
              <span className="flex items-center">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-1 animate-pulse"></div>
                Live updates active
              </span>
            ) : (
              <span className="text-gray-400">Updates paused</span>
            )}
          </div>
          <button
            onClick={() => void realTimeUpdates.forceUpdate()}
            className="text-xs text-primary-600 hover:text-primary-500 underline"
          >
            Refresh now
          </button>
        </div>
      </div>

      {/* Stats Grid - Compact */}
      <div className="grid grid-cols-4 gap-3">
        {stats.map(stat => {
          const isPaymentsCard = stat.name === 'Total Payments'
          const isSubscriptionCard = stat.name === 'Subscription'
          const displayValue =
            isPaymentsCard && paymentStatsLoading ? (
              <InlineLoading size="md" />
            ) : isSubscriptionCard && subscriptionStatsLoading ? (
              <InlineLoading size="md" />
            ) : (
              stat.value
            )

          return (
            <StatsCard
              key={stat.name}
              unstyled
              className={clsx(
                getCardClasses(),
                'p-3 transition-shadow hover:shadow-lg brand-element'
              )}
              contentClassName="p-0"
              icon={<stat.icon className="w-3 h-3 brand-element" />}
              iconWrapperClassName={`inline-flex items-center justify-center w-6 h-6 ${stat.bgColor} ${stat.iconColor} rounded-lg mb-2`}
              title={stat.name}
              titleClassName="text-xs font-medium text-gray-600 mb-1"
              value={displayValue}
              valueClassName="text-base font-bold text-gray-900 mb-1 brand-element"
              trend={stat.trend}
              trendClassName="text-xs font-medium text-success-600 bg-success-100 px-1 py-0.5 rounded"
            />
          )
        })}
      </div>

      {/* Quick Actions - Compact */}
      <div className={getCardClasses()}>
        <div className="flex items-center mb-3">
          <div className="w-6 h-6 gradient-brand rounded-lg flex items-center justify-center mr-2">
            <ChartBarIcon className="w-3 h-3 text-white brand-element" />
          </div>
          <h3 className="text-base font-semibold text-gray-900 brand-element">
            Quick Actions
          </h3>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <QuickActionCard
            title="Organizations"
            description="Manage memberships"
            href="/organizations"
            icon={BuildingOfficeIcon}
          />
          <QuickActionCard
            title="Payments"
            description="View transactions"
            href="/payments"
            icon={CreditCardIcon}
          />
          <QuickActionCard
            title="Subscription"
            description="Manage billing"
            href="/subscription"
            icon={DocumentTextIcon}
          />
        </div>
      </div>

      {/* Recent Activity - Compact */}
      {primaryOrg && (
        <div className={getCardClasses()}>
          <div className="flex items-center mb-3">
            <div className="w-6 h-6 bg-success-600 rounded-lg flex items-center justify-center mr-2">
              <BellIcon className="w-3 h-3 text-white brand-element" />
            </div>
            <h3 className="text-base font-semibold text-gray-900 brand-element">
              Recent Activity
            </h3>
          </div>
          <div className="space-y-2">
            <div className="flex items-center p-2 bg-success-50 rounded-lg border border-success-200">
              <div className="w-6 h-6 bg-success-600 rounded-full flex items-center justify-center mr-2">
                <UserGroupIcon className="w-3 h-3 text-white brand-element" />
              </div>
              <div className="flex-1">
                <p className="text-xs font-medium text-gray-900">
                  Organization{' '}
                  <span className="font-semibold brand-element">
                    {primaryOrg.name}
                  </span>{' '}
                  created
                </p>
                <p className="text-xs text-gray-500">
                  {format(new Date(primaryOrg.createdAt), 'MMM d, yyyy')}
                </p>
              </div>
              <div className="text-success-600">
                <svg
                  className="w-3 h-3"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                    clipRule="evenodd"
                  />
                </svg>
              </div>
            </div>

            {!!subscriptionStats?.status && (
              <div className="flex items-center p-2 bg-primary-50 rounded-lg border border-primary-200">
                <div className="w-6 h-6 bg-primary-600 rounded-full flex items-center justify-center mr-2 brand-bg">
                  <DocumentTextIcon className="w-3 h-3 text-white brand-element" />
                </div>
                <div className="flex-1">
                  <p className="text-xs font-medium text-gray-900">
                    Subscription:{' '}
                    <span className="font-semibold brand-element">
                      {subscriptionStats.status}
                    </span>
                  </p>
                  <p className="text-xs text-gray-500">Active subscription</p>
                </div>
                <div className="text-primary-600">
                  <CalendarDaysIcon className="w-3 h-3 brand-element" />
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

interface QuickActionCardProps {
  title: string
  description: string
  href: string
  icon: React.ComponentType<{ className?: string }>
}

const QuickActionCard: React.FC<QuickActionCardProps> = ({
  title,
  description,
  href,
  icon: Icon,
}) => {
  return (
    <div className="group relative bg-gray-50 hover:bg-gray-100 p-3 rounded-lg transition-colors border border-gray-200">
      <div className="relative">
        {/* Icon */}
        <div className="inline-flex items-center justify-center w-6 h-6 bg-gray-200 rounded-lg mb-2">
          <Icon className="w-3 h-3 text-gray-600 brand-element" />
        </div>

        {/* Content */}
        <div>
          <h3 className="text-sm font-semibold text-gray-900 mb-1 brand-element">
            <a href={href} className="focus:outline-none">
              <span className="absolute inset-0" aria-hidden="true" />
              {title}
            </a>
          </h3>
          <p className="text-xs text-gray-600">{description}</p>
        </div>

        {/* Arrow icon */}
        <div className="absolute top-2 right-2 text-gray-400 group-hover:text-gray-600 transition-colors">
          <svg
            className="w-3 h-3"
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
  )
}

export default DashboardPage
