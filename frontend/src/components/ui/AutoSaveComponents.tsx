import React from 'react'
import { format } from 'date-fns'
import {
  CloudIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ArrowPathIcon,
  ClockIcon,
} from '@heroicons/react/24/outline'
import { clsx } from 'clsx'
import type { AutoSaveStatus } from '../../hooks/useAutoSave'

interface AutoSaveIndicatorProps {
  status: AutoSaveStatus
  lastSaved?: Date | null
  error?: Error | null
  onRetry?: () => void
  className?: string
}

export const AutoSaveIndicator: React.FC<AutoSaveIndicatorProps> = ({
  status,
  lastSaved,
  error,
  onRetry,
  className,
}) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'saving':
        return {
          icon: ArrowPathIcon,
          text: 'Saving...',
          color: 'text-blue-500',
          bgColor: 'bg-blue-50',
          animate: 'animate-spin',
        }
      case 'saved':
        return {
          icon: CheckCircleIcon,
          text: lastSaved ? `Saved ${format(lastSaved, 'h:mm a')}` : 'Saved',
          color: 'text-green-500',
          bgColor: 'bg-green-50',
        }
      case 'error':
        return {
          icon: ExclamationTriangleIcon,
          text: 'Save failed',
          color: 'text-red-500',
          bgColor: 'bg-red-50',
        }
      default:
        return {
          icon: CloudIcon,
          text: 'Draft saved',
          color: 'text-gray-400',
          bgColor: 'bg-gray-50',
        }
    }
  }

  const config = getStatusConfig()
  const Icon = config.icon

  return (
    <div
      className={clsx(
        'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
        config.bgColor,
        config.color,
        className
      )}
    >
      <Icon className={clsx('h-3 w-3 mr-1.5', config.animate)} />
      <span>{config.text}</span>
      {status === 'error' && onRetry && (
        <button
          onClick={onRetry}
          className="ml-1.5 text-red-600 hover:text-red-800 underline"
        >
          retry
        </button>
      )}
      {error && (
        <div className="ml-1.5 text-xs text-red-400" title={error.message}>
          ({error.message.substring(0, 20)}...)
        </div>
      )}
    </div>
  )
}

interface UnsavedChangesWarningProps {
  hasUnsavedChanges: boolean
  onSave?: () => Promise<void>
  onDiscard?: () => void
  className?: string
}

export const UnsavedChangesWarning: React.FC<UnsavedChangesWarningProps> = ({
  hasUnsavedChanges,
  onSave,
  onDiscard,
  className,
}) => {
  const [isSaving, setIsSaving] = React.useState(false)

  const handleSave = async () => {
    if (!onSave) return

    setIsSaving(true)
    try {
      await onSave()
    } catch (error) {
      console.error('Manual save failed:', error)
    } finally {
      setIsSaving(false)
    }
  }

  if (!hasUnsavedChanges) {
    return null
  }

  return (
    <div
      className={clsx(
        'bg-yellow-50 border border-yellow-200 rounded-md p-3 mb-4',
        className
      )}
    >
      <div className="flex items-start">
        <ClockIcon className="h-4 w-4 text-yellow-600 mt-0.5 mr-2 flex-shrink-0" />
        <div className="flex-1">
          <p className="text-sm text-yellow-700">
            You have unsaved changes that haven't been auto-saved yet.
          </p>
          <div className="mt-2 flex space-x-2">
            {onSave && (
              <button
                onClick={handleSave}
                disabled={isSaving}
                className="text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded hover:bg-yellow-200 disabled:opacity-50 transition-colors"
              >
                {isSaving ? 'Saving...' : 'Save now'}
              </button>
            )}
            {onDiscard && (
              <button
                onClick={onDiscard}
                className="text-xs text-yellow-700 hover:text-yellow-900 underline"
              >
                Discard changes
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

interface AutoSaveBadgeProps {
  status: AutoSaveStatus
  className?: string
}

export const AutoSaveBadge: React.FC<AutoSaveBadgeProps> = ({
  status,
  className,
}) => {
  if (status === 'idle') return null

  const getStatusConfig = () => {
    switch (status) {
      case 'saving':
        return {
          text: 'Saving',
          color: 'bg-blue-100 text-blue-800',
          pulse: true,
        }
      case 'saved':
        return {
          text: 'Saved',
          color: 'bg-green-100 text-green-800',
        }
      case 'error':
        return {
          text: 'Error',
          color: 'bg-red-100 text-red-800',
        }
      default:
        return {
          text: 'Draft',
          color: 'bg-gray-100 text-gray-800',
        }
    }
  }

  const config = getStatusConfig()

  return (
    <span
      className={clsx(
        'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium',
        config.color,
        config.pulse && 'animate-pulse',
        className
      )}
    >
      {config.text}
    </span>
  )
}