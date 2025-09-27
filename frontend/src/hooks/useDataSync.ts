import { useEffect, useCallback, useRef } from 'react'

import { useAppDispatch } from '../store/hooks'

interface DataSyncOptions {
  /**
   * Keys of data that should trigger sync when changed
   */
  dependencies: string[]

  /**
   * Function to sync data across components
   */
  onSync: (data: Record<string, any>) => Promise<void> | void

  /**
   * Interval in milliseconds for periodic sync (optional)
   */
  interval?: number

  /**
   * Whether sync is enabled
   */
  enabled?: boolean

  /**
   * Debounce delay in milliseconds
   */
  debounceMs?: number
}

/**
 * Custom hook for synchronizing data across components
 * Helps maintain consistency when data changes in one component affect others
 */
export const useDataSync = (options: DataSyncOptions) => {
  const { dependencies, onSync, interval, enabled = true, debounceMs = 300 } = options
  const dispatch = useAppDispatch()
  const lastSyncRef = useRef<Record<string, any>>({})
  const timeoutRef = useRef<NodeJS.Timeout | null>(null)
  const intervalRef = useRef<NodeJS.Timeout | null>(null)

  const debouncedSync = useCallback((data: Record<string, any>) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    timeoutRef.current = setTimeout(() => {
      void onSync(data)
    }, debounceMs)
  }, [onSync, debounceMs])

  const triggerSync = useCallback((data: Record<string, any>) => {
    if (!enabled) return

    // Check if data has actually changed
    const hasChanged = dependencies.some(key => {
      const current = data[key]
      const previous = lastSyncRef.current[key]

      return JSON.stringify(current) !== JSON.stringify(previous)
    })

    if (hasChanged) {
      lastSyncRef.current = { ...data }
      debouncedSync(data)
    }
  }, [dependencies, enabled, debouncedSync])

  // Set up periodic sync if interval is provided
  useEffect(() => {
    if (interval && enabled) {
      intervalRef.current = setInterval(() => {
        void onSync(lastSyncRef.current)
      }, interval)
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [interval, enabled, onSync])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [])

  return {
    triggerSync,
    lastSync: lastSyncRef.current,
  }
}

/**
 * Hook for cross-component state synchronization
 * Useful when multiple components need to stay in sync with shared data
 */
export const useCrossComponentSync = () => {
  const dispatch = useAppDispatch()

  const syncOrganizationData = useCallback(() => {
    // Trigger refetch of organization data across all components
    dispatch({ type: 'api/invalidateTags', payload: ['Organization'] })
  }, [dispatch])

  const syncPaymentData = useCallback(() => {
    // Trigger refetch of payment data across all components
    dispatch({ type: 'api/invalidateTags', payload: ['Payment', 'PaymentStatistics'] })
  }, [dispatch])

  const syncSubscriptionData = useCallback(() => {
    // Trigger refetch of subscription data across all components
    dispatch({ type: 'api/invalidateTags', payload: ['Subscription'] })
  }, [dispatch])

  const syncUserData = useCallback(() => {
    // Trigger refetch of user data across all components
    dispatch({ type: 'api/invalidateTags', payload: ['User'] })
  }, [dispatch])

  const syncAllData = useCallback(() => {
    // Sync all data types
    syncOrganizationData()
    syncPaymentData()
    syncSubscriptionData()
    syncUserData()
  }, [syncOrganizationData, syncPaymentData, syncSubscriptionData, syncUserData])

  return {
    syncOrganizationData,
    syncPaymentData,
    syncSubscriptionData,
    syncUserData,
    syncAllData,
  }
}

/**
 * Hook for optimistic UI updates
 * Updates UI immediately while syncing with server in background
 */
export const useOptimisticUpdates = <T>() => {
  const optimisticStateRef = useRef<Map<string, T>>(new Map())

  const addOptimisticUpdate = useCallback((key: string, data: T) => {
    optimisticStateRef.current.set(key, data)
  }, [])

  const removeOptimisticUpdate = useCallback((key: string) => {
    optimisticStateRef.current.delete(key)
  }, [])

  const getOptimisticState = useCallback((key: string): T | undefined => {
    return optimisticStateRef.current.get(key)
  }, [])

  const clearOptimisticUpdates = useCallback(() => {
    optimisticStateRef.current.clear()
  }, [])

  return {
    addOptimisticUpdate,
    removeOptimisticUpdate,
    getOptimisticState,
    clearOptimisticUpdates,
  }
}