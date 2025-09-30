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

const API_BASE_URL = 'http://localhost:3000/api/v1/subscriptions';

const mockPlans = [
  {
    id: 'plan-1',
    name: 'Basic',
    description: 'Basic plan for small teams',
    price: 9.99,
    billingInterval: 'MONTH',
    features: ['5 users', '10GB storage', 'Email support'],
    active: true,
  },
  {
    id: 'plan-2',
    name: 'Professional',
    description: 'Professional plan for growing teams',
    price: 29.99,
    billingInterval: 'MONTH',
    features: ['25 users', '100GB storage', 'Priority support', 'Advanced analytics'],
    active: true,
  },
];

const mockSubscription = {
  id: 'sub-1',
  organizationId: 'org-1',
  planId: 'plan-1',
  status: 'ACTIVE',
  currentPeriodStart: '2025-09-01T00:00:00Z',
  currentPeriodEnd: '2025-10-01T00:00:00Z',
  cancelAtPeriodEnd: false,
  billingAmount: 9.99,
  billingCurrency: 'USD',
};

const handlers = [
  // GET /subscriptions/plans
  http.get(`${API_BASE_URL}/plans`, () => {
    return HttpResponse.json(mockPlans);
  }),

  // GET /subscriptions/plans/:id
  http.get(`${API_BASE_URL}/plans/:id`, ({ params }) => {
    const plan = mockPlans.find((p) => p.id === params.id);

    if (!plan) {
      return HttpResponse.json(
        { message: 'Plan not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(plan);
  }),

  // GET /subscriptions/current
  http.get(`${API_BASE_URL}/current`, ({ request }) => {
    const authHeader = request.headers.get('authorization');

    if (!authHeader) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    return HttpResponse.json(mockSubscription);
  }),

  // POST /subscriptions/create
  http.post(`${API_BASE_URL}/create`, async ({ request }) => {
    const body = await request.json() as any;

    if (!body.planId) {
      return HttpResponse.json(
        { message: 'Plan ID is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockSubscription,
      planId: body.planId,
      id: 'sub-new',
    }, { status: 201 });
  }),

  // POST /subscriptions/:id/upgrade
  http.post(`${API_BASE_URL}/:id/upgrade`, async ({ params, request }) => {
    const body = await request.json() as any;

    if (!body.newPlanId) {
      return HttpResponse.json(
        { message: 'New plan ID is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockSubscription,
      id: params.id,
      planId: body.newPlanId,
    });
  }),

  // POST /subscriptions/:id/cancel
  http.post(`${API_BASE_URL}/:id/cancel`, async ({ params, request }) => {
    const body = await request.json() as any;

    return HttpResponse.json({
      ...mockSubscription,
      id: params.id,
      cancelAtPeriodEnd: body.immediate ? false : true,
      status: body.immediate ? 'CANCELED' : 'ACTIVE',
    });
  }),

  // POST /subscriptions/:id/reactivate
  http.post(`${API_BASE_URL}/:id/reactivate`, ({ params }) => {
    return HttpResponse.json({
      ...mockSubscription,
      id: params.id,
      cancelAtPeriodEnd: false,
      status: 'ACTIVE',
    });
  }),

  // GET /subscriptions/:id/invoices
  http.get(`${API_BASE_URL}/:id/invoices`, ({ request }) => {
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get('page') || '0');
    const size = parseInt(url.searchParams.get('size') || '20');

    const mockInvoices = [
      {
        id: 'inv-1',
        subscriptionId: 'sub-1',
        amount: 9.99,
        currency: 'USD',
        status: 'PAID',
        invoiceDate: '2025-09-01T00:00:00Z',
        dueDate: '2025-09-08T00:00:00Z',
        paidDate: '2025-09-02T00:00:00Z',
      },
    ];

    return HttpResponse.json({
      content: mockInvoices,
      page,
      size,
      totalElements: mockInvoices.length,
      totalPages: 1,
    });
  }),

  // GET /subscriptions/invoices/:id/download
  http.get(`${API_BASE_URL}/invoices/:id/download`, ({ params }) => {
    return new HttpResponse('Mock PDF Content', {
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': `attachment; filename="invoice-${params.id}.pdf"`,
      },
    });
  }),

  // POST /subscriptions/:id/payment-method
  http.post(`${API_BASE_URL}/:id/payment-method`, async ({ request }) => {
    const body = await request.json() as any;

    if (!body.paymentMethodId) {
      return HttpResponse.json(
        { message: 'Payment method ID is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      success: true,
      paymentMethodId: body.paymentMethodId,
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const createTestStore = () => {
  return createApiTestStore(subscriptionApi);
};

describe('Subscription API', () => {
  describe('GET /subscriptions/plans', () => {
    it('should fetch all available plans', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlans.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.length).toBeGreaterThan(0);
    });

    it('should return plan details', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlans.initiate()
      );

      result.data?.forEach((plan) => {
        expect(plan).toHaveProperty('id');
        expect(plan).toHaveProperty('name');
        expect(plan).toHaveProperty('price');
        expect(plan).toHaveProperty('features');
      });
    });

    it('should include only active plans', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlans.initiate()
      );

      result.data?.forEach((plan) => {
        expect(plan.active).toBe(true);
      });
    });
  });

  describe('GET /subscriptions/plans/:id', () => {
    it('should fetch plan by ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlan.initiate('plan-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.id).toBe('plan-1');
    });

    it('should return 404 for non-existent plan', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlan.initiate('invalid-plan')
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 404,
      });
    });

    it('should include plan features', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlan.initiate('plan-1')
      );

      expect(result.data?.features).toBeDefined();
      expect(Array.isArray(result.data?.features)).toBe(true);
    });
  });

  describe('GET /subscriptions/current', () => {
    it('should fetch current subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getCurrentSubscription.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('id');
      expect(result.data).toHaveProperty('status');
    });

    it('should return subscription details', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getCurrentSubscription.initiate()
      );

      expect(result.data).toHaveProperty('planId');
      expect(result.data).toHaveProperty('currentPeriodStart');
      expect(result.data).toHaveProperty('currentPeriodEnd');
      expect(result.data).toHaveProperty('billingAmount');
    });

    it('should return 401 for unauthorized request', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/current`, () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      const result = await store.dispatch(
        subscriptionApi.endpoints.getCurrentSubscription.initiate()
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 401,
      });
    });
  });

  describe('POST /subscriptions/create', () => {
    it('should create new subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.createSubscription.initiate({
          planId: 'plan-1',
          paymentMethodId: 'pm-123',
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('id');
      expect(result.data?.planId).toBe('plan-1');
    });

    it('should return 400 for missing plan ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.createSubscription.initiate({
          planId: '',
          paymentMethodId: 'pm-123',
        })
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 400,
      });
    });
  });

  describe('POST /subscriptions/:id/upgrade', () => {
    it('should upgrade subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.upgradeSubscription.initiate({
          subscriptionId: 'sub-1',
          newPlanId: 'plan-2',
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.planId).toBe('plan-2');
    });

    it('should return 400 for missing new plan ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.upgradeSubscription.initiate({
          subscriptionId: 'sub-1',
          newPlanId: '',
        })
      );

      expect(result.status).toBe('rejected');
    });
  });

  describe('POST /subscriptions/:id/cancel', () => {
    it('should cancel subscription at period end', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.cancelSubscription.initiate({
          subscriptionId: 'sub-1',
          immediate: false,
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.cancelAtPeriodEnd).toBe(true);
      expect(result.data?.status).toBe('ACTIVE');
    });

    it('should cancel subscription immediately', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.cancelSubscription.initiate({
          subscriptionId: 'sub-1',
          immediate: true,
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.status).toBe('CANCELED');
    });
  });

  describe('POST /subscriptions/:id/reactivate', () => {
    it('should reactivate canceled subscription', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.reactivateSubscription.initiate('sub-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.cancelAtPeriodEnd).toBe(false);
      expect(result.data?.status).toBe('ACTIVE');
    });
  });

  describe('GET /subscriptions/:id/invoices', () => {
    it('should fetch subscription invoices', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getInvoices.initiate({
          subscriptionId: 'sub-1',
          page: 0,
          size: 20,
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('content');
      expect(Array.isArray(result.data?.content)).toBe(true);
    });

    it('should return invoice details', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.getInvoices.initiate({
          subscriptionId: 'sub-1',
          page: 0,
          size: 20,
        })
      );

      result.data?.content.forEach((invoice) => {
        expect(invoice).toHaveProperty('id');
        expect(invoice).toHaveProperty('amount');
        expect(invoice).toHaveProperty('status');
        expect(invoice).toHaveProperty('invoiceDate');
      });
    });
  });

  describe('GET /subscriptions/invoices/:id/download', () => {
    it('should download invoice PDF', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.downloadInvoice.initiate('inv-1')
      );

      expect(result.status).toBe('fulfilled');
    });
  });

  describe('POST /subscriptions/:id/payment-method', () => {
    it('should update payment method', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.updatePaymentMethod.initiate({
          subscriptionId: 'sub-1',
          paymentMethodId: 'pm-new',
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.success).toBe(true);
    });

    it('should return 400 for missing payment method', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        subscriptionApi.endpoints.updatePaymentMethod.initiate({
          subscriptionId: 'sub-1',
          paymentMethodId: '',
        })
      );

      expect(result.status).toBe('rejected');
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/plans`, () => {
          return HttpResponse.error();
        })
      );

      const result = await store.dispatch(
        subscriptionApi.endpoints.getPlans.initiate()
      );

      expect(result.status).toBe('rejected');
    });

    it('should handle server errors', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/current`, () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const result = await store.dispatch(
        subscriptionApi.endpoints.getCurrentSubscription.initiate()
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 500,
      });
    });
  });
});