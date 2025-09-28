import React, { useState } from 'react'
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  XCircleIcon,
  XMarkIcon,
  ArrowPathIcon,
} from '@heroicons/react/24/outline'
// Animation replacements using CSS transitions

// Enhanced notification types
export type NotificationVariant = 'success' | 'error' | 'warning' | 'info' | 'loading'

export interface NotificationData {
  id: string
  variant: NotificationVariant
  title: string
  message?: string
  duration?: number
  persistent?: boolean
  actions?: Array<{
    label: string
    onClick: () => void
    variant?: 'primary' | 'secondary'
  }>
}

// Global notification context
const NotificationContext = React.createContext<{
  notifications: NotificationData[]
  addNotification: (notification: Omit<NotificationData, 'id'>) => string
  removeNotification: (id: string) => void
  updateNotification: (id: string, updates: Partial<NotificationData>) => void
}>({
  notifications: [],
  addNotification: () => '',
  removeNotification: () => {},
  updateNotification: () => {},
})

// Notification provider component
export const NotificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [notifications, setNotifications] = useState<NotificationData[]>([])

  const addNotification = (notification: Omit<NotificationData, 'id'>): string => {
    const id = Math.random().toString(36).substr(2, 9)
    const newNotification: NotificationData = {
      id,
      duration: 5000, // 5 seconds default
      ...notification,
    }

    setNotifications(prev => [...prev, newNotification])

    // Auto-remove non-persistent notifications
    if (!notification.persistent && notification.variant !== 'loading') {
      setTimeout(() => {
        removeNotification(id)
      }, newNotification.duration)
    }

    return id
  }

  const removeNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id))
  }

  const updateNotification = (id: string, updates: Partial<NotificationData>) => {
    setNotifications(prev =>
      prev.map(n => n.id === id ? { ...n, ...updates } : n)
    )
  }

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        addNotification,
        removeNotification,
        updateNotification,
      }}
    >
      {children}
      <NotificationContainer />
    </NotificationContext.Provider>
  )
}

// Hook to use notifications
export const useNotifications = () => {
  const context = React.useContext(NotificationContext)
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider')
  }
  return context
}

