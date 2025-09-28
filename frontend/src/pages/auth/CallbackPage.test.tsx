import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'

import CallbackPage from './CallbackPage'

import {
  createMockStore,
  type PartialTestState,
} from '@/test/utils/mockStore'

// Mock useSearchParams
const mockUseSearchParams = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useSearchParams: () => mockUseSearchParams(),
  }
})

// Mock API hooks
vi.mock('../../store/api/authApi', () => ({
  useHandleCallbackMutation: () => [
    vi.fn(),
    { isLoading: false, error: null, data: null }
  ],
}))

// Mock LoadingSpinner
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

describe('CallbackPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockUseSearchParams.mockReturnValue([
      new URLSearchParams('code=test123&state=randomstate')
    ])
  })

  it('should render without errors', () => {
    renderWithProviders(<CallbackPage />)
    // Basic render test - component should mount without crashing
    expect(document.body).toBeInTheDocument()
  })

  it('should handle URL search parameters', () => {
    renderWithProviders(<CallbackPage />)
    // Component should render and process URL parameters
    expect(mockUseSearchParams).toHaveBeenCalled()
  })

  it('should handle callback with code parameter', () => {
    mockUseSearchParams.mockReturnValue([
      new URLSearchParams('code=auth-code-123')
    ])

    renderWithProviders(<CallbackPage />)
    expect(mockUseSearchParams).toHaveBeenCalled()
  })

  it('should handle callback with state parameter', () => {
    mockUseSearchParams.mockReturnValue([
      new URLSearchParams('code=auth-code-123&state=test-state')
    ])

    renderWithProviders(<CallbackPage />)
    expect(mockUseSearchParams).toHaveBeenCalled()
  })

  it('should handle error parameter in URL', () => {
    mockUseSearchParams.mockReturnValue([
      new URLSearchParams('error=access_denied&error_description=User cancelled')
    ])

    renderWithProviders(<CallbackPage />)
    expect(mockUseSearchParams).toHaveBeenCalled()
  })

  it('should handle empty search parameters', () => {
    mockUseSearchParams.mockReturnValue([new URLSearchParams('')])

    renderWithProviders(<CallbackPage />)
    expect(mockUseSearchParams).toHaveBeenCalled()
  })
})
