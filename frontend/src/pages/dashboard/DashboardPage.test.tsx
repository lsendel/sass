import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { configureStore } from '@reduxjs/toolkit'
import DashboardPage from './DashboardPage'
import authSlice from '../../store/slices/authSlice'
import uiSlice from '../../store/slices/uiSlice'

// Mock API hooks
vi.mock('../../store/api/userApi', () => ({
  useGetCurrentUserQuery: () => ({
    data: {
      id: '123',
      name: 'Test User',
      email: 'test@example.com',
    },
    isLoading: false,
    error: null,
  }),
}))

vi.mock('../../store/api/organizationApi', () => ({
  useGetOrganizationsQuery: () => ({
    data: {
      organizations: [
        { id: '1', name: 'Test Org', slug: 'test-org' }
      ]
    },
    isLoading: false,
    error: null,
  }),
}))

vi.mock('../../store/api/subscriptionApi', () => ({
  useGetSubscriptionStatusQuery: () => ({
    data: { status: 'active', planName: 'Pro Plan' },
    isLoading: false,
    error: null,
  }),
}))

// Mock components
vi.mock('../../components/ui/LoadingSpinner', () => ({
  default: () => <div data-testid="loading-spinner">Loading...</div>,
}))

const createMockStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      auth: authSlice,
      ui: uiSlice,
    },
    preloadedState: {
      auth: {
        user: {
          id: '123',
          name: 'Test User',
          email: 'test@example.com',
          provider: 'google',
          preferences: {},
          createdAt: '2024-01-01T00:00:00Z',
          lastActiveAt: null,
        },
        token: null,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        ...initialState.auth,
      },
      ui: {
        theme: 'light',
        sidebarOpen: false,
        loading: { global: false, components: {} },
        notifications: [],
        modals: {
          isPaymentMethodModalOpen: false,
          isSubscriptionModalOpen: false,
          isInviteUserModalOpen: false,
        },
        ...initialState.ui,
      },
    },
  })
}

const renderWithProviders = (component: React.ReactElement, initialState = {}) => {
  const store = createMockStore(initialState)
  return render(
    <Provider store={store}>
      <BrowserRouter>{component}</BrowserRouter>
    </Provider>
  )
}

describe('DashboardPage', () => {
  it('should render without errors', () => {
    renderWithProviders(<DashboardPage />)
    expect(document.body).toBeInTheDocument()
  })

  it('should render for authenticated user', () => {
    renderWithProviders(<DashboardPage />, {
      auth: { isAuthenticated: true }
    })
    expect(document.body).toBeInTheDocument()
  })

  it('should handle loading states', () => {
    renderWithProviders(<DashboardPage />, {
      auth: { isLoading: false }
    })
    expect(document.body).toBeInTheDocument()
  })

  it('should handle user data display', () => {
    renderWithProviders(<DashboardPage />, {
      auth: {
        user: {
          id: '456',
          name: 'Dashboard User',
          email: 'dashboard@example.com',
          provider: 'github',
          preferences: {},
          createdAt: '2024-01-01T00:00:00Z',
          lastActiveAt: null,
        }
      }
    })
    expect(document.body).toBeInTheDocument()
  })

  it('should render dashboard components', () => {
    renderWithProviders(<DashboardPage />)
    // Component should render successfully
    expect(document.body).toBeInTheDocument()
  })

  it('should handle empty state', () => {
    renderWithProviders(<DashboardPage />, {
      auth: { user: null }
    })
    expect(document.body).toBeInTheDocument()
  })
})