# Frontend Implementation Templates
*Ready-to-use code patterns and templates for the Spring Boot Modulith Payment Platform Frontend*

## Table of Contents
1. [Page Component Templates](#1-page-component-templates)
2. [API Integration Templates](#2-api-integration-templates)
3. [Form Component Templates](#3-form-component-templates)
4. [State Management Templates](#4-state-management-templates)
5. [Testing Templates](#5-testing-templates)
6. [Performance Optimization Templates](#6-performance-optimization-templates)
7. [Error Handling Templates](#7-error-handling-templates)

---

## 1. Page Component Templates

### 1.1 Basic Page Template

```typescript
// Template: Basic page component with loading and error states
// Path: src/pages/[module]/[PageName].tsx

import React from 'react'
import { useAppSelector } from '@/store/hooks'
import { selectCurrentUser } from '@/store/slices/authSlice'
import PageHeader from '@/components/ui/PageHeader'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import ErrorMessage from '@/components/ui/ErrorMessage'
import { use[Module]Query, use[Module]Mutation } from '@/store/api/[module]Api'

interface [PageName]Props {
  // Add page-specific props if needed
}

const [PageName]: React.FC<[PageName]Props> = () => {
  // Authentication check
  const currentUser = useAppSelector(selectCurrentUser)

  // Data fetching
  const {
    data: [dataName],
    isLoading,
    error,
    refetch
  } = use[Module]Query(/* query params */)

  // Mutations
  const [
    [mutationName],
    {
      isLoading: isMutating,
      error: mutationError
    }
  ] = use[Module]Mutation()

  // Event handlers
  const handle[Action] = async (data: [ActionData]) => {
    try {
      await [mutationName](data).unwrap()
      // Handle success
      toast.success('[Action] completed successfully')
    } catch (error) {
      // Error handling is automatic through RTK Query
      console.error('[Action] failed:', error)
    }
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="p-6">
        <ErrorMessage
          title="Failed to load [resource]"
          message="Please try again or contact support if the problem persists."
          onRetry={refetch}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="[Page Title]"
        description="[Page description]"
        action={
          <Button
            onClick={() => handle[Action](/* data */)}
            isLoading={isMutating}
            disabled={!currentUser?.permissions?.includes('[REQUIRED_PERMISSION]')}
          >
            [Action Button Text]
          </Button>
        }
      />

      <div className="grid gap-6">
        {/* Page content */}
        {[dataName]?.map((item) => (
          <[ItemComponent]
            key={item.id}
            item={item}
            onAction={handle[Action]}
          />
        ))}
      </div>
    </div>
  )
}

export default [PageName]
```

### 1.2 Dashboard Page Template

```typescript
// Template: Dashboard page with metrics and widgets
// Path: src/pages/dashboard/DashboardPage.tsx

import React from 'react'
import { useAppSelector } from '@/store/hooks'
import { selectCurrentUser } from '@/store/slices/authSlice'
import {
  useGetDashboardMetricsQuery,
  useGetRecentActivityQuery
} from '@/store/api/analyticsApi'
import PageHeader from '@/components/ui/PageHeader'
import StatsCard from '@/components/ui/StatsCard'
import RecentActivity from '@/components/dashboard/RecentActivity'
import QuickActions from '@/components/dashboard/QuickActions'
import LoadingSpinner from '@/components/ui/LoadingSpinner'

const DashboardPage: React.FC = () => {
  const currentUser = useAppSelector(selectCurrentUser)

  // Fetch dashboard data
  const {
    data: metrics,
    isLoading: metricsLoading
  } = useGetDashboardMetricsQuery()

  const {
    data: recentActivity,
    isLoading: activityLoading
  } = useGetRecentActivityQuery()

  if (metricsLoading || activityLoading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Welcome back, ${currentUser?.firstName || 'User'}!`}
        description="Here's what's happening with your account."
      />

      {/* Metrics Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard
          title="Total Revenue"
          value={metrics?.totalRevenue}
          format="currency"
          trend={metrics?.revenueTrend}
          icon="DollarSign"
        />
        <StatsCard
          title="Active Subscriptions"
          value={metrics?.activeSubscriptions}
          format="number"
          trend={metrics?.subscriptionsTrend}
          icon="Users"
        />
        <StatsCard
          title="Monthly Growth"
          value={metrics?.monthlyGrowth}
          format="percentage"
          trend={metrics?.growthTrend}
          icon="TrendingUp"
        />
        <StatsCard
          title="Customer Satisfaction"
          value={metrics?.customerSatisfaction}
          format="rating"
          trend={metrics?.satisfactionTrend}
          icon="Star"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Activity */}
        <div className="lg:col-span-2">
          <RecentActivity activities={recentActivity} />
        </div>

        {/* Quick Actions */}
        <div>
          <QuickActions />
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
```

### 1.3 List Page with Filters Template

```typescript
// Template: List page with search, filters, and pagination
// Path: src/pages/[module]/[ListPage].tsx

import React, { useState, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useGet[Items]Query } from '@/store/api/[module]Api'
import PageHeader from '@/components/ui/PageHeader'
import SearchAndFilter from '@/components/ui/SearchAndFilter'
import DataTable from '@/components/ui/DataTable'
import Pagination from '@/components/ui/Pagination'
import { [Item]Columns } from './[Item]Columns'

const [ListPage]: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams()

  // Extract query parameters
  const page = parseInt(searchParams.get('page') || '1', 10)
  const search = searchParams.get('search') || ''
  const sortBy = searchParams.get('sortBy') || 'createdAt'
  const sortOrder = searchParams.get('sortOrder') || 'desc'
  const filters = {
    status: searchParams.get('status'),
    category: searchParams.get('category'),
    dateRange: {
      from: searchParams.get('dateFrom'),
      to: searchParams.get('dateTo')
    }
  }

  // Fetch data with query parameters
  const {
    data: response,
    isLoading,
    error
  } = useGet[Items]Query({
    page,
    limit: 20,
    search,
    sortBy,
    sortOrder,
    ...filters
  })

  // Update URL parameters
  const updateParams = (updates: Record<string, string | null>) => {
    const newParams = new URLSearchParams(searchParams)

    Object.entries(updates).forEach(([key, value]) => {
      if (value === null || value === '') {
        newParams.delete(key)
      } else {
        newParams.set(key, value)
      }
    })

    setSearchParams(newParams)
  }

  // Search handler
  const handleSearch = (searchValue: string) => {
    updateParams({ search: searchValue, page: '1' })
  }

  // Filter handler
  const handleFilter = (filterKey: string, value: string | null) => {
    updateParams({ [filterKey]: value, page: '1' })
  }

  // Sort handler
  const handleSort = (column: string) => {
    const newOrder = sortBy === column && sortOrder === 'asc' ? 'desc' : 'asc'
    updateParams({ sortBy: column, sortOrder: newOrder })
  }

  // Pagination handler
  const handlePageChange = (newPage: number) => {
    updateParams({ page: newPage.toString() })
  }

  // Filter options
  const filterOptions = [
    {
      key: 'status',
      label: 'Status',
      options: [
        { value: 'active', label: 'Active' },
        { value: 'inactive', label: 'Inactive' },
        { value: 'pending', label: 'Pending' }
      ]
    },
    {
      key: 'category',
      label: 'Category',
      options: [
        { value: 'premium', label: 'Premium' },
        { value: 'standard', label: 'Standard' },
        { value: 'basic', label: 'Basic' }
      ]
    }
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="[Items]"
        description="Manage your [items] and their settings."
        action={
          <Button>
            Add [Item]
          </Button>
        }
      />

      <SearchAndFilter
        searchValue={search}
        onSearch={handleSearch}
        filters={filterOptions}
        activeFilters={filters}
        onFilter={handleFilter}
        showDateRange
        dateRange={filters.dateRange}
        onDateRangeChange={(range) => {
          updateParams({
            dateFrom: range.from,
            dateTo: range.to,
            page: '1'
          })
        }}
      />

      <DataTable
        data={response?.items || []}
        columns={[Item]Columns}
        isLoading={isLoading}
        error={error}
        sortBy={sortBy}
        sortOrder={sortOrder}
        onSort={handleSort}
      />

      {response && (
        <Pagination
          currentPage={page}
          totalPages={response.totalPages}
          totalItems={response.totalItems}
          onPageChange={handlePageChange}
        />
      )}
    </div>
  )
}

export default [ListPage]
```

---

## 2. API Integration Templates

### 2.1 RTK Query API Slice Template

```typescript
// Template: Complete RTK Query API slice
// Path: src/store/api/[module]Api.ts

import { createApi } from '@reduxjs/toolkit/query/react'
import { createValidatedBaseQuery, createValidatedEndpoint } from '@/lib/api/validation'
import {
  [Entity]Schema,
  Create[Entity]RequestSchema,
  Update[Entity]RequestSchema,
  [Entity]ListResponseSchema,
  type [Entity],
  type Create[Entity]Request,
  type Update[Entity]Request,
  type [Entity]ListResponse,
  type PaginationParams,
  type SortParams
} from '@/types/[module]'

export const [module]Api = createApi({
  reducerPath: '[module]Api',
  baseQuery: createValidatedBaseQuery('/api/v1/[module]', {
    validateResponse: true,
    timeout: 10000,
  }),
  tagTypes: ['[Entity]', '[RelatedEntity]'],
  endpoints: (builder) => ({

    // Query: Get paginated list
    get[Entities]: builder.query<
      [Entity]ListResponse,
      PaginationParams & SortParams & {
        search?: string
        status?: string
        category?: string
        dateFrom?: string
        dateTo?: string
      }
    >({
      query: (params) => ({
        url: '',
        params: {
          page: params.page || 1,
          limit: params.limit || 20,
          sortBy: params.sortBy || 'createdAt',
          sortOrder: params.sortOrder || 'desc',
          ...(params.search && { search: params.search }),
          ...(params.status && { status: params.status }),
          ...(params.category && { category: params.category }),
          ...(params.dateFrom && { dateFrom: params.dateFrom }),
          ...(params.dateTo && { dateTo: params.dateTo }),
        },
      }),
      transformResponse: (response: unknown) =>
        [Entity]ListResponseSchema.parse(response),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map(({ id }) => ({ type: '[Entity]' as const, id })),
              { type: '[Entity]', id: 'LIST' },
            ]
          : [{ type: '[Entity]', id: 'LIST' }],
    }),

    // Query: Get single entity
    get[Entity]: builder.query<[Entity], string>({
      query: (id) => `/${id}`,
      transformResponse: (response: unknown) => [Entity]Schema.parse(response),
      providesTags: (result, error, id) => [{ type: '[Entity]', id }],
    }),

    // Mutation: Create entity
    create[Entity]: builder.mutation<[Entity], Create[Entity]Request>({
      query: (data) => ({
        url: '',
        method: 'POST',
        body: data,
      }),
      transformResponse: (response: unknown) => [Entity]Schema.parse(response),
      invalidatesTags: [{ type: '[Entity]', id: 'LIST' }],
      // Optimistic update
      onQueryStarted: async (data, { dispatch, queryFulfilled }) => {
        try {
          const { data: created[Entity] } = await queryFulfilled

          // Update the cache optimistically
          dispatch(
            [module]Api.util.updateQueryData(
              'get[Entities]',
              { page: 1, limit: 20 }, // Default first page
              (draft) => {
                draft.items.unshift(created[Entity])
                draft.totalItems += 1
              }
            )
          )
        } catch {
          // Error handling is automatic
        }
      },
    }),

    // Mutation: Update entity
    update[Entity]: builder.mutation<
      [Entity],
      { id: string; data: Update[Entity]Request }
    >({
      query: ({ id, data }) => ({
        url: `/${id}`,
        method: 'PUT',
        body: data,
      }),
      transformResponse: (response: unknown) => [Entity]Schema.parse(response),
      invalidatesTags: (result, error, { id }) => [
        { type: '[Entity]', id },
        { type: '[Entity]', id: 'LIST' },
      ],
      // Optimistic update
      onQueryStarted: async ({ id, data }, { dispatch, queryFulfilled }) => {
        const patchResult = dispatch(
          [module]Api.util.updateQueryData('get[Entity]', id, (draft) => {
            Object.assign(draft, data)
          })
        )

        try {
          await queryFulfilled
        } catch {
          patchResult.undo()
        }
      },
    }),

    // Mutation: Delete entity
    delete[Entity]: builder.mutation<void, string>({
      query: (id) => ({
        url: `/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, id) => [
        { type: '[Entity]', id },
        { type: '[Entity]', id: 'LIST' },
      ],
      // Optimistic update
      onQueryStarted: async (id, { dispatch, queryFulfilled }) => {
        const patchResult = dispatch(
          [module]Api.util.updateQueryData(
            'get[Entities]',
            { page: 1, limit: 20 },
            (draft) => {
              draft.items = draft.items.filter(item => item.id !== id)
              draft.totalItems -= 1
            }
          )
        )

        try {
          await queryFulfilled
        } catch {
          patchResult.undo()
        }
      },
    }),

    // Query: Get related entities
    get[Entity][RelatedEntities]: builder.query<[RelatedEntity][], string>({
      query: (entityId) => `/${entityId}/[relatedEntities]`,
      transformResponse: (response: unknown) =>
        z.array([RelatedEntity]Schema).parse(response),
      providesTags: (result, error, entityId) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: '[RelatedEntity]' as const, id })),
              { type: '[RelatedEntity]', id: entityId },
            ]
          : [{ type: '[RelatedEntity]', id: entityId }],
    }),

  }),
})

// Export hooks
export const {
  useGet[Entities]Query,
  useGet[Entity]Query,
  useCreate[Entity]Mutation,
  useUpdate[Entity]Mutation,
  useDelete[Entity]Mutation,
  useGet[Entity][RelatedEntities]Query,
} = [module]Api

// Export utilities for manual cache management
export const {
  util: { prefetch, invalidateTags, updateQueryData }
} = [module]Api
```

### 2.2 Custom Hook Template

```typescript
// Template: Custom hook for complex business logic
// Path: src/hooks/use[FeatureName].ts

import { useCallback, useMemo } from 'react'
import { useAppSelector, useAppDispatch } from '@/store/hooks'
import {
  useGet[Entity]Query,
  useUpdate[Entity]Mutation,
  useCreate[Entity]Mutation,
} from '@/store/api/[module]Api'
import { selectCurrentUser } from '@/store/slices/authSlice'
import { toast } from 'react-hot-toast'

interface Use[FeatureName]Options {
  [entityId]?: string
  autoRefresh?: boolean
  onSuccess?: (data: [Entity]) => void
  onError?: (error: string) => void
}

interface Use[FeatureName]Return {
  // Data
  [entity]: [Entity] | undefined
  [entities]: [Entity][]

  // Loading states
  isLoading: boolean
  isSaving: boolean

  // Error states
  error: string | null

  // Actions
  create[Entity]: (data: Create[Entity]Request) => Promise<[Entity]>
  update[Entity]: (id: string, data: Update[Entity]Request) => Promise<[Entity]>
  refresh: () => void

  // Computed values
  canEdit: boolean
  canDelete: boolean

  // Utilities
  get[Entity]ById: (id: string) => [Entity] | undefined
}

export const use[FeatureName] = ({
  [entityId],
  autoRefresh = false,
  onSuccess,
  onError
}: Use[FeatureName]Options = {}): Use[FeatureName]Return => {
  const dispatch = useAppDispatch()
  const currentUser = useAppSelector(selectCurrentUser)

  // Queries
  const {
    data: [entity],
    isLoading: entityLoading,
    error: entityError,
    refetch: refetchEntity
  } = useGet[Entity]Query([entityId]!, {
    skip: ![entityId],
    pollingInterval: autoRefresh ? 30000 : undefined,
  })

  const {
    data: [entities]Response,
    isLoading: entitiesLoading,
    error: entitiesError,
    refetch: refetchEntities
  } = useGet[Entities]Query({
    page: 1,
    limit: 100,
  })

  // Mutations
  const [
    create[Entity]Mutation,
    { isLoading: isCreating, error: createError }
  ] = useCreate[Entity]Mutation()

  const [
    update[Entity]Mutation,
    { isLoading: isUpdating, error: updateError }
  ] = useUpdate[Entity]Mutation()

  // Computed states
  const isLoading = entityLoading || entitiesLoading
  const isSaving = isCreating || isUpdating
  const error = entityError || entitiesError || createError || updateError
  const [entities] = [entities]Response?.items || []

  // Permissions
  const canEdit = useMemo(() => {
    return currentUser?.permissions?.includes('[EDIT_PERMISSION]') || false
  }, [currentUser])

  const canDelete = useMemo(() => {
    return currentUser?.permissions?.includes('[DELETE_PERMISSION]') || false
  }, [currentUser])

  // Actions
  const create[Entity] = useCallback(async (data: Create[Entity]Request): Promise<[Entity]> => {
    try {
      const result = await create[Entity]Mutation(data).unwrap()

      toast.success('[Entity] created successfully')
      onSuccess?.(result)

      return result
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to create [entity]'
      toast.error(message)
      onError?.(message)
      throw error
    }
  }, [create[Entity]Mutation, onSuccess, onError])

  const update[Entity] = useCallback(async (
    id: string,
    data: Update[Entity]Request
  ): Promise<[Entity]> => {
    try {
      const result = await update[Entity]Mutation({ id, data }).unwrap()

      toast.success('[Entity] updated successfully')
      onSuccess?.(result)

      return result
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to update [entity]'
      toast.error(message)
      onError?.(message)
      throw error
    }
  }, [update[Entity]Mutation, onSuccess, onError])

  const refresh = useCallback(() => {
    refetchEntity()
    refetchEntities()
  }, [refetchEntity, refetchEntities])

  // Utilities
  const get[Entity]ById = useCallback((id: string): [Entity] | undefined => {
    return [entities].find(entity => entity.id === id)
  }, [[entities]])

  return {
    // Data
    [entity],
    [entities],

    // Loading states
    isLoading,
    isSaving,

    // Error states
    error: error ? String(error) : null,

    // Actions
    create[Entity],
    update[Entity],
    refresh,

    // Computed values
    canEdit,
    canDelete,

    // Utilities
    get[Entity]ById,
  }
}
```

---

## 3. Form Component Templates

### 3.1 React Hook Form with Zod Template

```typescript
// Template: Form component with React Hook Form + Zod validation
// Path: src/components/[module]/[EntityName]Form.tsx

import React from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Input,
  Select,
  Textarea,
  Checkbox,
  Button,
  Card,
  FormField,
  FormMessage
} from '@/components/ui'

// Zod schema for form validation
const [Entity]FormSchema = z.object({
  name: z.string()
    .min(1, 'Name is required')
    .max(100, 'Name must be less than 100 characters'),
  email: z.string()
    .email('Invalid email address')
    .optional()
    .or(z.literal('')),
  category: z.enum(['CATEGORY_1', 'CATEGORY_2', 'CATEGORY_3'], {
    required_error: 'Please select a category'
  }),
  description: z.string()
    .max(500, 'Description must be less than 500 characters')
    .optional(),
  isActive: z.boolean().default(true),
  tags: z.array(z.string()).optional(),
  metadata: z.record(z.string(), z.unknown()).optional(),
})

type [Entity]FormData = z.infer<typeof [Entity]FormSchema>

interface [Entity]FormProps {
  initialData?: Partial<[Entity]FormData>
  onSubmit: (data: [Entity]FormData) => Promise<void>
  onCancel?: () => void
  isLoading?: boolean
  mode?: 'create' | 'edit'
}

const [Entity]Form: React.FC<[Entity]FormProps> = ({
  initialData,
  onSubmit,
  onCancel,
  isLoading = false,
  mode = 'create'
}) => {
  const form = useForm<[Entity]FormData>({
    resolver: zodResolver([Entity]FormSchema),
    defaultValues: {
      name: '',
      email: '',
      category: undefined,
      description: '',
      isActive: true,
      tags: [],
      metadata: {},
      ...initialData,
    },
    mode: 'onBlur', // Validate on blur for better UX
  })

  const {
    control,
    handleSubmit,
    formState: { errors, isValid, isDirty },
    watch,
    setValue,
    reset
  } = form

  // Watch form values for dependent fields
  const watchedCategory = watch('category')

  // Handle form submission
  const handleFormSubmit = async (data: [Entity]FormData) => {
    try {
      await onSubmit(data)
      if (mode === 'create') {
        reset() // Reset form after successful creation
      }
    } catch (error) {
      // Error handling is done in parent component
      console.error('Form submission error:', error)
    }
  }

  // Category options
  const categoryOptions = [
    { value: 'CATEGORY_1', label: 'Category 1' },
    { value: 'CATEGORY_2', label: 'Category 2' },
    { value: 'CATEGORY_3', label: 'Category 3' },
  ]

  return (
    <Card className="p-6">
      <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
        {/* Basic Information Section */}
        <div className="space-y-4">
          <h3 className="text-lg font-medium text-gray-900">
            Basic Information
          </h3>

          <FormField>
            <Controller
              name="name"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  label="Name"
                  placeholder="Enter [entity] name"
                  error={errors.name?.message}
                  required
                />
              )}
            />
          </FormField>

          <FormField>
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  type="email"
                  label="Email"
                  placeholder="Enter email address"
                  error={errors.email?.message}
                />
              )}
            />
          </FormField>

          <FormField>
            <Controller
              name="category"
              control={control}
              render={({ field }) => (
                <Select
                  {...field}
                  label="Category"
                  placeholder="Select a category"
                  options={categoryOptions}
                  error={errors.category?.message}
                  required
                />
              )}
            />
          </FormField>
        </div>

        {/* Additional Information Section */}
        <div className="space-y-4">
          <h3 className="text-lg font-medium text-gray-900">
            Additional Information
          </h3>

          <FormField>
            <Controller
              name="description"
              control={control}
              render={({ field }) => (
                <Textarea
                  {...field}
                  label="Description"
                  placeholder="Enter description"
                  rows={4}
                  error={errors.description?.message}
                />
              )}
            />
          </FormField>

          <FormField>
            <Controller
              name="isActive"
              control={control}
              render={({ field: { value, onChange, ...field } }) => (
                <Checkbox
                  {...field}
                  checked={value}
                  onCheckedChange={onChange}
                  label="Active"
                  description="Enable this [entity] to be available for use"
                />
              )}
            />
          </FormField>
        </div>

        {/* Conditional Fields based on category */}
        {watchedCategory === 'CATEGORY_1' && (
          <div className="space-y-4">
            <h3 className="text-lg font-medium text-gray-900">
              Category 1 Settings
            </h3>
            {/* Add category-specific fields here */}
          </div>
        )}

        {/* Form Actions */}
        <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200">
          {onCancel && (
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={isLoading}
            >
              Cancel
            </Button>
          )}

          <Button
            type="submit"
            isLoading={isLoading}
            disabled={!isValid || (!isDirty && mode === 'edit')}
          >
            {mode === 'create' ? 'Create [Entity]' : 'Update [Entity]'}
          </Button>
        </div>

        {/* Development helper - remove in production */}
        {import.meta.env.DEV && (
          <details className="mt-4 p-4 bg-gray-50 rounded">
            <summary className="cursor-pointer text-sm text-gray-600">
              Form Debug Info
            </summary>
            <pre className="mt-2 text-xs text-gray-500">
              {JSON.stringify({
                values: form.getValues(),
                errors,
                isValid,
                isDirty
              }, null, 2)}
            </pre>
          </details>
        )}
      </form>
    </Card>
  )
}

export default [Entity]Form
```

### 3.2 Multi-Step Form Template

```typescript
// Template: Multi-step form with progress indicator
// Path: src/components/[module]/[MultiStep]Wizard.tsx

import React, { useState, useCallback } from 'react'
import { useForm, FormProvider } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Card,
  Button,
  Progress,
  StepIndicator
} from '@/components/ui'

// Step schemas
const Step1Schema = z.object({
  basicInfo: z.object({
    name: z.string().min(1, 'Name is required'),
    email: z.string().email('Invalid email'),
  })
})

const Step2Schema = z.object({
  details: z.object({
    category: z.string().min(1, 'Category is required'),
    description: z.string().optional(),
  })
})

const Step3Schema = z.object({
  settings: z.object({
    isActive: z.boolean(),
    notifications: z.boolean(),
  })
})

// Combined schema for final validation
const CompleteWizardSchema = Step1Schema.merge(Step2Schema).merge(Step3Schema)

type WizardData = z.infer<typeof CompleteWizardSchema>

interface [MultiStep]WizardProps {
  onComplete: (data: WizardData) => Promise<void>
  onCancel?: () => void
  initialData?: Partial<WizardData>
}

const steps = [
  { id: 1, title: 'Basic Information', description: 'Enter basic details' },
  { id: 2, title: 'Details', description: 'Provide additional information' },
  { id: 3, title: 'Settings', description: 'Configure settings' },
  { id: 4, title: 'Review', description: 'Review and confirm' },
]

const [MultiStep]Wizard: React.FC<[MultiStep]WizardProps> = ({
  onComplete,
  onCancel,
  initialData
}) => {
  const [currentStep, setCurrentStep] = useState(1)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Form setup with complete schema for final validation
  const methods = useForm<WizardData>({
    resolver: zodResolver(CompleteWizardSchema),
    defaultValues: {
      basicInfo: { name: '', email: '' },
      details: { category: '', description: '' },
      settings: { isActive: true, notifications: true },
      ...initialData,
    },
    mode: 'onChange',
  })

  const { trigger, getValues, formState: { errors } } = methods

  // Validate current step
  const validateStep = useCallback(async (step: number): Promise<boolean> => {
    switch (step) {
      case 1:
        return await trigger(['basicInfo.name', 'basicInfo.email'])
      case 2:
        return await trigger(['details.category'])
      case 3:
        return await trigger(['settings.isActive', 'settings.notifications'])
      default:
        return true
    }
  }, [trigger])

  // Navigation handlers
  const handleNext = async () => {
    const isStepValid = await validateStep(currentStep)

    if (isStepValid && currentStep < steps.length) {
      setCurrentStep(currentStep + 1)
    }
  }

  const handlePrevious = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1)
    }
  }

  const handleStepClick = async (step: number) => {
    // Only allow going to previous steps or if current step is valid
    if (step < currentStep || await validateStep(currentStep)) {
      setCurrentStep(step)
    }
  }

  // Final submission
  const handleComplete = async () => {
    const isValid = await trigger()

    if (!isValid) {
      // Find first error and go to that step
      const errorFields = Object.keys(errors)
      if (errorFields.length > 0) {
        const firstError = errorFields[0]
        if (firstError.startsWith('basicInfo')) setCurrentStep(1)
        else if (firstError.startsWith('details')) setCurrentStep(2)
        else if (firstError.startsWith('settings')) setCurrentStep(3)
      }
      return
    }

    setIsSubmitting(true)
    try {
      await onComplete(getValues())
    } finally {
      setIsSubmitting(false)
    }
  }

  const progress = (currentStep / steps.length) * 100

  return (
    <div className="max-w-4xl mx-auto">
      {/* Progress Header */}
      <div className="mb-8">
        <Progress value={progress} className="mb-4" />
        <StepIndicator
          steps={steps}
          currentStep={currentStep}
          onStepClick={handleStepClick}
        />
      </div>

      <FormProvider {...methods}>
        <Card className="p-6">
          {/* Step Content */}
          <div className="min-h-96">
            {currentStep === 1 && <Step1BasicInfo />}
            {currentStep === 2 && <Step2Details />}
            {currentStep === 3 && <Step3Settings />}
            {currentStep === 4 && <Step4Review />}
          </div>

          {/* Navigation */}
          <div className="flex justify-between items-center pt-6 border-t border-gray-200">
            <div>
              {currentStep > 1 && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={handlePrevious}
                  disabled={isSubmitting}
                >
                  Previous
                </Button>
              )}
            </div>

            <div className="flex space-x-3">
              {onCancel && (
                <Button
                  type="button"
                  variant="ghost"
                  onClick={onCancel}
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
              )}

              {currentStep < steps.length ? (
                <Button
                  type="button"
                  onClick={handleNext}
                  disabled={isSubmitting}
                >
                  Next
                </Button>
              ) : (
                <Button
                  type="button"
                  onClick={handleComplete}
                  isLoading={isSubmitting}
                >
                  Complete Setup
                </Button>
              )}
            </div>
          </div>
        </Card>
      </FormProvider>
    </div>
  )
}

// Step Components
const Step1BasicInfo: React.FC = () => {
  const { control, formState: { errors } } = useFormContext<WizardData>()

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-gray-900">Basic Information</h2>
        <p className="text-gray-600">Let's start with the basic details.</p>
      </div>

      <div className="space-y-4">
        <Controller
          name="basicInfo.name"
          control={control}
          render={({ field }) => (
            <Input
              {...field}
              label="Name"
              placeholder="Enter name"
              error={errors.basicInfo?.name?.message}
              required
            />
          )}
        />

        <Controller
          name="basicInfo.email"
          control={control}
          render={({ field }) => (
            <Input
              {...field}
              type="email"
              label="Email"
              placeholder="Enter email"
              error={errors.basicInfo?.email?.message}
              required
            />
          )}
        />
      </div>
    </div>
  )
}

// ... implement other step components similarly

export default [MultiStep]Wizard
```

---

## 4. State Management Templates

### 4.1 Redux Slice Template

```typescript
// Template: Redux slice for client-side state
// Path: src/store/slices/[feature]Slice.ts

import { createSlice, createSelector, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/store'

// Types
interface [Feature]State {
  // UI state
  isLoading: boolean
  error: string | null

  // Feature-specific state
  selectedItems: string[]
  filters: {
    search: string
    category: string | null
    status: string | null
    dateRange: {
      from: string | null
      to: string | null
    }
  }

  // UI preferences
  viewMode: 'grid' | 'list'
  sortBy: string
  sortOrder: 'asc' | 'desc'

  // Modal/dialog state
  modals: {
    create: boolean
    edit: boolean
    delete: boolean
  }

  // Pagination state
  pagination: {
    page: number
    limit: number
    total: number
  }
}

// Initial state
const initialState: [Feature]State = {
  isLoading: false,
  error: null,
  selectedItems: [],
  filters: {
    search: '',
    category: null,
    status: null,
    dateRange: {
      from: null,
      to: null,
    },
  },
  viewMode: 'grid',
  sortBy: 'createdAt',
  sortOrder: 'desc',
  modals: {
    create: false,
    edit: false,
    delete: false,
  },
  pagination: {
    page: 1,
    limit: 20,
    total: 0,
  },
}

// Slice
const [feature]Slice = createSlice({
  name: '[feature]',
  initialState,
  reducers: {
    // Loading state
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload
    },

    // Error state
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload
    },
    clearError: (state) => {
      state.error = null
    },

    // Selection
    selectItem: (state, action: PayloadAction<string>) => {
      if (!state.selectedItems.includes(action.payload)) {
        state.selectedItems.push(action.payload)
      }
    },
    deselectItem: (state, action: PayloadAction<string>) => {
      state.selectedItems = state.selectedItems.filter(id => id !== action.payload)
    },
    selectAllItems: (state, action: PayloadAction<string[]>) => {
      state.selectedItems = action.payload
    },
    clearSelection: (state) => {
      state.selectedItems = []
    },

    // Filters
    setSearchFilter: (state, action: PayloadAction<string>) => {
      state.filters.search = action.payload
      state.pagination.page = 1 // Reset to first page
    },
    setCategoryFilter: (state, action: PayloadAction<string | null>) => {
      state.filters.category = action.payload
      state.pagination.page = 1
    },
    setStatusFilter: (state, action: PayloadAction<string | null>) => {
      state.filters.status = action.payload
      state.pagination.page = 1
    },
    setDateRangeFilter: (state, action: PayloadAction<{
      from: string | null
      to: string | null
    }>) => {
      state.filters.dateRange = action.payload
      state.pagination.page = 1
    },
    clearFilters: (state) => {
      state.filters = initialState.filters
      state.pagination.page = 1
    },

    // View preferences
    setViewMode: (state, action: PayloadAction<'grid' | 'list'>) => {
      state.viewMode = action.payload
    },
    setSorting: (state, action: PayloadAction<{
      sortBy: string
      sortOrder: 'asc' | 'desc'
    }>) => {
      state.sortBy = action.payload.sortBy
      state.sortOrder = action.payload.sortOrder
    },

    // Modal state
    openModal: (state, action: PayloadAction<keyof [Feature]State['modals']>) => {
      state.modals[action.payload] = true
    },
    closeModal: (state, action: PayloadAction<keyof [Feature]State['modals']>) => {
      state.modals[action.payload] = false
    },
    closeAllModals: (state) => {
      Object.keys(state.modals).forEach(key => {
        state.modals[key as keyof typeof state.modals] = false
      })
    },

    // Pagination
    setPage: (state, action: PayloadAction<number>) => {
      state.pagination.page = action.payload
    },
    setLimit: (state, action: PayloadAction<number>) => {
      state.pagination.limit = action.payload
      state.pagination.page = 1 // Reset to first page
    },
    setTotal: (state, action: PayloadAction<number>) => {
      state.pagination.total = action.payload
    },

    // Reset state
    reset: () => initialState,
  },
})

// Actions
export const {
  setLoading,
  setError,
  clearError,
  selectItem,
  deselectItem,
  selectAllItems,
  clearSelection,
  setSearchFilter,
  setCategoryFilter,
  setStatusFilter,
  setDateRangeFilter,
  clearFilters,
  setViewMode,
  setSorting,
  openModal,
  closeModal,
  closeAllModals,
  setPage,
  setLimit,
  setTotal,
  reset,
} = [feature]Slice.actions

// Basic selectors
export const select[Feature]State = (state: RootState) => state.[feature]
export const select[Feature]Loading = (state: RootState) => state.[feature].isLoading
export const select[Feature]Error = (state: RootState) => state.[feature].error
export const select[Feature]SelectedItems = (state: RootState) => state.[feature].selectedItems
export const select[Feature]Filters = (state: RootState) => state.[feature].filters
export const select[Feature]ViewMode = (state: RootState) => state.[feature].viewMode
export const select[Feature]Sorting = (state: RootState) => ({
  sortBy: state.[feature].sortBy,
  sortOrder: state.[feature].sortOrder,
})
export const select[Feature]Modals = (state: RootState) => state.[feature].modals
export const select[Feature]Pagination = (state: RootState) => state.[feature].pagination

// Memoized selectors
export const select[Feature]HasActiveFilters = createSelector(
  [select[Feature]Filters],
  (filters) => {
    return !!(
      filters.search ||
      filters.category ||
      filters.status ||
      filters.dateRange.from ||
      filters.dateRange.to
    )
  }
)

export const select[Feature]HasSelection = createSelector(
  [select[Feature]SelectedItems],
  (selectedItems) => selectedItems.length > 0
)

export const select[Feature]SelectionCount = createSelector(
  [select[Feature]SelectedItems],
  (selectedItems) => selectedItems.length
)

export const select[Feature]PaginationInfo = createSelector(
  [select[Feature]Pagination],
  (pagination) => ({
    ...pagination,
    totalPages: Math.ceil(pagination.total / pagination.limit),
    hasNextPage: pagination.page < Math.ceil(pagination.total / pagination.limit),
    hasPreviousPage: pagination.page > 1,
    startItem: (pagination.page - 1) * pagination.limit + 1,
    endItem: Math.min(pagination.page * pagination.limit, pagination.total),
  })
)

// Export reducer
export default [feature]Slice.reducer
```

---

## 5. Testing Templates

### 5.1 Component Test Template

```typescript
// Template: Component test with React Testing Library
// Path: src/components/[module]/__tests__/[Component].test.tsx

import React from 'react'
import {
  render,
  screen,
  fireEvent,
  waitFor,
  within
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { configureStore } from '@reduxjs/toolkit'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'

import [Component] from '../[Component]'
import { [module]Api } from '@/store/api/[module]Api'
import [feature]Reducer from '@/store/slices/[feature]Slice'

// Mock data
const mock[Entity] = {
  id: '1',
  name: 'Test [Entity]',
  email: 'test@example.com',
  status: 'active',
  createdAt: '2023-01-01T00:00:00Z',
  updatedAt: '2023-01-01T00:00:00Z',
}

const mock[Entities] = [
  mock[Entity],
  {
    id: '2',
    name: 'Test [Entity] 2',
    email: 'test2@example.com',
    status: 'inactive',
    createdAt: '2023-01-02T00:00:00Z',
    updatedAt: '2023-01-02T00:00:00Z',
  },
]

// Test utilities
const createTestStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      [feature]: [feature]Reducer,
      [[module]Api.reducerPath]: [module]Api.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat([module]Api.middleware),
    preloadedState: initialState,
  })
}

const createQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
})

interface RenderOptions {
  initialState?: any
  route?: string
}

const renderWithProviders = (
  ui: React.ReactElement,
  {
    initialState = {},
    route = '/',
  }: RenderOptions = {}
) => {
  const store = createTestStore(initialState)
  const queryClient = createQueryClient()
  const user = userEvent.setup()

  const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>
          {children}
        </MemoryRouter>
      </QueryClientProvider>
    </Provider>
  )

  return {
    user,
    store,
    queryClient,
    ...render(ui, { wrapper: Wrapper }),
  }
}

// Mock API calls
const mock[Module]Api = {
  useGet[Entities]Query: vi.fn(),
  useGet[Entity]Query: vi.fn(),
  useCreate[Entity]Mutation: vi.fn(),
  useUpdate[Entity]Mutation: vi.fn(),
  useDelete[Entity]Mutation: vi.fn(),
}

vi.mock('@/store/api/[module]Api', () => ({
  [module]Api: mock[Module]Api,
}))

describe('[Component]', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Loading State', () => {
    it('shows loading spinner when data is loading', () => {
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: undefined,
        isLoading: true,
        error: null,
      })

      renderWithProviders(<[Component] />)

      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
    })
  })

  describe('Error State', () => {
    it('shows error message when API call fails', () => {
      const errorMessage = 'Failed to load data'
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: { message: errorMessage },
      })

      renderWithProviders(<[Component] />)

      expect(screen.getByText(/failed to load/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
    })

    it('allows retrying after error', async () => {
      const refetch = vi.fn()
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: { message: 'Error' },
        refetch,
      })

      const { user } = renderWithProviders(<[Component] />)

      const retryButton = screen.getByRole('button', { name: /try again/i })
      await user.click(retryButton)

      expect(refetch).toHaveBeenCalledTimes(1)
    })
  })

  describe('Data Display', () => {
    beforeEach(() => {
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: { items: mock[Entities], totalItems: mock[Entities].length },
        isLoading: false,
        error: null,
      })
    })

    it('displays list of entities', () => {
      renderWithProviders(<[Component] />)

      mock[Entities].forEach(entity => {
        expect(screen.getByText(entity.name)).toBeInTheDocument()
        expect(screen.getByText(entity.email)).toBeInTheDocument()
      })
    })

    it('shows empty state when no data', () => {
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: { items: [], totalItems: 0 },
        isLoading: false,
        error: null,
      })

      renderWithProviders(<[Component] />)

      expect(screen.getByText(/no [entities] found/i)).toBeInTheDocument()
    })
  })

  describe('User Interactions', () => {
    beforeEach(() => {
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: { items: mock[Entities], totalItems: mock[Entities].length },
        isLoading: false,
        error: null,
      })

      mock[Module]Api.useCreate[Entity]Mutation.mockReturnValue([
        vi.fn().mockResolvedValue({ unwrap: () => Promise.resolve(mock[Entity]) }),
        { isLoading: false, error: null },
      ])
    })

    it('opens create modal when add button is clicked', async () => {
      const { user } = renderWithProviders(<[Component] />)

      const addButton = screen.getByRole('button', { name: /add [entity]/i })
      await user.click(addButton)

      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText(/create [entity]/i)).toBeInTheDocument()
    })

    it('filters entities by search term', async () => {
      const { user } = renderWithProviders(<[Component] />)

      const searchInput = screen.getByPlaceholderText(/search/i)
      await user.type(searchInput, 'Test [Entity] 2')

      // Verify search state is updated (you might need to mock the search functionality)
      expect(searchInput).toHaveValue('Test [Entity] 2')
    })

    it('selects entity when checkbox is clicked', async () => {
      const { user } = renderWithProviders(<[Component] />)

      const firstEntityRow = screen.getByTestId(`entity-row-${mock[Entities][0].id}`)
      const checkbox = within(firstEntityRow).getByRole('checkbox')

      await user.click(checkbox)

      expect(checkbox).toBeChecked()
    })
  })

  describe('Form Interactions', () => {
    it('submits form with valid data', async () => {
      const createMutation = vi.fn().mockResolvedValue({
        unwrap: () => Promise.resolve(mock[Entity])
      })

      mock[Module]Api.useCreate[Entity]Mutation.mockReturnValue([
        createMutation,
        { isLoading: false, error: null },
      ])

      const { user } = renderWithProviders(<[Component] />)

      // Open create modal
      const addButton = screen.getByRole('button', { name: /add [entity]/i })
      await user.click(addButton)

      // Fill form
      const nameInput = screen.getByLabelText(/name/i)
      const emailInput = screen.getByLabelText(/email/i)

      await user.type(nameInput, 'New [Entity]')
      await user.type(emailInput, 'new@example.com')

      // Submit form
      const submitButton = screen.getByRole('button', { name: /create [entity]/i })
      await user.click(submitButton)

      await waitFor(() => {
        expect(createMutation).toHaveBeenCalledWith({
          name: 'New [Entity]',
          email: 'new@example.com',
        })
      })
    })

    it('shows validation errors for invalid data', async () => {
      const { user } = renderWithProviders(<[Component] />)

      // Open create modal
      const addButton = screen.getByRole('button', { name: /add [entity]/i })
      await user.click(addButton)

      // Submit without filling required fields
      const submitButton = screen.getByRole('button', { name: /create [entity]/i })
      await user.click(submitButton)

      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument()
      })
    })
  })

  describe('Accessibility', () => {
    beforeEach(() => {
      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: { items: mock[Entities], totalItems: mock[Entities].length },
        isLoading: false,
        error: null,
      })
    })

    it('has proper ARIA labels and roles', () => {
      renderWithProviders(<[Component] />)

      expect(screen.getByRole('main')).toBeInTheDocument()
      expect(screen.getByRole('table')).toBeInTheDocument()
      expect(screen.getAllByRole('row')).toHaveLength(mock[Entities].length + 1) // +1 for header
    })

    it('supports keyboard navigation', async () => {
      const { user } = renderWithProviders(<[Component] />)

      const addButton = screen.getByRole('button', { name: /add [entity]/i })

      // Tab to the button
      await user.tab()
      expect(addButton).toHaveFocus()

      // Press Enter to activate
      await user.keyboard('{Enter}')
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
  })

  describe('Performance', () => {
    it('does not re-render unnecessarily', () => {
      const renderSpy = vi.fn()

      const TestComponent = () => {
        renderSpy()
        return <[Component] />
      }

      mock[Module]Api.useGet[Entities]Query.mockReturnValue({
        data: { items: mock[Entities], totalItems: mock[Entities].length },
        isLoading: false,
        error: null,
      })

      const { rerender } = renderWithProviders(<TestComponent />)

      expect(renderSpy).toHaveBeenCalledTimes(1)

      // Re-render with same props
      rerender(<TestComponent />)

      // Should only render once more due to React.memo or similar optimization
      expect(renderSpy).toHaveBeenCalledTimes(2)
    })
  })
})
```

### 5.2 E2E Test Template

```typescript
// Template: End-to-end test with Playwright
// Path: tests/e2e/[feature].spec.ts

import { test, expect, Page } from '@playwright/test'

// Test data
const testUser = {
  email: 'test@example.com',
  password: 'password123',
  firstName: 'Test',
  lastName: 'User',
}

const test[Entity] = {
  name: 'Test [Entity]',
  email: 'entity@example.com',
  category: 'CATEGORY_1',
  description: 'Test description',
}

// Page object model
class [Feature]Page {
  constructor(private page: Page) {}

  // Navigation
  async navigateTo() {
    await this.page.goto('/[feature]')
    await this.page.waitForLoadState('networkidle')
  }

  // Elements
  get addButton() {
    return this.page.getByRole('button', { name: /add [entity]/i })
  }

  get searchInput() {
    return this.page.getByPlaceholder(/search/i)
  }

  get loadingSpinner() {
    return this.page.getByTestId('loading-spinner')
  }

  get errorMessage() {
    return this.page.getByTestId('error-message')
  }

  get entityTable() {
    return this.page.getByRole('table')
  }

  // Actions
  async searchFor(term: string) {
    await this.searchInput.fill(term)
    await this.page.keyboard.press('Enter')
    await this.page.waitForLoadState('networkidle')
  }

  async openCreateModal() {
    await this.addButton.click()
    await this.page.waitForSelector('[role="dialog"]')
  }

  async fillCreateForm(data: typeof test[Entity]) {
    await this.page.getByLabel(/name/i).fill(data.name)
    await this.page.getByLabel(/email/i).fill(data.email)
    await this.page.getByLabel(/category/i).selectOption(data.category)

    if (data.description) {
      await this.page.getByLabel(/description/i).fill(data.description)
    }
  }

  async submitCreateForm() {
    await this.page.getByRole('button', { name: /create [entity]/i }).click()
    await this.page.waitForLoadState('networkidle')
  }

  async getEntityByName(name: string) {
    return this.page.getByRole('row').filter({ hasText: name })
  }

  async selectEntity(name: string) {
    const entityRow = await this.getEntityByName(name)
    await entityRow.getByRole('checkbox').check()
  }

  async deleteSelectedEntities() {
    await this.page.getByRole('button', { name: /delete selected/i }).click()
    await this.page.getByRole('button', { name: /confirm/i }).click()
    await this.page.waitForLoadState('networkidle')
  }
}

// Authentication helper
async function loginAsTestUser(page: Page) {
  await page.goto('/auth/login')
  await page.getByTestId('email').fill(testUser.email)
  await page.getByTestId('password').fill(testUser.password)
  await page.getByTestId('login-button').click()
  await page.waitForURL('/dashboard')
}

test.describe('[Feature] Management', () => {
  let [feature]Page: [Feature]Page

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await loginAsTestUser(page)

    // Initialize page object
    [feature]Page = new [Feature]Page(page)
    await [feature]Page.navigateTo()
  })

  test.describe('Page Load', () => {
    test('loads [feature] page successfully', async ({ page }) => {
      await expect(page).toHaveTitle(/[feature]/i)
      await expect([feature]Page.entityTable).toBeVisible()
    })

    test('shows loading state initially', async ({ page }) => {
      // Navigate to page and check for loading state
      await page.goto('/[feature]')

      // Loading spinner should be visible briefly
      await expect([feature]Page.loadingSpinner).toBeVisible()

      // Then data should load
      await expect([feature]Page.entityTable).toBeVisible()
      await expect([feature]Page.loadingSpinner).not.toBeVisible()
    })

    test('handles API errors gracefully', async ({ page }) => {
      // Mock API error
      await page.route('/api/v1/[feature]*', route => {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Internal server error' }),
        })
      })

      await [feature]Page.navigateTo()

      await expect([feature]Page.errorMessage).toBeVisible()
      await expect([feature]Page.errorMessage).toContainText(/failed to load/i)
    })
  })

  test.describe('Entity Creation', () => {
    test('creates new [entity] successfully', async ({ page }) => {
      await [feature]Page.openCreateModal()

      // Verify modal opened
      await expect(page.getByRole('dialog')).toBeVisible()
      await expect(page.getByText(/create [entity]/i)).toBeVisible()

      // Fill form
      await [feature]Page.fillCreateForm(test[Entity])

      // Submit form
      await [feature]Page.submitCreateForm()

      // Verify success
      await expect(page.getByText(/[entity] created successfully/i)).toBeVisible()
      await expect([feature]Page.getEntityByName(test[Entity].name)).toBeVisible()
    })

    test('validates required fields', async ({ page }) => {
      await [feature]Page.openCreateModal()

      // Try to submit without filling required fields
      await page.getByRole('button', { name: /create [entity]/i }).click()

      // Check validation errors
      await expect(page.getByText(/name is required/i)).toBeVisible()
      await expect(page.getByText(/email is required/i)).toBeVisible()
    })

    test('cancels creation', async ({ page }) => {
      await [feature]Page.openCreateModal()

      // Click cancel
      await page.getByRole('button', { name: /cancel/i }).click()

      // Modal should close
      await expect(page.getByRole('dialog')).not.toBeVisible()
    })
  })

  test.describe('Search and Filter', () => {
    test('searches entities by name', async ({ page }) => {
      // Assume we have test data
      await [feature]Page.searchFor('Test')

      // Check that search results are filtered
      const searchResults = page.getByRole('row').filter({ hasText: 'Test' })
      await expect(searchResults).toHaveCount.greaterThan(0)
    })

    test('shows no results for non-existent search', async ({ page }) => {
      await [feature]Page.searchFor('NonExistentEntity123')

      await expect(page.getByText(/no [entities] found/i)).toBeVisible()
    })

    test('clears search filter', async ({ page }) => {
      // Search for something
      await [feature]Page.searchFor('Test')

      // Clear search
      await [feature]Page.searchInput.clear()
      await page.keyboard.press('Enter')
      await page.waitForLoadState('networkidle')

      // Should show all entities again
      const allRows = page.getByRole('row')
      await expect(allRows).toHaveCount.greaterThan(1)
    })
  })

  test.describe('Entity Management', () => {
    test('selects and deletes entities', async ({ page }) => {
      // Create test entity first
      await [feature]Page.openCreateModal()
      await [feature]Page.fillCreateForm({
        ...test[Entity],
        name: 'Entity to Delete',
      })
      await [feature]Page.submitCreateForm()

      // Select the entity
      await [feature]Page.selectEntity('Entity to Delete')

      // Delete it
      await [feature]Page.deleteSelectedEntities()

      // Verify deletion
      await expect(page.getByText(/deleted successfully/i)).toBeVisible()
      await expect([feature]Page.getEntityByName('Entity to Delete')).not.toBeVisible()
    })

    test('bulk selects entities', async ({ page }) => {
      // Select all checkbox
      const selectAllCheckbox = page.getByRole('checkbox').first()
      await selectAllCheckbox.check()

      // Verify all entities are selected
      const entityCheckboxes = page.getByRole('row').getByRole('checkbox')
      const count = await entityCheckboxes.count()

      for (let i = 1; i < count; i++) { // Skip header checkbox
        await expect(entityCheckboxes.nth(i)).toBeChecked()
      }
    })
  })

  test.describe('Responsive Design', () => {
    test('works on mobile viewport', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 })
      await [feature]Page.navigateTo()

      // Check mobile-specific elements
      await expect([feature]Page.entityTable).toBeVisible()

      // Mobile menu should be available
      const mobileMenuButton = page.getByRole('button', { name: /menu/i })
      if (await mobileMenuButton.isVisible()) {
        await mobileMenuButton.click()
        await expect(page.getByRole('navigation')).toBeVisible()
      }
    })

    test('works on tablet viewport', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 })
      await [feature]Page.navigateTo()

      await expect([feature]Page.entityTable).toBeVisible()
      await expect([feature]Page.addButton).toBeVisible()
    })
  })

  test.describe('Keyboard Navigation', () => {
    test('navigates with keyboard', async ({ page }) => {
      // Tab through interactive elements
      await page.keyboard.press('Tab')
      await expect([feature]Page.searchInput).toBeFocused()

      await page.keyboard.press('Tab')
      await expect([feature]Page.addButton).toBeFocused()

      // Use Enter to activate
      await page.keyboard.press('Enter')
      await expect(page.getByRole('dialog')).toBeVisible()
    })

    test('closes modal with Escape', async ({ page }) => {
      await [feature]Page.openCreateModal()

      await page.keyboard.press('Escape')

      await expect(page.getByRole('dialog')).not.toBeVisible()
    })
  })

  test.describe('Performance', () => {
    test('loads within acceptable time', async ({ page }) => {
      const startTime = Date.now()

      await [feature]Page.navigateTo()
      await expect([feature]Page.entityTable).toBeVisible()

      const loadTime = Date.now() - startTime
      expect(loadTime).toBeLessThan(3000) // 3 seconds max
    })

    test('handles large datasets', async ({ page }) => {
      // Mock large dataset
      await page.route('/api/v1/[feature]*', route => {
        const mockData = {
          items: Array.from({ length: 100 }, (_, i) => ({
            id: String(i + 1),
            name: `Entity ${i + 1}`,
            email: `entity${i + 1}@example.com`,
            status: 'active',
            createdAt: new Date().toISOString(),
          })),
          totalItems: 100,
          totalPages: 5,
        }

        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockData),
        })
      })

      await [feature]Page.navigateTo()

      // Should still load quickly
      await expect([feature]Page.entityTable).toBeVisible()

      // Pagination should be present
      await expect(page.getByText(/page 1 of 5/i)).toBeVisible()
    })
  })
})
```

---

## 6. Performance Optimization Templates

### 6.1 Lazy Loading Template

```typescript
// Template: Lazy loading with Suspense
// Path: src/components/lazy/LazyComponents.tsx

import React, { Suspense, lazy } from 'react'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import ErrorBoundary from '@/components/ui/ErrorBoundary'

// Lazy load components
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage'))
const PaymentsPage = lazy(() => import('@/pages/payments/PaymentsPage'))
const OrganizationsPage = lazy(() => import('@/pages/organizations/OrganizationsPage'))
const SubscriptionPage = lazy(() => import('@/pages/subscription/SubscriptionPage'))
const SettingsPage = lazy(() => import('@/pages/settings/SettingsPage'))

// Advanced lazy loading with retry
const lazyWithRetry = (importFunc: () => Promise<any>, retries = 3) => {
  return lazy(() => {
    const retry = (attempt: number): Promise<any> => {
      return importFunc().catch((error) => {
        if (attempt <= retries) {
          console.warn(`Lazy loading failed, retrying... (${attempt}/${retries})`)
          return retry(attempt + 1)
        }
        throw error
      })
    }

    return retry(1)
  })
}

// Component with retry mechanism
const AdvancedAnalyticsPage = lazyWithRetry(
  () => import('@/pages/analytics/AdvancedAnalyticsPage')
)

// HOC for lazy loading with fallback
interface LazyWrapperProps {
  fallback?: React.ComponentType
  onError?: (error: Error, errorInfo: React.ErrorInfo) => void
}

export const withLazyLoading = <P extends object>(
  Component: React.LazyExoticComponent<React.ComponentType<P>>,
  options: LazyWrapperProps = {}
) => {
  const { fallback: Fallback = LoadingSpinner, onError } = options

  return React.forwardRef<any, P>((props, ref) => (
    <ErrorBoundary onError={onError}>
      <Suspense
        fallback={
          <div className="flex items-center justify-center min-h-96">
            <Fallback />
          </div>
        }
      >
        <Component {...props} ref={ref} />
      </Suspense>
    </ErrorBoundary>
  ))
}

// Usage in routes
export const LazyDashboardPage = withLazyLoading(DashboardPage)
export const LazyPaymentsPage = withLazyLoading(PaymentsPage)
export const LazyOrganizationsPage = withLazyLoading(OrganizationsPage)
export const LazySubscriptionPage = withLazyLoading(SubscriptionPage)
export const LazySettingsPage = withLazyLoading(SettingsPage)
```

### 6.2 Memoization Template

```typescript
// Template: Performance optimization with memoization
// Path: src/components/optimized/OptimizedComponents.tsx

import React, { memo, useMemo, useCallback, useState } from 'react'
import { useAppSelector } from '@/store/hooks'
import { selectCurrentUser } from '@/store/slices/authSlice'

// Memoized data processing
interface ExpensiveCalculationProps {
  data: any[]
  filters: Record<string, any>
  sortBy: string
  sortOrder: 'asc' | 'desc'
}

const useExpensiveCalculation = ({
  data,
  filters,
  sortBy,
  sortOrder
}: ExpensiveCalculationProps) => {
  return useMemo(() => {
    console.log('🔄 Performing expensive calculation...')

    // Expensive filtering operation
    const filtered = data.filter(item => {
      return Object.entries(filters).every(([key, value]) => {
        if (!value) return true
        return item[key]?.toString().toLowerCase().includes(value.toLowerCase())
      })
    })

    // Expensive sorting operation
    const sorted = [...filtered].sort((a, b) => {
      const aValue = a[sortBy]
      const bValue = b[sortBy]

      if (sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1
      } else {
        return aValue < bValue ? 1 : -1
      }
    })

    // Expensive aggregation
    const aggregations = {
      total: sorted.length,
      categories: sorted.reduce((acc, item) => {
        acc[item.category] = (acc[item.category] || 0) + 1
        return acc
      }, {} as Record<string, number>),
      averageValue: sorted.reduce((sum, item) => sum + (item.value || 0), 0) / sorted.length || 0,
    }

    return {
      processedData: sorted,
      aggregations,
      hasData: sorted.length > 0,
    }
  }, [data, filters, sortBy, sortOrder])
}

// Memoized component with complex props
interface DataTableRowProps {
  item: any
  isSelected: boolean
  onSelect: (id: string) => void
  onEdit: (id: string) => void
  onDelete: (id: string) => void
  formatValue: (value: any) => string
}

const DataTableRow = memo<DataTableRowProps>(({
  item,
  isSelected,
  onSelect,
  onEdit,
  onDelete,
  formatValue
}) => {
  console.log(`🔄 Rendering row for ${item.id}`)

  return (
    <tr className={`border-b ${isSelected ? 'bg-blue-50' : 'hover:bg-gray-50'}`}>
      <td className="px-4 py-2">
        <input
          type="checkbox"
          checked={isSelected}
          onChange={() => onSelect(item.id)}
        />
      </td>
      <td className="px-4 py-2">{item.name}</td>
      <td className="px-4 py-2">{formatValue(item.value)}</td>
      <td className="px-4 py-2">
        <div className="flex space-x-2">
          <button
            onClick={() => onEdit(item.id)}
            className="text-blue-600 hover:text-blue-800"
          >
            Edit
          </button>
          <button
            onClick={() => onDelete(item.id)}
            className="text-red-600 hover:text-red-800"
          >
            Delete
          </button>
        </div>
      </td>
    </tr>
  )
}, (prevProps, nextProps) => {
  // Custom comparison function for memo
  return (
    prevProps.item.id === nextProps.item.id &&
    prevProps.item.name === nextProps.item.name &&
    prevProps.item.value === nextProps.item.value &&
    prevProps.isSelected === nextProps.isSelected
    // Note: We don't compare functions as they should be memoized in parent
  )
})

DataTableRow.displayName = 'DataTableRow'

// Optimized list component
interface OptimizedDataTableProps {
  data: any[]
  filters: Record<string, any>
  sortBy: string
  sortOrder: 'asc' | 'desc'
}

const OptimizedDataTable: React.FC<OptimizedDataTableProps> = ({
  data,
  filters,
  sortBy,
  sortOrder
}) => {
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set())
  const currentUser = useAppSelector(selectCurrentUser)

  // Expensive calculation with memoization
  const { processedData, aggregations } = useExpensiveCalculation({
    data,
    filters,
    sortBy,
    sortOrder
  })

  // Memoized callbacks to prevent child re-renders
  const handleSelect = useCallback((id: string) => {
    setSelectedItems(prev => {
      const newSet = new Set(prev)
      if (newSet.has(id)) {
        newSet.delete(id)
      } else {
        newSet.add(id)
      }
      return newSet
    })
  }, [])

  const handleEdit = useCallback((id: string) => {
    console.log('Edit item:', id)
    // Handle edit logic
  }, [])

  const handleDelete = useCallback((id: string) => {
    console.log('Delete item:', id)
    // Handle delete logic
  }, [])

  // Memoized value formatter
  const formatValue = useCallback((value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value)
  }, [])

  // Memoized permission check
  const canEdit = useMemo(() => {
    return currentUser?.permissions?.includes('EDIT_PERMISSION') || false
  }, [currentUser?.permissions])

  // Memoized selected count
  const selectedCount = useMemo(() => selectedItems.size, [selectedItems])

  return (
    <div className="space-y-4">
      {/* Summary info */}
      <div className="bg-gray-50 p-4 rounded-lg">
        <div className="grid grid-cols-3 gap-4 text-sm">
          <div>Total: {aggregations.total}</div>
          <div>Selected: {selectedCount}</div>
          <div>Average: {formatValue(aggregations.averageValue)}</div>
        </div>
      </div>

      {/* Data table */}
      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-2 text-left">Select</th>
              <th className="px-4 py-2 text-left">Name</th>
              <th className="px-4 py-2 text-left">Value</th>
              {canEdit && (
                <th className="px-4 py-2 text-left">Actions</th>
              )}
            </tr>
          </thead>
          <tbody>
            {processedData.map((item) => (
              <DataTableRow
                key={item.id}
                item={item}
                isSelected={selectedItems.has(item.id)}
                onSelect={handleSelect}
                onEdit={handleEdit}
                onDelete={handleDelete}
                formatValue={formatValue}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default memo(OptimizedDataTable)
```

### 6.3 Virtual Scrolling Template

```typescript
// Template: Virtual scrolling for large datasets
// Path: src/components/ui/VirtualScrollList.tsx

import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react'

interface VirtualScrollListProps<T> {
  items: T[]
  itemHeight: number
  containerHeight: number
  renderItem: (item: T, index: number) => React.ReactNode
  overscan?: number
  onLoadMore?: () => void
  hasMore?: boolean
  isLoading?: boolean
}

function VirtualScrollList<T extends { id: string | number }>({
  items,
  itemHeight,
  containerHeight,
  renderItem,
  overscan = 5,
  onLoadMore,
  hasMore = false,
  isLoading = false
}: VirtualScrollListProps<T>) {
  const [scrollTop, setScrollTop] = useState(0)
  const scrollElementRef = useRef<HTMLDivElement>(null)

  // Calculate visible range
  const { startIndex, endIndex, visibleItems } = useMemo(() => {
    const start = Math.floor(scrollTop / itemHeight)
    const end = Math.min(
      start + Math.ceil(containerHeight / itemHeight),
      items.length - 1
    )

    const startWithOverscan = Math.max(0, start - overscan)
    const endWithOverscan = Math.min(items.length - 1, end + overscan)

    return {
      startIndex: startWithOverscan,
      endIndex: endWithOverscan,
      visibleItems: items.slice(startWithOverscan, endWithOverscan + 1)
    }
  }, [scrollTop, itemHeight, containerHeight, items, overscan])

  // Handle scroll
  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    setScrollTop(e.currentTarget.scrollTop)

    // Load more when near bottom
    if (onLoadMore && hasMore && !isLoading) {
      const { scrollTop, scrollHeight, clientHeight } = e.currentTarget
      if (scrollTop + clientHeight >= scrollHeight - (itemHeight * 5)) {
        onLoadMore()
      }
    }
  }, [onLoadMore, hasMore, isLoading, itemHeight])

  // Scroll to index
  const scrollToIndex = useCallback((index: number) => {
    if (scrollElementRef.current) {
      scrollElementRef.current.scrollTop = index * itemHeight
    }
  }, [itemHeight])

  // Total height of all items
  const totalHeight = items.length * itemHeight

  return (
    <div
      ref={scrollElementRef}
      className="overflow-auto"
      style={{ height: containerHeight }}
      onScroll={handleScroll}
    >
      <div style={{ height: totalHeight, position: 'relative' }}>
        <div
          style={{
            transform: `translateY(${startIndex * itemHeight}px)`,
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
          }}
        >
          {visibleItems.map((item, index) => (
            <div
              key={item.id}
              style={{ height: itemHeight }}
              className="flex items-center"
            >
              {renderItem(item, startIndex + index)}
            </div>
          ))}

          {isLoading && (
            <div
              style={{ height: itemHeight }}
              className="flex items-center justify-center"
            >
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-gray-900" />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// Usage example
interface VirtualListUsageProps {
  data: Array<{ id: string; name: string; value: number }>
}

const VirtualListUsage: React.FC<VirtualListUsageProps> = ({ data }) => {
  const [hasMore, setHasMore] = useState(true)
  const [isLoading, setIsLoading] = useState(false)

  const handleLoadMore = useCallback(async () => {
    if (isLoading) return

    setIsLoading(true)
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      // Load more data logic here
      setHasMore(false) // Set to false when no more data
    } finally {
      setIsLoading(false)
    }
  }, [isLoading])

  const renderItem = useCallback((item: typeof data[0], index: number) => (
    <div className="px-4 py-2 border-b flex justify-between items-center w-full">
      <div>
        <div className="font-medium">{item.name}</div>
        <div className="text-sm text-gray-500">Index: {index}</div>
      </div>
      <div className="text-lg font-semibold">
        ${item.value.toLocaleString()}
      </div>
    </div>
  ), [])

  return (
    <div className="border rounded-lg">
      <VirtualScrollList
        items={data}
        itemHeight={60}
        containerHeight={400}
        renderItem={renderItem}
        onLoadMore={handleLoadMore}
        hasMore={hasMore}
        isLoading={isLoading}
        overscan={3}
      />
    </div>
  )
}

export default VirtualScrollList
```

### 6.4 Preloading Strategy Template

```typescript
// Template: Intelligent preloading and prefetching
// Path: src/utils/preloadingStrategy.tsx

import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAppDispatch } from '@/store/hooks'
import { authApi } from '@/store/api/authApi'
import { userApi } from '@/store/api/userApi'
import { organizationApi } from '@/store/api/organizationApi'
import { paymentApi } from '@/store/api/paymentApi'

// Navigation patterns tracking
interface NavigationPattern {
  from: string
  to: string
  count: number
  lastAccessed: number
}

class NavigationTracker {
  private patterns: Map<string, NavigationPattern> = new Map()
  private readonly STORAGE_KEY = 'navigation_patterns'
  private readonly MAX_PATTERNS = 100
  private readonly PATTERN_EXPIRY = 7 * 24 * 60 * 60 * 1000 // 7 days

  constructor() {
    this.loadPatterns()
  }

  private loadPatterns() {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY)
      if (stored) {
        const data = JSON.parse(stored)
        this.patterns = new Map(data.patterns || [])
        this.cleanExpiredPatterns()
      }
    } catch (error) {
      console.warn('Failed to load navigation patterns:', error)
    }
  }

  private savePatterns() {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify({
        patterns: Array.from(this.patterns.entries()),
        lastUpdated: Date.now()
      }))
    } catch (error) {
      console.warn('Failed to save navigation patterns:', error)
    }
  }

  private cleanExpiredPatterns() {
    const now = Date.now()
    for (const [key, pattern] of this.patterns.entries()) {
      if (now - pattern.lastAccessed > this.PATTERN_EXPIRY) {
        this.patterns.delete(key)
      }
    }
  }

  trackNavigation(from: string, to: string) {
    const key = `${from}->${to}`
    const existing = this.patterns.get(key)

    if (existing) {
      existing.count++
      existing.lastAccessed = Date.now()
    } else {
      // Remove oldest pattern if we're at capacity
      if (this.patterns.size >= this.MAX_PATTERNS) {
        let oldestKey = ''
        let oldestTime = Date.now()

        for (const [k, p] of this.patterns.entries()) {
          if (p.lastAccessed < oldestTime) {
            oldestTime = p.lastAccessed
            oldestKey = k
          }
        }

        if (oldestKey) {
          this.patterns.delete(oldestKey)
        }
      }

      this.patterns.set(key, {
        from,
        to,
        count: 1,
        lastAccessed: Date.now()
      })
    }

    this.savePatterns()
  }

  getPredictedRoutes(currentRoute: string, limit = 3): string[] {
    const predictions: Array<{ route: string; score: number }> = []

    for (const pattern of this.patterns.values()) {
      if (pattern.from === currentRoute) {
        // Score based on frequency and recency
        const recencyScore = 1 / (1 + (Date.now() - pattern.lastAccessed) / (24 * 60 * 60 * 1000))
        const frequencyScore = Math.log(pattern.count + 1)
        const score = recencyScore * frequencyScore

        predictions.push({ route: pattern.to, score })
      }
    }

    return predictions
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)
      .map(p => p.route)
  }
}

const navigationTracker = new NavigationTracker()

// Preloading configuration
const PRELOAD_ROUTES = {
  '/dashboard': [
    { api: organizationApi, endpoint: 'getOrganizations', params: {} },
    { api: userApi, endpoint: 'getCurrentUser', params: {} },
  ],
  '/organizations': [
    { api: organizationApi, endpoint: 'getOrganizations', params: {} },
    { api: userApi, endpoint: 'getOrganizationMembers', params: null }, // Dynamic params
  ],
  '/payments': [
    { api: paymentApi, endpoint: 'getPaymentMethods', params: {} },
    { api: paymentApi, endpoint: 'getTransactions', params: { page: 1, limit: 20 } },
  ],
}

// Navigation tracking hook
export const useNavigationTracking = () => {
  const location = useLocation()
  const previousLocation = useRef<string>('')

  useEffect(() => {
    const currentPath = location.pathname

    if (previousLocation.current && previousLocation.current !== currentPath) {
      navigationTracker.trackNavigation(previousLocation.current, currentPath)
    }

    previousLocation.current = currentPath
  }, [location.pathname])

  return navigationTracker
}

// Preloading hook
export const useIntelligentPreloading = () => {
  const location = useLocation()
  const dispatch = useAppDispatch()
  const tracker = useNavigationTracking()

  useEffect(() => {
    const currentPath = location.pathname

    // Preload data for current route
    const preloadConfig = PRELOAD_ROUTES[currentPath as keyof typeof PRELOAD_ROUTES]
    if (preloadConfig) {
      preloadConfig.forEach(({ api, endpoint, params }) => {
        if (params !== null) {
          // @ts-ignore - Dynamic API calls
          dispatch(api.util.prefetch(endpoint, params))
        }
      })
    }

    // Preload predicted routes
    const predictedRoutes = tracker.getPredictedRoutes(currentPath)
    predictedRoutes.forEach(route => {
      const routeConfig = PRELOAD_ROUTES[route as keyof typeof PRELOAD_ROUTES]
      if (routeConfig) {
        routeConfig.forEach(({ api, endpoint, params }) => {
          if (params !== null) {
            // @ts-ignore - Dynamic API calls with lower priority
            setTimeout(() => {
              dispatch(api.util.prefetch(endpoint, params))
            }, 1000) // Delay to not interfere with current page
          }
        })
      }
    })
  }, [location.pathname, dispatch, tracker])
}

// Link with preloading on hover
interface PreloadLinkProps {
  to: string
  children: React.ReactNode
  className?: string
  preloadDelay?: number
}

export const PreloadLink: React.FC<PreloadLinkProps> = ({
  to,
  children,
  className,
  preloadDelay = 100
}) => {
  const navigate = useNavigate()
  const dispatch = useAppDispatch()
  const preloadTimeoutRef = useRef<NodeJS.Timeout>()

  const handleMouseEnter = () => {
    preloadTimeoutRef.current = setTimeout(() => {
      const preloadConfig = PRELOAD_ROUTES[to as keyof typeof PRELOAD_ROUTES]
      if (preloadConfig) {
        preloadConfig.forEach(({ api, endpoint, params }) => {
          if (params !== null) {
            // @ts-ignore - Dynamic API calls
            dispatch(api.util.prefetch(endpoint, params))
          }
        })
      }
    }, preloadDelay)
  }

  const handleMouseLeave = () => {
    if (preloadTimeoutRef.current) {
      clearTimeout(preloadTimeoutRef.current)
    }
  }

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault()
    navigate(to)
  }

  return (
    <a
      href={to}
      className={className}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onClick={handleClick}
    >
      {children}
    </a>
  )
}

// Image preloading utility
export const useImagePreloader = (imageUrls: string[]) => {
  useEffect(() => {
    const preloadImages = imageUrls.map(url => {
      return new Promise((resolve, reject) => {
        const img = new Image()
        img.onload = resolve
        img.onerror = reject
        img.src = url
      })
    })

    Promise.allSettled(preloadImages).then(results => {
      const failed = results.filter(result => result.status === 'rejected')
      if (failed.length > 0) {
        console.warn(`Failed to preload ${failed.length} images`)
      }
    })
  }, [imageUrls])
}

// Service worker for advanced caching
export const registerServiceWorker = () => {
  if ('serviceWorker' in navigator && import.meta.env.PROD) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js')
        .then(registration => {
          console.log('SW registered: ', registration)
        })
        .catch(registrationError => {
          console.log('SW registration failed: ', registrationError)
        })
    })
  }
}
```

---

## 7. Error Handling Templates

### 7.1 Error Boundary Template

```typescript
// Template: Comprehensive error boundary with reporting
// Path: src/components/ui/ErrorBoundary.tsx

import React, { Component, ErrorInfo, ReactNode } from 'react'
import { Button } from '@/components/ui/Button'
import { AlertTriangle, RefreshCw, Home } from 'lucide-react'

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
  errorInfo: ErrorInfo | null
  errorId: string
}

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: (error: Error, errorId: string, retry: () => void) => ReactNode
  onError?: (error: Error, errorInfo: ErrorInfo, errorId: string) => void
  resetOnPropsChange?: boolean
  resetKeys?: Array<string | number>
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  private resetTimeoutId: number | null = null
  private previousResetKeys: Array<string | number> = []

  constructor(props: ErrorBoundaryProps) {
    super(props)

    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: '',
    }
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    const errorId = generateErrorId()

    return {
      hasError: true,
      error,
      errorId,
    }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    const { onError } = this.props
    const { errorId } = this.state

    this.setState({ errorInfo })

    // Log error to console in development
    if (import.meta.env.DEV) {
      console.group('🚨 Error Boundary Caught Error')
      console.error('Error:', error)
      console.error('Component Stack:', errorInfo.componentStack)
      console.error('Error ID:', errorId)
      console.groupEnd()
    }

    // Report error to monitoring service
    this.reportError(error, errorInfo, errorId)

    // Call custom error handler
    onError?.(error, errorInfo, errorId)
  }

  componentDidUpdate(prevProps: ErrorBoundaryProps) {
    const { resetKeys, resetOnPropsChange } = this.props
    const { hasError } = this.state

    if (hasError && prevProps.children !== this.props.children && resetOnPropsChange) {
      this.resetError()
    }

    if (hasError && resetKeys) {
      if (prevProps.resetKeys === undefined ||
          resetKeys.some((key, index) => key !== prevProps.resetKeys?.[index])) {
        this.resetError()
      }
    }
  }

  componentWillUnmount() {
    if (this.resetTimeoutId) {
      clearTimeout(this.resetTimeoutId)
    }
  }

  private reportError = async (error: Error, errorInfo: ErrorInfo, errorId: string) => {
    try {
      // Get user context
      const userContext = this.getUserContext()

      // Prepare error report
      const errorReport = {
        errorId,
        message: error.message,
        stack: error.stack,
        componentStack: errorInfo.componentStack,
        userAgent: navigator.userAgent,
        url: window.location.href,
        timestamp: new Date().toISOString(),
        userContext,
        buildInfo: {
          version: import.meta.env.VITE_APP_VERSION,
          environment: import.meta.env.MODE,
        },
      }

      // Send to error reporting service
      if (import.meta.env.VITE_ERROR_REPORTING_URL) {
        await fetch(import.meta.env.VITE_ERROR_REPORTING_URL, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(errorReport),
        })
      }

      // Store locally for debugging
      localStorage.setItem(`error_${errorId}`, JSON.stringify(errorReport))
    } catch (reportingError) {
      console.error('Failed to report error:', reportingError)
    }
  }

  private getUserContext = () => {
    try {
      // Get Redux store if available
      const store = (window as any).__REDUX_STORE__
      const state = store?.getState()

      return {
        userId: state?.auth?.user?.id,
        organizationId: state?.auth?.user?.organizationId,
        isAuthenticated: state?.auth?.isAuthenticated,
        currentRoute: window.location.pathname,
      }
    } catch {
      return null
    }
  }

  private resetError = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      errorId: '',
    })
  }

  private handleRetry = () => {
    this.resetError()
  }

  private handleGoHome = () => {
    window.location.href = '/'
  }

  private handleReload = () => {
    window.location.reload()
  }

  render() {
    const { hasError, error, errorId } = this.state
    const { children, fallback } = this.props

    if (hasError && error) {
      if (fallback) {
        return fallback(error, errorId, this.handleRetry)
      }

      return (
        <ErrorFallback
          error={error}
          errorId={errorId}
          onRetry={this.handleRetry}
          onGoHome={this.handleGoHome}
          onReload={this.handleReload}
        />
      )
    }

    return children
  }
}

// Default error fallback component
interface ErrorFallbackProps {
  error: Error
  errorId: string
  onRetry: () => void
  onGoHome: () => void
  onReload: () => void
}

const ErrorFallback: React.FC<ErrorFallbackProps> = ({
  error,
  errorId,
  onRetry,
  onGoHome,
  onReload,
}) => {
  const [showDetails, setShowDetails] = React.useState(false)

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-6">
        <div className="flex items-center space-x-3 mb-4">
          <AlertTriangle className="h-8 w-8 text-red-500" />
          <div>
            <h1 className="text-lg font-semibold text-gray-900">
              Something went wrong
            </h1>
            <p className="text-sm text-gray-500">Error ID: {errorId}</p>
          </div>
        </div>

        <p className="text-gray-600 mb-6">
          We're sorry, but something unexpected happened. Our team has been
          notified and will look into this issue.
        </p>

        <div className="space-y-3 mb-6">
          <Button
            onClick={onRetry}
            className="w-full"
            variant="primary"
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            Try Again
          </Button>

          <div className="flex space-x-3">
            <Button
              onClick={onGoHome}
              variant="outline"
              className="flex-1"
            >
              <Home className="h-4 w-4 mr-2" />
              Go Home
            </Button>

            <Button
              onClick={onReload}
              variant="outline"
              className="flex-1"
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Reload Page
            </Button>
          </div>
        </div>

        {import.meta.env.DEV && (
          <div className="border-t pt-4">
            <button
              onClick={() => setShowDetails(!showDetails)}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              {showDetails ? 'Hide' : 'Show'} Error Details
            </button>

            {showDetails && (
              <div className="mt-3 p-3 bg-gray-50 rounded text-xs">
                <div className="font-mono text-red-600 mb-2">
                  {error.message}
                </div>
                <div className="font-mono text-gray-600 whitespace-pre-wrap overflow-auto max-h-32">
                  {error.stack}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

// Utility function to generate unique error IDs
const generateErrorId = (): string => {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

// Hook for handling async errors
export const useAsyncError = () => {
  const [_, setError] = React.useState()

  return React.useCallback((error: Error) => {
    setError(() => {
      throw error
    })
  }, [])
}

// HOC for wrapping components with error boundary
export const withErrorBoundary = <P extends object>(
  Component: React.ComponentType<P>,
  errorBoundaryProps?: Omit<ErrorBoundaryProps, 'children'>
) => {
  const WrappedComponent = React.forwardRef<any, P>((props, ref) => (
    <ErrorBoundary {...errorBoundaryProps}>
      <Component {...props} ref={ref} />
    </ErrorBoundary>
  ))

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`

  return WrappedComponent
}

export default ErrorBoundary
```

### 7.2 API Error Handling Template

```typescript
// Template: Comprehensive API error handling
// Path: src/lib/api/errorHandling.ts

import { toast } from 'react-hot-toast'
import { FetchBaseQueryError } from '@reduxjs/toolkit/query'
import { SerializedError } from '@reduxjs/toolkit'

// Error types
export interface APIError {
  status: number
  message: string
  code?: string
  details?: Record<string, any>
  correlationId?: string
}

export interface ValidationError {
  field: string
  message: string
  code: string
}

export interface APIErrorResponse {
  error: string
  message: string
  statusCode: number
  correlationId: string
  timestamp: string
  path: string
  details?: {
    validationErrors?: ValidationError[]
    [key: string]: any
  }
}

// Error classification
export enum ErrorType {
  VALIDATION = 'VALIDATION',
  AUTHENTICATION = 'AUTHENTICATION',
  AUTHORIZATION = 'AUTHORIZATION',
  NOT_FOUND = 'NOT_FOUND',
  RATE_LIMIT = 'RATE_LIMIT',
  NETWORK = 'NETWORK',
  SERVER = 'SERVER',
  UNKNOWN = 'UNKNOWN',
}

// Error severity levels
export enum ErrorSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

// Error context
interface ErrorContext {
  userId?: string
  organizationId?: string
  route: string
  userAgent: string
  timestamp: string
  sessionId?: string
}

// Error classifier
export const classifyError = (error: FetchBaseQueryError | SerializedError): ErrorType => {
  if ('status' in error) {
    switch (error.status) {
      case 400:
        return ErrorType.VALIDATION
      case 401:
        return ErrorType.AUTHENTICATION
      case 403:
        return ErrorType.AUTHORIZATION
      case 404:
        return ErrorType.NOT_FOUND
      case 429:
        return ErrorType.RATE_LIMIT
      case 'FETCH_ERROR':
      case 'TIMEOUT_ERROR':
        return ErrorType.NETWORK
      case 500:
      case 502:
      case 503:
      case 504:
        return ErrorType.SERVER
      default:
        return ErrorType.UNKNOWN
    }
  }

  return ErrorType.UNKNOWN
}

// Error severity classifier
export const getErrorSeverity = (errorType: ErrorType): ErrorSeverity => {
  switch (errorType) {
    case ErrorType.VALIDATION:
    case ErrorType.NOT_FOUND:
      return ErrorSeverity.LOW
    case ErrorType.AUTHENTICATION:
    case ErrorType.AUTHORIZATION:
    case ErrorType.RATE_LIMIT:
      return ErrorSeverity.MEDIUM
    case ErrorType.NETWORK:
    case ErrorType.SERVER:
      return ErrorSeverity.HIGH
    default:
      return ErrorSeverity.CRITICAL
  }
}

// Error parser
export const parseAPIError = (error: FetchBaseQueryError | SerializedError): APIError => {
  if ('status' in error && error.data) {
    const errorData = error.data as APIErrorResponse
    return {
      status: error.status as number,
      message: errorData.message || 'An error occurred',
      code: errorData.error,
      details: errorData.details,
      correlationId: errorData.correlationId,
    }
  }

  if ('message' in error) {
    return {
      status: 0,
      message: error.message || 'An unexpected error occurred',
    }
  }

  return {
    status: 0,
    message: 'An unknown error occurred',
  }
}

// Error handler class
export class ErrorHandler {
  private errorLog: Array<{
    id: string
    error: APIError
    type: ErrorType
    severity: ErrorSeverity
    context: ErrorContext
    timestamp: number
    handled: boolean
  }> = []

  private readonly MAX_LOG_SIZE = 100
  private retryAttempts = new Map<string, number>()
  private readonly MAX_RETRIES = 3

  constructor(private reportingService?: (error: any) => Promise<void>) {}

  async handleError(
    error: FetchBaseQueryError | SerializedError,
    context: Partial<ErrorContext> = {}
  ): Promise<{
    shouldRetry: boolean
    shouldShowToast: boolean
    shouldRedirect: string | null
    retryDelay?: number
  }> {
    const apiError = parseAPIError(error)
    const errorType = classifyError(error)
    const severity = getErrorSeverity(errorType)

    const errorId = this.generateErrorId()
    const fullContext: ErrorContext = {
      route: window.location.pathname,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString(),
      ...context,
    }

    // Log error
    this.logError({
      id: errorId,
      error: apiError,
      type: errorType,
      severity,
      context: fullContext,
      timestamp: Date.now(),
      handled: false,
    })

    // Report to monitoring service
    if (this.reportingService && severity >= ErrorSeverity.HIGH) {
      try {
        await this.reportingService({
          id: errorId,
          error: apiError,
          type: errorType,
          severity,
          context: fullContext,
        })
      } catch (reportingError) {
        console.error('Failed to report error:', reportingError)
      }
    }

    // Determine handling strategy
    return this.getHandlingStrategy(errorType, apiError, errorId)
  }

  private logError(errorEntry: typeof this.errorLog[0]) {
    this.errorLog.unshift(errorEntry)

    if (this.errorLog.length > this.MAX_LOG_SIZE) {
      this.errorLog = this.errorLog.slice(0, this.MAX_LOG_SIZE)
    }

    // Store in localStorage for debugging
    try {
      localStorage.setItem('api_errors', JSON.stringify(this.errorLog.slice(0, 10)))
    } catch (storageError) {
      console.warn('Failed to store error log:', storageError)
    }
  }

  private getHandlingStrategy(
    errorType: ErrorType,
    error: APIError,
    errorId: string
  ): {
    shouldRetry: boolean
    shouldShowToast: boolean
    shouldRedirect: string | null
    retryDelay?: number
  } {
    const currentRetries = this.retryAttempts.get(errorId) || 0

    switch (errorType) {
      case ErrorType.VALIDATION:
        return {
          shouldRetry: false,
          shouldShowToast: false, // Form validation should handle this
          shouldRedirect: null,
        }

      case ErrorType.AUTHENTICATION:
        return {
          shouldRetry: false,
          shouldShowToast: true,
          shouldRedirect: '/auth/login',
        }

      case ErrorType.AUTHORIZATION:
        return {
          shouldRetry: false,
          shouldShowToast: true,
          shouldRedirect: null,
        }

      case ErrorType.NOT_FOUND:
        return {
          shouldRetry: false,
          shouldShowToast: true,
          shouldRedirect: null,
        }

      case ErrorType.RATE_LIMIT:
        return {
          shouldRetry: currentRetries < this.MAX_RETRIES,
          shouldShowToast: true,
          shouldRedirect: null,
          retryDelay: Math.pow(2, currentRetries) * 1000, // Exponential backoff
        }

      case ErrorType.NETWORK:
        return {
          shouldRetry: currentRetries < this.MAX_RETRIES,
          shouldShowToast: true,
          shouldRedirect: null,
          retryDelay: 1000,
        }

      case ErrorType.SERVER:
        return {
          shouldRetry: currentRetries < 1, // Limited retries for server errors
          shouldShowToast: true,
          shouldRedirect: null,
          retryDelay: 2000,
        }

      default:
        return {
          shouldRetry: false,
          shouldShowToast: true,
          shouldRedirect: null,
        }
    }
  }

  markAsRetried(errorId: string) {
    const currentRetries = this.retryAttempts.get(errorId) || 0
    this.retryAttempts.set(errorId, currentRetries + 1)
  }

  private generateErrorId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  // Get error statistics for monitoring
  getErrorStats() {
    const now = Date.now()
    const recentErrors = this.errorLog.filter(e => now - e.timestamp < 60000) // Last minute

    return {
      totalErrors: this.errorLog.length,
      recentErrors: recentErrors.length,
      errorsByType: this.errorLog.reduce((acc, error) => {
        acc[error.type] = (acc[error.type] || 0) + 1
        return acc
      }, {} as Record<ErrorType, number>),
      errorsBySeverity: this.errorLog.reduce((acc, error) => {
        acc[error.severity] = (acc[error.severity] || 0) + 1
        return acc
      }, {} as Record<ErrorSeverity, number>),
    }
  }

  // Clear error log
  clearErrorLog() {
    this.errorLog = []
    this.retryAttempts.clear()
    localStorage.removeItem('api_errors')
  }
}

// Global error handler instance
export const globalErrorHandler = new ErrorHandler(
  // Error reporting service
  async (error) => {
    if (import.meta.env.VITE_ERROR_REPORTING_URL) {
      await fetch(import.meta.env.VITE_ERROR_REPORTING_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(error),
      })
    }
  }
)

// Toast notifications for different error types
export const showErrorToast = (error: APIError, errorType: ErrorType) => {
  const toastOptions = {
    duration: 5000,
    style: {
      maxWidth: '500px',
    },
  }

  switch (errorType) {
    case ErrorType.AUTHENTICATION:
      toast.error('Please log in to continue', toastOptions)
      break

    case ErrorType.AUTHORIZATION:
      toast.error('You don\'t have permission to perform this action', toastOptions)
      break

    case ErrorType.NOT_FOUND:
      toast.error('The requested resource was not found', toastOptions)
      break

    case ErrorType.RATE_LIMIT:
      toast.error('Too many requests. Please wait a moment and try again', toastOptions)
      break

    case ErrorType.NETWORK:
      toast.error('Network connection error. Please check your internet connection', toastOptions)
      break

    case ErrorType.SERVER:
      toast.error('Server error. Our team has been notified', toastOptions)
      break

    default:
      toast.error(error.message || 'An unexpected error occurred', toastOptions)
  }
}

// React hook for error handling
export const useErrorHandler = () => {
  const handleError = React.useCallback(async (
    error: FetchBaseQueryError | SerializedError,
    context?: Partial<ErrorContext>
  ) => {
    const strategy = await globalErrorHandler.handleError(error, context)
    const apiError = parseAPIError(error)
    const errorType = classifyError(error)

    if (strategy.shouldShowToast) {
      showErrorToast(apiError, errorType)
    }

    if (strategy.shouldRedirect) {
      window.location.href = strategy.shouldRedirect
    }

    return strategy
  }, [])

  return {
    handleError,
    errorStats: globalErrorHandler.getErrorStats(),
    clearErrors: globalErrorHandler.clearErrorLog.bind(globalErrorHandler),
  }
}

export default ErrorHandler
```

---

This completes the comprehensive Frontend Implementation Templates with:

1. **Page Component Templates** - Standard patterns for different page types
2. **API Integration Templates** - RTK Query slices and custom hooks
3. **Form Component Templates** - React Hook Form with Zod validation patterns
4. **State Management Templates** - Redux slices with comprehensive patterns
5. **Testing Templates** - Unit and E2E testing with complete examples
6. **Performance Optimization Templates** - Lazy loading, memoization, virtual scrolling, preloading
7. **Error Handling Templates** - Error boundaries and API error management

These templates provide ready-to-use, production-ready code patterns that follow best practices and can be easily adapted for different features in the payment platform frontend.