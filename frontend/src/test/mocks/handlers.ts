import { http, HttpResponse } from 'msw'

import { generateRealisticMockData } from '@/lib/api/testing'
import {
  UserSchema,
  PaymentSchema,
  PaymentMethodSchema,
  SubscriptionSchema,
  OrganizationSchema,
  AuthMethodsResponseSchema,
  LoginResponseSchema,
  SessionInfoSchema,
} from '@/types/api'

/**
 * Mock Service Worker (MSW) handlers for testing
 * Provides realistic API responses for all endpoints
 */

const API_BASE_URL = 'http://localhost:3000/api'

// Generate mock data
const mockUser = generateRealisticMockData(UserSchema)
const mockPayment = generateRealisticMockData(PaymentSchema)
const mockPaymentMethod = generateRealisticMockData(PaymentMethodSchema)
const mockSubscription = generateRealisticMockData(SubscriptionSchema)
const mockOrganization = generateRealisticMockData(OrganizationSchema)

export const handlers = [
  // Authentication endpoints
  http.get(`${API_BASE_URL}/auth/methods`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        methods: ['PASSWORD', 'OAUTH2'],
        passwordAuthEnabled: true,
        oauth2Providers: ['google', 'github'],
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.post(`${API_BASE_URL}/auth/mock-login`, async ({ request }) => {
    const credentials = await request.json() as any

    // Simulate different scenarios based on email
    if (credentials.email === 'locked@example.com') {
      return HttpResponse.json(
        {
          success: false,
          error: {
            code: 'AUTH_002',
            message: 'Account locked due to too many failed attempts',
          },
          timestamp: new Date().toISOString(),
        },
        { status: 423 }
      )
    }

    if (credentials.email === 'invalid@example.com') {
      return HttpResponse.json(
        {
          success: false,
          error: {
            code: 'AUTH_001',
            message: 'Invalid email or password',
          },
          timestamp: new Date().toISOString(),
        },
        { status: 401 }
      )
    }

    return HttpResponse.json({
      success: true,
      data: {
        user: mockUser,
        token: 'mock-token-' + Math.random().toString(36).substring(7),
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.post(`${API_BASE_URL}/auth/register`, async ({ request }) => {
    const userData = await request.json() as any

    if (userData.email === 'existing@example.com') {
      return HttpResponse.json(
        {
          success: false,
          error: {
            code: 'VAL_003',
            message: 'Email address already in use',
          },
          timestamp: new Date().toISOString(),
        },
        { status: 409 }
      )
    }

    return HttpResponse.json({
      success: true,
      data: {
        user: { ...mockUser, email: userData.email },
        token: 'mock-token-' + Math.random().toString(36).substring(7),
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.get(`${API_BASE_URL}/auth/session`, ({ request }) => {
    const authHeader = request.headers.get('authorization')

    if (!authHeader?.startsWith('Bearer ')) {
      return HttpResponse.json(
        {
          success: false,
          error: {
            code: 'AUTH_003',
            message: 'Session expired',
          },
          timestamp: new Date().toISOString(),
        },
        { status: 401 }
      )
    }

    return HttpResponse.json({
      success: true,
      data: {
        user: mockUser,
        session: {
          activeTokens: 1,
          lastActiveAt: new Date().toISOString(),
          createdAt: new Date(Date.now() - 3600000).toISOString(),
        },
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.post(`${API_BASE_URL}/auth/logout`, () => {
    return HttpResponse.json({
      success: true,
      timestamp: new Date().toISOString(),
    })
  }),

  // Payment endpoints
  http.post(`${API_BASE_URL}/payments/intents`, async ({ request }) => {
    const paymentData = await request.json() as any

    return HttpResponse.json({
      success: true,
      data: {
        id: 'pi_mock_' + Math.random().toString(36).substring(7),
        clientSecret: 'pi_mock_secret_' + Math.random().toString(36).substring(7),
        status: 'requires_payment_method',
        amount: paymentData.amount,
        currency: paymentData.currency,
        description: paymentData.description,
        metadata: paymentData.metadata,
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.get(`${API_BASE_URL}/payments/organizations/:organizationId`, ({ params }) => {
    return HttpResponse.json({
      success: true,
      data: {
        items: [mockPayment, { ...mockPayment, id: 'payment-2', status: 'PENDING' }],
        pagination: {
          page: 0,
          size: 10,
          totalElements: 2,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false,
        },
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.get(`${API_BASE_URL}/payments/organizations/:organizationId/statistics`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        totalSuccessfulPayments: 42,
        totalAmount: 99900, // $999.00 in cents
        recentAmount: 19900, // $199.00 in cents
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.get(`${API_BASE_URL}/payments/methods/organizations/:organizationId`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        items: [
          mockPaymentMethod,
          { ...mockPaymentMethod, id: 'pm-2', isDefault: false },
        ],
        pagination: {
          page: 0,
          size: 10,
          totalElements: 2,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false,
        },
      },
      timestamp: new Date().toISOString(),
    })
  }),

  // Subscription endpoints
  http.get(`${API_BASE_URL}/subscriptions/organizations/:organizationId`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        items: [mockSubscription],
        pagination: {
          page: 0,
          size: 10,
          totalElements: 1,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false,
        },
      },
      timestamp: new Date().toISOString(),
    })
  }),

  http.get(`${API_BASE_URL}/subscriptions/plans`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        items: [
          {
            id: 'plan-basic',
            name: 'Basic Plan',
            description: 'Basic features for small teams',
            amount: 999, // $9.99
            currency: 'USD',
            interval: 'MONTHLY',
            features: ['Feature 1', 'Feature 2'],
            maxUsers: 5,
            isActive: true,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
          {
            id: 'plan-pro',
            name: 'Pro Plan',
            description: 'Advanced features for growing teams',
            amount: 1999, // $19.99
            currency: 'USD',
            interval: 'MONTHLY',
            features: ['Feature 1', 'Feature 2', 'Feature 3'],
            maxUsers: 20,
            isActive: true,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
        ],
        pagination: {
          page: 0,
          size: 10,
          totalElements: 2,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false,
        },
      },
      timestamp: new Date().toISOString(),
    })
  }),

  // Organization endpoints
  http.get(`${API_BASE_URL}/organizations/:organizationId`, () => {
    return HttpResponse.json({
      success: true,
      data: mockOrganization,
      timestamp: new Date().toISOString(),
    })
  }),

  http.get(`${API_BASE_URL}/organizations/:organizationId/members`, () => {
    return HttpResponse.json({
      success: true,
      data: {
        items: [
          { ...mockUser, role: 'ORGANIZATION_ADMIN' },
          { ...mockUser, id: 'user-2', email: 'member@example.com', role: 'USER' },
        ],
        pagination: {
          page: 0,
          size: 10,
          totalElements: 2,
          totalPages: 1,
          hasNext: false,
          hasPrevious: false,
        },
      },
      timestamp: new Date().toISOString(),
    })
  }),

  // Error simulation endpoints
  http.get(`${API_BASE_URL}/test/500`, () => {
    return HttpResponse.json(
      {
        success: false,
        error: {
          code: 'SYS_001',
          message: 'Internal server error',
        },
        timestamp: new Date().toISOString(),
      },
      { status: 500 }
    )
  }),

  http.get(`${API_BASE_URL}/test/429`, () => {
    return HttpResponse.json(
      {
        success: false,
        error: {
          code: 'RATE_001',
          message: 'Too many requests',
        },
        timestamp: new Date().toISOString(),
      },
      { status: 429 }
    )
  }),

  http.get(`${API_BASE_URL}/test/validation-error`, () => {
    return HttpResponse.json(
      {
        success: false,
        error: {
          code: 'VAL_001',
          message: 'Validation failed',
          details: {
            field: 'email',
            issue: 'Invalid email format',
          },
        },
        timestamp: new Date().toISOString(),
      },
      { status: 422 }
    )
  }),

  // Network delay simulation
  http.get(`${API_BASE_URL}/test/slow`, async () => {
    await new Promise(resolve => setTimeout(resolve, 2000)) // 2 second delay
    return HttpResponse.json({
      success: true,
      data: { message: 'Slow response' },
      timestamp: new Date().toISOString(),
    })
  }),

  // Fallback handler for unhandled requests
  http.all('*', ({ request }) => {
    console.warn(`Unhandled ${request.method} request to ${request.url}`)
    return HttpResponse.json(
      {
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: `Endpoint not found: ${request.method} ${request.url}`,
        },
        timestamp: new Date().toISOString(),
      },
      { status: 404 }
    )
  }),
]