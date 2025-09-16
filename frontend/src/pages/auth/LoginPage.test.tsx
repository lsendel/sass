import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { configureStore } from '@reduxjs/toolkit'
import LoginPage from './LoginPage'
import authSlice from '../../store/slices/authSlice'

// Simple mock for the API hook
vi.mock('../../store/api/authApi', () => ({
  useGetProvidersQuery: () => ({
    data: [
      { name: 'google', displayName: 'Google', enabled: true },
      { name: 'github', displayName: 'GitHub', enabled: true },
      { name: 'microsoft', displayName: 'Microsoft', enabled: true },
    ],
    isLoading: false,
    error: null,
  }),
}))

const createMockStore = () => {
  return configureStore({
    reducer: {
      auth: authSlice,
    },
    preloadedState: {
      auth: {
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      },
    },
  })
}

const renderWithProviders = (component: React.ReactElement) => {
  const store = createMockStore()
  return render(
    <Provider store={store}>
      <BrowserRouter>
        {component}
      </BrowserRouter>
    </Provider>
  )
}

describe('LoginPage', () => {
  it('renders login form', () => {
    renderWithProviders(<LoginPage />)

    expect(screen.getByText('Sign in to your account')).toBeInTheDocument()
    expect(screen.getByText('Choose your preferred authentication method')).toBeInTheDocument()
  })

  it('shows no providers available when providers fail to load', () => {
    renderWithProviders(<LoginPage />)

    expect(screen.getByText('No authentication providers available')).toBeInTheDocument()
  })

  it('shows OAuth security message', () => {
    renderWithProviders(<LoginPage />)

    expect(screen.getByText('Secure authentication powered by OAuth 2.0')).toBeInTheDocument()
    expect(screen.getByText('By signing in, you agree to our Terms of Service and Privacy Policy.')).toBeInTheDocument()
  })
})