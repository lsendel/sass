import React, { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  XMarkIcon
} from '@heroicons/react/24/outline'
import { toast } from 'react-hot-toast'
import { clsx } from 'clsx'

import { useCreateOrganizationMutation } from '../../store/api/organizationApi'
import { parseApiError } from '../../utils/apiError'
import { logger } from '../../utils/logger'

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
}

const CreateOrganizationModal: React.FC<CreateOrganizationModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [createOrganization, { isLoading }] = useCreateOrganizationMutation()

  const {
    register,
    handleSubmit,
    watch,
    setValue,
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

  // Load draft on modal open
  useEffect(() => {
    if (isOpen) {
      const draft = localStorage.getItem('createOrganizationDraft')
      if (draft) {
        try {
          const parsedDraft = JSON.parse(draft)
          // Only load draft if it's less than 24 hours old
          if (Date.now() - parsedDraft.timestamp < 86400000) {
            setValue('name', parsedDraft.name ?? '')
            setValue('slug', parsedDraft.slug ?? '')
            setValue('description', parsedDraft.description ?? '')
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

  const getFieldState = (fieldName: keyof CreateOrganizationForm) => {
    const hasError = !!errors[fieldName]
    const isTouched = !!dirtyFields[fieldName]
    const hasValue = fieldName === 'name' ? !!watchedName.trim() :
                    fieldName === 'slug' ? !!watchedSlug.trim() :
                    fieldName === 'description' ? !!watchedDescription?.trim() : false

    if (hasError && isTouched) return 'error'
    if (!hasError && isTouched && hasValue) return 'valid'
    return 'default'
  }

  const onSubmit = async (data: CreateOrganizationForm) => {
    const organizationData: any = {
      name: data.name,
      slug: data.slug,
    }
    if (data.description) {
      organizationData.settings = { description: data.description }
    }

    try {
      await createOrganization(organizationData).unwrap()
      localStorage.removeItem('createOrganizationDraft')
      reset()
      onClose()
      toast.success(`Organization "${data.name}" created successfully`)
    } catch (err) {
      const parsed = parseApiError(err)
      logger.error('Failed to create organization:', parsed)
      toast.error(parsed.message ?? 'Failed to create organization')
    }
  }

  const handleClose = () => {
    reset()
    onClose()
  }

  if (!isOpen) return null

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
              <h3 className="text-lg leading-6 font-medium text-gray-900">
                Create New Organization
              </h3>

              <div className="mt-2">
                <p className="text-sm text-gray-500">
                  Create a new organization to manage your team and subscriptions.
                </p>
              </div>

              <form
                onSubmit={handleSubmit(onSubmit)}
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
                      {...register('name')}
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
                  </div>
                  <div className="mt-1 flex justify-between">
                    <div>
                      {errors.name && (
                        <p className="text-sm text-red-600">
                          {errors.name.message}
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
                        {...register('slug')}
                        type="text"
                        className={clsx(
                          'flex-1 block w-full rounded-none rounded-r-md sm:text-sm border pr-10',
                          {
                            'border-gray-300 focus:ring-primary-500 focus:border-primary-500': getFieldState('slug') === 'default',
                            'border-green-500 focus:ring-green-500 focus:border-green-500': getFieldState('slug') === 'valid',
                            'border-red-500 focus:ring-red-500 focus:border-red-500': getFieldState('slug') === 'error',
                          }
                        )}
                        placeholder="acme-corp"
                        maxLength={50}
                      />
                    </div>
                  </div>
                  <div className="mt-1 flex justify-between">
                    <div>
                      {errors.slug && (
                        <p className="text-sm text-red-600">
                          {errors.slug.message}
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
                  <div className="mt-1">
                    <textarea
                      {...register('description')}
                      rows={3}
                      className="shadow-sm block w-full sm:text-sm border border-gray-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                      placeholder="Brief description of your organization..."
                      maxLength={200}
                    />
                    <div className="mt-1 text-right">
                      <span className="text-xs text-gray-500">
                        {watchedDescription?.length ?? 0}/200
                      </span>
                    </div>
                  </div>
                </div>

                <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                  <button
                    type="submit"
                    disabled={isLoading || !isValid}
                    className={clsx(
                      'w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 text-base font-medium text-white focus:outline-none focus:ring-2 focus:ring-offset-2 sm:col-start-2 sm:text-sm',
                      {
                        'bg-primary-600 hover:bg-primary-700 focus:ring-primary-500': isValid && !isLoading,
                        'bg-gray-400 cursor-not-allowed': !isValid || isLoading,
                      }
                    )}
                  >
                    {isLoading ? 'Creating...' : 'Create Organization'}
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
