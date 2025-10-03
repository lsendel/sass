import { useCallback } from 'react'

import {
  useNotifications,
  createNotificationHelpers,
} from '../components/ui/FeedbackSystem'

import {
  useOptimisticUpdates,
  type OptimisticUpdateOptions,
} from './useOptimisticUpdates'

/**
 * Enhanced hook that integrates optimistic updates with the notification system
 * for better user feedback and error handling
 */
export const useOptimisticNotifications = <T>() => {
  const notifications = useNotifications()
  const optimisticUpdates = useOptimisticUpdates<T>()
  const helpers = createNotificationHelpers()

  const addOptimisticUpdateWithNotifications = useCallback(
    async <R>(
      data: T,
      mutationFn: (data: T) => Promise<R>,
      options: OptimisticUpdateOptions<T> & {
        loadingTitle?: string
        loadingMessage?: string
        successTitle?: string
        successMessage?: string
        errorTitle?: string
        showLoadingNotification?: boolean
        showSuccessNotification?: boolean
        autoCloseLoading?: boolean
      } = {}
    ): Promise<R | null> => {
      const {
        loadingTitle = 'Processing...',
        loadingMessage,
        successTitle = 'Success!',
        successMessage,
        errorTitle = 'Operation failed',
        showLoadingNotification = true,
        showSuccessNotification = true,
        autoCloseLoading = true,
        ...optimisticOptions
      } = options

      let loadingNotificationId: string | null = null

      // Show loading notification
      if (showLoadingNotification) {
        loadingNotificationId = helpers.showLoading(
          loadingTitle,
          loadingMessage
        )
      }

      try {
        const result = await optimisticUpdates.addOptimisticUpdate(
          data,
          mutationFn,
          {
            ...optimisticOptions,
            onSuccess: data => {
              // Close loading notification
              if (loadingNotificationId && autoCloseLoading) {
                notifications.removeNotification(loadingNotificationId)
              }

              // Show success notification
              if (showSuccessNotification) {
                helpers.showSuccess(successTitle, successMessage)
              }

              // Call original success callback
              optimisticOptions.onSuccess?.(data)
            },
            onError: (error, rollbackData) => {
              // Close loading notification
              if (loadingNotificationId && autoCloseLoading) {
                notifications.removeNotification(loadingNotificationId)
              }

              // Show error notification with rollback action
              helpers.showActionable(
                errorTitle,
                error.message || 'An unexpected error occurred',
                [
                  {
                    label: 'Retry',
                    onClick: () => {
                      // Retry the operation
                      void addOptimisticUpdateWithNotifications(
                        data,
                        mutationFn,
                        options
                      )
                    },
                    variant: 'primary',
                  },
                  {
                    label: 'Undo',
                    onClick: () => {
                      // Trigger rollback through original callback
                      optimisticOptions.onError?.(error, rollbackData)
                    },
                    variant: 'secondary',
                  },
                ]
              )
            },
          }
        )

        return result
      } catch (error) {
        // Close loading notification on immediate error
        if (loadingNotificationId && autoCloseLoading) {
          notifications.removeNotification(loadingNotificationId)
        }
        throw error
      }
    },
    [optimisticUpdates, notifications, helpers]
  )

  const updateLoadingNotification = useCallback(
    (notificationId: string, progress: number, message?: string) => {
      notifications.updateNotification(notificationId, {
        title: `Processing... ${progress}%`,
        ...(message ? { message } : {}),
      })
    },
    [notifications]
  )

  return {
    ...optimisticUpdates,
    addOptimisticUpdateWithNotifications,
    updateLoadingNotification,
    notifications,
    ...helpers,
  }
}

/**
 * Hook for enhanced form submission with integrated notifications
 */
