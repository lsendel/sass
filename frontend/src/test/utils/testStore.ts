import { configureStore } from '@reduxjs/toolkit';
import type { Middleware, Reducer } from '@reduxjs/toolkit';

import authReducer from '../../store/slices/authSlice';
import type { RootState } from '../../store';

/**
 * Test Store Utilities
 *
 * Provides helper functions to create properly configured test stores
 * for API and component testing
 */

/**
 * Create a test store with API slice and auth slice
 *
 * @param api - The complete API slice object (e.g., authApi, auditApi)
 * @param preloadedState - Optional initial state
 * @returns Configured Redux store for testing
 */
export const createApiTestStore = (
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  api: any,
  preloadedState?: Partial<RootState>
) => {
  return configureStore({
    reducer: {
      auth: authReducer,
      [api.reducerPath]: api.reducer as Reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(api.middleware as Middleware),
    preloadedState: preloadedState as RootState | undefined,
  });
};

/**
 * Create a test store with multiple API slices
 *
 * @param apis - Array of API slice objects (e.g., [authApi, auditApi])
 * @param preloadedState - Optional initial state
 * @returns Configured Redux store for testing
 */
export const createMultiApiTestStore = (
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  apis: any[],
  preloadedState?: Partial<RootState>
) => {
  const reducers: Record<string, Reducer> = {
    auth: authReducer,
  };

  const middlewares: Middleware[] = [];

  apis.forEach((api) => {
    reducers[api.reducerPath] = api.reducer as Reducer;
    middlewares.push(api.middleware as Middleware);
  });

  return configureStore({
    reducer: reducers,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(...middlewares),
    preloadedState: preloadedState as RootState | undefined,
  });
};

/**
 * Create a test store with auth state pre-populated
 *
 * @param api - The complete API slice object (e.g., authApi, auditApi)
 * @param user - User object to pre-populate
 * @returns Configured Redux store with authenticated state
 */
export const createAuthenticatedApiTestStore = (
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  api: any,
  user?: {
    id: string;
    email: string;
    firstName?: string;
    lastName?: string;
    role: string;
    token?: string;
  }
) => {
  const defaultUser = user || {
    id: 'test-user-1',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    role: 'USER' as const,
    emailVerified: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  return createApiTestStore(api, {
    auth: {
      user: defaultUser as any,
      isAuthenticated: true,
      token: (user?.token!) || null,
      isLoading: false,
      error: null,
    },
  });
};