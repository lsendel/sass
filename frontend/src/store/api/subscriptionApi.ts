import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

import { withAuthHeader } from './utils'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api/v1'

export type SubscriptionStatus =
  | 'ACTIVE'
  | 'TRIALING'
  | 'PAST_DUE'
  | 'CANCELED'
  | 'UNPAID'
  | 'INCOMPLETE'
  | 'INCOMPLETE_EXPIRED'

export interface Subscription {
  id: string
  organizationId: string
  planId: string
  stripeSubscriptionId: string
  status: SubscriptionStatus
  currentPeriodStart: string
  currentPeriodEnd: string
  trialEnd: string | null
  cancelAt: string | null
  createdAt: string
  updatedAt: string
}

export type PlanInterval = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'

export interface Plan {
  id: string
  name: string
  slug: string
  description: string
  amount: number
  currency: string
  interval: PlanInterval
  intervalCount: number
  trialDays: number | null
  active: boolean
  displayOrder: number
  features: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export type InvoiceStatus = 'DRAFT' | 'OPEN' | 'PAID' | 'VOID' | 'UNCOLLECTIBLE'

export interface Invoice {
  id: string
  organizationId: string
  subscriptionId: string
  stripeInvoiceId: string
  invoiceNumber: string
  status: InvoiceStatus
  subtotalAmount: number
  taxAmount: number
  totalAmount: number
  currency: string
  dueDate: string | null
  paidAt: string | null
  createdAt: string
}

export interface SubscriptionStatistics {
  status: SubscriptionStatus | null
  totalInvoices: number
  totalAmount: number
  recentAmount: number
}

export interface CreateSubscriptionRequest {
  organizationId: string
  planId: string
  paymentMethodId?: string
  trialEligible?: boolean
}

export interface ChangePlanRequest {
  organizationId: string
  newPlanId: string
  prorationBehavior?: boolean
}

export interface CancelSubscriptionRequest {
  organizationId: string
  immediate?: boolean
  cancelAt?: string
}

export interface ReactivateSubscriptionRequest {
  organizationId: string
}

export const subscriptionApi = createApi({
  reducerPath: 'subscriptionApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}`,
    prepareHeaders: (headers, { getState }) => withAuthHeader(headers, getState),
  }),
  tagTypes: ['Subscription', 'Plan', 'Invoice', 'SubscriptionStatistics'],
  endpoints: builder => ({
    // Subscription endpoints
    createSubscription: builder.mutation<
      Subscription,
      CreateSubscriptionRequest
    >({
      query: body => ({
        url: '/subscriptions',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Subscription', 'SubscriptionStatistics'],
    }),

    getOrganizationSubscription: builder.query<Subscription, string>({
      query: organizationId => `/subscriptions/organizations/${organizationId}`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'Subscription', id: organizationId },
      ],
    }),

    changeSubscriptionPlan: builder.mutation<
      Subscription,
      {
        subscriptionId: string
      } & ChangePlanRequest
    >({
      query: ({ subscriptionId, ...body }) => ({
        url: `/subscriptions/${subscriptionId}/plan`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['Subscription', 'SubscriptionStatistics'],
    }),

    cancelSubscription: builder.mutation<
      Subscription,
      {
        subscriptionId: string
      } & CancelSubscriptionRequest
    >({
      query: ({ subscriptionId, ...body }) => ({
        url: `/subscriptions/${subscriptionId}/cancel`,
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Subscription', 'SubscriptionStatistics'],
    }),

    reactivateSubscription: builder.mutation<
      Subscription,
      {
        subscriptionId: string
      } & ReactivateSubscriptionRequest
    >({
      query: ({ subscriptionId, ...body }) => ({
        url: `/subscriptions/${subscriptionId}/reactivate`,
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Subscription', 'SubscriptionStatistics'],
    }),

    getSubscriptionStatistics: builder.query<SubscriptionStatistics, string>({
      query: organizationId =>
        `/subscriptions/organizations/${organizationId}/statistics`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'SubscriptionStatistics', id: organizationId },
      ],
    }),

    getOrganizationInvoices: builder.query<Invoice[], string>({
      query: organizationId =>
        `/subscriptions/organizations/${organizationId}/invoices`,
      providesTags: (_result, _error, organizationId) => [
        { type: 'Invoice', id: organizationId },
      ],
    }),

    // Plan endpoints
    getAvailablePlans: builder.query<Plan[], void>({
      query: () => '/plans',
      providesTags: ['Plan'],
    }),

    getPlansByInterval: builder.query<Plan[], PlanInterval>({
      query: interval => `/plans/interval/${interval}`,
      providesTags: ['Plan'],
    }),

    getPlanBySlug: builder.query<Plan, string>({
      query: slug => `/plans/slug/${slug}`,
      providesTags: result => (result ? [{ type: 'Plan', id: result.id }] : []),
    }),
  }),
})

export const {
  useCreateSubscriptionMutation,
  useGetOrganizationSubscriptionQuery,
  useChangeSubscriptionPlanMutation,
  useCancelSubscriptionMutation,
  useReactivateSubscriptionMutation,
  useGetSubscriptionStatisticsQuery,
  useGetOrganizationInvoicesQuery,
  useGetAvailablePlansQuery,
  useGetPlansByIntervalQuery,
  useGetPlanBySlugQuery,
  // Export for lazy queries
  useLazyGetOrganizationSubscriptionQuery,
  useLazyGetSubscriptionStatisticsQuery,
  useLazyGetPlanBySlugQuery,
} = subscriptionApi
