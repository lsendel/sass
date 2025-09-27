import { useState, useCallback, useRef } from 'react'
import { toast } from 'react-hot-toast'

import { logger } from '../utils/logger'

export interface OptimisticUpdateOptions<T> {
  onSuccess?: (data: T) => void
  onError?: (error: Error, rollbackData: T) => void
  successMessage?: string
  errorMessage?: string
  rollbackDelay?: number
}

export interface OptimisticUpdate<T> {
  id: string
  data: T
  timestamp: number
  status: 'pending' | 'confirmed' | 'failed' | 'rolledBack'
}

export const useOptimisticUpdates = <T>() => {
  const [optimisticUpdates, setOptimisticUpdates] = useState<Array<OptimisticUpdate<T>>>([])
  const timeoutRefs = useRef<Record<string, NodeJS.Timeout>>({})

  const addOptimisticUpdate = useCallback(
    async <R>(
      data: T,
      mutationFn: (data: T) => Promise<R>,
      options: OptimisticUpdateOptions<T> = {}
    ): Promise<R | null> => {
      const {
        onSuccess,
        onError,
        successMessage,
        errorMessage = 'Operation failed',
        rollbackDelay = 5000
      } = options

      const updateId = `optimistic-${Date.now()}-${Math.random()}`
      const update: OptimisticUpdate<T> = {
        id: updateId,
        data,
        timestamp: Date.now(),
        status: 'pending'
      }

      // Add optimistic update immediately
      setOptimisticUpdates(prev => [...prev, update])

      try {
        // Execute the actual mutation
        const result = await mutationFn(data)

        // Mark as confirmed
        setOptimisticUpdates(prev =>
          prev.map(u =>
            u.id === updateId
              ? { ...u, status: 'confirmed' as const }
              : u
          )
        )

        // Show success message
        if (successMessage) {
          toast.success(successMessage)
        }

        // Call success callback
        onSuccess?.(data)

        // Clean up confirmed update after a delay
        setTimeout(() => {
          setOptimisticUpdates(prev =>
            prev.filter(u => u.id !== updateId)
          )
        }, 3000)

        return result
      } catch (error) {
        const err = error as Error
        logger.error('Optimistic update failed:', err)

        // Mark as failed
        setOptimisticUpdates(prev =>
          prev.map(u =>
            u.id === updateId
              ? { ...u, status: 'failed' as const }
              : u
          )
        )

        // Show error message with rollback option
        toast.error(
          `${errorMessage} Click to undo.`,
          {
            duration: rollbackDelay,
            onClick: () => rollbackUpdate(updateId, data, onError)
          }
        )

        // Auto-rollback after delay
        timeoutRefs.current[updateId] = setTimeout(() => {
          rollbackUpdate(updateId, data, onError)
        }, rollbackDelay)

        return null
      }
    },
    []
  )

  const rollbackUpdate = useCallback(
    (updateId: string, data: T, onError?: (error: Error, rollbackData: T) => void) => {
      // Clear timeout if it exists
      if (timeoutRefs.current[updateId]) {
        clearTimeout(timeoutRefs.current[updateId])
        delete timeoutRefs.current[updateId]
      }

      // Mark as rolled back
      setOptimisticUpdates(prev =>
        prev.map(u =>
          u.id === updateId
            ? { ...u, status: 'rolledBack' as const }
            : u
        )
      )

      // Call error callback for rollback logic
      onError?.(new Error('Operation rolled back'), data)

      toast.success('Changes have been reverted')

      // Clean up after rollback
      setTimeout(() => {
        setOptimisticUpdates(prev =>
          prev.filter(u => u.id !== updateId)
        )
      }, 1000)
    },
    []
  )

  const cancelOptimisticUpdate = useCallback((updateId: string) => {
    if (timeoutRefs.current[updateId]) {
      clearTimeout(timeoutRefs.current[updateId])
      delete timeoutRefs.current[updateId]
    }

    setOptimisticUpdates(prev =>
      prev.filter(u => u.id !== updateId)
    )
  }, [])

  const getPendingUpdates = useCallback(() => {
    return optimisticUpdates.filter(u => u.status === 'pending')
  }, [optimisticUpdates])

  const getFailedUpdates = useCallback(() => {
    return optimisticUpdates.filter(u => u.status === 'failed')
  }, [optimisticUpdates])

  const hasOptimisticUpdates = optimisticUpdates.length > 0

  const optimisticData = optimisticUpdates.filter(u =>
    u.status === 'pending' || u.status === 'confirmed'
  ).map(u => u.data)

  return {
    optimisticUpdates,
    addOptimisticUpdate,
    rollbackUpdate,
    cancelOptimisticUpdate,
    getPendingUpdates,
    getFailedUpdates,
    hasOptimisticUpdates,
    optimisticData
  }
}

