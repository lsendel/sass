import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

import { withAuthHeader } from './utils'

import type { User } from '@/types/api'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000/api/v1'

export interface UpdateProfileRequest {
  name: string
  preferences: Record<string, unknown>
}

export interface UpdatePreferencesRequest {
  preferences: Record<string, unknown>
}

export interface UserStatistics {
  totalUsers: number
  newUsersThisWeek: number
  usersByProvider: Record<string, number>
}

export interface PagedUserResponse {
  users: User[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export const userApi = createApi({
  reducerPath: 'userApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}/users`,
    prepareHeaders: (headers, { getState }) =>
      withAuthHeader(headers, getState),
  }),
  tagTypes: ['User', 'UserProfile'],
  endpoints: builder => ({
    getCurrentUser: builder.query<User, void>({
      query: () => '/me',
      providesTags: ['UserProfile'],
    }),

    getUserById: builder.query<User, string>({
      query: userId => `/${userId}`,
      providesTags: (_result, _error, userId) => [{ type: 'User', id: userId }],
    }),

    updateProfile: builder.mutation<User, UpdateProfileRequest>({
      query: body => ({
        url: '/me/profile',
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['UserProfile'],
    }),

    updatePreferences: builder.mutation<User, UpdatePreferencesRequest>({
      query: body => ({
        url: '/me/preferences',
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['UserProfile'],
    }),

    deleteCurrentUser: builder.mutation<void, void>({
      query: () => ({
        url: '/me',
        method: 'DELETE',
      }),
      invalidatesTags: ['UserProfile'],
    }),

    searchUsers: builder.query<User[], string>({
      query: name => ({
        url: '/search',
        params: { name },
      }),
      providesTags: ['User'],
    }),

    getAllUsers: builder.query<
      PagedUserResponse,
      {
        page?: number
        size?: number
        sortBy?: string
        sortDirection?: 'asc' | 'desc'
      }
    >({
      query: ({
        page = 0,
        size = 20,
        sortBy = 'createdAt',
        sortDirection = 'desc',
      }) => ({
        url: '',
        params: { page, size, sortBy, sortDirection },
      }),
      providesTags: ['User'],
    }),

    getRecentUsers: builder.query<User[], string>({
      query: since => ({
        url: '/recent',
        params: { since },
      }),
      providesTags: ['User'],
    }),

    getUserStatistics: builder.query<UserStatistics, void>({
      query: () => '/statistics',
      providesTags: ['User'],
    }),

    countActiveUsers: builder.query<number, void>({
      query: () => '/count',
      providesTags: ['User'],
    }),

    getUsersByProvider: builder.query<User[], string>({
      query: provider => `/providers/${provider}`,
      providesTags: ['User'],
    }),

    restoreUser: builder.mutation<User, string>({
      query: userId => ({
        url: `/${userId}/restore`,
        method: 'POST',
      }),
      invalidatesTags: ['User'],
    }),
  }),
})

export const {
  useGetCurrentUserQuery,
  useGetUserByIdQuery,
  useUpdateProfileMutation,
  useUpdatePreferencesMutation,
  useDeleteCurrentUserMutation,
  useSearchUsersQuery,
  useGetAllUsersQuery,
  useGetRecentUsersQuery,
  useGetUserStatisticsQuery,
  useCountActiveUsersQuery,
  useGetUsersByProviderQuery,
  useRestoreUserMutation,
  // Export for lazy queries
  useLazySearchUsersQuery,
  useLazyGetAllUsersQuery,
} = userApi
