import React, { ReactElement } from 'react'
import { render, RenderOptions, screen, waitFor } from '@testing-library/react'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { configureStore } from '@reduxjs/toolkit'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'

import type { RootState } from '@/store'
import { authSlice } from '@/store/slices/authSlice'
import { uiSlice } from '@/store/slices/uiSlice'
import { authApi } from '@/store/api/authApi'
import { paymentApi } from '@/store/api/paymentApi'
import ErrorBoundary from '@/components/ui/ErrorBoundary'

/**
 * Comprehensive testing utilities for React components with Redux and routing
 */

// Mock store configuration
interface MockStoreOptions {
  preloadedState?: Partial<RootState>
  apis?: any[]
}

export function createMockStore(options: MockStoreOptions = {}) {
  const { preloadedState = {}, apis = [authApi, paymentApi] } = options

  return configureStore({
    reducer: {
      auth: authSlice.reducer,
      ui: uiSlice.reducer,
      ...apis.reduce((acc, api) => {
        acc[api.reducerPath] = api.reducer
        return acc
      }, {}),
    },
    preloadedState,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        serializableCheck: {
          ignoredActions: [
            // Ignore RTK Query actions
            ...apis.flatMap(api => [
              `${api.reducerPath}/executeQuery/pending`,
              `${api.reducerPath}/executeQuery/fulfilled`,
              `${api.reducerPath}/executeQuery/rejected`,
            ]),
          ],
        },
      }).concat(...apis.map(api => api.middleware)),
  })
}

// Default mock state
export const defaultMockState: Partial<RootState> = {
  auth: {
    user: {
      id: 'user-123',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      role: 'USER',
      emailVerified: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    token: 'mock-token-123',
    isAuthenticated: true,
    isLoading: false,
    error: null,
  },
  ui: {
    theme: 'light',
    sidebar: {
      isOpen: false,
      isCollapsed: false,
    },
    modals: {},
    notifications: [],
  },
}

// Authenticated state
export const authenticatedState: Partial<RootState> = {
  ...defaultMockState,
  auth: {
    ...defaultMockState.auth!,
    isAuthenticated: true,
  },
}

// Unauthenticated state
export const unauthenticatedState: Partial<RootState> = {
  ...defaultMockState,
  auth: {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
  },
}

// Admin user state
export const adminUserState: Partial<RootState> = {
  ...defaultMockState,
  auth: {
    ...defaultMockState.auth!,
    user: {
      ...defaultMockState.auth!.user!,
      role: 'ADMIN',
    },
  },
}

// Loading state
export const loadingState: Partial<RootState> = {
  ...defaultMockState,
  auth: {
    ...defaultMockState.auth!,
    isLoading: true,
  },
}

// Error state
export const errorState: Partial<RootState> = {
  ...defaultMockState,
  auth: {
    ...defaultMockState.auth!,
    isAuthenticated: false,
    error: 'Authentication failed',
  },
}

// Custom render function with providers
interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  preloadedState?: Partial<RootState>
  store?: ReturnType<typeof createMockStore>
  withRouter?: boolean
  withErrorBoundary?: boolean
  route?: string
}

export function customRender(
  ui: ReactElement,
  options: CustomRenderOptions = {}
) {
  const {
    preloadedState = defaultMockState,
    store = createMockStore({ preloadedState }),
    withRouter = true,
    withErrorBoundary = false,
    route = '/',
    ...renderOptions
  } = options

  // Set initial route
  if (withRouter && route !== '/') {
    window.history.pushState({}, 'Test page', route)
  }

  const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    let content = (
      <Provider store={store}>
        {children}
      </Provider>
    )

    if (withRouter) {
      content = (
        <BrowserRouter>
          {content}
        </BrowserRouter>
      )
    }

    if (withErrorBoundary) {
      content = (
        <ErrorBoundary>
          {content}
        </ErrorBoundary>
      )
    }

    return content
  }

  return {
    user: userEvent.setup(),
    store,
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
  }
}