// Individual notification component
const NotificationItem: React.FC<{
  notification: NotificationData
  onRemove: () => void
}> = ({ notification, onRemove }) => {
  const getVariantConfig = () => {
    switch (notification.variant) {
      case 'success':
        return {
          icon: CheckCircleIcon,
          iconColor: 'text-green-600',
          bgColor: 'bg-green-50',
          borderColor: 'border-green-200',
          titleColor: 'text-green-800',
          messageColor: 'text-green-700',
        }
      case 'error':
        return {
          icon: XCircleIcon,
          iconColor: 'text-red-600',
          bgColor: 'bg-red-50',
          borderColor: 'border-red-200',
          titleColor: 'text-red-800',
          messageColor: 'text-red-700',
        }
      case 'warning':
        return {
          icon: ExclamationTriangleIcon,
          iconColor: 'text-yellow-600',
          bgColor: 'bg-yellow-50',
          borderColor: 'border-yellow-200',
          titleColor: 'text-yellow-800',
          messageColor: 'text-yellow-700',
        }
      case 'loading':
        return {
          icon: ArrowPathIcon,
          iconColor: 'text-blue-600 animate-spin',
          bgColor: 'bg-blue-50',
          borderColor: 'border-blue-200',
          titleColor: 'text-blue-800',
          messageColor: 'text-blue-700',
        }
      default: // info
        return {
          icon: InformationCircleIcon,
          iconColor: 'text-blue-600',
          bgColor: 'bg-blue-50',
          borderColor: 'border-blue-200',
          titleColor: 'text-blue-800',
          messageColor: 'text-blue-700',
        }
    }
  }

  const config = getVariantConfig()
  const Icon = config.icon

  return (
    <div
      className={`
        max-w-sm w-full shadow-lg rounded-lg pointer-events-auto ring-1 ring-black ring-opacity-5 transform transition-all duration-300 ease-in-out
        ${config.bgColor} ${config.borderColor} border animate-slide-in-right
      `}
    >
      <div className="p-4">
        <div className="flex items-start">
          <div className="flex-shrink-0">
            <Icon className={`h-6 w-6 ${config.iconColor}`} />
          </div>

          <div className="ml-3 w-0 flex-1">
            <p className={`text-sm font-medium ${config.titleColor}`}>
              {notification.title}
            </p>
            {notification.message && (
              <p className={`mt-1 text-sm ${config.messageColor}`}>
                {notification.message}
              </p>
            )}

            {notification.actions && (
              <div className="mt-3 flex space-x-2">
                {notification.actions.map((action, index) => (
                  <button
                    key={index}
                    onClick={action.onClick}
                    className={`
                      text-sm font-medium rounded-md px-3 py-1.5 transition-colors
                      ${
                        action.variant === 'primary'
                          ? 'bg-white text-gray-900 hover:bg-gray-50 border border-gray-300'
                          : 'text-gray-700 hover:text-gray-500'
                      }
                    `}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            )}
          </div>

          {!notification.persistent && notification.variant !== 'loading' && (
            <div className="ml-4 flex-shrink-0 flex">
              <button
                onClick={onRemove}
                className={`
                  inline-flex rounded-md p-1.5 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500
                  ${config.messageColor} hover:${config.titleColor}
                `}
              >
                <XMarkIcon className="h-5 w-5" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// Notification container
const NotificationContainer: React.FC = () => {
  const { notifications, removeNotification } = useNotifications()

  return (
    <div className="fixed inset-0 flex items-end justify-center px-4 py-6 pointer-events-none sm:p-6 sm:items-start sm:justify-end z-50">
      <div className="w-full flex flex-col items-center space-y-4 sm:items-end">
        {notifications.map(notification => (
          <NotificationItem
            key={notification.id}
            notification={notification}
            onRemove={() => removeNotification(notification.id)}
          />
        ))}
      </div>
    </div>
  )
}

// Helper functions for common notification types
export const createNotificationHelpers = () => {
  const { addNotification, updateNotification } = useNotifications()

  const showSuccess = (title: string, message?: string) =>
    addNotification({ variant: 'success', title, ...(message ? { message } : {}) })

  const showError = (title: string, message?: string) =>
    addNotification({ variant: 'error', title, ...(message ? { message } : {}) })

  const showWarning = (title: string, message?: string) =>
    addNotification({ variant: 'warning', title, ...(message ? { message } : {}) })

  const showInfo = (title: string, message?: string) =>
    addNotification({ variant: 'info', title, ...(message ? { message } : {}) })

  const showLoading = (title: string, message?: string) =>
    addNotification({ variant: 'loading', title, persistent: true, ...(message ? { message } : {}) })

  const showActionable = (
    title: string,
    message: string,
    actions: Array<{ label: string; onClick: () => void; variant?: 'primary' | 'secondary' }>
  ) =>
    addNotification({
      variant: 'info',
      title,
      message,
      actions,
      persistent: true,
    })

  return {
    showSuccess,
    showError,
    showWarning,
    showInfo,
    showLoading,
    showActionable,
    updateNotification,
  }
}

// Progress indicator component
interface ProgressIndicatorProps {
  progress: number // 0-100
  variant?: 'primary' | 'success' | 'warning' | 'error'
  size?: 'sm' | 'md' | 'lg'
  showPercentage?: boolean
  label?: string
}

export const ProgressIndicator: React.FC<ProgressIndicatorProps> = ({
  progress,
  variant = 'primary',
  size = 'md',
  showPercentage = true,
  label,
}) => {
  const getVariantConfig = () => {
    switch (variant) {
      case 'success':
        return 'bg-green-500'
      case 'warning':
        return 'bg-yellow-500'
      case 'error':
        return 'bg-red-500'
      default:
        return 'bg-blue-500'
    }
  }

  const getSizeConfig = () => {
    switch (size) {
      case 'sm':
        return 'h-1'
      case 'lg':
        return 'h-3'
      default:
        return 'h-2'
    }
  }

  return (
    <div className="w-full">
      {label && (
        <div className="flex justify-between text-sm text-gray-700 mb-1">
          <span>{label}</span>
          {showPercentage && <span>{progress}%</span>}
        </div>
      )}
      <div className={`w-full bg-gray-200 rounded-full ${getSizeConfig()}`}>
        <div
          className={`${getSizeConfig()} ${getVariantConfig()} rounded-full transition-all duration-500 ease-out`}
          style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
        />
      </div>
    </div>
  )
}

export default NotificationProvider