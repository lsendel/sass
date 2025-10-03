import { createApi } from '@reduxjs/toolkit/query/react'
import { fetchBaseQuery } from '@reduxjs/toolkit/query/react'

import { withAuthHeader } from './utils'

import type {
  User,
  SessionInfo,
  LoginRequest,
  PasswordLoginRequest,
  PasswordRegisterRequest,
  AuthMethodsResponse,
} from '@/types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}/auth`,
    prepareHeaders: (headers: any, { getState }: any) =>
      withAuthHeader(headers, getState),
  }),
  tagTypes: ['Session'],
  endpoints: builder => ({
    getAuthMethods: builder.query<AuthMethodsResponse, void>({
      query: () => '/methods',
    }),

    passwordLogin: builder.mutation<
      { user: User; token: string },
      PasswordLoginRequest
    >({
      query: (credentials: any) => ({
        url: '/login',
        method: 'POST',
        body: credentials,
      }),
      invalidatesTags: ['Session'],
    }),

    passwordRegister: builder.mutation<
      { user: User; token: string },
      PasswordRegisterRequest
    >({
      query: (userData: any) => ({
        url: '/register',
        method: 'POST',
        body: userData,
      }),
      invalidatesTags: ['Session'],
    }),

    getAuthUrl: builder.query<{ authUrl: string }, LoginRequest>({
      query: ({ provider, redirectUri }: any) => ({
        url: '/authorize',
        params: { provider, redirect_uri: redirectUri },
      }),
    }),

    getSession: builder.query<SessionInfo, void>({
      query: () => '/session',
      providesTags: ['Session'],
    }),

    handleCallback: builder.mutation<
      { user: User; token: string },
      { code: string; state?: string }
    >({
      query: ({ code, state }: any) => ({
        url: '/callback',
        method: 'POST',
        body: { code, state },
      }),
      invalidatesTags: ['Session'],
    }),

    logout: builder.mutation<void, void>({
      query: () => ({
        url: '/logout',
        method: 'POST',
      }),
      invalidatesTags: ['Session'],
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          await queryFulfilled
          // Clear auth state on successful logout
          dispatch(authApi.util.resetApiState())
        } catch {
          // Even if logout fails on server, clear local state
          dispatch(authApi.util.resetApiState())
        }
      },
    }),

    // Refresh token endpoint (if implementing token refresh)
    refreshToken: builder.mutation<{ token: string }, void>({
      query: () => ({
        url: '/refresh',
        method: 'POST',
      }),
    }),
  }),
})

export const {
  useGetAuthMethodsQuery,
  usePasswordLoginMutation,
  usePasswordRegisterMutation,
  useGetAuthUrlQuery,
  useGetSessionQuery,
  useHandleCallbackMutation,
  useLogoutMutation,
  useRefreshTokenMutation,
  // Export for lazy queries
  useLazyGetAuthUrlQuery,
  useLazyGetSessionQuery,
} = authApi
