import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../index'
import type { User } from '../slices/authSlice'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export type OAuth2Provider = {
  name: string
  displayName: string
  iconUrl?: string
  authUrl: string
}

export type SessionInfo = {
  user: User
  session: {
    activeTokens: number
    lastActiveAt: string
    createdAt: string
  }
}

export type LoginRequest = {
  provider: string
  redirectUri: string
}

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}/auth`,
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  tagTypes: ['Session'],
  endpoints: (builder) => ({
    getProviders: builder.query<{ providers: OAuth2Provider[] }, void>({
      query: () => '/providers',
    }),

    getAuthUrl: builder.query<{ authUrl: string }, LoginRequest>({
      query: ({ provider, redirectUri }) => ({
        url: '/authorize',
        params: { provider, redirect_uri: redirectUri },
      }),
    }),

    getSession: builder.query<SessionInfo, void>({
      query: () => '/session',
      providesTags: ['Session'],
    }),

    handleCallback: builder.mutation<{ user: User; token: string }, { code: string; state?: string }>({
      query: ({ code, state }) => ({
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
  useGetProvidersQuery,
  useGetAuthUrlQuery,
  useGetSessionQuery,
  useHandleCallbackMutation,
  useLogoutMutation,
  useRefreshTokenMutation,
  // Export for lazy queries
  useLazyGetAuthUrlQuery,
  useLazyGetSessionQuery,
} = authApi