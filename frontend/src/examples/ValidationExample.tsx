import React from 'react';
import { z } from 'zod';

import { useValidatedQuery, useValidatedMutation } from '@/hooks/useValidatedApi';
import {
  UserSchema,
  PaymentSchema,
  CreatePaymentIntentRequestSchema,
  type User,
  type Payment,
  type CreatePaymentIntentRequest
} from '@/types/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

/**
 * Comprehensive example demonstrating runtime type validation with API calls
 */

// Mock API functions for demonstration
const mockApiClient = {
  getUser(id: string): Promise<unknown> {
    // Simulate API response - could be invalid in real scenarios
    return Promise.resolve({
      success: true,
      data: {
        id: id,
        email: 'user@example.com',
        firstName: 'John',
        lastName: 'Doe',
        role: 'USER',
        emailVerified: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    });
  },

  getPayments(organizationId: string): Promise<unknown> {
    return Promise.resolve({
      success: true,
      data: {
        items: [
          {
            id: crypto.randomUUID(),
            amount: 1999,
            currency: 'USD',
            status: 'COMPLETED',
            paymentMethodId: 'pm_123',
            customerId: 'cus_123',
            organizationId,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            paidAt: new Date().toISOString(),
          },
          {
            id: crypto.randomUUID(),
            amount: 4999,
            currency: 'USD',
            status: 'PENDING',
            paymentMethodId: 'pm_456',
            customerId: 'cus_123',
            organizationId,
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
    });
  },

  createPayment(request: CreatePaymentIntentRequest): Promise<unknown> {
    return Promise.resolve({
      success: true,
      data: {
        id: crypto.randomUUID(),
        clientSecret: 'pi_test_' + Math.random().toString(36).substr(2, 9),
        status: 'requires_payment_method',
        amount: request.amount,
        currency: request.currency,
        description: request.description || null,
        metadata: request.metadata || null,
      },
      timestamp: new Date().toISOString(),
    });
  },
};

// Response schemas that wrap our data schemas
const UserResponseSchema = z.object({
  success: z.literal(true),
  data: UserSchema,
  timestamp: z.string().datetime(),
});

const PaymentsResponseSchema = z.object({
  success: z.literal(true),
  data: z.object({
    items: z.array(PaymentSchema),
    pagination: z.object({
      page: z.number().nonnegative(),
      size: z.number().positive(),
      totalElements: z.number().nonnegative(),
      totalPages: z.number().nonnegative(),
      hasNext: z.boolean(),
      hasPrevious: z.boolean(),
    }),
  }),
  timestamp: z.string().datetime(),
});

const PaymentIntentResponseSchema = z.object({
  success: z.literal(true),
  data: z.object({
    id: z.string(),
    clientSecret: z.string(),
    status: z.string(),
    amount: z.number().positive(),
    currency: z.string().length(3),
    description: z.string().nullable(),
    metadata: z.record(z.string()).nullable(),
  }),
  timestamp: z.string().datetime(),
});

// Example component using validated queries
const UserProfile: React.FC<{ userId: string }> = ({ userId }) => {
  const {
    data: userResponse,
    isLoading,
    isError,
    error,
    refetch,
  } = useValidatedQuery(
    `user-${userId}`,
    () => mockApiClient.getUser(userId),
    UserResponseSchema,
    {
      onSuccess: (data) => {
        console.log('✅ User data validated successfully:', data.data);
      },
      onError: (error) => {
        console.error('❌ User query failed:', error.message);
      },
    }
  );

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center p-6">
          <LoadingSpinner className="mr-2" />
          Loading user profile...
        </CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-red-600 mb-4">
            Error loading user: {error?.message}
          </div>
          <Button onClick={refetch} variant="outline">
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (!userResponse) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-gray-500">No user data available</div>
        </CardContent>
      </Card>
    );
  }

  const user = userResponse.data;

  return (
    <Card>
      <CardHeader>
        <CardTitle>User Profile</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <div><strong>ID:</strong> {user.id}</div>
          <div><strong>Email:</strong> {user.email}</div>
          <div><strong>Name:</strong> {user.firstName} {user.lastName}</div>
          <div><strong>Role:</strong> {user.role}</div>
          <div><strong>Email Verified:</strong> {user.emailVerified ? 'Yes' : 'No'}</div>
          <div><strong>Created:</strong> {new Date(user.createdAt).toLocaleDateString()}</div>
        </div>
        <Button onClick={refetch} variant="outline" className="mt-4">
          Refresh
        </Button>
      </CardContent>
    </Card>
  );
};

// Example component using validated mutations
const PaymentCreator: React.FC<{ organizationId: string }> = ({ organizationId }) => {
  const {
    data: paymentIntentResponse,
    isLoading,
    isError,
    error,
    mutate: createPayment,
    reset,
  } = useValidatedMutation(
    (request: CreatePaymentIntentRequest) => mockApiClient.createPayment(request),
    CreatePaymentIntentRequestSchema,
    PaymentIntentResponseSchema,
    {
      onSuccess: (data) => {
        console.log('✅ Payment intent created successfully:', data.data);
      },
      onError: (error) => {
        console.error('❌ Payment creation failed:', error.message);
      },
    }
  );

  const handleCreatePayment = async () => {
    try {
      await createPayment({
        organizationId,
        amount: 2999, // $29.99 in cents
        currency: 'USD',
        description: 'Example payment',
        metadata: {
          source: 'validation-example',
          timestamp: new Date().toISOString(),
        },
      });
    } catch (error) {
      // Error is already handled by the mutation hook
      console.log('Payment creation error caught in component:', error);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create Payment Intent</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {paymentIntentResponse && (
            <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
              <h4 className="font-semibold text-green-800 mb-2">Payment Intent Created</h4>
              <div className="space-y-1 text-sm text-green-700">
                <div><strong>ID:</strong> {paymentIntentResponse.data.id}</div>
                <div><strong>Amount:</strong> ${(paymentIntentResponse.data.amount / 100).toFixed(2)}</div>
                <div><strong>Status:</strong> {paymentIntentResponse.data.status}</div>
                <div><strong>Client Secret:</strong> {paymentIntentResponse.data.clientSecret.substring(0, 20)}...</div>
              </div>
            </div>
          )}

          {isError && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
              <h4 className="font-semibold text-red-800 mb-2">Error</h4>
              <div className="text-sm text-red-700">{error?.message}</div>
            </div>
          )}

          <div className="flex gap-2">
            <Button
              onClick={handleCreatePayment}
              loading={isLoading}
              disabled={isLoading}
            >
              {isLoading ? 'Creating...' : 'Create Payment Intent'}
            </Button>

            {(paymentIntentResponse || isError) && (
              <Button
                onClick={reset}
                variant="outline"
                disabled={isLoading}
              >
                Reset
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

// Example component showing payment list with validation
const PaymentList: React.FC<{ organizationId: string }> = ({ organizationId }) => {
  const {
    data: paymentsResponse,
    isLoading,
    isError,
    error,
    refetch,
  } = useValidatedQuery(
    `payments-${organizationId}`,
    () => mockApiClient.getPayments(organizationId),
    PaymentsResponseSchema,
    {
      staleTime: 60000, // 1 minute
      onSuccess: (data) => {
        console.log('✅ Payments data validated successfully:', data.data);
      },
      onError: (error) => {
        console.error('❌ Payments query failed:', error.message);
      },
    }
  );

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center p-6">
          <LoadingSpinner className="mr-2" />
          Loading payments...
        </CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-red-600 mb-4">
            Error loading payments: {error?.message}
          </div>
          <Button onClick={refetch} variant="outline">
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (!paymentsResponse) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-gray-500">No payment data available</div>
        </CardContent>
      </Card>
    );
  }

  const { items: payments, pagination } = paymentsResponse.data;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Payment History ({pagination.totalElements} total)</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {payments.map((payment) => (
            <div
              key={payment.id}
              className="p-4 border border-gray-200 rounded-lg"
            >
              <div className="flex justify-between items-start mb-2">
                <div className="font-semibold">
                  ${(payment.amount / 100).toFixed(2)} {payment.currency}
                </div>
                <div className={`px-2 py-1 rounded-full text-xs font-medium ${
                  payment.status === 'COMPLETED'
                    ? 'bg-green-100 text-green-800'
                    : payment.status === 'PENDING'
                    ? 'bg-yellow-100 text-yellow-800'
                    : 'bg-red-100 text-red-800'
                }`}>
                  {payment.status}
                </div>
              </div>
              <div className="text-sm text-gray-600 space-y-1">
                <div><strong>ID:</strong> {payment.id}</div>
                <div><strong>Created:</strong> {new Date(payment.createdAt).toLocaleString()}</div>
                {payment.paidAt && (
                  <div><strong>Paid:</strong> {new Date(payment.paidAt).toLocaleString()}</div>
                )}
              </div>
            </div>
          ))}

          <Button onClick={refetch} variant="outline" className="w-full">
            Refresh Payments
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

// Main example component
export const ValidationExample: React.FC = () => {
  const exampleUserId = 'user-123';
  const exampleOrganizationId = 'org-456';

  return (
    <div className="container mx-auto p-6 space-y-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-4">Runtime API Validation Example</h1>
        <p className="text-gray-600">
          This example demonstrates runtime type validation for API responses using Zod schemas.
          All API calls are automatically validated, with proper error handling and retry logic.
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <UserProfile userId={exampleUserId} />
        <PaymentCreator organizationId={exampleOrganizationId} />
      </div>

      <PaymentList organizationId={exampleOrganizationId} />

      <Card>
        <CardHeader>
          <CardTitle>Validation Features</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <h4 className="font-semibold mb-2">✅ Implemented Features</h4>
              <ul className="text-sm space-y-1 text-gray-600">
                <li>• Runtime type validation with Zod</li>
                <li>• Automatic error handling and retry logic</li>
                <li>• Request and response validation</li>
                <li>• User-friendly error messages</li>
                <li>• Caching with staleness control</li>
                <li>• Loading and error states</li>
                <li>• Optimistic updates for mutations</li>
                <li>• Type-safe API hooks</li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold mb-2">🔧 Technical Details</h4>
              <ul className="text-sm space-y-1 text-gray-600">
                <li>• Comprehensive Zod schemas for all API types</li>
                <li>• RTK Query integration with validation</li>
                <li>• Custom hooks for validated queries/mutations</li>
                <li>• Error mapping with severity levels</li>
                <li>• Performance monitoring and benchmarking</li>
                <li>• Mock data generation for testing</li>
                <li>• Development mode validation logging</li>
                <li>• Production error reporting integration</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};