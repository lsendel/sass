import React from 'react'
import { useAppSelector } from '../../store/hooks'
import { selectCurrentUser } from '../../store/slices/authSlice'
import { useGetUserOrganizationsQuery } from '../../store/api/organizationApi'
import { useGetPaymentStatisticsQuery } from '../../store/api/paymentApi'
import { useGetSubscriptionStatisticsQuery } from '../../store/api/subscriptionApi'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import {
  BuildingOfficeIcon,
  CreditCardIcon,
  DocumentTextIcon,
  UserGroupIcon,
  ArrowTrendingUpIcon,
  CalendarDaysIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'

const DashboardPage: React.FC = () => {
  const user = useAppSelector(selectCurrentUser)
  const { data: organizations, isLoading: orgsLoading } = useGetUserOrganizationsQuery()

  // Get statistics for the first organization (primary org)
  const primaryOrg = organizations?.[0]
  const {
    data: paymentStats,
    isLoading: paymentStatsLoading
  } = useGetPaymentStatisticsQuery(primaryOrg?.id || '', {
    skip: !primaryOrg?.id
  })

  const {
    data: subscriptionStats,
    isLoading: subscriptionStatsLoading
  } = useGetSubscriptionStatisticsQuery(primaryOrg?.id || '', {
    skip: !primaryOrg?.id
  })

  if (orgsLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  const stats = [
    {
      name: 'Organizations',
      value: organizations?.length || 0,
      icon: BuildingOfficeIcon,
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
    },
    {
      name: 'Total Payments',
      value: paymentStats?.totalSuccessfulPayments || 0,
      icon: CreditCardIcon,
      color: 'text-green-600',
      bgColor: 'bg-green-100',
    },
    {
      name: 'Revenue (All Time)',
      value: paymentStats ? `$${paymentStats.totalAmount.toFixed(2)}` : '$0.00',
      icon: ArrowTrendingUpIcon,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
    },
    {
      name: 'Subscription Status',
      value: subscriptionStats?.status || 'None',
      icon: DocumentTextIcon,
      color: 'text-indigo-600',
      bgColor: 'bg-indigo-100',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome back, {user?.name}!
        </h1>
        <p className="mt-1 text-sm text-gray-500">
          Here's what's happening with your account today.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <div
            key={stat.name}
            className="relative bg-white pt-5 px-4 pb-12 sm:pt-6 sm:px-6 shadow rounded-lg overflow-hidden"
          >
            <dt>
              <div className={`absolute ${stat.bgColor} rounded-md p-3`}>
                <stat.icon className={`h-6 w-6 ${stat.color}`} />
              </div>
              <p className="ml-16 text-sm font-medium text-gray-500 truncate">
                {stat.name}
              </p>
            </dt>
            <dd className="ml-16 pb-6 flex items-baseline sm:pb-7">
              <p className="text-2xl font-semibold text-gray-900">
                {stat.name === 'Total Payments' && paymentStatsLoading ? (
                  <LoadingSpinner size="sm" />
                ) : stat.name === 'Subscription Status' && subscriptionStatsLoading ? (
                  <LoadingSpinner size="sm" />
                ) : (
                  stat.value
                )}
              </p>
            </dd>
          </div>
        ))}
      </div>

      {/* Quick Actions */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">
            Quick Actions
          </h3>
          <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <QuickActionCard
              title="Manage Organizations"
              description="View and manage your organization memberships"
              href="/organizations"
              icon={BuildingOfficeIcon}
            />
            <QuickActionCard
              title="View Payments"
              description="See your payment history and transactions"
              href="/payments"
              icon={CreditCardIcon}
            />
            <QuickActionCard
              title="Subscription Settings"
              description="Manage your subscription plan and billing"
              href="/subscription"
              icon={DocumentTextIcon}
            />
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      {primaryOrg && (
        <div className="bg-white shadow rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              Recent Activity
            </h3>
            <div className="mt-5">
              <div className="flow-root">
                <ul className="-mb-8">
                  <li>
                    <div className="relative pb-8">
                      <div className="relative flex space-x-3">
                        <div>
                          <span className="h-8 w-8 rounded-full bg-green-100 flex items-center justify-center ring-8 ring-white">
                            <UserGroupIcon className="h-5 w-5 text-green-600" />
                          </span>
                        </div>
                        <div className="min-w-0 flex-1 pt-1.5 flex justify-between space-x-4">
                          <div>
                            <p className="text-sm text-gray-500">
                              Organization <span className="font-medium text-gray-900">{primaryOrg.name}</span> created
                            </p>
                          </div>
                          <div className="text-right text-sm whitespace-nowrap text-gray-500">
                            <time dateTime={primaryOrg.createdAt}>
                              {format(new Date(primaryOrg.createdAt), 'MMM d, yyyy')}
                            </time>
                          </div>
                        </div>
                      </div>
                    </div>
                  </li>

                  {subscriptionStats?.status && subscriptionStats.status !== 'None' && (
                    <li>
                      <div className="relative pb-8">
                        <div className="relative flex space-x-3">
                          <div>
                            <span className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center ring-8 ring-white">
                              <DocumentTextIcon className="h-5 w-5 text-blue-600" />
                            </span>
                          </div>
                          <div className="min-w-0 flex-1 pt-1.5 flex justify-between space-x-4">
                            <div>
                              <p className="text-sm text-gray-500">
                                Subscription status: <span className="font-medium text-gray-900">{subscriptionStats.status}</span>
                              </p>
                            </div>
                            <div className="text-right text-sm whitespace-nowrap text-gray-500">
                              <CalendarDaysIcon className="h-4 w-4 inline" />
                            </div>
                          </div>
                        </div>
                      </div>
                    </li>
                  )}
                </ul>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

type QuickActionCardProps = {
  title: string
  description: string
  href: string
  icon: React.ComponentType<{ className?: string }>
}

const QuickActionCard: React.FC<QuickActionCardProps> = ({ title, description, href, icon: Icon }) => {
  return (
    <div className="relative group bg-white p-6 focus-within:ring-2 focus-within:ring-inset focus-within:ring-primary-500 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors">
      <div>
        <span className="rounded-lg inline-flex p-3 bg-primary-50 text-primary-600 group-hover:bg-primary-100">
          <Icon className="h-6 w-6" />
        </span>
      </div>
      <div className="mt-4">
        <h3 className="text-lg font-medium">
          <a href={href} className="focus:outline-none">
            <span className="absolute inset-0" aria-hidden="true" />
            {title}
          </a>
        </h3>
        <p className="mt-2 text-sm text-gray-500">
          {description}
        </p>
      </div>
      <span
        className="pointer-events-none absolute top-6 right-6 text-gray-300 group-hover:text-gray-400"
        aria-hidden="true"
      >
        <svg className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
          <path d="M20 4h1a1 1 0 00-1-1v1zm-1 12a1 1 0 102 0h-2zM8 3a1 1 0 000 2V3zM3.293 19.293a1 1 0 101.414 1.414l-1.414-1.414zM19 4v12h2V4h-2zm1-1H8v2h12V3zm-.707.293l-16 16 1.414 1.414 16-16-1.414-1.414z" />
        </svg>
      </span>
    </div>
  )
}

export default DashboardPage