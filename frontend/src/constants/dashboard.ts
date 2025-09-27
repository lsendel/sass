/**
 * Constants for dashboard configuration and UI elements.
 * Centralized constants for better maintainability and consistency.
 */

export const DASHBOARD_CONSTANTS = {
  // Real-time update configuration
  UPDATE_INTERVAL: 30 * 1000, // 30 seconds
  INACTIVITY_TIMEOUT: 5 * 60 * 1000, // 5 minutes
  FORCE_UPDATE_DEBOUNCE: 1000, // 1 second

  // Statistics configuration
  STATISTICS_PERIOD_DAYS: 30,
  DEFAULT_CURRENCY: 'USD',

  // UI Configuration
  STATS_GRID_COLUMNS: 4,
  QUICK_ACTIONS_COLUMNS: 3,

  // Statistics card names
  STATS: {
    ORGANIZATIONS: 'Organizations',
    TOTAL_PAYMENTS: 'Total Payments',
    REVENUE: 'Revenue',
    SUBSCRIPTION: 'Subscription',
  } as const,

  // Color classes for stats cards
  COLORS: {
    PRIMARY: {
      BG: 'bg-primary-50',
      ICON: 'text-primary-600',
    },
    SUCCESS: {
      BG: 'bg-success-50',
      ICON: 'text-success-600',
    },
    ACCENT: {
      BG: 'bg-accent-50',
      ICON: 'text-accent-600',
    },
    GRAY: {
      BG: 'bg-gray-50',
      ICON: 'text-gray-600',
    },
  } as const,

  // Trends
  TRENDS: {
    POSITIVE: '+',
    NEGATIVE: '-',
    NEUTRAL: '',
  } as const,

  // Loading states
  LOADING: {
    SKELETON_ROWS: 3,
    INLINE_SIZE: 'md' as const,
  },

  // Error messages
  ERRORS: {
    LOAD_FAILED: 'Failed to load dashboard data',
    REFRESH_FAILED: 'Failed to refresh data',
    NO_ORGANIZATION: 'No organization selected',
  } as const,
} as const;

/**
 * Type definitions derived from constants for better type safety.
 */
export type StatName = typeof DASHBOARD_CONSTANTS.STATS[keyof typeof DASHBOARD_CONSTANTS.STATS];
export type ColorTheme = keyof typeof DASHBOARD_CONSTANTS.COLORS;
export type TrendType = typeof DASHBOARD_CONSTANTS.TRENDS[keyof typeof DASHBOARD_CONSTANTS.TRENDS];

/**
 * Default values for dashboard components.
 */
export const DASHBOARD_DEFAULTS = {
  TREND: 'Current',
  CURRENCY_SYMBOL: '$',
  ZERO_VALUE: '0.00',
  EMPTY_STATE: 'No data available',
} as const;