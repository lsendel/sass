import React from 'react'
import clsx from 'clsx'

import StatsCard from '../ui/StatsCard'
import { InlineLoading } from '../ui/LoadingStates'
import { getCardClasses } from '../../lib/theme'
import { DASHBOARD_CONSTANTS } from '../../constants/dashboard'

/**
 * Data structure for individual statistics.
 */
export interface StatData {
  name: string
  value: string | number
  icon: React.ComponentType<{ className?: string }>
  colorTheme: keyof typeof DASHBOARD_CONSTANTS.COLORS
  trend: string
  isLoading?: boolean
}

interface DashboardStatsProps {
  stats: StatData[]
  className?: string
}

/**
 * Dashboard statistics component displaying key metrics in a grid layout.
 * Separated from main dashboard for better component organization.
 */
export const DashboardStats: React.FC<DashboardStatsProps> = ({
  stats,
  className,
}) => {
  return (
    <div
      className={clsx(
        `grid grid-cols-${DASHBOARD_CONSTANTS.STATS_GRID_COLUMNS} gap-3`,
        className
      )}
    >
      {stats.map(stat => {
        const colorConfig = DASHBOARD_CONSTANTS.COLORS[stat.colorTheme]

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
            iconWrapperClassName={clsx(
              'inline-flex items-center justify-center w-6 h-6 rounded-lg mb-2',
              colorConfig.BG,
              colorConfig.ICON
            )}
            title={stat.name}
            titleClassName="text-xs font-medium text-gray-600 mb-1"
            value={
              stat.isLoading ? (
                <InlineLoading size={DASHBOARD_CONSTANTS.LOADING.INLINE_SIZE} />
              ) : (
                stat.value
              )
            }
            valueClassName="text-base font-bold text-gray-900 mb-1 brand-element"
            trend={stat.trend}
            trendClassName="text-xs font-medium text-success-600 bg-success-100 px-1 py-0.5 rounded"
          />
        )
      })}
    </div>
  )
}

/**
 * Data structure for payment statistics.
 */
export interface PaymentStats {
  totalSuccessfulPayments?: number
  totalAmount?: number
}

/**
 * Data structure for subscription statistics.
 */
export interface SubscriptionStats {
  status?: string
}

/**
 * Hook to transform raw statistics data into display format.
 */
export const useStatsData = (
  organizations: unknown[],
  paymentStats: PaymentStats | null | undefined,
  subscriptionStats: SubscriptionStats | null | undefined,
  paymentStatsLoading: boolean,
  subscriptionStatsLoading: boolean
): StatData[] => {
  return React.useMemo(
    () => [
      {
        name: DASHBOARD_CONSTANTS.STATS.ORGANIZATIONS,
        value: organizations?.length ?? 0,
        icon: ({ className }) => (
          <svg
            className={className}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-4m-5 0H3m2 0h4m-4 0v-4a1 1 0 011-1h2a1 1 0 011 1v4m-4 0V9a1 1 0 011-1h2a1 1 0 011 1v10M9 21h4"
            />
          </svg>
        ),
        colorTheme: 'PRIMARY',
        trend: '+12%',
      },
      {
        name: DASHBOARD_CONSTANTS.STATS.TOTAL_PAYMENTS,
        value: paymentStats?.totalSuccessfulPayments || 0,
        icon: ({ className }) => (
          <svg
            className={className}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
            />
          </svg>
        ),
        colorTheme: 'SUCCESS',
        trend: '+18%',
        isLoading: paymentStatsLoading,
      },
      {
        name: DASHBOARD_CONSTANTS.STATS.REVENUE,
        value:
          paymentStats && typeof paymentStats.totalAmount === 'number'
            ? `$${paymentStats.totalAmount.toFixed(2)}`
            : '$0.00',
        icon: ({ className }) => (
          <svg
            className={className}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
            />
          </svg>
        ),
        colorTheme: 'ACCENT',
        trend: '+32%',
        isLoading: paymentStatsLoading,
      },
      {
        name: DASHBOARD_CONSTANTS.STATS.SUBSCRIPTION,
        value: String(subscriptionStats?.status ?? 'Active'),
        icon: ({ className }) => (
          <svg
            className={className}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
        ),
        colorTheme: 'GRAY',
        trend: 'Current',
        isLoading: subscriptionStatsLoading,
      },
    ],
    [
      organizations,
      paymentStats,
      subscriptionStats,
      paymentStatsLoading,
      subscriptionStatsLoading,
    ]
  )
}
