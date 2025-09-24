import { createApi } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../index'
import { createValidatedBaseQuery, createValidatedEndpoint, wrapSuccessResponse } from '@/lib/api/validation'
import {
  UserSchema,
  AuthMethodsResponseSchema,
  LoginResponseSchema,
  AuthUrlResponseSchema,
  SessionInfoSchema,
  PasswordLoginRequestSchema,
  PasswordRegisterRequestSchema,
  LoginRequestSchema,
  CallbackRequestSchema,
  type User,
  type OAuth2Provider,
  type SessionInfo,
  type LoginRequest,
  type PasswordLoginRequest,
  type PasswordRegisterRequest,
  type AuthMethodsResponse,
} from '@/types/api'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api/v1'

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: createValidatedBaseQuery(`${API_BASE_URL}/auth`, {
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  tagTypes: ['Session'],
  endpoints: builder => ({
    getAuthMethods: builder.query<AuthMethodsResponse, void>({
      ...createValidatedEndpoint(wrapSuccessResponse(AuthMethodsResponseSchema), {
        query: () => '/methods',
      }),
    }),

    passwordLogin: builder.mutation<
      { user: User; token: string },
      PasswordLoginRequest
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(LoginResponseSchema), {
        query: credentials => {
          // Validate request data
          PasswordLoginRequestSchema.parse(credentials);
          return {
            url: '/mock-login',
            method: 'POST',
            body: credentials,
          };
        },
        method: 'POST',
      }),
      invalidatesTags: ['Session'],
    }),

    passwordRegister: builder.mutation<
      { user: User; token: string },
      PasswordRegisterRequest
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(LoginResponseSchema), {
        query: userData => {
          // Validate request data
          PasswordRegisterRequestSchema.parse(userData);
          return {
            url: '/register',
            method: 'POST',
            body: userData,
          };
        },
        method: 'POST',
      }),
      invalidatesTags: ['Session'],
    }),

    getAuthUrl: builder.query<{ authUrl: string }, LoginRequest>({
      ...createValidatedEndpoint(wrapSuccessResponse(AuthUrlResponseSchema), {
        query: ({ provider, redirectUri }) => {
          // Validate request data
          LoginRequestSchema.parse({ provider, redirectUri });
          return {
            url: '/authorize',
            params: { provider, redirect_uri: redirectUri },
          };
        },
      }),
    }),

    getSession: builder.query<SessionInfo, void>({
      ...createValidatedEndpoint(wrapSuccessResponse(SessionInfoSchema), {
        query: () => '/session',
      }),
      providesTags: ['Session'],
    }),

    handleCallback: builder.mutation<
      { user: User; token: string },
      { code: string; state?: string }
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(LoginResponseSchema), {
        query: ({ code, state }) => {
          // Validate request data
          CallbackRequestSchema.parse({ code, state });
          return {
            url: '/callback',
            method: 'POST',
            body: { code, state },
          };
        },
        method: 'POST',
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
