import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { RootState } from '../index'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082/api/v1'

export type PaymentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELED'

export type Payment = {
  id: string
  organizationId: string
  stripePaymentIntentId: string
  amount: number
  currency: string
  description: string | null
  status: PaymentStatus
  subscriptionId: string | null
  invoiceId: string | null
  metadata: Record<string, string> | null
  createdAt: string
  updatedAt: string
}

export type PaymentMethodType =
  | 'CARD'
  | 'BANK_ACCOUNT'
  | 'SEPA_DEBIT'
  | 'ACH_DEBIT'

export type CardDetails = {
  lastFour: string
  brand: string
  expMonth: number
  expYear: number
}

export type BillingAddress = {
  addressLine1: string
  addressLine2: string | null
  city: string
  state: string
  postalCode: string
  country: string
}

export type BillingDetails = {
  name: string | null
  email: string | null
  address: BillingAddress | null
}

export type PaymentMethod = {
  id: string
  organizationId: string
  stripePaymentMethodId: string
  type: PaymentMethodType
  isDefault: boolean
  displayName: string
  cardDetails: CardDetails | null
  billingDetails: BillingDetails | null
  createdAt: string
}

export type PaymentIntent = {
  id: string
  clientSecret: string
  status: string
  amount: number
  currency: string
  description: string | null
  metadata: Record<string, string> | null
}

export type PaymentStatistics = {
  totalSuccessfulPayments: number
  totalAmount: number
  recentAmount: number
}

export type CreatePaymentIntentRequest = {
  organizationId: string
  amount: number
  currency: string
  description?: string
  metadata?: Record<string, string>
}

export type ConfirmPaymentIntentRequest = {
  paymentMethodId: string
}

export type AttachPaymentMethodRequest = {
  organizationId: string
  stripePaymentMethodId: string
}

export const paymentApi = createApi({
  reducerPath: 'paymentApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}/payments`,
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }
      return headers
    },
  }),
  tagTypes: ['Payment', 'PaymentMethod', 'PaymentStatistics'],
  endpoints: builder => ({
    createPaymentIntent: builder.mutation<
      PaymentIntent,
      CreatePaymentIntentRequest
    >({
      query: body => ({
        url: '/intents',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Payment'],
    }),

    confirmPaymentIntent: builder.mutation<
      PaymentIntent,
      {
        paymentIntentId: string
      } & ConfirmPaymentIntentRequest
    >({
      query: ({ paymentIntentId, ...body }) => ({
        url: `/intents/${paymentIntentId}/confirm`,
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Payment', 'PaymentStatistics'],
    }),

    getOrganizationPayments: builder.query<Payment[], string>({
      query: organizationId => `/organizations/${organizationId}`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'Payment', id: organizationId },
      ],
    }),

    getPaymentsByStatus: builder.query<
      Payment[],
      {
        organizationId: string
        status: PaymentStatus
      }
    >({
      query: ({ organizationId, status }) =>
        `/organizations/${organizationId}/status/${status}`,
      providesTags: (_result, _error, { organizationId, status }) => [
        { type: 'Payment', id: `${organizationId}-${status}` },
      ],
    }),

    getPaymentStatistics: builder.query<PaymentStatistics, string>({
      query: organizationId => `/organizations/${organizationId}/statistics`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'PaymentStatistics', id: organizationId },
      ],
    }),

    attachPaymentMethod: builder.mutation<
      PaymentMethod,
      AttachPaymentMethodRequest
    >({
      query: body => ({
        url: '/methods',
        method: 'POST',
        body,
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
      query: ({ paymentMethodId, organizationId }) => ({
        url: `/methods/${paymentMethodId}`,
        method: 'DELETE',
        params: { organizationId },
      }),
      invalidatesTags: ['PaymentMethod'],
    }),

    setDefaultPaymentMethod: builder.mutation<
      PaymentMethod,
      {
        paymentMethodId: string
        organizationId: string
      }
    >({
      query: ({ paymentMethodId, organizationId }) => ({
        url: `/methods/${paymentMethodId}/default`,
        method: 'PUT',
        params: { organizationId },
      }),
      invalidatesTags: ['PaymentMethod'],
    }),

    getOrganizationPaymentMethods: builder.query<PaymentMethod[], string>({
      query: organizationId => `/methods/organizations/${organizationId}`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'PaymentMethod', id: organizationId },
      ],
    }),
  }),
})

export const {
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
