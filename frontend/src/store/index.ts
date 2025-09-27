import { configureStore } from '@reduxjs/toolkit'

import { authApi } from './api/authApi'
import { userApi } from './api/userApi'
import { organizationApi } from './api/organizationApi'
import { paymentApi } from './api/paymentApi'
import { subscriptionApi } from './api/subscriptionApi'
import { mfaApi } from './api/mfaApi'
import { rbacApi } from './api/rbacApi'
import { tenantApi } from './api/tenantApi'
import { analyticsApi } from './api/analyticsApi'
import authReducer from './slices/authSlice'
import uiReducer from './slices/uiSlice'
import { createCacheEnhancerMiddleware } from './middleware/cacheEnhancer'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    ui: uiReducer,
    [authApi.reducerPath]: authApi.reducer,
    [userApi.reducerPath]: userApi.reducer,
    [organizationApi.reducerPath]: organizationApi.reducer,
    [paymentApi.reducerPath]: paymentApi.reducer,
    [subscriptionApi.reducerPath]: subscriptionApi.reducer,
    [mfaApi.reducerPath]: mfaApi.reducer,
    [rbacApi.reducerPath]: rbacApi.reducer,
    [tenantApi.reducerPath]: tenantApi.reducer,
    [analyticsApi.reducerPath]: analyticsApi.reducer,
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
      .concat(paymentApi.middleware)
      .concat(subscriptionApi.middleware)
      .concat(mfaApi.middleware)
      .concat(rbacApi.middleware)
      .concat(tenantApi.middleware)
      .concat(analyticsApi.middleware)
      .concat(createCacheEnhancerMiddleware()),
  devTools: import.meta.env.DEV,
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

// Export hooks with proper typing
export { useAppDispatch, useAppSelector } from './hooks'
