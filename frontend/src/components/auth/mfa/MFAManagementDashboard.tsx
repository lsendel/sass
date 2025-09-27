/**
 * MFA Management Dashboard
 *
 * Comprehensive multi-factor authentication management interface:
 * - View and manage all MFA methods
 * - Security analytics and recommendations
 * - Backup codes management
 * - Trusted devices management
 * - Audit log and activity tracking
 * - Recovery options
 */

import React, { useState } from 'react'
import {
  ShieldCheckIcon,
  DevicePhoneMobileIcon,
  EnvelopeIcon,
  KeyIcon,
  FingerPrintIcon,
  QrCodeIcon,
  DocumentTextIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  PlusIcon,
  TrashIcon,
  ClockIcon,
  ChartBarIcon,
  Cog6ToothIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { clsx } from 'clsx'
import { toast } from 'react-hot-toast'

import {
  useGetMFAMethodsQuery,
  useGetMFAAnalyticsQuery,
  useGetMFAAuditLogQuery,
  useUpdateMFAMethodMutation,
  useDeleteMFAMethodMutation,
} from '../../../store/api/mfaApi'
import { Button } from '../../ui/button'
import { LoadingCard } from '../../ui/LoadingStates'
import { ApiErrorDisplay, EmptyState } from '../../ui/ErrorStates'
import { logger } from '../../../utils/logger'
import type { AnyMFAMethod, MFAMethodType } from '../../../types/mfa'

import TOTPSetup from './TOTPSetup'

interface MFAManagementDashboardProps {
  className?: string
}

type ActiveModal = 'none' | 'add-method' | 'backup-codes' | 'settings' | 'audit-log' | 'trusted-devices'

const MFAManagementDashboard: React.FC<MFAManagementDashboardProps> = ({ className }) => {
  const [activeModal, setActiveModal] = useState<ActiveModal>('none')

  // API queries
  const { data: methods, isLoading: methodsLoading, error: methodsError, refetch: refetchMethods } = useGetMFAMethodsQuery()
  const { data: analytics, isLoading: analyticsLoading } = useGetMFAAnalyticsQuery()
  const { data: auditLog, isLoading: auditLoading } = useGetMFAAuditLogQuery({ limit: 10 })

  // Mutations
  const [updateMethod] = useUpdateMFAMethodMutation()
  const [deleteMethod] = useDeleteMFAMethodMutation()

  const getMethodIcon = (type: MFAMethodType) => {
    switch (type) {
      case 'totp':
        return <QrCodeIcon className="w-5 h-5" />
      case 'sms':
        return <DevicePhoneMobileIcon className="w-5 h-5" />
      case 'email':
        return <EnvelopeIcon className="w-5 h-5" />
      case 'webauthn':
        return <KeyIcon className="w-5 h-5" />
      case 'biometric':
        return <FingerPrintIcon className="w-5 h-5" />
      case 'backup_codes':
        return <DocumentTextIcon className="w-5 h-5" />
      default:
        return <ShieldCheckIcon className="w-5 h-5" />
    }
  }

  const getMethodTypeLabel = (type: MFAMethodType): string => {
    switch (type) {
      case 'totp':
        return 'Authenticator App'
      case 'sms':
        return 'SMS'
      case 'email':
        return 'Email'
      case 'webauthn':
        return 'Security Key'
      case 'biometric':
        return 'Biometric'
      case 'backup_codes':
        return 'Backup Codes'
      default:
        return 'Unknown'
    }
  }

  const getSecurityLevelColor = (score: number) => {
    if (score >= 90) return 'text-green-600 bg-green-100'
    if (score >= 70) return 'text-yellow-600 bg-yellow-100'
    return 'text-red-600 bg-red-100'
  }

  const getSecurityLevelLabel = (score: number) => {
    if (score >= 90) return 'Excellent'
    if (score >= 70) return 'Good'
    if (score >= 50) return 'Fair'
    return 'Poor'
  }

  const handleToggleMethod = async (method: AnyMFAMethod) => {
    try {
      await updateMethod({
        methodId: method.id,
        updates: { isEnabled: !method.isEnabled }
      }).unwrap()

      toast.success(`${method.name} ${method.isEnabled ? 'disabled' : 'enabled'}`)
      void refetchMethods()
    } catch (error) {
      logger.error('Failed to toggle MFA method:', error)
      toast.error('Failed to update method. Please try again.')
    }
  }

  const handleDeleteMethod = async (method: AnyMFAMethod) => {
    if (!confirm(`Are you sure you want to remove "${method.name}"? This cannot be undone.`)) {
      return
    }

    try {
      await deleteMethod(method.id).unwrap()
      toast.success(`${method.name} removed successfully`)
      void refetchMethods()
    } catch (error) {
      logger.error('Failed to delete MFA method:', error)
      toast.error('Failed to remove method. Please try again.')
    }
  }



  const renderSecurityOverview = () => {
    if (analyticsLoading) {
      return <LoadingCard title="Loading Security Overview" />
    }

    const securityScore = analytics?.securityScore ?? 0
    const activeMethods = methods?.filter(m => m.isEnabled).length ?? 0

    return (
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-medium text-gray-900">Security Overview</h3>
          <ChartBarIcon className="w-5 h-5 text-gray-400" />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="text-center">
            <div className={clsx('inline-flex items-center px-3 py-1 rounded-full text-sm font-medium', getSecurityLevelColor(securityScore))}>
              {getSecurityLevelLabel(securityScore)}
            </div>
            <p className="text-2xl font-bold text-gray-900 mt-2">{securityScore}%</p>
            <p className="text-sm text-gray-500">Security Score</p>
          </div>

          <div className="text-center">
            <p className="text-2xl font-bold text-blue-600">{activeMethods}</p>
            <p className="text-sm text-gray-500">Active Methods</p>
          </div>

          <div className="text-center">
            <p className="text-2xl font-bold text-green-600">
              {analytics?.loginAttempts.successful ?? 0}
            </p>
            <p className="text-sm text-gray-500">Recent Logins</p>
          </div>
        </div>

        {analytics?.recommendations && analytics.recommendations.length > 0 && (
          <div className="mt-4 p-3 bg-blue-50 rounded-lg">
            <h4 className="text-sm font-medium text-blue-900 mb-2">Recommendations</h4>
            <ul className="text-sm text-blue-800 space-y-1">
              {analytics.recommendations.slice(0, 3).map((rec, index) => (
                <li key={index}>• {rec}</li>
              ))}
            </ul>
          </div>
        )}
      </div>
    )
  }

  const renderMFAMethods = () => {
    if (methodsLoading) {
      return <LoadingCard title="Loading MFA Methods" />
    }

    if (methodsError) {
      return <ApiErrorDisplay error={methodsError} onRetry={() => void refetchMethods()} />
    }

    if (!methods || methods.length === 0) {
      return (
        <EmptyState
          title="No MFA methods configured"
          message="Add your first authentication method to secure your account."
          action={{
            label: "Add MFA Method",
            onClick: () => setActiveModal('add-method')
          }}
        />
      )
    }

    return (
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900">Authentication Methods</h3>
            <Button
              onClick={() => setActiveModal('add-method')}
              size="sm"
            >
              <PlusIcon className="w-4 h-4 mr-1" />
              Add Method
            </Button>
          </div>
        </div>

        <div className="divide-y divide-gray-200">
          {methods.map((method) => (
            <div key={method.id} className="px-6 py-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <div className={clsx(
                    'flex items-center justify-center w-10 h-10 rounded-lg',
                    method.isEnabled ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'
                  )}>
                    {getMethodIcon(method.type)}
                  </div>
                  <div className="ml-4">
                    <div className="flex items-center">
                      <h4 className="text-sm font-medium text-gray-900">{method.name}</h4>
                      {method.isPrimary && (
                        <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                          Primary
                        </span>
                      )}
                      {!method.isEnabled && (
                        <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                          Disabled
                        </span>
                      )}
                    </div>
                    <div className="flex items-center mt-1 text-sm text-gray-500">
                      <span>{getMethodTypeLabel(method.type)}</span>
                      {method.lastUsed && (
                        <>
                          <span className="mx-2">•</span>
                          <span>Last used {format(new Date(method.lastUsed), 'MMM d, yyyy')}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => void handleToggleMethod(method)}
                  >
                    {method.isEnabled ? 'Disable' : 'Enable'}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => void handleDeleteMethod(method)}
                    className="text-red-600 hover:text-red-700"
                  >
                    <TrashIcon className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  const renderQuickActions = () => (
    <div className="bg-white shadow rounded-lg p-6">
      <h3 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Button
          variant="outline"
          onClick={() => setActiveModal('backup-codes')}
          className="justify-start"
        >
          <DocumentTextIcon className="w-4 h-4 mr-2" />
          Backup Codes
        </Button>
        <Button
          variant="outline"
          onClick={() => setActiveModal('settings')}
          className="justify-start"
        >
          <Cog6ToothIcon className="w-4 h-4 mr-2" />
          Settings
        </Button>
        <Button
          variant="outline"
          onClick={() => setActiveModal('trusted-devices')}
          className="justify-start"
        >
          <KeyIcon className="w-4 h-4 mr-2" />
          Trusted Devices
        </Button>
        <Button
          variant="outline"
          onClick={() => setActiveModal('audit-log')}
          className="justify-start"
        >
          <ClockIcon className="w-4 h-4 mr-2" />
          Activity Log
        </Button>
      </div>
    </div>
  )

  const renderRecentActivity = () => {
    if (auditLoading) {
      return <LoadingCard title="Loading Recent Activity" />
    }

    return (
      <div className="bg-white shadow rounded-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Recent Activity</h3>
        </div>

        {!auditLog || auditLog.length === 0 ? (
          <div className="p-6 text-center text-gray-500">
            <ClockIcon className="w-8 h-8 mx-auto mb-2 text-gray-400" />
            <p>No recent activity</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {auditLog.slice(0, 5).map((event) => (
              <div key={event.id} className="px-6 py-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center">
                    <div className={clsx(
                      'w-2 h-2 rounded-full mr-3',
                      event.success ? 'bg-green-400' : 'bg-red-400'
                    )} />
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {event.action.replace('_', ' ').charAt(0).toUpperCase() + event.action.slice(1)}
                      </p>
                      <p className="text-xs text-gray-500">
                        {format(new Date(event.timestamp), 'MMM d, yyyy h:mm a')}
                      </p>
                    </div>
                  </div>
                  {event.success ? (
                    <CheckCircleIcon className="w-4 h-4 text-green-500" />
                  ) : (
                    <ExclamationTriangleIcon className="w-4 h-4 text-red-500" />
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    )
  }

  const renderAddMethodModal = () => {
    if (activeModal !== 'add-method') return null

    return (
      <div className="fixed inset-0 z-50 bg-black bg-opacity-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div className="px-6 py-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-medium text-gray-900">Add MFA Method</h3>
              <button
                onClick={() => setActiveModal('none')}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>
          </div>

          <div className="p-6">
            <TOTPSetup
              onComplete={() => {
                toast.success('TOTP method added successfully!')
                setActiveModal('none')
                void refetchMethods()
              }}
              onCancel={() => setActiveModal('none')}
            />
          </div>
        </div>
      </div>
    )
  }

  if (methodsLoading) {
    return (
      <div className={clsx('space-y-6', className)}>
        <LoadingCard title="Loading MFA Dashboard" />
      </div>
    )
  }

  return (
    <div className={clsx('space-y-6', className)}>
      {/* Security Overview */}
      {renderSecurityOverview()}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* MFA Methods - 2 columns */}
        <div className="lg:col-span-2">
          {renderMFAMethods()}
        </div>

        {/* Sidebar - 1 column */}
        <div className="space-y-6">
          {renderQuickActions()}
          {renderRecentActivity()}
        </div>
      </div>

      {/* Modals */}
      {renderAddMethodModal()}
    </div>
  )
}

export default MFAManagementDashboard
