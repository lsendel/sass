import React from 'react'
import {
  ExclamationTriangleIcon,
  WifiIcon,
  ServerIcon,
  XCircleIcon,
  ArrowPathIcon,
  HomeIcon,
} from '@heroicons/react/24/outline'

import { getCardClasses } from '../../lib/theme'

import { Button } from './button'

export interface ErrorStateProps {
  title?: string
  message?: string
  action?: {
    label: string
    onClick: () => void
  }
  showRetry?: boolean
  onRetry?: () => void
  variant?: 'error' | 'network' | 'server' | 'not-found' | 'empty'
}

const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Something went wrong',
  message = 'An unexpected error occurred. Please try again.',
  action,
  showRetry = true,
  onRetry,
  variant = 'error',
}) => {
  const getVariantConfig = () => {
    switch (variant) {
      case 'network':
        return {
          icon: WifiIcon,
          iconColor: 'text-orange-600',
          bgColor: 'bg-orange-100',
          title: 'Network Error',
          message: 'Unable to connect to the server. Please check your internet connection.',
        }
      case 'server':
        return {
          icon: ServerIcon,
          iconColor: 'text-red-600',
          bgColor: 'bg-red-100',
          title: 'Server Error',
          message: 'The server is currently unavailable. Please try again later.',
        }
      case 'not-found':
        return {
          icon: XCircleIcon,
          iconColor: 'text-gray-600',
          bgColor: 'bg-gray-100',
          title: 'Not Found',
          message: 'The requested resource could not be found.',
        }
      case 'empty':
        return {
          icon: ExclamationTriangleIcon,
          iconColor: 'text-gray-500',
          bgColor: 'bg-gray-100',
          title: 'No Data',
          message: 'No data available to display.',
        }
      default:
        return {
          icon: ExclamationTriangleIcon,
          iconColor: 'text-red-600',
          bgColor: 'bg-red-100',
          title,
          message,
        }
    }
  }

  const config = getVariantConfig()
  const Icon = config.icon

  return (
    <div className={`${getCardClasses('subtle')} text-center py-12`}>
      <div className={`inline-flex items-center justify-center w-16 h-16 ${config.bgColor} rounded-full mb-6`}>
        <Icon className={`w-8 h-8 ${config.iconColor}`} />
      </div>

      <h3 className="text-xl font-semibold text-gray-900 mb-2">
        {config.title}
      </h3>

      <p className="text-gray-600 mb-6 max-w-md mx-auto">
        {config.message}
      </p>

      <div className="flex flex-col sm:flex-row gap-3 justify-center">
        {showRetry && onRetry && (
          <Button onClick={onRetry} variant="primary" size="md">
            <ArrowPathIcon className="w-4 h-4 mr-2" />
            Try Again
          </Button>
        )}

        {action && (
          <Button onClick={action.onClick} variant="secondary" size="md">
            {action.label}
          </Button>
        )}

        {variant === 'not-found' && (
          <Button onClick={() => window.location.href = '/'} variant="secondary" size="md">
            <HomeIcon className="w-4 h-4 mr-2" />
            Go Home
          </Button>
        )}
      </div>
    </div>
  )
}

// Specific error state components
export const NetworkError: React.FC<Omit<ErrorStateProps, 'variant'>> = (props) => (
  <ErrorState {...props} variant="network" />
)

export const ServerError: React.FC<Omit<ErrorStateProps, 'variant'>> = (props) => (
  <ErrorState {...props} variant="server" />
)

export const NotFoundError: React.FC<Omit<ErrorStateProps, 'variant'>> = (props) => (
  <ErrorState {...props} variant="not-found" />
)

export const EmptyState: React.FC<Omit<ErrorStateProps, 'variant'>> = (props) => (
  <ErrorState {...props} variant="empty" />
)

// API Error handler component
interface ApiErrorDisplayProps {
  error: unknown
  onRetry?: () => void
  fallbackMessage?: string
}

export const ApiErrorDisplay: React.FC<ApiErrorDisplayProps> = ({
  error,
  onRetry,
  fallbackMessage = 'Failed to load data. Please try again.',
}) => {
  // Parse different error types
  const getErrorDetails = () => {
    if (!error) {
      return { message: fallbackMessage, variant: 'error' as const }
    }

    // RTK Query error
    if (typeof error === 'object' && 'status' in error) {
      const rtqError = error as { status: number | string; data?: any }

      switch (rtqError.status) {
        case 404:
          return { variant: 'not-found' as const }
        case 500:
        case 503:
          return { variant: 'server' as const }
        case 'FETCH_ERROR':
          return { variant: 'network' as const }
        default:
          return {
            message: rtqError.data?.message || fallbackMessage,
            variant: 'error' as const
          }
      }
    }

    // Standard Error object
    if (error instanceof Error) {
      return { message: error.message, variant: 'error' as const }
    }

    return { message: fallbackMessage, variant: 'error' as const }
  }

  const errorDetails = getErrorDetails()

  return (
    <ErrorState
      {...errorDetails}
      onRetry={onRetry}
      showRetry={!!onRetry}
    />
  )
}

export default ErrorState