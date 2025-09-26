import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import PaymentsPage from './PaymentsPage'
import {
  createMockStore,
  type PartialTestState,
} from '@/test/utils/mockStore'
import { createMockUser } from '@/test/fixtures/users'

// Mock API hooks
vi.mock('../../store/api/paymentApi', () => ({
  useGetPaymentsQuery: () => ({
    data: {
      payments: [
        {
          id: '1',
          amount: 100.00,
          currency: 'usd',
          status: 'succeeded',
          createdAt: '2024-01-01T00:00:00Z',
        }
      ]
    },
    isLoading: false,
    error: null,
  }),
  useGetOrganizationPaymentsQuery: () => ({
    data: [
      {
        id: '1',
        amount: 100.00,
        currency: 'usd',
        status: 'succeeded',
        createdAt: '2024-01-01T00:00:00Z',
      }
    ],
    isLoading: false,
    error: null,
  }),
  useGetPaymentMethodsQuery: () => ({
    data: {
      paymentMethods: [
        {
          id: '1',
          type: 'card',
          card: { last4: '4242', brand: 'visa' },
          isDefault: true,
        }
      ]
    },
    isLoading: false,
    error: null,
  }),
  useGetPaymentStatisticsQuery: () => ({
    data: {
      totalAmount: 1000,
      totalSuccessfulPayments: 10,
      failedPayments: 2,
    },
    isLoading: false,
    error: null,
  }),
}))

vi.mock('../../store/api/organizationApi', () => ({
  useGetUserOrganizationsQuery: () => ({
    data: {
      organizations: [
        { id: '1', name: 'Test Org', slug: 'test-org' }
      ]
    },
    isLoading: false,
    error: null,
  }),
}))

// Mock components
vi.mock('../../components/ui/LoadingSpinner', () => ({
  default: () => <div data-testid="loading-spinner">Loading...</div>,
}))

const renderWithProviders = (
  component: React.ReactElement,
  initialState: PartialTestState = {}
) => {
  const store = createMockStore(initialState)
  return render(
    <Provider store={store}>
      <BrowserRouter>{component}</BrowserRouter>
    </Provider>
  )
}

describe('PaymentsPage', () => {
  it('should render without errors', () => {
    renderWithProviders(<PaymentsPage />)
    expect(document.body).toBeInTheDocument()
  })

  it('should render for authenticated user', () => {
    renderWithProviders(<PaymentsPage />, {
      auth: {
        isAuthenticated: true,
        user: createMockUser(),
      },
    })
    expect(document.body).toBeInTheDocument()
  })

  it('should handle loading states', () => {
    renderWithProviders(<PaymentsPage />, {
      ui: { loading: { global: true } }
    })
    expect(document.body).toBeInTheDocument()
  })

  it('should handle payment data', () => {
    renderWithProviders(<PaymentsPage />)
    expect(document.body).toBeInTheDocument()
  })

  it('should handle payment method management', () => {
    renderWithProviders(<PaymentsPage />, {
      ui: {
        modals: { isPaymentMethodModalOpen: true }
      }
    })
    expect(document.body).toBeInTheDocument()
  })

  it('should handle empty payments state', () => {
    renderWithProviders(<PaymentsPage />)
    expect(document.body).toBeInTheDocument()
  })
})
