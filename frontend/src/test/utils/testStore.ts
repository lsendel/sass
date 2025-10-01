import { configureStore } from '@reduxjs/toolkit';
import type { Api, Middleware, Reducer } from '@reduxjs/toolkit/query/react';

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
export const createApiTestStore = <
  T extends Api<any, Record<string, any>, string, string>
>(
  api: T,
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
export const createMultiApiTestStore = <
  T extends Api<any, Record<string, any>, string, string>
>(
  apis: T[],
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
export const createAuthenticatedApiTestStore = <
  T extends Api<any, Record<string, any>, string, string>
>(
  api: T,
  user?: {
    id: string;
    email: string;
    name: string;
    role: string;
    token?: string;
  }
) => {
  const defaultUser = user || {
    id: 'test-user-1',
    email: 'test@example.com',
    name: 'Test User',
    role: 'USER',
  };

  return createApiTestStore(api, {
    auth: {
      user: defaultUser,
      isAuthenticated: true,
      token: user?.token,
    },
  });
};