export const useFormSubmissionNotifications = () => {
  const helpers = createNotificationHelpers()

  const submitFormWithNotifications = useCallback(
    async <T, R>(
      data: T,
      submitFn: (data: T) => Promise<R>,
      options: {
        loadingTitle?: string
        successTitle?: string
        successMessage?: string
        errorTitle?: string
        onSuccess?: (result: R) => void
        onError?: (error: Error) => void
        validateBeforeSubmit?: (data: T) => string | null
      } = {}
    ): Promise<R | null> => {
      const {
        loadingTitle = 'Submitting...',
        successTitle = 'Submitted successfully!',
        successMessage,
        errorTitle = 'Submission failed',
        onSuccess,
        onError,
        validateBeforeSubmit,
      } = options

      // Pre-submission validation
      if (validateBeforeSubmit) {
        const validationError = validateBeforeSubmit(data)
        if (validationError) {
          helpers.showError('Validation Error', validationError)
          return null
        }
      }

      const loadingId = helpers.showLoading(loadingTitle)

      try {
        const result = await submitFn(data)

        helpers.updateNotification(loadingId, {
          variant: 'success',
          title: successTitle,
          ...(successMessage ? { message: successMessage } : {}),
          persistent: false,
          duration: 4000,
        })

        onSuccess?.(result)
        return result
      } catch (error) {
        const err = error as Error

        helpers.updateNotification(loadingId, {
          variant: 'error',
          title: errorTitle,
          message: err.message || 'An unexpected error occurred',
          persistent: false,
          duration: 6000,
        })

        onError?.(err)
        return null
      }
    },
    [helpers]
  )

  return {
    submitFormWithNotifications,
    ...helpers,
  }
}

/**
 * Hook for batch operations with progress tracking
 */
export const useBatchOperationNotifications = () => {
  const helpers = createNotificationHelpers()
  const notifications = useNotifications()

  const executeBatchOperation = useCallback(
    async <T, R>(
      items: T[],
      operationFn: (item: T, index: number) => Promise<R>,
      options: {
        batchTitle?: string
        successTitle?: string
        errorTitle?: string
        showProgress?: boolean
        onItemComplete?: (item: T, result: R, index: number) => void
        onItemError?: (item: T, error: Error, index: number) => void
        onComplete?: (results: Array<R | null>) => void
      } = {}
    ): Promise<Array<R | null>> => {
      const {
        batchTitle = 'Processing items...',
        successTitle = 'Batch operation completed',
        errorTitle = 'Batch operation completed with errors',
        showProgress = true,
        onItemComplete,
        onItemError,
        onComplete,
      } = options

      if (items.length === 0) {
        helpers.showInfo('No items to process')
        return []
      }

      const notificationId = helpers.showLoading(
        batchTitle,
        `Processing ${items.length} items`
      )
      const results: Array<R | null> = []
      let completed = 0
      let errors = 0

      for (let i = 0; i < items.length; i++) {
        try {
          const result = await operationFn(items[i], i)
          results.push(result)
          completed++
          onItemComplete?.(items[i], result, i)
        } catch (error) {
          const err = error as Error
          results.push(null)
          errors++
          onItemError?.(items[i], err, i)
        }

        // Update progress
        if (showProgress) {
          const progress = Math.round(((i + 1) / items.length) * 100)
          notifications.updateNotification(notificationId, {
            title: `${batchTitle} ${progress}%`,
            message: `${completed} completed, ${errors} errors`,
          })
        }
      }

      // Show final result
      const hasErrors = errors > 0
      notifications.updateNotification(notificationId, {
        variant: hasErrors ? 'warning' : 'success',
        title: hasErrors ? errorTitle : successTitle,
        message: `${completed} completed successfully${hasErrors ? `, ${errors} failed` : ''}`,
        persistent: false,
        duration: 5000,
        ...(hasErrors && {
          actions: [
            {
              label: 'View Details',
              onClick: () => {
                helpers.showInfo(
                  'Operation Details',
                  `Completed: ${completed}\nFailed: ${errors}\nTotal: ${items.length}`
                )
              },
              variant: 'primary',
            },
          ],
        }),
      })

      onComplete?.(results)
      return results
    },
    [helpers, notifications]
  )

  return {
    executeBatchOperation,
    ...helpers,
  }
}
