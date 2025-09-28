/**
 * Tenant Configuration Manager
 *
 * Comprehensive tenant configuration interface supporting:
 * - General tenant settings and preferences
 * - Security and compliance configuration
 * - Branding and customization options
 * - Integration settings and API configuration
 * - Billing and subscription management
 * - Resource quotas and limits management
 */

import React, { useState, useEffect } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  CogIcon,
  ShieldCheckIcon,
  PaintBrushIcon,
  CreditCardIcon,
  ChartBarIcon,
  LinkIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  InformationCircleIcon,
} from '@heroicons/react/24/outline'

import { useTenant } from './TenantProvider'

import {
  useGetTenantSettingsQuery,
  useUpdateTenantSettingsMutation,
  useUpdateTenantMutation,
  useGetTenantQuotasQuery,
  useUpdateTenantQuotasMutation,
} from '@/store/api/tenantApi'
// Types imported via API
import { TenantBrandingSchema } from '@/types/multitenancy'

// Icons

type ConfigurationTab = 'general' | 'security' | 'branding' | 'integrations' | 'billing' | 'quotas'

// Form schemas
const GeneralSettingsSchema = z.object({
  allowUserRegistration: z.boolean(),
  defaultUserRole: z.string(),
  emailVerificationRequired: z.boolean(),
  userInviteExpirationDays: z.number().min(1).max(365),
})

const SecuritySettingsSchema = z.object({
  passwordPolicy: z.object({
    minLength: z.number().min(8).max(128),
    requireUppercase: z.boolean(),
    requireLowercase: z.boolean(),
    requireNumbers: z.boolean(),
    requireSpecialChars: z.boolean(),
    preventReuse: z.number().min(0).max(24),
    expirationDays: z.number().min(1).max(365).optional(),
  }),
  sessionSettings: z.object({
    timeoutMinutes: z.number().min(5).max(1440),
    maxConcurrentSessions: z.number().min(1).max(50),
    requireReauthForSensitive: z.boolean(),
  }),
  auditSettings: z.object({
    enabled: z.boolean(),
    retentionDays: z.number().min(1).max(2555), // 7 years max
    logLevel: z.enum(['basic', 'detailed', 'comprehensive']),
    realTimeAlerts: z.boolean(),
  }),
})

const BrandingSettingsSchema = TenantBrandingSchema

const QuotaUpdateSchema = z.object({
  users: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  organizations: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  storage: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  apiCalls: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  bandwidth: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  customDomains: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  ssoConnections: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  webhooks: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  auditRetention: z.number().min(1).max(2555),
  concurrentSessions: z.object({
    limit: z.number().min(-1),
    softLimit: z.number().min(0).optional(),
  }),
  rateLimit: z.number().min(1),
})

interface TenantConfigurationManagerProps {
  className?: string
}

