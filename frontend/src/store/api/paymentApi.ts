import { createApi } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../index'
import { createValidatedBaseQuery, createValidatedEndpoint, wrapSuccessResponse } from '@/lib/api/validation'
import {
  PaymentSchema,
  PaymentMethodSchema,
  PaginatedResponseSchema,
  type Payment,
  type PaymentMethod,
} from '@/types/api'
import { z } from 'zod'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api/v1'

// Payment Intent schemas
const PaymentIntentSchema = z.object({
  id: z.string(),
  clientSecret: z.string(),
  status: z.string(),
  amount: z.number().positive(),
  currency: z.string().length(3),
  description: z.string().nullable(),
  metadata: z.record(z.string()).nullable(),
});

// Setup Intent schemas for adding payment methods
const SetupIntentSchema = z.object({
  id: z.string(),
  client_secret: z.string(),
  status: z.string(),
  usage: z.string().optional(),
});

const CreateSetupIntentRequestSchema = z.object({
  organizationId: z.string().uuid(),
  usage: z.string().default('off_session').optional(),
});

const PaymentStatisticsSchema = z.object({
  totalSuccessfulPayments: z.number().nonnegative(),
  totalAmount: z.number().nonnegative(),
  recentAmount: z.number().nonnegative(),
});

const CreatePaymentIntentRequestSchema = z.object({
  organizationId: z.string().uuid(),
  amount: z.number().positive(),
  currency: z.string().length(3),
  description: z.string().optional(),
  metadata: z.record(z.string()).optional(),
});

const ConfirmPaymentIntentRequestSchema = z.object({
  paymentMethodId: z.string(),
});

const AttachPaymentMethodRequestSchema = z.object({
  organizationId: z.string().uuid(),
  stripePaymentMethodId: z.string(),
});

// Export types
export type PaymentIntent = z.infer<typeof PaymentIntentSchema>;
export type SetupIntent = z.infer<typeof SetupIntentSchema>;
export type PaymentStatistics = z.infer<typeof PaymentStatisticsSchema>;
export type CreatePaymentIntentRequest = z.infer<typeof CreatePaymentIntentRequestSchema>;
export type CreateSetupIntentRequest = z.infer<typeof CreateSetupIntentRequestSchema>;
export type ConfirmPaymentIntentRequest = z.infer<typeof ConfirmPaymentIntentRequestSchema>;
export type AttachPaymentMethodRequest = z.infer<typeof AttachPaymentMethodRequestSchema>;

