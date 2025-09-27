import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'

import LoginPage from './LoginPage'

import { createMockStore } from '@/test/utils/mockStore'

// Mock the PasswordLoginForm component
vi.mock('../../components/auth/PasswordLoginForm', () => ({
  default: () => <form role="form">Password Login Form</form>,
}))

// Simple mock for the API hook
vi.mock('../../store/api/authApi', () => ({
  useGetAuthMethodsQuery: () => ({
    data: {
      methods: ['password', 'oauth2'],
      passwordAuthEnabled: true,
      oauth2Providers: ['google', 'github', 'microsoft'],
    },
    isLoading: false,
    error: null,
  }),
}))

const renderWithProviders = (component: React.ReactElement) => {
  const store = createMockStore()
  return render(
    <Provider store={store}>
      <BrowserRouter>{component}</BrowserRouter>
    </Provider>
  )
}

describe('LoginPage', () => {
  it('renders login form', () => {
    renderWithProviders(<LoginPage />)
    // The login page should be rendered with password login form
    expect(screen.getByRole('form')).toBeInTheDocument()
  })

  it('shows password login form when enabled', () => {
    renderWithProviders(<LoginPage />)
    // Password form should be available
    expect(screen.getByRole('form')).toBeInTheDocument()
  })

  it('renders without errors', () => {
    const { container } = renderWithProviders(<LoginPage />)
    expect(container).toBeInTheDocument()
  })
})
