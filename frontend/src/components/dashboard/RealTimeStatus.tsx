import React from 'react'
import clsx from 'clsx'

interface RealTimeStatusProps {
  isActive: boolean
  onRefresh: () => void
  className?: string
  lastUpdated?: Date
}

/**
 * Component displaying real-time update status and manual refresh controls.
 * Provides visual feedback about data freshness and update state.
 */
export const RealTimeStatus: React.FC<RealTimeStatusProps> = ({
  isActive,
  onRefresh,
  className,
  lastUpdated,
}) => {
  const formatLastUpdated = (date: Date) => {
    const now = new Date()
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000)

    if (diffInSeconds < 60) {
      return 'Just now'
    } else if (diffInSeconds < 3600) {
      const minutes = Math.floor(diffInSeconds / 60)
      return `${minutes}m ago`
    } else {
      const hours = Math.floor(diffInSeconds / 3600)
      return `${hours}h ago`
    }
  }

  return (
    <div className={clsx('text-right', className)}>
      <div className="text-xs text-gray-500 mb-1">
        {isActive ? (
          <span className="flex items-center justify-end">
            <div className="w-2 h-2 bg-green-500 rounded-full mr-1 animate-pulse" />
            <span className="text-green-600">Live updates active</span>
          </span>
        ) : (
          <span className="flex items-center justify-end">
            <div className="w-2 h-2 bg-gray-400 rounded-full mr-1" />
            <span className="text-gray-400">Updates paused</span>
          </span>
        )}
      </div>

      {lastUpdated && (
        <div className="text-xs text-gray-400 mb-1">
          Last updated: {formatLastUpdated(lastUpdated)}
        </div>
      )}

      <button
        onClick={onRefresh}
        className={clsx(
          'text-xs underline transition-colors duration-200',
          'text-primary-600 hover:text-primary-500',
          'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50 rounded'
        )}
      >
        Refresh now
      </button>
    </div>
  )
}

/**
 * Hook for managing real-time status state and actions.
 */
export const useRealTimeStatus = (onRefresh: () => Promise<void>) => {
  const [isRefreshing, setIsRefreshing] = React.useState(false)
  const [lastUpdated, setLastUpdated] = React.useState<Date | undefined>()

  const handleRefresh = React.useCallback(async () => {
    if (isRefreshing) return

    setIsRefreshing(true)
    try {
      await onRefresh()
      setLastUpdated(new Date())
    } catch (error) {
      console.error('Failed to refresh:', error)
    } finally {
      setIsRefreshing(false)
    }
  }, [onRefresh, isRefreshing])

  return {
    isRefreshing,
    lastUpdated,
    handleRefresh,
  }
}
