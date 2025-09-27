import { useAppSelector } from '../store/hooks';
import { selectCurrentUser } from '../store/slices/authSlice';
import { useGetUserOrganizationsQuery } from '../store/api/organizationApi';
import { useGetPaymentStatisticsQuery } from '../store/api/paymentApi';
import { useGetSubscriptionStatisticsQuery } from '../store/api/subscriptionApi';

/**
 * Custom hook for managing dashboard data fetching and state.
 * Centralizes all dashboard-related data logic for better maintainability.
 */
export const useDashboardData = () => {
  const user = useAppSelector(selectCurrentUser);

  const {
    data: organizations,
    isLoading: orgsLoading,
    error: orgsError,
    refetch: refetchOrganizations
  } = useGetUserOrganizationsQuery();

  // Get primary organization (first one for now)
  const primaryOrg = organizations?.[0];
  const organizationId = primaryOrg?.id || '';

  const {
    data: paymentStats,
    isLoading: paymentStatsLoading,
    error: paymentStatsError,
    refetch: refetchPaymentStats
  } = useGetPaymentStatisticsQuery(organizationId, {
    skip: !organizationId,
  });

  const {
    data: subscriptionStats,
    isLoading: subscriptionStatsLoading,
    error: subscriptionStatsError,
    refetch: refetchSubscriptionStats
  } = useGetSubscriptionStatisticsQuery(organizationId, {
    skip: !organizationId,
  });

  // Aggregate loading state
  const isLoading = orgsLoading || paymentStatsLoading || subscriptionStatsLoading;

  // Aggregate error state
  const hasError = !!(orgsError || paymentStatsError || subscriptionStatsError);
  const errors = {
    organizations: orgsError,
    payments: paymentStatsError,
    subscriptions: subscriptionStatsError,
  };

  // Refetch all data
  const refetchAll = async () => {
    const results = await Promise.allSettled([
      refetchOrganizations(),
      refetchPaymentStats(),
      refetchSubscriptionStats(),
    ]);

    return results;
  };

  return {
    // User data
    user,

    // Organization data
    organizations,
    primaryOrg,
    organizationId,

    // Statistics data
    paymentStats,
    subscriptionStats,

    // Loading states
    isLoading,
    orgsLoading,
    paymentStatsLoading,
    subscriptionStatsLoading,

    // Error states
    hasError,
    errors,

    // Actions
    refetch: refetchAll,
    refetchOrganizations,
    refetchPaymentStats,
    refetchSubscriptionStats,
  };
};

/**
 * Hook specifically for dashboard statistics with better error handling.
 */
export const useDashboardStats = (organizationId?: string) => {
  const {
    data: paymentStats,
    isLoading: paymentLoading,
    error: paymentError
  } = useGetPaymentStatisticsQuery(organizationId || '', {
    skip: !organizationId,
  });

  const {
    data: subscriptionStats,
    isLoading: subscriptionLoading,
    error: subscriptionError
  } = useGetSubscriptionStatisticsQuery(organizationId || '', {
    skip: !organizationId,
  });

  return {
    paymentStats,
    subscriptionStats,
    isLoading: paymentLoading || subscriptionLoading,
    hasError: !!(paymentError || subscriptionError),
    errors: {
      payment: paymentError,
      subscription: subscriptionError,
    },
  };
};