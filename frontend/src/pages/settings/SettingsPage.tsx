import React, { useState } from 'react'
import {
  UserCircleIcon,
  Cog6ToothIcon,
  BellIcon,
  ShieldCheckIcon,
  KeyIcon,
  TrashIcon,
} from '@heroicons/react/24/outline'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

import { logger } from '../../utils/logger'
import {
  useGetCurrentUserQuery,
  useUpdateProfileMutation,
} from '../../store/api/userApi'

import { LoadingCard, LoadingButton } from '../../components/ui/LoadingStates'
import { ApiErrorDisplay } from '../../components/ui/ErrorStates'
import { parseApiError } from '../../utils/apiError'
import { useCrossComponentSync } from '../../hooks/useDataSync'
import { useNotifications } from '../../components/ui/FeedbackSystem'
import PageHeader from '../../components/ui/PageHeader'
import AccessibleFormField, { EmailField, PasswordField } from '../../components/forms/AccessibleFormField'

const profileSchema = z.object({
  name: z
    .string()
    .min(1, 'Name is required')
    .max(100, 'Name must be less than 100 characters'),
  email: z.string().email('Please enter a valid email address'),
  timezone: z.string().optional(),
  language: z.string().optional(),
  notifications: z
    .object({
      email: z.boolean(),
      push: z.boolean(),
      sms: z.boolean(),
    })
    .optional(),
})

type ProfileForm = z.infer<typeof profileSchema>

const SettingsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('profile')
  const { data: profile, isLoading, error } = useGetCurrentUserQuery()
  const [updateProfile, { isLoading: isUpdating }] = useUpdateProfileMutation()
  const { syncUserData } = useCrossComponentSync()
  const { addNotification } = useNotifications()
  const showSuccess = (message: string) => addNotification({ variant: 'success', title: message })
  const showError = (message: string) => addNotification({ variant: 'error', title: message })

  const headerDescription = 'Manage your account settings and preferences.'
  const renderHeader = () => (
    <PageHeader title="Settings" description={headerDescription} />
  )

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    ...(profile ? {
      defaultValues: {
        name: (profile as any).name || (profile as any).firstName || '',
        email: profile.email || '',
        timezone: 'UTC',
        language: 'en',
        notifications: {
          email: true,
          push: true,
          sms: false,
        },
      }
    } : {}),
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <LoadingCard
          title="Loading Settings"
          message="Fetching your profile and preferences..."
        />
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-6">
        {renderHeader()}

        <ApiErrorDisplay
          error={error}
          onRetry={async () => {
            window.location.reload()
          }}
          fallbackMessage="Failed to load settings. Please try again."
        />
      </div>
    )
  }

  const onSubmit = async (data: any) => {
    try {
      await updateProfile({
        name: data.name,
        preferences: {
          timezone: data.timezone || 'UTC',
          language: data.language || 'en',
          notifications: data.notifications || {
            email: true,
            push: true,
            sms: false,
          },
        },
      }).unwrap()

      showSuccess('Your profile has been updated successfully.')
      await syncUserData()
    } catch (err) {
      const parsed = parseApiError(err)
      logger.error('Failed to update profile:', parsed)
      showError(parsed.message || 'Failed to update profile. Please try again.')
    }
  }

  const tabs = [
    { id: 'profile', name: 'Profile', icon: UserCircleIcon },
    { id: 'notifications', name: 'Notifications', icon: BellIcon },
    { id: 'security', name: 'Security', icon: ShieldCheckIcon },
    { id: 'account', name: 'Account', icon: Cog6ToothIcon },
  ]

  return (
    <div className="space-y-6">
      {renderHeader()}

      <div className="bg-white shadow overflow-hidden sm:rounded-lg">
        {/* Tabs */}
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex space-x-8 px-6" aria-label="Tabs">
            {tabs.map(tab => {
              const Icon = tab.icon
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`${
                    activeTab === tab.id
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm flex items-center space-x-2`}
                >
                  <Icon className="h-5 w-5" />
                  <span>{tab.name}</span>
                </button>
              )
            })}
          </nav>
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {activeTab === 'profile' && (
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Profile Information
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  Update your personal information and preferences.
                </p>
              </div>

              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor="name"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Full Name
                  </label>
                  <div className="mt-1">
                    <input
                      {...register('name')}
                      type="text"
                      className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                      placeholder="Your full name"
                    />
                  </div>
                  {errors.name && (
                    <p className="mt-2 text-sm text-red-600">
                      {errors.name.message}
                    </p>
                  )}
                </div>

                <div>
                  <label
                    htmlFor="email"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Email Address
                  </label>
                  <div className="mt-1">
                    <input
                      {...register('email')}
                      type="email"
                      disabled
                      className="shadow-sm bg-gray-50 focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                      placeholder="your@email.com"
                    />
                  </div>
                  <p className="mt-2 text-sm text-gray-500">
                    Email cannot be changed. Contact support if you need to
                    update it.
                  </p>
                </div>

                <div>
                  <label
                    htmlFor="timezone"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Timezone
                  </label>
                  <div className="mt-1">
                    <select
                      {...register('timezone')}
                      className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                    >
                      <option value="UTC">UTC</option>
                      <option value="America/New_York">Eastern Time</option>
                      <option value="America/Chicago">Central Time</option>
                      <option value="America/Denver">Mountain Time</option>
                      <option value="America/Los_Angeles">Pacific Time</option>
                      <option value="Europe/London">London</option>
                      <option value="Europe/Paris">Paris</option>
                      <option value="Asia/Tokyo">Tokyo</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="language"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Language
                  </label>
                  <div className="mt-1">
                    <select
                      {...register('language')}
                      className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                    >
                      <option value="en">English</option>
                      <option value="es">Spanish</option>
                      <option value="fr">French</option>
                      <option value="de">German</option>
                      <option value="ja">Japanese</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="flex justify-end">
                <LoadingButton
                  type="submit"
                  isLoading={isUpdating}
                  variant="primary"
                  size="md"
                >
                  Save Changes
                </LoadingButton>
              </div>
            </form>
          )}

          {activeTab === 'notifications' && (
            <div className="space-y-6">
              <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Notification Preferences
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  Choose how you want to be notified about important updates.
                </p>
              </div>

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <label
                        htmlFor="email-notifications"
                        className="text-sm font-medium text-gray-700"
                      >
                        Email Notifications
                      </label>
                      <p className="text-sm text-gray-500">
                        Receive notifications via email about account activity.
                      </p>
                    </div>
                    <input
                      {...register('notifications.email')}
                      type="checkbox"
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <label
                        htmlFor="push-notifications"
                        className="text-sm font-medium text-gray-700"
                      >
                        Push Notifications
                      </label>
                      <p className="text-sm text-gray-500">
                        Receive push notifications in your browser.
                      </p>
                    </div>
                    <input
                      {...register('notifications.push')}
                      type="checkbox"
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <label
                        htmlFor="sms-notifications"
                        className="text-sm font-medium text-gray-700"
                      >
                        SMS Notifications
                      </label>
                      <p className="text-sm text-gray-500">
                        Receive text messages for critical updates.
                      </p>
                    </div>
                    <input
                      {...register('notifications.sms')}
                      type="checkbox"
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                    />
                  </div>
                </div>

                <div className="flex justify-end">
                  <LoadingButton
                    type="submit"
                    isLoading={isUpdating}
                    variant="primary"
                    size="md"
                  >
                    Save Changes
                  </LoadingButton>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'security' && (
            <div className="space-y-6">
              <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Security Settings
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  Manage your account security and authentication methods.
                </p>
              </div>

              <div className="space-y-6">
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center">
                    <KeyIcon className="h-5 w-5 text-gray-400 mr-3" />
                    <div className="flex-1">
                      <h4 className="text-sm font-medium text-gray-900">
                        OAuth Authentication
                      </h4>
                      <p className="text-sm text-gray-500">
                        You&apos;re signed in with OAuth. Manage your authentication
                        providers in your OAuth provider settings.
                      </p>
                    </div>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      Active
                    </span>
                  </div>
                </div>

                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center">
                    <ShieldCheckIcon className="h-5 w-5 text-gray-400 mr-3" />
                    <div className="flex-1">
                      <h4 className="text-sm font-medium text-gray-900">
                        Two-Factor Authentication
                      </h4>
                      <p className="text-sm text-gray-500">
                        Add an extra layer of security to your account.
                      </p>
                    </div>
                    <LoadingButton
                      variant="secondary"
                      size="sm"
                      onClick={() => showError('Two-factor authentication is not yet implemented.')}
                    >
                      Enable
                    </LoadingButton>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'account' && (
            <div className="space-y-6">
              <div>
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Account Management
                </h3>
                <p className="mt-1 text-sm text-gray-500">
                  Manage your account settings and data.
                </p>
              </div>

              <div className="space-y-6">
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-sm font-medium text-gray-900">
                        Download Account Data
                      </h4>
                      <p className="text-sm text-gray-500">
                        Export all your account data and information.
                      </p>
                    </div>
                    <LoadingButton
                      variant="secondary"
                      size="sm"
                      onClick={() => showError('Data export is not yet implemented.')}
                    >
                      Export Data
                    </LoadingButton>
                  </div>
                </div>

                <div className="bg-red-50 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="text-sm font-medium text-red-900">
                        Delete Account
                      </h4>
                      <p className="text-sm text-red-700">
                        Permanently delete your account and all associated data.
                        This action cannot be undone.
                      </p>
                    </div>
                    <LoadingButton
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        if (confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
                          showError('Account deletion is not yet implemented. Please contact support.')
                        }
                      }}
                    >
                      <TrashIcon className="h-4 w-4 mr-1" />
                      Delete
                    </LoadingButton>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default SettingsPage
