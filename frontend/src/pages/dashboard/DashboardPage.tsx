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
  SparklesIcon,
  ChartBarIcon,
  BellIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'

const DashboardPage: React.FC = () => {
  const user = useAppSelector(selectCurrentUser)
  const { data: organizations, isLoading: orgsLoading } =
    useGetUserOrganizationsQuery()

  // Get statistics for the first organization (primary org)
  const primaryOrg = organizations?.[0]
  const { data: paymentStats, isLoading: paymentStatsLoading } =
    useGetPaymentStatisticsQuery(primaryOrg?.id || '', {
      skip: !primaryOrg?.id,
    })

  const { data: subscriptionStats, isLoading: subscriptionStatsLoading } =
    useGetSubscriptionStatisticsQuery(primaryOrg?.id || '', {
      skip: !primaryOrg?.id,
    })

  if (orgsLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-purple-50 to-indigo-50 flex justify-center items-center">
        <div className="bg-white bg-opacity-80 backdrop-blur-lg rounded-3xl p-8 shadow-2xl">
          <LoadingSpinner size="lg" />
          <p className="mt-4 text-gray-600 text-center">Loading your dashboard...</p>
        </div>
      </div>
    )
  }

  const stats = [
    {
      name: 'Organizations',
      value: organizations?.length || 0,
      icon: BuildingOfficeIcon,
      gradient: 'from-blue-500 to-cyan-500',
      bgGradient: 'from-blue-50 to-cyan-50',
      trend: '+12%',
    },
    {
      name: 'Total Payments',
      value: paymentStats?.totalSuccessfulPayments || 0,
      icon: CreditCardIcon,
      gradient: 'from-green-500 to-emerald-500',
      bgGradient: 'from-green-50 to-emerald-50',
      trend: '+18%',
    },
    {
      name: 'Revenue',
      value: paymentStats ? `$${paymentStats.totalAmount.toFixed(2)}` : '$0.00',
      icon: ArrowTrendingUpIcon,
      gradient: 'from-purple-500 to-pink-500',
      bgGradient: 'from-purple-50 to-pink-50',
      trend: '+32%',
    },
    {
      name: 'Subscription',
      value: String(subscriptionStats?.status ?? 'Active'),
      icon: DocumentTextIcon,
      gradient: 'from-indigo-500 to-purple-500',
      bgGradient: 'from-indigo-50 to-purple-50',
      trend: 'Current',
    },
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-purple-50 to-indigo-50">
      {/* Floating background elements */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-20 left-20 w-64 h-64 bg-purple-200 bg-opacity-30 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute top-40 right-20 w-96 h-96 bg-indigo-200 bg-opacity-20 rounded-full blur-3xl animate-pulse delay-1000"></div>
        <div className="absolute bottom-20 left-1/2 w-80 h-80 bg-pink-200 bg-opacity-25 rounded-full blur-3xl animate-pulse delay-2000"></div>
      </div>

      <div className="relative space-y-8 p-6">
        {/* Welcome Header */}
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-tr from-purple-500 to-indigo-600 rounded-3xl shadow-lg mb-6">
            <SparklesIcon className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-gray-900 via-purple-900 to-indigo-900 bg-clip-text text-transparent mb-3">
            Welcome back, {user?.name}! 👋
          </h1>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto">
            Here's your personalized dashboard overview with all the latest insights and metrics.
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4 max-w-7xl mx-auto">
          {stats.map((stat, index) => (
            <div
              key={stat.name}
              className={`group relative bg-gradient-to-br ${stat.bgGradient} p-6 rounded-3xl shadow-lg hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-2 border border-white border-opacity-50 backdrop-blur-sm`}
              style={{ animationDelay: `${index * 100}ms` }}
            >
              {/* Icon */}
              <div className={`inline-flex items-center justify-center w-14 h-14 bg-gradient-to-r ${stat.gradient} rounded-2xl shadow-lg mb-4 group-hover:scale-110 transition-transform duration-300`}>
                <stat.icon className="h-7 w-7 text-white" />
              </div>

              {/* Content */}
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">{stat.name}</p>
                <p className="text-3xl font-bold text-gray-900 mb-2">
                  {stat.name === 'Total Payments' && paymentStatsLoading ? (
                    <LoadingSpinner size="sm" />
                  ) : stat.name === 'Subscription' && subscriptionStatsLoading ? (
                    <LoadingSpinner size="sm" />
                  ) : (
                    stat.value
                  )}
                </p>
                <div className="flex items-center">
                  <span className="text-sm font-medium text-green-600 bg-green-100 px-2 py-1 rounded-full">
                    {stat.trend}
                  </span>
                </div>
              </div>

              {/* Decorative gradient */}
              <div className={`absolute top-0 right-0 w-20 h-20 bg-gradient-to-br ${stat.gradient} opacity-10 rounded-full blur-xl`}></div>
            </div>
          ))}
        </div>

        {/* Quick Actions */}
        <div className="max-w-7xl mx-auto">
          <div className="bg-white bg-opacity-80 backdrop-blur-lg rounded-3xl p-8 shadow-2xl border border-white border-opacity-50">
            <div className="flex items-center mb-6">
              <div className="w-12 h-12 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-2xl flex items-center justify-center mr-4">
                <ChartBarIcon className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-gray-900">Quick Actions</h3>
            </div>
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
              <QuickActionCard
                title="Manage Organizations"
                description="View and manage your organization memberships"
                href="/organizations"
                icon={BuildingOfficeIcon}
                gradient="from-blue-500 to-cyan-500"
              />
              <QuickActionCard
                title="View Payments"
                description="See your payment history and transactions"
                href="/payments"
                icon={CreditCardIcon}
                gradient="from-green-500 to-emerald-500"
              />
              <QuickActionCard
                title="Subscription Settings"
                description="Manage your subscription plan and billing"
                href="/subscription"
                icon={DocumentTextIcon}
                gradient="from-purple-500 to-pink-500"
              />
            </div>
          </div>
        </div>

        {/* Recent Activity */}
        {primaryOrg && (
          <div className="max-w-7xl mx-auto">
            <div className="bg-white bg-opacity-80 backdrop-blur-lg rounded-3xl p-8 shadow-2xl border border-white border-opacity-50">
              <div className="flex items-center mb-6">
                <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-500 rounded-2xl flex items-center justify-center mr-4">
                  <BellIcon className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-2xl font-bold text-gray-900">Recent Activity</h3>
              </div>
              <div className="space-y-4">
                <div className="flex items-center p-4 bg-gradient-to-r from-green-50 to-emerald-50 rounded-2xl border border-green-200">
                  <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-500 rounded-full flex items-center justify-center mr-4">
                    <UserGroupIcon className="h-6 w-6 text-white" />
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900">
                      Organization <span className="font-bold">{primaryOrg.name}</span> created
                    </p>
                    <p className="text-xs text-gray-500">
                      {format(new Date(primaryOrg.createdAt), 'MMM d, yyyy')}
                    </p>
                  </div>
                  <div className="text-green-600">
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                    </svg>
                  </div>
                </div>

                {!!subscriptionStats?.status && (
                  <div className="flex items-center p-4 bg-gradient-to-r from-indigo-50 to-purple-50 rounded-2xl border border-indigo-200">
                    <div className="w-12 h-12 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full flex items-center justify-center mr-4">
                      <DocumentTextIcon className="h-6 w-6 text-white" />
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-900">
                        Subscription status: <span className="font-bold">{subscriptionStats.status}</span>
                      </p>
                      <p className="text-xs text-gray-500">Active subscription</p>
                    </div>
                    <div className="text-indigo-600">
                      <CalendarDaysIcon className="h-5 w-5" />
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

type QuickActionCardProps = {
  title: string
  description: string
  href: string
  icon: React.ComponentType<{ className?: string }>
  gradient: string
}

const QuickActionCard: React.FC<QuickActionCardProps> = ({
  title,
  description,
  href,
  icon: Icon,
  gradient,
}) => {
  return (
    <div className="group relative bg-white bg-opacity-60 backdrop-blur-sm p-6 rounded-2xl border border-white border-opacity-50 hover:bg-opacity-80 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1 overflow-hidden">
      {/* Background gradient effect */}
      <div className={`absolute inset-0 bg-gradient-to-br ${gradient} opacity-0 group-hover:opacity-5 transition-opacity duration-300`}></div>

      <div className="relative">
        {/* Icon */}
        <div className={`inline-flex items-center justify-center w-12 h-12 bg-gradient-to-r ${gradient} rounded-xl shadow-lg mb-4 group-hover:scale-110 transition-transform duration-300`}>
          <Icon className="h-6 w-6 text-white" />
        </div>

        {/* Content */}
        <div>
          <h3 className="text-lg font-bold text-gray-900 mb-2 group-hover:text-gray-800 transition-colors">
            <a href={href} className="focus:outline-none">
              <span className="absolute inset-0" aria-hidden="true" />
              {title}
            </a>
          </h3>
          <p className="text-sm text-gray-600 group-hover:text-gray-700 transition-colors">
            {description}
          </p>
        </div>

        {/* Arrow icon */}
        <div className="absolute top-6 right-6 text-gray-400 group-hover:text-gray-600 group-hover:translate-x-1 transition-all duration-300">
          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
          </svg>
        </div>

        {/* Decorative corner element */}
        <div className={`absolute bottom-0 right-0 w-16 h-16 bg-gradient-to-tl ${gradient} opacity-10 rounded-full blur-lg group-hover:opacity-20 transition-opacity duration-300`}></div>
      </div>
    </div>
  )
}

export default DashboardPage
