import React, { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  XMarkIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline'
import { toast } from 'react-hot-toast'
import { clsx } from 'clsx'

import { useCreateOrganizationMutation } from '../../store/api/organizationApi'
import { parseApiError } from '../../utils/apiError'
import { logger } from '../../utils/logger'
import { useAutoSave } from '../../hooks/useAutoSave'
import { AutoSaveIndicator, UnsavedChangesWarning } from '../ui/AutoSaveComponents'
import { useFormSubmissionNotifications } from '../../hooks/useNotificationIntegration'

const createOrganizationSchema = z.object({
  name: z
    .string()
    .min(3, 'Name must be at least 3 characters')
    .max(50, 'Name must be less than 50 characters')
    .regex(/^[a-zA-Z0-9\s-_.]+$/, 'Name can only contain letters, numbers, spaces, hyphens, underscores, and periods'),
  slug: z
    .string()
    .min(3, 'Slug must be at least 3 characters')
    .max(50, 'Slug must be less than 50 characters')
    .regex(
      /^[a-z0-9-]+$/,
      'Only lowercase letters, numbers, and dashes allowed'
    )
    .refine(
      slug => !slug.startsWith('-') && !slug.endsWith('-'),
      'Cannot start or end with dash'
    ),
  description: z
    .string()
    .max(200, 'Keep description under 200 characters')
    .optional(),
})

type CreateOrganizationForm = z.infer<typeof createOrganizationSchema>

interface CreateOrganizationModalProps {
  isOpen: boolean
  onClose: () => void
  onOptimisticCreate?: (organization: any) => Promise<any>
}

const CreateOrganizationModal: React.FC<CreateOrganizationModalProps> = ({
  isOpen,
  onClose,
  onOptimisticCreate,
}) => {
  const [createOrganization, { isLoading }] = useCreateOrganizationMutation()
  const [slugAvailability, setSlugAvailability] = useState<'unknown' | 'checking' | 'available' | 'taken'>('unknown')
  const [fieldTouched, setFieldTouched] = useState<Record<string, boolean>>({})
  const { submitFormWithNotifications } = useFormSubmissionNotifications()

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    trigger,
    formState: { errors, isValid, dirtyFields },
    reset,
  } = useForm<CreateOrganizationForm>({
    resolver: zodResolver(createOrganizationSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  })

  const watchedName = watch('name', '')
  const watchedSlug = watch('slug', '')
  const watchedDescription = watch('description', '')

  // Auto-save form data to localStorage
  const formData = { name: watchedName, slug: watchedSlug, description: watchedDescription }
  const autoSave = useAutoSave(formData, {
    delay: 2000,
    onSave: (data) => {
      // Only save if there's actually some content and the modal is open
      if (isOpen && (data.name || data.slug || data.description)) {
        localStorage.setItem('createOrganizationDraft', JSON.stringify({
          ...data,
          timestamp: Date.now()
        }))
      }
    },
    onError: (error) => {
      logger.error('Auto-save failed:', error)
    }
  })

  // Load draft on modal open
  useEffect(() => {
    if (isOpen) {
      const draft = localStorage.getItem('createOrganizationDraft')
      if (draft) {
        try {
          const parsedDraft = JSON.parse(draft)
          // Only load draft if it's less than 24 hours old
          if (Date.now() - parsedDraft.timestamp < 86400000) {
            setValue('name', parsedDraft.name || '')
            setValue('slug', parsedDraft.slug || '')
            setValue('description', parsedDraft.description || '')
            toast.success('Draft restored from your last session')
          } else {
            localStorage.removeItem('createOrganizationDraft')
          }
        } catch (error) {
          logger.error('Failed to parse organization draft:', error)
          localStorage.removeItem('createOrganizationDraft')
        }
      }
    }
  }, [isOpen, setValue])

  // Helper function to get field validation state
  const getFieldState = (fieldName: keyof CreateOrganizationForm) => {
    const hasError = !!errors[fieldName]
    const isTouched = fieldTouched[fieldName] || !!dirtyFields[fieldName]
    const hasValue = fieldName === 'name' ? !!watchedName.trim() :
                    fieldName === 'slug' ? !!watchedSlug.trim() :
                    fieldName === 'description' ? !!watchedDescription?.trim() : false

    if (hasError && isTouched) return 'error'
    if (!hasError && isTouched && hasValue) return 'valid'
    return 'default'
  }

  // Auto-generate slug from name
  useEffect(() => {
    if (watchedName && !fieldTouched.slug) {
      const slug = watchedName
        .toLowerCase()
        .replace(/[^a-z0-9\s-]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .substring(0, 50)
      setValue('slug', slug)
      void trigger('slug') // Re-validate slug when auto-generated
    }
  }, [watchedName, setValue, trigger, fieldTouched.slug])

  // Check slug availability (simulated - replace with real API call)
  useEffect(() => {
    if (watchedSlug.length >= 3 && getFieldState('slug') !== 'error') {
      setSlugAvailability('checking')
      const timer = setTimeout(() => {
        // Simulate API call - replace with real endpoint
        const isAvailable = !['admin', 'api', 'www', 'test', 'demo'].includes(watchedSlug.toLowerCase())
        setSlugAvailability(isAvailable ? 'available' : 'taken')
      }, 800)
      return () => clearTimeout(timer)
    } else {
      setSlugAvailability('unknown')
    }
  }, [watchedSlug, getFieldState])

  const handleFieldBlur = (fieldName: string) => {
    setFieldTouched(prev => ({ ...prev, [fieldName]: true }))
  }

  const onSubmit = async (data: CreateOrganizationForm) => {
    const organizationData = {
      name: data.name,
      slug: data.slug,
      settings: data.description
        ? { description: data.description }
        : undefined,
    }

    if (onOptimisticCreate) {
      // Use optimistic updates if callback is provided
      const optimisticOrganization = {
        id: `temp-${Date.now()}`, // Temporary ID
        name: data.name,
        slug: data.slug,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        // Add other required fields with defaults
      }

      try {
        await onOptimisticCreate(optimisticOrganization)
        localStorage.removeItem('createOrganizationDraft')
        reset()
        onClose()
      } catch (err) {
        const parsed = parseApiError(err)
        logger.error('Failed to create organization:', parsed)
        // Error already handled by optimistic updates hook
      }
    } else {
      // Use enhanced notification system for regular submission
      const result = await submitFormWithNotifications(
        organizationData,
        async (orgData) => {
          return createOrganization(orgData).unwrap()
        },
        {
          loadingTitle: 'Creating organization...',
          successTitle: 'Organization created!',
          successMessage: `"${data.name}" has been created successfully`,
          errorTitle: 'Failed to create organization',
          validateBeforeSubmit: (orgData) => {
            // Additional validation if needed
            if (slugAvailability === 'taken') {
              return 'This organization slug is already taken. Please choose a different one.'
            }
            if (slugAvailability === 'checking') {
              return 'Please wait while we check slug availability.'
            }
            return null
          },
          onSuccess: () => {
            localStorage.removeItem('createOrganizationDraft')
            reset()
            onClose()
          },
          onError: (error) => {
            logger.error('Failed to create organization:', error)
          }
        }
      )
    }
  }

  const handleClose = () => {
    reset()
    setSlugAvailability('unknown')
    setFieldTouched({})
    onClose()
  }

  const clearDraft = () => {
    localStorage.removeItem('createOrganizationDraft')
    reset()
    toast.success('Draft cleared')
  }

  if (!isOpen) {return null}

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-screen items-end justify-center px-4 pt-4 pb-20 text-center sm:block sm:p-0">
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={handleClose}
        />

        <span className="hidden sm:inline-block sm:align-middle sm:h-screen">
          &#8203;
        </span>

        <div className="relative inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
          <div className="absolute top-0 right-0 pt-4 pr-4">
            <button
              type="button"
              className="bg-white rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              onClick={handleClose}
            >
              <XMarkIcon className="h-6 w-6" />
            </button>
          </div>

          <div className="sm:flex sm:items-start">
            <div className="w-full mt-3 text-center sm:mt-0 sm:text-left">
              {/* Header with auto-save indicator */}
              <div className="flex justify-between items-start mb-2">
                <h3 className="text-lg leading-6 font-medium text-gray-900">
                  Create New Organization
                </h3>
                <AutoSaveIndicator
                  status={autoSave.status}
                  lastSaved={autoSave.lastSaved}
                  error={autoSave.error}
                  onRetry={autoSave.retry}
                />
              </div>

              {/* Unsaved changes warning */}
              <UnsavedChangesWarning
                hasUnsavedChanges={autoSave.hasUnsavedChanges}
                onSave={autoSave.save}
                onDiscard={clearDraft}
              />

              <div className="mt-2">
                <p className="text-sm text-gray-500">
                  Create a new organization to manage your team and subscriptions.
                  Your progress is automatically saved.
                </p>
              </div>

              <form
                onSubmit={(e) => void handleSubmit(onSubmit)(e)}
                className="mt-6 space-y-6"
              >
                <div>
                  <label
                    htmlFor="name"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Organization Name <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 relative">
                    <input
                      {...register('name', {
                        onBlur: () => handleFieldBlur('name')
                      })}
                      type="text"
                      className={clsx(
                        'shadow-sm block w-full sm:text-sm border rounded-md pr-10',
                        {
                          'border-gray-300 focus:ring-primary-500 focus:border-primary-500': getFieldState('name') === 'default',
                          'border-green-500 focus:ring-green-500 focus:border-green-500': getFieldState('name') === 'valid',
                          'border-red-500 focus:ring-red-500 focus:border-red-500': getFieldState('name') === 'error',
                        }
                      )}
                      placeholder="e.g., Acme Corporation"
                      maxLength={50}
                    />
                    {/* Validation Icon */}
                    <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                      {getFieldState('name') === 'valid' && (
                        <CheckCircleIcon className="h-5 w-5 text-green-500" />
                      )}
                      {getFieldState('name') === 'error' && (
                        <ExclamationTriangleIcon className="h-5 w-5 text-red-500" />
                      )}
                    </div>
                  </div>
                  <div className="mt-1 flex justify-between">
                    <div>
                      {errors.name && (
                        <p className="text-sm text-red-600 flex items-center">
                          <ExclamationTriangleIcon className="h-4 w-4 mr-1" />
                          {errors.name.message}
                        </p>
                      )}
                      {getFieldState('name') === 'valid' && (
                        <p className="text-sm text-green-600 flex items-center">
                          <CheckCircleIcon className="h-4 w-4 mr-1" />
                          Looks good!
                        </p>
                      )}
                    </div>
                    <span className="text-xs text-gray-500">
                      {watchedName.length}/50
                    </span>
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="slug"
                    className="block text-sm font-medium text-gray-700"
                  >
                    URL Slug <span className="text-red-500">*</span>
                  </label>
                  <div className="mt-1 flex rounded-md shadow-sm relative">
                    <span className="inline-flex items-center px-3 rounded-l-md border border-r-0 border-gray-300 bg-gray-50 text-gray-500 text-sm">
                      /
                    </span>
                    <div className="relative flex-1">
                      <input
                        {...register('slug', {
                          onBlur: () => handleFieldBlur('slug'),
                          onChange: () => setSlugAvailability('unknown')
                        })}
                        type="text"
                        className={clsx(
                          'flex-1 block w-full rounded-none rounded-r-md sm:text-sm border pr-10',
                          {
                            'border-gray-300 focus:ring-primary-500 focus:border-primary-500': getFieldState('slug') === 'default',
                            'border-green-500 focus:ring-green-500 focus:border-green-500': getFieldState('slug') === 'valid' && slugAvailability === 'available',
                            'border-red-500 focus:ring-red-500 focus:border-red-500': getFieldState('slug') === 'error' || slugAvailability === 'taken',
                          }
                        )}
                        placeholder="acme-corp"
                        maxLength={50}
                      />
                      {/* Availability Icon */}
                      <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
                        {slugAvailability === 'checking' && (
                          <div className="animate-spin h-4 w-4 border-2 border-primary-500 border-t-transparent rounded-full" />
                        )}
                        {slugAvailability === 'available' && getFieldState('slug') !== 'error' && (
                          <CheckCircleIcon className="h-5 w-5 text-green-500" />
                        )}
                        {(slugAvailability === 'taken' || getFieldState('slug') === 'error') && (
                          <ExclamationTriangleIcon className="h-5 w-5 text-red-500" />
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="mt-1 flex justify-between">
                    <div>
                      {errors.slug && (
                        <p className="text-sm text-red-600 flex items-center">
                          <ExclamationTriangleIcon className="h-4 w-4 mr-1" />
                          {errors.slug.message}
                        </p>
                      )}
                      {slugAvailability === 'taken' && !errors.slug && (
                        <p className="text-sm text-red-600 flex items-center">
                          <ExclamationTriangleIcon className="h-4 w-4 mr-1" />
                          This slug is already taken
                        </p>
                      )}
                      {slugAvailability === 'available' && !errors.slug && (
                        <p className="text-sm text-green-600 flex items-center">
                          <CheckCircleIcon className="h-4 w-4 mr-1" />
                          Available!
                        </p>
                      )}
                      {slugAvailability === 'checking' && !errors.slug && (
                        <p className="text-sm text-gray-600 flex items-center">
                          <InformationCircleIcon className="h-4 w-4 mr-1" />
                          Checking availability...
                        </p>
                      )}
                      {!errors.slug && getFieldState('slug') === 'default' && (
                        <p className="text-sm text-gray-500">
                          Only lowercase letters, numbers, and dashes allowed
                        </p>
                      )}
                    </div>
                    <span className="text-xs text-gray-500">
                      {watchedSlug.length}/50
                    </span>
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="description"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Description (Optional)
                  </label>
                  <div className="mt-1 relative">
                    <textarea
                      {...register('description', {
                        onBlur: () => handleFieldBlur('description')
                      })}
                      rows={3}
                      className={clsx(
                        'shadow-sm block w-full sm:text-sm border rounded-md',
                        {
                          'border-gray-300 focus:ring-primary-500 focus:border-primary-500': getFieldState('description') === 'default',
                          'border-green-500 focus:ring-green-500 focus:border-green-500': getFieldState('description') === 'valid',
                          'border-red-500 focus:ring-red-500 focus:border-red-500': getFieldState('description') === 'error',
                        }
                      )}
                      placeholder="Brief description of your organization..."
                      maxLength={200}
                    />
                  </div>
                  <div className="mt-1 flex justify-between">
                    <div>
                      {errors.description && (
                        <p className="text-sm text-red-600 flex items-center">
                          <ExclamationTriangleIcon className="h-4 w-4 mr-1" />
                          {errors.description.message}
                        </p>
                      )}
                    </div>
                    <span className="text-xs text-gray-500">
                      {watchedDescription?.length || 0}/200
                    </span>
                  </div>
                </div>

                <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                  <button
                    type="submit"
                    disabled={isLoading || !isValid || slugAvailability === 'taken' || slugAvailability === 'checking'}
                    className={clsx(
                      'w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 text-base font-medium text-white focus:outline-none focus:ring-2 focus:ring-offset-2 sm:col-start-2 sm:text-sm transition-colors',
                      {
                        'bg-primary-600 hover:bg-primary-700 focus:ring-primary-500': isValid && slugAvailability !== 'taken' && slugAvailability !== 'checking' && !isLoading,
                        'bg-gray-400 cursor-not-allowed': !isValid || slugAvailability === 'taken' || slugAvailability === 'checking' || isLoading,
                      }
                    )}
                  >
                    {isLoading ? (
                      <div className="flex items-center">
                        <div className="animate-spin -ml-1 mr-3 h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
                        Creating...
                      </div>
                    ) : (
                      'Create Organization'
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={handleClose}
                    className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:mt-0 sm:col-start-1 sm:text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default CreateOrganizationModal