const TenantConfigurationManager: React.FC<TenantConfigurationManagerProps> = ({
  className = '',
}) => {
  const { tenant, tenantContext } = useTenant()
  const [activeTab, setActiveTab] = useState<ConfigurationTab>('general')
  const [isSaving, setIsSaving] = useState(false)
  const [saveStatus, setSaveStatus] = useState<'idle' | 'success' | 'error'>('idle')

  if (!tenant || !tenantContext) {
    return (
      <div className="text-center py-12">
        <ExclamationTriangleIcon className="mx-auto h-12 w-12 text-gray-400" />
        <h3 className="mt-2 text-sm font-semibold text-gray-900">No Tenant Context</h3>
        <p className="mt-1 text-sm text-gray-500">
          Tenant configuration requires a valid tenant context.
        </p>
      </div>
    )
  }

  const {
    data: tenantSettings,
    isLoading: isLoadingSettings,
    refetch: refetchSettings,
  } = useGetTenantSettingsQuery({ tenantId: tenant.id })

  const {
    data: tenantQuotas,
    isLoading: isLoadingQuotas,
    refetch: refetchQuotas,
  } = useGetTenantQuotasQuery({ tenantId: tenant.id })

  const [updateTenantSettings] = useUpdateTenantSettingsMutation()
  const [updateTenant] = useUpdateTenantMutation()
  const [updateTenantQuotas] = useUpdateTenantQuotasMutation()

  const tabs: Array<{ key: ConfigurationTab; label: string; icon: any; description: string }> = [
    {
      key: 'general',
      label: 'General',
      icon: CogIcon,
      description: 'Basic tenant settings and preferences',
    },
    {
      key: 'security',
      label: 'Security',
      icon: ShieldCheckIcon,
      description: 'Password policies and security settings',
    },
    {
      key: 'branding',
      label: 'Branding',
      icon: PaintBrushIcon,
      description: 'Visual customization and branding',
    },
    {
      key: 'integrations',
      label: 'Integrations',
      icon: LinkIcon,
      description: 'Third-party integrations and APIs',
    },
    {
      key: 'billing',
      label: 'Billing',
      icon: CreditCardIcon,
      description: 'Billing and subscription settings',
    },
    {
      key: 'quotas',
      label: 'Quotas',
      icon: ChartBarIcon,
      description: 'Resource limits and usage quotas',
    },
  ]

  // General settings form
  const generalForm = useForm({
    resolver: zodResolver(GeneralSettingsSchema),
    defaultValues: tenantSettings?.general || {
      allowUserRegistration: false,
      defaultUserRole: 'user',
      emailVerificationRequired: true,
      userInviteExpirationDays: 7,
    },
  })

  // Security settings form
  const securityForm = useForm({
    resolver: zodResolver(SecuritySettingsSchema),
    defaultValues: tenantSettings?.security || {
      passwordPolicy: {
        minLength: 12,
        requireUppercase: true,
        requireLowercase: true,
        requireNumbers: true,
        requireSpecialChars: true,
        preventReuse: 5,
        expirationDays: 90,
      },
      sessionSettings: {
        timeoutMinutes: 30,
        maxConcurrentSessions: 5,
        requireReauthForSensitive: true,
      },
      auditSettings: {
        enabled: true,
        retentionDays: 365,
        logLevel: 'detailed' as const,
        realTimeAlerts: false,
      },
    },
  })

  // Branding form
  const brandingForm = useForm({
    resolver: zodResolver(BrandingSettingsSchema),
    defaultValues: tenant.branding || {
      primaryColor: '#3B82F6',
      secondaryColor: '#1E40AF',
      logoUrl: '',
      faviconUrl: '',
      customCss: '',
      hideProviderBranding: false,
      customFooter: '',
      customEmailTemplate: '',
      theme: 'light' as const,
      fontFamily: '',
    },
  })

  // Quotas form
  const quotasForm = useForm({
    resolver: zodResolver(QuotaUpdateSchema),
    defaultValues: tenantQuotas?.quotas || {
      users: { limit: 100, softLimit: 80 },
      organizations: { limit: 10, softLimit: 8 },
      storage: { limit: 50, softLimit: 40 },
      apiCalls: { limit: 10000, softLimit: 8000 },
      bandwidth: { limit: 100, softLimit: 80 },
      customDomains: { limit: 1, softLimit: 1 },
      ssoConnections: { limit: 2, softLimit: 2 },
      webhooks: { limit: 5, softLimit: 4 },
      auditRetention: 365,
      concurrentSessions: { limit: 100, softLimit: 80 },
      rateLimit: 100,
    },
  })

  // Update form defaults when data loads
  useEffect(() => {
    if (tenantSettings) {
      generalForm.reset(tenantSettings.general)
      securityForm.reset(tenantSettings.security)
    }
  }, [tenantSettings, generalForm, securityForm])

  useEffect(() => {
    if (tenant.branding) {
      brandingForm.reset(tenant.branding)
    }
  }, [tenant.branding, brandingForm])

  useEffect(() => {
    if (tenantQuotas) {
      quotasForm.reset(tenantQuotas.quotas)
    }
  }, [tenantQuotas, quotasForm])

  const showSaveStatus = (status: 'success' | 'error') => {
    setSaveStatus(status)
    setTimeout(() => setSaveStatus('idle'), 3000)
  }

  const handleSaveGeneral = async (data: any) => {
    try {
      setIsSaving(true)
      await updateTenantSettings({
        tenantId: tenant.id,
        settings: { general: data },
      }).unwrap()
      showSaveStatus('success')
      refetchSettings()
    } catch (error) {
      console.error('Failed to save general settings:', error)
      showSaveStatus('error')
    } finally {
      setIsSaving(false)
    }
  }

  const handleSaveSecurity = async (data: any) => {
    try {
      setIsSaving(true)
      await updateTenantSettings({
        tenantId: tenant.id,
        settings: { security: data },
      }).unwrap()
      showSaveStatus('success')
      refetchSettings()
    } catch (error) {
      console.error('Failed to save security settings:', error)
      showSaveStatus('error')
    } finally {
      setIsSaving(false)
    }
  }

  const handleSaveBranding = async (data: any) => {
    try {
      setIsSaving(true)
      await updateTenant({
        tenantId: tenant.id,
        updates: { branding: data },
      }).unwrap()
      showSaveStatus('success')
    } catch (error) {
      console.error('Failed to save branding settings:', error)
      showSaveStatus('error')
    } finally {
      setIsSaving(false)
    }
  }

  const handleSaveQuotas = async (data: any) => {
    try {
      setIsSaving(true)
      await updateTenantQuotas({
        tenantId: tenant.id,
        quotas: data,
      }).unwrap()
      showSaveStatus('success')
      refetchQuotas()
    } catch (error) {
      console.error('Failed to save quotas:', error)
      showSaveStatus('error')
    } finally {
      setIsSaving(false)
    }
  }

  const renderTabContent = () => {
    switch (activeTab) {
      case 'general':
        return (
          <form onSubmit={generalForm.handleSubmit(handleSaveGeneral)} className="space-y-6">
            <div className="grid grid-cols-1 gap-6">
              <div className="flex items-center">
                <Controller
                  name="allowUserRegistration"
                  control={generalForm.control}
                  render={({ field }) => (
                    <input
                      type="checkbox"
                      checked={field.value}
                      onChange={field.onChange}
                      className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                    />
                  )}
                />
                <label className="ml-3 block text-sm font-medium text-gray-700">
                  Allow User Registration
                </label>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Default User Role
                </label>
                <Controller
                  name="defaultUserRole"
                  control={generalForm.control}
                  render={({ field }) => (
                    <select
                      {...field}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    >
                      <option value="user">User</option>
                      <option value="member">Member</option>
                      <option value="viewer">Viewer</option>
                    </select>
                  )}
                />
              </div>

              <div className="flex items-center">
                <Controller
                  name="emailVerificationRequired"
                  control={generalForm.control}
                  render={({ field }) => (
                    <input
                      type="checkbox"
                      checked={field.value}
                      onChange={field.onChange}
                      className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                    />
                  )}
                />
                <label className="ml-3 block text-sm font-medium text-gray-700">
                  Require Email Verification
                </label>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  User Invite Expiration (Days)
                </label>
                <Controller
                  name="userInviteExpirationDays"
                  control={generalForm.control}
                  render={({ field }) => (
                    <input
                      type="number"
                      min="1"
                      max="365"
                      {...field}
                      onChange={(e) => field.onChange(parseInt(e.target.value))}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    />
                  )}
                />
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isSaving}
                className="ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
              >
                {isSaving ? 'Saving...' : 'Save General Settings'}
              </button>
            </div>
          </form>
        )

      case 'security':
        return (
          <form onSubmit={securityForm.handleSubmit(handleSaveSecurity)} className="space-y-8">
            {/* Password Policy */}
            <div>
              <h4 className="text-lg font-medium text-gray-900 mb-4">Password Policy</h4>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Minimum Length
                  </label>
                  <Controller
                    name="passwordPolicy.minLength"
                    control={securityForm.control}
                    render={({ field }) => (
                      <input
                        type="number"
                        min="8"
                        max="128"
                        {...field}
                        onChange={(e) => field.onChange(parseInt(e.target.value))}
                        className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      />
                    )}
                  />
                </div>

                <div className="space-y-3">
                  {[
                    { key: 'requireUppercase', label: 'Require Uppercase' },
                    { key: 'requireLowercase', label: 'Require Lowercase' },
                    { key: 'requireNumbers', label: 'Require Numbers' },
                    { key: 'requireSpecialChars', label: 'Require Special Characters' },
                  ].map(({ key, label }) => (
                    <div key={key} className="flex items-center">
                      <Controller
                        name={`passwordPolicy.${key}` as any}
                        control={securityForm.control}
                        render={({ field }) => (
                          <input
                            type="checkbox"
                            checked={field.value}
                            onChange={field.onChange}
                            className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                          />
                        )}
                      />
                      <label className="ml-3 block text-sm text-gray-700">
                        {label}
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Session Settings */}
            <div>
              <h4 className="text-lg font-medium text-gray-900 mb-4">Session Settings</h4>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Session Timeout (Minutes)
                  </label>
                  <Controller
                    name="sessionSettings.timeoutMinutes"
                    control={securityForm.control}
                    render={({ field }) => (
                      <input
                        type="number"
                        min="5"
                        max="1440"
                        {...field}
                        onChange={(e) => field.onChange(parseInt(e.target.value))}
                        className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      />
                    )}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Max Concurrent Sessions
                  </label>
                  <Controller
                    name="sessionSettings.maxConcurrentSessions"
                    control={securityForm.control}
                    render={({ field }) => (
                      <input
                        type="number"
                        min="1"
                        max="50"
                        {...field}
                        onChange={(e) => field.onChange(parseInt(e.target.value))}
                        className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      />
                    )}
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isSaving}
                className="ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
              >
                {isSaving ? 'Saving...' : 'Save Security Settings'}
              </button>
            </div>
          </form>
        )

      case 'branding':
        return (
          <form onSubmit={brandingForm.handleSubmit(handleSaveBranding)} className="space-y-6">
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Primary Color
                </label>
                <Controller
                  name="primaryColor"
                  control={brandingForm.control}
                  render={({ field }) => (
                    <input
                      type="color"
                      {...field}
                      className="mt-1 block w-full h-10 border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    />
                  )}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Secondary Color
                </label>
                <Controller
                  name="secondaryColor"
                  control={brandingForm.control}
                  render={({ field }) => (
                    <input
                      type="color"
                      {...field}
                      className="mt-1 block w-full h-10 border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    />
                  )}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Logo URL
                </label>
                <Controller
                  name="logoUrl"
                  control={brandingForm.control}
                  render={({ field }) => (
                    <input
                      type="url"
                      {...field}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                      placeholder="https://example.com/logo.png"
                    />
                  )}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Theme
                </label>
                <Controller
                  name="theme"
                  control={brandingForm.control}
                  render={({ field }) => (
                    <select
                      {...field}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                    >
                      <option value="light">Light</option>
                      <option value="dark">Dark</option>
                      <option value="auto">Auto</option>
                    </select>
                  )}
                />
              </div>
            </div>

            <div className="flex items-center">
              <Controller
                name="hideProviderBranding"
                control={brandingForm.control}
                render={({ field }) => (
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                  />
                )}
              />
              <label className="ml-3 block text-sm font-medium text-gray-700">
                Hide Provider Branding
              </label>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isSaving}
                className="ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
              >
                {isSaving ? 'Saving...' : 'Save Branding Settings'}
              </button>
            </div>
          </form>
        )

      case 'quotas':
        return (
          <form onSubmit={quotasForm.handleSubmit(handleSaveQuotas)} className="space-y-6">
            <div className="space-y-6">
              {[
                { key: 'users', label: 'Users' },
                { key: 'organizations', label: 'Organizations' },
                { key: 'storage', label: 'Storage (GB)' },
                { key: 'apiCalls', label: 'API Calls (per month)' },
                { key: 'bandwidth', label: 'Bandwidth (GB per month)' },
              ].map(({ key, label }) => (
                <div key={key} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      {label} - Hard Limit
                    </label>
                    <Controller
                      name={`${key}.limit` as any}
                      control={quotasForm.control}
                      render={({ field }) => (
                        <input
                          type="number"
                          min="-1"
                          {...field}
                          onChange={(e) => field.onChange(parseInt(e.target.value))}
                          className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                          placeholder="-1 for unlimited"
                        />
                      )}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">
                      {label} - Warning Threshold
                    </label>
                    <Controller
                      name={`${key}.softLimit` as any}
                      control={quotasForm.control}
                      render={({ field }) => (
                        <input
                          type="number"
                          min="0"
                          {...field}
                          onChange={(e) => field.onChange(e.target.value ? parseInt(e.target.value) : undefined)}
                          className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                        />
                      )}
                    />
                  </div>
                </div>
              ))}
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isSaving}
                className="ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
              >
                {isSaving ? 'Saving...' : 'Save Quota Settings'}
              </button>
            </div>
          </form>
        )

      default:
        return (
          <div className="text-center py-12">
            <InformationCircleIcon className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-semibold text-gray-900">Coming Soon</h3>
            <p className="mt-1 text-sm text-gray-500">
              This configuration section is under development.
            </p>
          </div>
        )
    }
  }

  if (isLoadingSettings || isLoadingQuotas) {
    return (
      <div className="text-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
        <p className="mt-4 text-sm text-gray-500">Loading tenant configuration...</p>
      </div>
    )
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="border-b border-gray-200 pb-5">
        <h3 className="text-2xl font-semibold leading-6 text-gray-900">
          Tenant Configuration
        </h3>
        <p className="mt-2 text-sm text-gray-700">
          Manage your tenant settings, security policies, branding, and resource quotas.
        </p>
      </div>

      {/* Save Status */}
      {saveStatus === 'success' && (
        <div className="rounded-md bg-green-50 p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <CheckCircleIcon className="h-5 w-5 text-green-400" />
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-green-800">
                Settings saved successfully!
              </p>
            </div>
          </div>
        </div>
      )}

      {saveStatus === 'error' && (
        <div className="rounded-md bg-red-50 p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-red-800">
                Failed to save settings. Please try again.
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="lg:grid lg:grid-cols-12 lg:gap-x-5">
        {/* Sidebar Navigation */}
        <aside className="py-6 px-2 sm:px-6 lg:py-0 lg:px-0 lg:col-span-3">
          <nav className="space-y-1">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`group rounded-md px-3 py-2 flex items-center text-sm font-medium w-full text-left ${
                  activeTab === tab.key
                    ? 'bg-gray-50 text-indigo-700 hover:text-indigo-700 hover:bg-gray-50'
                    : 'text-gray-900 hover:text-gray-900 hover:bg-gray-50'
                }`}
              >
                <tab.icon
                  className={`flex-shrink-0 -ml-1 mr-3 h-6 w-6 ${
                    activeTab === tab.key
                      ? 'text-indigo-500 group-hover:text-indigo-500'
                      : 'text-gray-400 group-hover:text-gray-500'
                  }`}
                />
                <span className="truncate">{tab.label}</span>
              </button>
            ))}
          </nav>
        </aside>

        {/* Main Content */}
        <div className="space-y-6 sm:px-6 lg:px-0 lg:col-span-9">
          <div className="bg-white shadow sm:rounded-md">
            <div className="px-4 py-5 sm:p-6">
              <div className="mb-6">
                <h4 className="text-lg font-medium text-gray-900">
                  {tabs.find(tab => tab.key === activeTab)?.label}
                </h4>
                <p className="mt-1 text-sm text-gray-500">
                  {tabs.find(tab => tab.key === activeTab)?.description}
                </p>
              </div>

              {renderTabContent()}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default TenantConfigurationManager