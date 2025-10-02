import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

import { subscriptionApi } from '../../store/api/subscriptionApi';
import { createApiTestStore } from '../utils/testStore';

/**
 * Subscription API Integration Tests
 *
 * Tests for subscription management, billing, and plan operations
 */

const API_BASE_URL = 'http://localhost:3000/api/v1';
const PLANS_BASE_URL = `${API_BASE_URL}/plans`;
const SUBSCRIPTIONS_BASE_URL = `${API_BASE_URL}/subscriptions`;

const mockPlans = [
  {
    id: 'plan-1',
    name: 'Basic',
    slug: 'basic',
    description: 'Basic plan for small teams',
    amount: 999,
    currency: 'USD',
    interval: 'MONTH',
    intervalCount: 1,
    trialDays: null,
    active: true,
    displayOrder: 1,
    features: { users: 5, storage: '10GB', support: 'Email' },
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
  {
    id: 'plan-2',
    name: 'Professional',
    slug: 'professional',
    description: 'Professional plan for growing teams',
    amount: 2999,
    currency: 'USD',
    interval: 'MONTH',
    intervalCount: 1,
    trialDays: 14,
    active: true,
    displayOrder: 2,
    features: { users: 25, storage: '100GB', support: 'Priority', analytics: true },
    createdAt: '2025-01-01T00:00:00Z',
    updatedAt: '2025-01-01T00:00:00Z',
  },
];

const mockSubscription = {
  id: 'sub-1',
  organizationId: 'org-1',
  planId: 'plan-1',
  stripeSubscriptionId: 'sub_stripe_123',
  status: 'ACTIVE',
  currentPeriodStart: '2025-09-01T00:00:00Z',
  currentPeriodEnd: '2025-10-01T00:00:00Z',
  trialEnd: null,
  cancelAt: null,
  createdAt: '2025-09-01T00:00:00Z',
  updatedAt: '2025-09-01T00:00:00Z',
};

const mockInvoices = [
  {
    id: 'inv-1',
    organizationId: 'org-1',
    subscriptionId: 'sub-1',
    stripeInvoiceId: 'in_stripe_123',
    invoiceNumber: 'INV-2025-001',
    status: 'PAID',
    subtotalAmount: 999,
    taxAmount: 0,
    totalAmount: 999,
    currency: 'USD',
    dueDate: '2025-09-08T00:00:00Z',
    paidAt: '2025-09-02T00:00:00Z',
    createdAt: '2025-09-01T00:00:00Z',
  },
];

const mockStatistics = {
  status: 'ACTIVE',
  totalInvoices: 1,
  totalAmount: 999,
  recentAmount: 999,
};

const handlers = [
  // Plan endpoints
  // GET /plans - Get all available plans
  http.get(`${PLANS_BASE_URL}`, () => {
    return HttpResponse.json(mockPlans);
  }),

  // GET /plans/interval/:interval - Get plans by interval
  http.get(`${PLANS_BASE_URL}/interval/:interval`, ({ params }) => {
    const filteredPlans = mockPlans.filter((p) => p.interval === params.interval);
    return HttpResponse.json(filteredPlans);
  }),

  // GET /plans/slug/:slug - Get plan by slug
  http.get(`${PLANS_BASE_URL}/slug/:slug`, ({ params }) => {
    const plan = mockPlans.find((p) => p.slug === params.slug);

    if (!plan) {
      return HttpResponse.json(
        { message: 'Plan not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(plan);
  }),

  // Subscription endpoints
  // POST /subscriptions - Create subscription
  http.post(`${SUBSCRIPTIONS_BASE_URL}`, async ({ request }) => {
    const body = await request.json() as any;

    if (!body.organizationId || !body.planId) {
      return HttpResponse.json(
        { message: 'Organization ID and Plan ID are required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockSubscription,
      organizationId: body.organizationId,
      planId: body.planId,
      id: 'sub-new',
    }, { status: 201 });
  }),

  // GET /subscriptions/organizations/:orgId - Get organization subscription
  http.get(`${SUBSCRIPTIONS_BASE_URL}/organizations/:orgId`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');

    if (!authHeader) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    if (params.orgId !== mockSubscription.organizationId) {
      return HttpResponse.json(
        { message: 'Subscription not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(mockSubscription);
  }),

  // GET /subscriptions/organizations/:orgId/statistics - Get subscription statistics
  http.get(`${SUBSCRIPTIONS_BASE_URL}/organizations/:orgId/statistics`, ({ params }) => {
    if (params.orgId !== mockSubscription.organizationId) {
      return HttpResponse.json(
        { message: 'Organization not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(mockStatistics);
  }),

  // GET /subscriptions/organizations/:orgId/invoices - Get organization invoices
  http.get(`${SUBSCRIPTIONS_BASE_URL}/organizations/:orgId/invoices`, ({ params }) => {
    if (params.orgId !== mockSubscription.organizationId) {
      return HttpResponse.json(
        { message: 'Organization not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(mockInvoices);
  }),

  // PUT /subscriptions/:id/plan - Change subscription plan
  http.put(`${SUBSCRIPTIONS_BASE_URL}/:id/plan`, async ({ params, request }) => {
    const body = await request.json() as any;

    if (!body.organizationId || !body.newPlanId) {
      return HttpResponse.json(
        { message: 'Organization ID and New Plan ID are required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockSubscription,
      id: params.id as string,
      planId: body.newPlanId,
    });
  }),

  // POST /subscriptions/:id/cancel - Cancel subscription
  http.post(`${SUBSCRIPTIONS_BASE_URL}/:id/cancel`, async ({ params, request }) => {
    const body = await request.json() as any;

    if (!body.organizationId) {
      return HttpResponse.json(
        { message: 'Organization ID is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockSubscription,
      id: params.id as string,
      cancelAt: body.immediate ? new Date().toISOString() : mockSubscription.currentPeriodEnd,
      status: body.immediate ? 'CANCELED' : 'ACTIVE',
    });
  }),

  // POST /subscriptions/:id/reactivate - Reactivate subscription
  http.post(`${SUBSCRIPTIONS_BASE_URL}/:id/reactivate`, async ({ params, request }) => {
    const body = await request.json() as any;

    if (!body.organizationId) {
      return HttpResponse.json(
        { message: 'Organization ID is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockSubscription,
      id: params.id as string,
      cancelAt: null,
      status: 'ACTIVE',
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const createTestStore = () => {
  return createApiTestStore(subscriptionApi, {
    auth: {
      user: {
        id: 'test-user-1',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
        role: 'USER' as const,
        emailVerified: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      } as any,
      token: 'mock-test-token',
      isAuthenticated: true,
      isLoading: false,
      error: null,
    },
  });
};

describe('Subscription API', () => {
  const mockOrganizationId = 'org-1';
  describe('GET /plans', () => {
    it('should fetch all available plans', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getAvailablePlans.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.length).toBeGreaterThan(0);
    });

    it('should return plan details', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getAvailablePlans.initiate()
      );

      result.data?.forEach((plan) => {
        expect(plan).toHaveProperty('id');
        expect(plan).toHaveProperty('name');
        expect(plan).toHaveProperty('slug');
        expect(plan).toHaveProperty('amount');
        expect(plan).toHaveProperty('currency');
        expect(plan).toHaveProperty('interval');
        expect(plan).toHaveProperty('features');
      });
    });

    it('should include only active plans', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getAvailablePlans.initiate()
      );

      result.data?.forEach((plan) => {
        expect(plan.active).toBe(true);
      });
    });
  });

  describe('GET /plans/slug/:slug', () => {
    it('should fetch plan by slug', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlanBySlug.initiate('basic')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.slug).toBe('basic');
    });

    it('should return 404 for non-existent plan', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlanBySlug.initiate('invalid-plan')
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 404,
      });
    });

    it('should include plan features', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlanBySlug.initiate('basic')
      );

      expect(result.data?.features).toBeDefined();
      expect(typeof result.data?.features).toBe('object');
    });
  });

  describe('GET /subscriptions/organizations/:orgId', () => {
    it('should fetch organization subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getOrganizationSubscription.initiate(mockOrganizationId)
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('id');
      expect(result.data).toHaveProperty('status');
      expect(result.data?.organizationId).toBe(mockOrganizationId);
    });

    it('should return subscription details', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getOrganizationSubscription.initiate(mockOrganizationId)
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('planId');
      expect(result.data).toHaveProperty('stripeSubscriptionId');
      expect(result.data).toHaveProperty('currentPeriodStart');
      expect(result.data).toHaveProperty('currentPeriodEnd');
    });

    it('should return 401 for unauthorized request', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${SUBSCRIPTIONS_BASE_URL}/organizations/:orgId`, () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      const result = await store.dispatch(
        subscriptionApi.endpoints.getOrganizationSubscription.initiate(mockOrganizationId)
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 401,
      });
    });
  });

  describe('POST /subscriptions', () => {
    it('should create new subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.createSubscription.initiate({
          organizationId: mockOrganizationId,
          planId: 'plan-1',
          paymentMethodId: 'pm-123',
        })
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('id');
      expect(result.data?.planId).toBe('plan-1');
      expect(result.data?.organizationId).toBe(mockOrganizationId);
    });

    it('should return 400 for missing organization ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.createSubscription.initiate({
          organizationId: '',
          planId: 'plan-1',
          paymentMethodId: 'pm-123',
        })
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 400,
      });
    });
  });

  describe('PUT /subscriptions/:id/plan', () => {
    it('should change subscription plan', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.changeSubscriptionPlan.initiate({
          subscriptionId: 'sub-1',
          organizationId: mockOrganizationId,
          newPlanId: 'plan-2',
        })
      );

      expect(result.error).toBeUndefined();
      expect(result.data?.planId).toBe('plan-2');
    });

    it('should return 400 for missing organization ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.changeSubscriptionPlan.initiate({
          subscriptionId: 'sub-1',
          organizationId: '',
          newPlanId: 'plan-2',
        })
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 400,
      });
    });
  });

  describe('POST /subscriptions/:id/cancel', () => {
    it('should cancel subscription at period end', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.cancelSubscription.initiate({
          subscriptionId: 'sub-1',
          organizationId: mockOrganizationId,
          immediate: false,
        })
      );

      expect(result.error).toBeUndefined();
      expect(result.data?.cancelAt).toBeDefined();
      expect(result.data?.status).toBe('ACTIVE');
    });

    it('should cancel subscription immediately', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.cancelSubscription.initiate({
          subscriptionId: 'sub-1',
          organizationId: mockOrganizationId,
          immediate: true,
        })
      );

      expect(result.error).toBeUndefined();
      expect(result.data?.status).toBe('CANCELED');
    });
  });

  describe('POST /subscriptions/:id/reactivate', () => {
    it('should reactivate canceled subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.reactivateSubscription.initiate({
          subscriptionId: 'sub-1',
          organizationId: mockOrganizationId,
        })
      );

      expect(result.error).toBeUndefined();
      expect(result.data?.cancelAt).toBeNull();
      expect(result.data?.status).toBe('ACTIVE');
    });
  });

  describe('GET /subscriptions/organizations/:orgId/invoices', () => {
    it('should fetch organization invoices', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getOrganizationInvoices.initiate(mockOrganizationId)
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
    });

    it('should return invoice details', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getOrganizationInvoices.initiate(mockOrganizationId)
      );

      result.data?.forEach((invoice) => {
        expect(invoice).toHaveProperty('id');
        expect(invoice).toHaveProperty('organizationId');
        expect(invoice).toHaveProperty('subscriptionId');
        expect(invoice).toHaveProperty('status');
        expect(invoice).toHaveProperty('totalAmount');
        expect(invoice).toHaveProperty('invoiceNumber');
      });
    });
  });

  describe('GET /subscriptions/organizations/:orgId/statistics', () => {
    it('should fetch subscription statistics', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getSubscriptionStatistics.initiate(mockOrganizationId)
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('status');
      expect(result.data).toHaveProperty('totalInvoices');
      expect(result.data).toHaveProperty('totalAmount');
      expect(result.data).toHaveProperty('recentAmount');
    });

    it('should return statistics with correct structure', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getSubscriptionStatistics.initiate(mockOrganizationId)
      );

      expect(result.data?.status).toBe('ACTIVE');
      expect(typeof result.data?.totalInvoices).toBe('number');
      expect(typeof result.data?.totalAmount).toBe('number');
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${PLANS_BASE_URL}`, () => {
          return HttpResponse.error();
        })
      );

      const result = await store.dispatch(
        subscriptionApi.endpoints.getAvailablePlans.initiate()
      );

      expect(result.status).toBe('rejected');
    });

    it('should handle server errors', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${SUBSCRIPTIONS_BASE_URL}/organizations/:orgId`, () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const result = await store.dispatch(
        subscriptionApi.endpoints.getOrganizationSubscription.initiate(mockOrganizationId)
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 500,
      });
    });
  });
});