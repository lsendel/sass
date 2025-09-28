import { configureStore } from '@reduxjs/toolkit'

import { authApi } from './api/authApi'
import { userApi } from './api/userApi'
import { organizationApi } from './api/organizationApi'
import { subscriptionApi } from './api/subscriptionApi'
// Deleted APIs: paymentApi, mfaApi, rbacApi, tenantApi, analyticsApi
import authReducer from './slices/authSlice'
import uiReducer from './slices/uiSlice'
// import { createCacheEnhancerMiddleware } from './middleware/cacheEnhancer' // Temporarily disabled

export const store = configureStore({
  reducer: {
    auth: authReducer,
    ui: uiReducer,
    [authApi.reducerPath]: authApi.reducer,
    [userApi.reducerPath]: userApi.reducer,
    [organizationApi.reducerPath]: organizationApi.reducer,
    [subscriptionApi.reducerPath]: subscriptionApi.reducer,
  },
  middleware: getDefaultMiddleware =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [
          'persist/PERSIST',
          'persist/REHYDRATE',
          'persist/PAUSE',
          'persist/PURGE',
          'persist/REGISTER',
        ],
      },
    })
      .concat(authApi.middleware)
      .concat(userApi.middleware)
      .concat(organizationApi.middleware)
      .concat(subscriptionApi.middleware),
      // .concat(createCacheEnhancerMiddleware()), // Temporarily disabled
  devTools: import.meta.env.DEV,
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

// Export hooks with proper typing
export { useAppDispatch, useAppSelector } from './hooks'
