import { configureStore } from '@reduxjs/toolkit'
import type { BaseQueryFn, Api } from '@reduxjs/toolkit/query'

import authSlice from '../store/slices/authSlice'
import uiSlice from '../store/slices/uiSlice'

// Helper function to create a store for API testing
export function setupApiStore<T extends Api<BaseQueryFn, any, any, any>>(
  api: T,
  extraReducers?: Record<string, any>,
  preloadedState?: any
) {
  const store = configureStore({
    reducer: {
      [api.reducerPath]: api.reducer,
      auth: authSlice,
      ui: uiSlice,
      ...extraReducers,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(api.middleware),
    preloadedState,
  })

  return store
}

// Helper type for API store
export type ApiStore<T extends Api<BaseQueryFn, any, any, any>> = ReturnType<typeof setupApiStore<T>>

// Mock response helpers
export const createMockUser = (overrides = {}) => ({
  id: '123',
  name: 'Test User',
  email: 'test@example.com',
  provider: 'google',
  preferences: {},
  createdAt: '2024-01-01T00:00:00Z',
  lastActiveAt: null,
  ...overrides,
})

export const createMockSessionInfo = (overrides = {}) => ({
  user: createMockUser(),
  session: {
    activeTokens: 1,
    lastActiveAt: '2024-01-15T10:30:00Z',
    createdAt: '2024-01-01T09:00:00Z',
  },
  ...overrides,
})

export const createMockAuthMethods = (overrides = {}) => ({
  methods: ['password', 'oauth2'],
  passwordAuthEnabled: true,
  oauth2Providers: ['google', 'github', 'microsoft'],
  ...overrides,
})