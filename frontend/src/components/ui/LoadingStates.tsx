import React from 'react'
import { clsx } from 'clsx'
import LoadingSpinner from './LoadingSpinner'

// Standard loading button component
interface LoadingButtonProps {
  isLoading?: boolean
  disabled?: boolean
  loadingText?: string
  children: React.ReactNode
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  className?: string
  type?: 'button' | 'submit' | 'reset'
  onClick?: () => void
  'data-testid'?: string
}

export const LoadingButton: React.FC<LoadingButtonProps> = ({
  isLoading = false,
  disabled,
  loadingText = 'Processing...',
  children,
  variant = 'primary',
  size = 'md',
  className = '',
  type = 'button',
  onClick,
  'data-testid': dataTestId,
}) => {
  const baseClasses = 'inline-flex items-center justify-center font-medium rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed'

  const variantClasses = {
    primary: 'bg-primary-600 text-white hover:bg-primary-700 focus:ring-primary-500',
    secondary: 'bg-gray-600 text-white hover:bg-gray-700 focus:ring-gray-500',
    outline: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus:ring-primary-500',
    ghost: 'text-gray-700 hover:bg-gray-50 focus:ring-primary-500',
  }

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base',
  }

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={isLoading || disabled}
      data-testid={dataTestId}
      className={clsx(
        baseClasses,
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
    >
      {isLoading ? (
        <>
          <div className="animate-spin -ml-1 mr-2 h-4 w-4 border-2 border-current border-t-transparent rounded-full" />
          {loadingText}
        </>
      ) : (
        children
      )}
    </button>
  )
}

// Standard loading overlay for forms
interface LoadingOverlayProps {
  isLoading: boolean
  message?: string
  className?: string
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  isLoading,
  message = 'Loading...',
  className = '',
}) => {
  if (!isLoading) return null

  return (
    <div className={clsx(
      'absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-50',
      className
    )}>
      <div className="flex flex-col items-center">
        <LoadingSpinner size="lg" />
        <p className="mt-2 text-sm text-gray-600">{message}</p>
      </div>
    </div>
  )
}

// Standard loading card for content areas
interface LoadingCardProps {
  title?: string
  message?: string
  className?: string
}

export const LoadingCard: React.FC<LoadingCardProps> = ({
  title = 'Loading',
  message = 'Please wait while we load your data...',
  className = '',
}) => {
  return (
    <div className={clsx(
      'bg-white border border-gray-200 rounded-lg p-6 text-center animate-pulse',
      className
    )}>
      <div className="flex justify-center mb-4">
        <LoadingSpinner size="lg" />
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-2">{title}</h3>
      <p className="text-sm text-gray-600">{message}</p>
    </div>
  )
}

// Skeleton loading components
interface SkeletonProps {
  className?: string
  variant?: 'text' | 'circular' | 'rectangular'
  width?: string | number
  height?: string | number
  animation?: 'pulse' | 'wave'
}

export const Skeleton: React.FC<SkeletonProps> = ({
  className = '',
  variant = 'text',
  width,
  height,
  animation = 'pulse',
}) => {
  const baseClasses = clsx(
    'bg-gray-200',
    animation === 'pulse' && 'animate-pulse',
    animation === 'wave' && 'animate-pulse',
  )

  const variantClasses = {
    text: 'h-4 rounded',
    circular: 'rounded-full',
    rectangular: 'rounded',
  }

  const style = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
  }

  return (
    <div
      className={clsx(baseClasses, variantClasses[variant], className)}
      style={style}
    />
  )
}

// Table loading skeleton
export const TableSkeleton: React.FC<{ rows?: number; columns?: number }> = ({
  rows = 5,
  columns = 4,
}) => {
  return (
    <div className="animate-pulse">
      <div className="bg-gray-50 border-b border-gray-200 p-4">
        <div className="flex space-x-4">
          {Array.from({ length: columns }).map((_, index) => (
            <Skeleton key={index} className="h-4 flex-1" />
          ))}
        </div>
      </div>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="border-b border-gray-200 p-4">
          <div className="flex space-x-4">
            {Array.from({ length: columns }).map((_, colIndex) => (
              <Skeleton key={colIndex} className="h-4 flex-1" />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

// List loading skeleton
export const ListSkeleton: React.FC<{ items?: number }> = ({ items = 3 }) => {
  return (
    <div className="space-y-4">
      {Array.from({ length: items }).map((_, index) => (
        <div key={index} className="flex items-center space-x-4 p-4 border border-gray-200 rounded-lg animate-pulse">
          <Skeleton variant="circular" width={48} height={48} />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
          <Skeleton className="h-8 w-16" />
        </div>
      ))}
    </div>
  )
}

// Card loading skeleton
export const CardSkeleton: React.FC<{ className?: string }> = ({ className = '' }) => {
  return (
    <div className={clsx('p-4 border border-gray-200 rounded-lg animate-pulse', className)}>
      <div className="flex items-center space-x-3 mb-4">
        <Skeleton variant="circular" width={32} height={32} />
        <Skeleton className="h-5 w-32" />
      </div>
      <div className="space-y-2">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-4/5" />
        <Skeleton className="h-4 w-3/5" />
      </div>
      <div className="mt-4 flex justify-between">
        <Skeleton className="h-8 w-20" />
        <Skeleton className="h-8 w-16" />
      </div>
    </div>
  )
}

// Inline loading indicator
interface InlineLoadingProps {
  size?: 'sm' | 'md' | 'lg'
  text?: string
  className?: string
}

export const InlineLoading: React.FC<InlineLoadingProps> = ({
  size = 'sm',
  text,
  className = '',
}) => {
  const spinnerSizes = {
    sm: 'h-3 w-3',
    md: 'h-4 w-4',
    lg: 'h-5 w-5',
  }

  return (
    <div className={clsx('flex items-center', className)}>
      <div className={clsx(
        'animate-spin border-2 border-current border-t-transparent rounded-full',
        spinnerSizes[size]
      )} />
      {text && (
        <span className="ml-2 text-sm text-gray-600">{text}</span>
      )}
    </div>
  )
}

// Loading state hook for consistent loading management
export const useLoadingState = (initialState = false) => {
  const [isLoading, setIsLoading] = React.useState(initialState)

  const startLoading = React.useCallback(() => setIsLoading(true), [])
  const stopLoading = React.useCallback(() => setIsLoading(false), [])
  const toggleLoading = React.useCallback(() => setIsLoading(prev => !prev), [])

  return {
    isLoading,
    startLoading,
    stopLoading,
    toggleLoading,
    setIsLoading,
  }
}