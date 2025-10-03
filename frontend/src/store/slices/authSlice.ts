import { createSlice, PayloadAction } from '@reduxjs/toolkit'

import { User } from '@/types/api'

// Re-export User type for components
export type { User }

export interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

const initialState: AuthState = {
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ user: User }>) => {
      state.user = action.payload.user
      // Token handled via httpOnly cookies, not stored in state
      state.isAuthenticated = true
      state.error = null
    },
    updateUser: (state, action: PayloadAction<Partial<User>>) => {
      if (state.user) {
        state.user = { ...state.user, ...action.payload }
      }
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload
    },
    logout: state => {
      state.user = null
      state.token = null
      state.isAuthenticated = false
      state.error = null
      // Token cleared via httpOnly cookie on server
    },
    clearError: state => {
      state.error = null
    },
  },
})

export const {
  setCredentials,
  updateUser,
  setLoading,
  setError,
  logout,
  clearError,
} = authSlice.actions

export default authSlice.reducer

// Selectors
export const selectCurrentUser = (state: { auth: AuthState }) => state.auth.user
export const selectToken = (state: { auth: AuthState }) => state.auth.token
export const selectIsAuthenticated = (state: { auth: AuthState }) =>
  state.auth.isAuthenticated
export const selectAuthLoading = (state: { auth: AuthState }) =>
  state.auth.isLoading
export const selectAuthError = (state: { auth: AuthState }) => state.auth.error
