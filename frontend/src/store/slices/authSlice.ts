import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export type User = {
  id: string
  email: string
  name: string
  provider: string
  preferences: Record<string, unknown>
  createdAt: string
  lastActiveAt: string | null
}

export type AuthState = {
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
    setCredentials: (
      state,
      action: PayloadAction<{ user: User; token: string }>
    ) => {
      state.user = action.payload.user
      state.token = action.payload.token
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
      // Clear stored auth from localStorage
      localStorage.removeItem('auth-token')
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