// Helper functions for common testing patterns
export const testUtils = {
  // Wait for loading to complete
  async waitForLoadingToComplete() {
    await waitFor(() => {
      expect(screen.queryByText(/loading/i)).not.toBeInTheDocument()
    })
  },

  // Wait for error to appear
  async waitForError(errorText?: string) {
    if (errorText) {
      await waitFor(() => {
        expect(screen.getByText(new RegExp(errorText, 'i'))).toBeInTheDocument()
      })
    } else {
      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
      })
    }
  },

  // Mock API responses
  mockApiSuccess: (data: any) => ({
    success: true,
    data,
    timestamp: new Date().toISOString(),
  }),

  mockApiError: (code: string, message: string, status = 400) => ({
    success: false,
    error: { code, message },
    timestamp: new Date().toISOString(),
  }),

  // Generate test IDs
  generateTestId: (prefix: string) => `test-${prefix}-${Math.random().toString(36).substring(7)}`,

  // Form helpers
  async fillForm(user: ReturnType<typeof userEvent.setup>, formData: Record<string, string>) {
    for (const [field, value] of Object.entries(formData)) {
      const input = screen.getByLabelText(new RegExp(field, 'i'))
      await user.clear(input)
      await user.type(input, value)
    }
  },

  async submitForm(user: ReturnType<typeof userEvent.setup>, buttonText = 'submit') {
    const submitButton = screen.getByRole('button', { name: new RegExp(buttonText, 'i') })
    await user.click(submitButton)
  },

  // Navigation helpers
  async navigateTo(user: ReturnType<typeof userEvent.setup>, linkText: string) {
    const link = screen.getByRole('link', { name: new RegExp(linkText, 'i') })
    await user.click(link)
  },

  // Modal helpers
  async openModal(user: ReturnType<typeof userEvent.setup>, triggerText: string) {
    const trigger = screen.getByRole('button', { name: new RegExp(triggerText, 'i') })
    await user.click(trigger)
  },

  async closeModal(user: ReturnType<typeof userEvent.setup>) {
    const closeButton = screen.getByRole('button', { name: /close|cancel/i })
    await user.click(closeButton)
  },

  // Accessibility helpers
  async checkA11y() {
    // Check for required ARIA attributes
    const buttons = screen.getAllByRole('button')
    buttons.forEach(button => {
      expect(button).toHaveAttribute('type')
    })

    // Check for form labels
    const inputs = screen.getAllByRole('textbox')
    inputs.forEach(input => {
      expect(input).toHaveAccessibleName()
    })
  },

  // Performance helpers
  measureRenderTime: async (renderFn: () => void) => {
    const start = performance.now()
    renderFn()
    await waitFor(() => {
      expect(document.body).toBeInTheDocument()
    })
    const end = performance.now()
    return end - start
  },
}

// Custom matchers for common assertions
export const customMatchers = {
  toBeVisible: (element: HTMLElement) => {
    return {
      pass: element.style.display !== 'none' && element.style.visibility !== 'hidden',
      message: () => `Expected element to be visible`,
    }
  },

  toHaveValidationError: (form: HTMLElement, fieldName: string, errorMessage?: string) => {
    const field = form.querySelector(`[name="${fieldName}"]`)
    const errorElement = form.querySelector(`[data-testid="${fieldName}-error"]`)

    const hasError = field?.getAttribute('aria-invalid') === 'true' ||
                    errorElement?.textContent?.includes(errorMessage || '') ||
                    false

    return {
      pass: hasError,
      message: () => `Expected field ${fieldName} to have validation error${errorMessage ? `: ${errorMessage}` : ''}`,
    }
  },

  toHaveLoadingState: (element: HTMLElement) => {
    const hasLoadingIndicator = element.querySelector('[data-testid="loading"]') ||
                               element.querySelector('[role="status"]') ||
                               element.textContent?.includes('Loading')

    return {
      pass: !!hasLoadingIndicator,
      message: () => 'Expected element to have loading state',
    }
  },
}

// Test data factories
export const createMockUser = (overrides = {}) => ({
  id: 'user-' + Math.random().toString(36).substring(7),
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  role: 'USER' as const,
  emailVerified: true,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  ...overrides,
})

export const createMockPayment = (overrides = {}) => ({
  id: 'payment-' + Math.random().toString(36).substring(7),
  amount: 1999,
  currency: 'USD',
  status: 'COMPLETED' as const,
  paymentMethodId: 'pm-123',
  customerId: 'cus-123',
  organizationId: 'org-123',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  paidAt: new Date().toISOString(),
  ...overrides,
})

export const createMockOrganization = (overrides = {}) => ({
  id: 'org-' + Math.random().toString(36).substring(7),
  name: 'Test Organization',
  plan: 'PRO' as const,
  status: 'ACTIVE' as const,
  maxUsers: 10,
  currentUsers: 3,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  ...overrides,
})

// Mock hooks for testing
export const mockHooks = {
  useNavigate: vi.fn(),
  useLocation: vi.fn(() => ({ pathname: '/', search: '', hash: '', state: null })),
  useParams: vi.fn(() => ({})),
  useSearchParams: vi.fn(() => [new URLSearchParams(), vi.fn()]),
}

// Cleanup function for tests
export function cleanupTests() {
  vi.clearAllMocks()
  localStorage.clear()
  sessionStorage.clear()
}

// Re-export everything from testing library
export * from '@testing-library/react'
export { userEvent }
