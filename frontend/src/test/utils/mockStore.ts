import { configureStore } from '@reduxjs/toolkit'
import authReducer, { type AuthState } from '@/store/slices/authSlice'
import uiReducer, { type UiState } from '@/store/slices/uiSlice'

export type PartialTestState = {
  auth?: Partial<AuthState>
  ui?: Partial<UiState>
}

export const createMockAuthState = (
  overrides: Partial<AuthState> = {}
): AuthState => ({
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  ...overrides,
})

export const createMockUiState = (
  overrides: Partial<UiState> = {}
): UiState => ({
  theme: 'light',
  sidebarOpen: false,
  loading: { global: false, components: {} },
  notifications: [],
  modals: {
    isPaymentMethodModalOpen: false,
    isSubscriptionModalOpen: false,
    isInviteUserModalOpen: false,
  },
  ...overrides,
})

export const createMockStore = (initialState: PartialTestState = {}) =>
  configureStore({
    reducer: {
      auth: authReducer,
      ui: uiReducer,
    },
    preloadedState: {
      auth: createMockAuthState(initialState.auth),
      ui: createMockUiState(initialState.ui),
    },
  })

export type MockStore = ReturnType<typeof createMockStore>