// Helper hook for optimistic list operations
export const useOptimisticList = <T extends { id: string }>(
  initialData: T[] = []
) => {
  const [baseData, setBaseData] = useState<T[]>(initialData)
  const { addOptimisticUpdate, optimisticUpdates, hasOptimisticUpdates } = useOptimisticUpdates<{
    type: 'add' | 'update' | 'delete'
    item: T
    index?: number
  }>()

  // Compute the optimistic list by applying pending updates
  const optimisticList = baseData.slice()

  optimisticUpdates
    .filter(update => update.status === 'pending' || update.status === 'confirmed')
    .forEach(update => {
      const { type, item, index } = update.data

      switch (type) {
        case 'add':
          if (index !== undefined) {
            optimisticList.splice(index, 0, item)
          } else {
            optimisticList.unshift(item) // Add to beginning by default
          }
          break
        case 'update':
          const updateIndex = optimisticList.findIndex(i => i.id === item.id)
          if (updateIndex !== -1) {
            optimisticList[updateIndex] = item
          }
          break
        case 'delete':
          const deleteIndex = optimisticList.findIndex(i => i.id === item.id)
          if (deleteIndex !== -1) {
            optimisticList.splice(deleteIndex, 1)
          }
          break
      }
    })

  const addItem = useCallback(
    async (item: T, mutationFn: (item: T) => Promise<T>, index?: number) => {
      return addOptimisticUpdate(
        { type: 'add', item, index },
        async () => mutationFn(item),
        {
          successMessage: 'Item added successfully',
          onError: () => {
            // Remove from optimistic list on error
            setBaseData(prev => prev.filter(i => i.id !== item.id))
          }
        }
      )
    },
    [addOptimisticUpdate]
  )

  const updateItem = useCallback(
    async (item: T, mutationFn: (item: T) => Promise<T>) => {
      const originalItem = baseData.find(i => i.id === item.id)

      return addOptimisticUpdate(
        { type: 'update', item },
        async () => mutationFn(item),
        {
          successMessage: 'Item updated successfully',
          onError: () => {
            // Revert to original item on error
            if (originalItem) {
              setBaseData(prev =>
                prev.map(i => i.id === item.id ? originalItem : i)
              )
            }
          }
        }
      )
    },
    [addOptimisticUpdate, baseData]
  )

  const deleteItem = useCallback(
    async (item: T, mutationFn: (id: string) => Promise<void>) => {
      return addOptimisticUpdate(
        { type: 'delete', item },
        async () => mutationFn(item.id),
        {
          successMessage: 'Item deleted successfully',
          onError: () => {
            // Restore item on error
            setBaseData(prev => [...prev, item])
          }
        }
      )
    },
    [addOptimisticUpdate]
  )

  const setData = useCallback((data: T[]) => {
    setBaseData(data)
  }, [])

  return {
    data: optimisticList,
    baseData,
    setData,
    addItem,
    updateItem,
    deleteItem,
    hasOptimisticUpdates,
    optimisticUpdates
  }
}