export const paymentApi = createApi({
  reducerPath: 'paymentApi',
  baseQuery: createValidatedBaseQuery(`${API_BASE_URL}/payments`, {
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  tagTypes: ['Payment', 'PaymentMethod', 'PaymentStatistics', 'SetupIntent'],
  endpoints: builder => ({
    createSetupIntent: builder.mutation<
      SetupIntent,
      CreateSetupIntentRequest
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(SetupIntentSchema), {
        query: body => {
          // Validate request data
          CreateSetupIntentRequestSchema.parse(body);
          return {
            url: '/setup-intents',
            method: 'POST',
            body,
          };
        },
        method: 'POST',
      }),
      invalidatesTags: ['SetupIntent'],
    }),

    createPaymentIntent: builder.mutation<
      PaymentIntent,
      CreatePaymentIntentRequest
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(PaymentIntentSchema), {
        query: body => {
          // Validate request data
          CreatePaymentIntentRequestSchema.parse(body);
          return {
            url: '/intents',
            method: 'POST',
            body,
          };
        },
        method: 'POST',
      }),
      invalidatesTags: ['Payment'],
    }),

    confirmPaymentIntent: builder.mutation<
      PaymentIntent,
      {
        paymentIntentId: string
      } & ConfirmPaymentIntentRequest
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(PaymentIntentSchema), {
        query: ({ paymentIntentId, ...body }) => {
          // Validate request data
          ConfirmPaymentIntentRequestSchema.parse(body);
          z.string().uuid().parse(paymentIntentId);
          return {
            url: `/intents/${paymentIntentId}/confirm`,
            method: 'POST',
            body,
          };
        },
        method: 'POST',
      }),
      invalidatesTags: ['Payment', 'PaymentStatistics'],
    }),

    getOrganizationPayments: builder.query<Payment[], string>({
      ...createValidatedEndpoint(PaginatedResponseSchema(PaymentSchema), {
        query: organizationId => {
          z.string().uuid().parse(organizationId);
          return `/organizations/${organizationId}`;
        },
      }),
      transformResponse: (response: any) => response.data.items,
      providesTags: (_result, _error, organizationId) => [
        { type: 'Payment', id: organizationId },
      ],
    }),

    getPaymentsByStatus: builder.query<
      Payment[],
      {
        organizationId: string
        status: string
      }
    >({
      ...createValidatedEndpoint(PaginatedResponseSchema(PaymentSchema), {
        query: ({ organizationId, status }) => {
          z.string().uuid().parse(organizationId);
          z.string().parse(status);
          return `/organizations/${organizationId}/status/${status}`;
        },
      }),
      transformResponse: (response: any) => response.data.items,
      providesTags: (_result, _error, { organizationId, status }) => [
        { type: 'Payment', id: `${organizationId}-${status}` },
      ],
    }),

    getPaymentStatistics: builder.query<PaymentStatistics, string>({
      ...createValidatedEndpoint(wrapSuccessResponse(PaymentStatisticsSchema), {
        query: organizationId => {
          z.string().uuid().parse(organizationId);
          return `/organizations/${organizationId}/statistics`;
        },
      }),
      transformResponse: (response: any) => response.data,
      providesTags: (_result, _error, organizationId) => [
        { type: 'PaymentStatistics', id: organizationId },
      ],
    }),

    attachPaymentMethod: builder.mutation<
      PaymentMethod,
      AttachPaymentMethodRequest
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(PaymentMethodSchema), {
        query: body => {
          // Validate request data
          AttachPaymentMethodRequestSchema.parse(body);
          return {
            url: '/methods',
            method: 'POST',
            body,
          };
        },
        method: 'POST',
      }),
      invalidatesTags: ['PaymentMethod'],
    }),

    detachPaymentMethod: builder.mutation<
      void,
      {
        paymentMethodId: string
        organizationId: string
      }
    >({
      query: ({ paymentMethodId, organizationId }) => {
        z.string().parse(paymentMethodId);
        z.string().uuid().parse(organizationId);
        return {
          url: `/methods/${paymentMethodId}`,
          method: 'DELETE',
          params: { organizationId },
        };
      },
      invalidatesTags: ['PaymentMethod'],
    }),

    setDefaultPaymentMethod: builder.mutation<
      PaymentMethod,
      {
        paymentMethodId: string
        organizationId: string
      }
    >({
      ...createValidatedEndpoint(wrapSuccessResponse(PaymentMethodSchema), {
        query: ({ paymentMethodId, organizationId }) => {
          z.string().parse(paymentMethodId);
          z.string().uuid().parse(organizationId);
          return {
            url: `/methods/${paymentMethodId}/default`,
            method: 'PUT',
            params: { organizationId },
          };
        },
        method: 'PUT',
      }),
      invalidatesTags: ['PaymentMethod'],
    }),

    getOrganizationPaymentMethods: builder.query<PaymentMethod[], string>({
      ...createValidatedEndpoint(PaginatedResponseSchema(PaymentMethodSchema), {
        query: organizationId => {
          z.string().uuid().parse(organizationId);
          return `/methods/organizations/${organizationId}`;
        },
      }),
      transformResponse: (response: any) => response.data.items,
      providesTags: (_result, _error, organizationId) => [
        { type: 'PaymentMethod', id: organizationId },
      ],
    }),
  }),
})

export const {
  useCreateSetupIntentMutation,
  useCreatePaymentIntentMutation,
  useConfirmPaymentIntentMutation,
  useGetOrganizationPaymentsQuery,
  useGetPaymentsByStatusQuery,
  useGetPaymentStatisticsQuery,
  useAttachPaymentMethodMutation,
  useDetachPaymentMethodMutation,
  useSetDefaultPaymentMethodMutation,
  useGetOrganizationPaymentMethodsQuery,
  // Export for lazy queries
  useLazyGetOrganizationPaymentsQuery,
  useLazyGetPaymentStatisticsQuery,
} = paymentApi
