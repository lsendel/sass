import React from 'react'
import clsx from 'clsx'
import { ChartBarIcon } from '@heroicons/react/24/outline'

import { getCardClasses } from '../../lib/theme'
import { DASHBOARD_CONSTANTS } from '../../constants/dashboard'

interface QuickActionItem {
  title: string
  description: string
  href: string
  icon: React.ComponentType<{ className?: string }>
  colorTheme: keyof typeof DASHBOARD_CONSTANTS.COLORS
}

interface QuickActionsProps {
  actions: QuickActionItem[]
  className?: string
}

/**
 * Quick actions component for dashboard navigation shortcuts.
 * Provides easy access to common tasks and pages.
 */
export const QuickActions: React.FC<QuickActionsProps> = ({
  actions,
  className,
}) => {
  return (
    <div className={clsx(getCardClasses(), className)}>
      <div className="flex items-center mb-3">
        <div className="w-6 h-6 gradient-brand rounded-lg flex items-center justify-center mr-2">
          <ChartBarIcon className="w-3 h-3 text-white brand-element" />
        </div>
        <h3 className="text-base font-semibold text-gray-900 brand-element">
          Quick Actions
        </h3>
      </div>

      <div
        className={clsx(
          `grid grid-cols-${DASHBOARD_CONSTANTS.QUICK_ACTIONS_COLUMNS} gap-3`
        )}
      >
        {actions.map(action => (
          <QuickActionCard key={action.title} {...action} />
        ))}
      </div>
    </div>
  )
}

interface QuickActionCardProps extends QuickActionItem {}

/**
 * Individual quick action card component.
 */
const QuickActionCard: React.FC<QuickActionCardProps> = ({
  title,
  description,
  href,
  icon: Icon,
  colorTheme,
}) => {
  const colorConfig = DASHBOARD_CONSTANTS.COLORS[colorTheme]

  return (
    <a
      href={href}
      className={clsx(
        'block p-3 rounded-lg border border-gray-200 hover:border-gray-300',
        'transition-all duration-200 hover:shadow-sm group',
        'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50'
      )}
    >
      <div className="flex items-start space-x-2">
        <div
          className={clsx(
            'flex-shrink-0 w-8 h-8 rounded-lg flex items-center justify-center',
            colorConfig.BG,
            'group-hover:scale-105 transition-transform duration-200'
          )}
        >
          <Icon className={clsx('w-4 h-4', colorConfig.ICON)} />
        </div>
        <div className="min-w-0 flex-1">
          <h4 className="text-sm font-medium text-gray-900 group-hover:text-primary-600 transition-colors duration-200">
            {title}
          </h4>
          <p className="text-xs text-gray-500 mt-1 line-clamp-2">
            {description}
          </p>
        </div>
      </div>
    </a>
  )
}

/**
 * Default quick actions configuration.
 */
export const useDefaultQuickActions = (): QuickActionItem[] => {
  return React.useMemo(
    () => [
      {
        title: 'Organizations',
        description: 'Manage your organizations and team members',
        href: '/organizations',
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
      },
      {
        title: 'Payments',
        description: 'View payment history and manage transactions',
        href: '/payments',
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
      },
      {
        title: 'Subscription',
        description: 'Manage your subscription and billing',
        href: '/subscription',
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
        colorTheme: 'ACCENT',
      },
    ],
    []
  )
}
