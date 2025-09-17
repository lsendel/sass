import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useCreateOrganizationMutation } from '../../store/api/organizationApi'
import { XMarkIcon } from '@heroicons/react/24/outline'
import { parseApiError } from '../../utils/apiError'
import { toast } from 'react-hot-toast'

const createOrganizationSchema = z.object({
  name: z
    .string()
    .min(1, 'Organization name is required')
    .max(100, 'Name must be less than 100 characters'),
  slug: z
    .string()
    .min(1, 'Slug is required')
    .max(50, 'Slug must be less than 50 characters')
    .regex(
      /^[a-z0-9-]+$/,
      'Slug can only contain lowercase letters, numbers, and dashes'
    )
    .refine(
      slug => !slug.startsWith('-') && !slug.endsWith('-'),
      'Slug cannot start or end with a dash'
    ),
  description: z
    .string()
    .max(500, 'Description must be less than 500 characters')
    .optional(),
})

type CreateOrganizationForm = z.infer<typeof createOrganizationSchema>

type CreateOrganizationModalProps = {
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
    formState: { errors },
    reset,
  } = useForm<CreateOrganizationForm>({
    resolver: zodResolver(createOrganizationSchema),
  })

  const watchedName = watch('name', '')

  // Auto-generate slug from name
  React.useEffect(() => {
    if (watchedName) {
      const slug = watchedName
        .toLowerCase()
        .replace(/[^a-z0-9\s-]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .substring(0, 50)
      setValue('slug', slug)
    }
  }, [watchedName, setValue])

  const onSubmit = async (data: CreateOrganizationForm) => {
    try {
      await createOrganization({
        name: data.name,
        slug: data.slug,
        settings: data.description
          ? { description: data.description }
          : undefined,
      }).unwrap()

      toast.success('Organization created successfully!')
      reset()
      onClose()
    } catch (err) {
      const parsed = parseApiError(err)
      console.error('Failed to create organization:', parsed)
      toast.error(parsed.message || 'Failed to create organization')
    }
  }

  const handleClose = () => {
    reset()
    onClose()
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
              <h3 className="text-lg leading-6 font-medium text-gray-900">
                Create New Organization
              </h3>
              <div className="mt-2">
                <p className="text-sm text-gray-500">
                  Create a new organization to manage your team and
                  subscriptions.
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
                    Organization Name
                  </label>
                  <div className="mt-1">
                    <input
                      {...register('name')}
                      type="text"
                      className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                      placeholder="e.g., Acme Corp"
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
                    htmlFor="slug"
                    className="block text-sm font-medium text-gray-700"
                  >
                    URL Slug
                  </label>
                  <div className="mt-1 flex rounded-md shadow-sm">
                    <span className="inline-flex items-center px-3 rounded-l-md border border-r-0 border-gray-300 bg-gray-50 text-gray-500 text-sm">
                      /
                    </span>
                    <input
                      {...register('slug')}
                      type="text"
                      className="focus:ring-primary-500 focus:border-primary-500 flex-1 block w-full rounded-none rounded-r-md sm:text-sm border-gray-300"
                      placeholder="acme-corp"
                    />
                  </div>
                  {errors.slug && (
                    <p className="mt-2 text-sm text-red-600">
                      {errors.slug.message}
                    </p>
                  )}
                  <p className="mt-2 text-sm text-gray-500">
                    This will be used in your organization&apos;s URL. Only lowercase
                    letters, numbers, and dashes are allowed.
                  </p>
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
                      className="shadow-sm focus:ring-primary-500 focus:border-primary-500 block w-full sm:text-sm border-gray-300 rounded-md"
                      placeholder="Brief description of your organization..."
                    />
                  </div>
                  {errors.description && (
                    <p className="mt-2 text-sm text-red-600">
                      {errors.description.message}
                    </p>
                  )}
                </div>

                <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary-600 text-base font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:col-start-2 sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
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
