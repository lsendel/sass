import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../../store/slices/authSlice';

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
  api: any,
  preloadedState?: any
) => {
  return configureStore({
    reducer: {
      auth: authReducer,
      [api.reducerPath]: api.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(api.middleware),
    preloadedState,
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
  apis: any[],
  preloadedState?: any
) => {
  const reducers: any = {
    auth: authReducer,
  };

  const middlewares: any[] = [];

  apis.forEach((api) => {
    reducers[api.reducerPath] = api.reducer;
    middlewares.push(api.middleware);
  });

  return configureStore({
    reducer: reducers,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(...middlewares),
    preloadedState,
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
  api: any,
  user?: any
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
    },
  });
};