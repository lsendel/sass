import React, { useState, useCallback, useRef } from 'react';
import { z } from 'zod';

import { useApiErrorHandler } from '@/lib/api/errorHandling';
import { validateApiResponse, ApiValidationError } from '@/lib/api/validation';

/**
 * React hooks for validated API calls with error handling and retry logic
 */

export interface ValidatedQueryState<T> {
  data: T | null;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  isValidating: boolean;
  refetch: () => Promise<void>;
}

export interface ValidatedMutationState<T> {
  data: T | null;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  mutate: (variables?: any) => Promise<T>;
  reset: () => void;
}

export interface ValidatedApiOptions {
  enabled?: boolean;
  retry?: boolean;
  maxRetries?: number;
  retryDelay?: number;
  onSuccess?: (data: any) => void;
  onError?: (error: Error) => void;
  staleTime?: number;
}

/**
 * Hook for validated queries with automatic caching and error handling
 */
export function useValidatedQuery<T>(
  queryKey: string,
  queryFn: () => Promise<unknown>,
  responseSchema: z.ZodSchema<T>,
  options: ValidatedApiOptions = {}
): ValidatedQueryState<T> {
  const {
    enabled = true,
    retry = true,
    maxRetries = 3,
    retryDelay = 1000,
    onSuccess,
    onError,
    staleTime = 300000, // 5 minutes default
  } = options;

  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [_lastFetch, setLastFetch] = useState<number>(0);

  const cacheRef = useRef<Map<string, { data: T; timestamp: number }>>(new Map());
  const { handleError, createRetryHandler } = useApiErrorHandler();

  const executeQuery = useCallback(async (force = false) => {
    if (!enabled) return;

    const now = Date.now();
    const cached = cacheRef.current.get(queryKey);

    // Return cached data if still fresh and not forcing refresh
    if (!force && cached && (now - cached.timestamp) < staleTime) {
      setData(cached.data);
      setIsError(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setIsValidating(true);
    setIsError(false);
    setError(null);

    try {
      const retryableQuery = retry
        ? createRetryHandler(queryFn, maxRetries, retryDelay)
        : queryFn;

      const response = await retryableQuery();
      const validatedData = validateApiResponse(responseSchema, response, queryKey);

      // Cache the validated data
      cacheRef.current.set(queryKey, {
        data: validatedData,
        timestamp: now,
      });

      setData(validatedData);
      setLastFetch(now);
      onSuccess?.(validatedData);
    } catch (err) {
      const apiError = handleError(err, queryKey);
      setError(apiError);
      setIsError(true);
      onError?.(apiError);
    } finally {
      setIsLoading(false);
      setIsValidating(false);
    }
  }, [
    enabled,
    queryKey,
    queryFn,
    responseSchema,
    retry,
    maxRetries,
    retryDelay,
    staleTime,
    onSuccess,
    onError,
    handleError,
    createRetryHandler,
  ]);

  // Auto-execute on mount and when dependencies change
  React.useEffect(() => {
    void executeQuery();
  }, [executeQuery]);

  const refetch = useCallback(() => executeQuery(true), [executeQuery]);

  return {
    data,
    isLoading,
    isError,
    error,
    isValidating,
    refetch,
  };
}

/**
 * Hook for validated mutations with optimistic updates
 */
export function useValidatedMutation<TRequest, TResponse>(
  mutationFn: (variables: TRequest) => Promise<unknown>,
  requestSchema: z.ZodSchema<TRequest>,
  responseSchema: z.ZodSchema<TResponse>,
  options: ValidatedApiOptions & {
    optimisticUpdate?: (variables: TRequest) => TResponse;
    rollbackUpdate?: () => void;
  } = {}
): ValidatedMutationState<TResponse> {
  const {
    retry = false,
    maxRetries = 1,
    retryDelay = 1000,
    onSuccess,
    onError,
    optimisticUpdate,
    rollbackUpdate,
  } = options;

  const [data, setData] = useState<TResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const { handleError, createRetryHandler } = useApiErrorHandler();

  const mutate = useCallback(async (variables: TRequest): Promise<TResponse> => {
    setIsLoading(true);
    setIsError(false);
    setError(null);

    // Validate request data
    try {
      requestSchema.parse(variables);
    } catch (err) {
      const validationError = new ApiValidationError(
        'Request validation failed',
        err as z.ZodError,
        variables
      );
      const apiError = handleError(validationError, 'mutation request');
      setError(apiError);
      setIsError(true);
      setIsLoading(false);
      throw apiError;
    }

    // Optimistic update
    let optimisticData: TResponse | null = null;
    if (optimisticUpdate) {
      optimisticData = optimisticUpdate(variables);
      setData(optimisticData);
    }

    try {
      const retryableMutation = retry
        ? createRetryHandler(() => mutationFn(variables), maxRetries, retryDelay)
        : () => mutationFn(variables);

      const response = await retryableMutation();
      const validatedData = validateApiResponse(responseSchema, response, 'mutation');

      setData(validatedData);
      onSuccess?.(validatedData);
      return validatedData;
    } catch (err) {
      // Rollback optimistic update on error
      if (optimisticData && rollbackUpdate) {
        rollbackUpdate();
        setData(null);
      }

      const apiError = handleError(err, 'mutation');
      setError(apiError);
      setIsError(true);
      onError?.(apiError);
      throw apiError;
    } finally {
      setIsLoading(false);
    }
  }, [
    mutationFn,
    requestSchema,
    responseSchema,
    retry,
    maxRetries,
    retryDelay,
    onSuccess,
    onError,
    optimisticUpdate,
    rollbackUpdate,
    handleError,
    createRetryHandler,
  ]);

  const reset = useCallback(() => {
    setData(null);
    setIsLoading(false);
    setIsError(false);
    setError(null);
  }, []);

  return {
    data,
    isLoading,
    isError,
    error,
    mutate,
    reset,
  };
}

/**
 * Hook for infinite queries with validation
 */
export function useValidatedInfiniteQuery<T>(
  queryKey: string,
  queryFn: (pageParam: number) => Promise<unknown>,
  responseSchema: z.ZodSchema<T>,
  options: ValidatedApiOptions & {
    getNextPageParam?: (lastPage: T, allPages: T[]) => number | undefined;
    getPreviousPageParam?: (firstPage: T, allPages: T[]) => number | undefined;
  } = {}
) {
  const {
    enabled = true,
    getNextPageParam,
    getPreviousPageParam,
    onSuccess,
    onError,
  } = options;

  const [pages, setPages] = useState<T[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [isFetchingNextPage, setIsFetchingNextPage] = useState(false);
  const [isFetchingPreviousPage, setIsFetchingPreviousPage] = useState(false);
  const [hasNextPage, setHasNextPage] = useState(true);
  const [hasPreviousPage, setHasPreviousPage] = useState(false);

  const { handleError } = useApiErrorHandler();

  const fetchPage = useCallback(async (pageParam: number, isNext = true) => {
    if (isNext) {
      setIsFetchingNextPage(true);
    } else {
      setIsFetchingPreviousPage(true);
    }

    try {
      const response = await queryFn(pageParam);
      const validatedData = validateApiResponse(responseSchema, response, `${queryKey}-page-${pageParam}`);

      setPages(currentPages => {
        const newPages = isNext
          ? [...currentPages, validatedData]
          : [validatedData, ...currentPages];

        // Update pagination state
        if (getNextPageParam) {
          const nextParam = getNextPageParam(validatedData, newPages);
          setHasNextPage(nextParam !== undefined);
        }

        if (getPreviousPageParam) {
          const prevParam = getPreviousPageParam(validatedData, newPages);
          setHasPreviousPage(prevParam !== undefined);
        }

        return newPages;
      });

      onSuccess?.(validatedData);
    } catch (err) {
      const apiError = handleError(err, `${queryKey}-infinite`);
      setError(apiError);
      setIsError(true);
      onError?.(apiError);
    } finally {
      if (isNext) {
        setIsFetchingNextPage(false);
      } else {
        setIsFetchingPreviousPage(false);
      }
      setIsLoading(false);
    }
  }, [queryKey, queryFn, responseSchema, getNextPageParam, getPreviousPageParam, onSuccess, onError, handleError]);

  const fetchNextPage = useCallback(() => {
    if (!hasNextPage || isFetchingNextPage) return;

    const nextParam = getNextPageParam?.(pages[pages.length - 1], pages);
    if (nextParam !== undefined) {
      void fetchPage(nextParam, true);
    }
  }, [pages, hasNextPage, isFetchingNextPage, getNextPageParam, fetchPage]);

  const fetchPreviousPage = useCallback(() => {
    if (!hasPreviousPage || isFetchingPreviousPage) return;

    const prevParam = getPreviousPageParam?.(pages[0], pages);
    if (prevParam !== undefined) {
      void fetchPage(prevParam, false);
    }
  }, [pages, hasPreviousPage, isFetchingPreviousPage, getPreviousPageParam, fetchPage]);

  // Initial fetch
  React.useEffect(() => {
    if (enabled && pages.length === 0) {
      setIsLoading(true);
      void fetchPage(0);
    }
  }, [enabled, pages.length, fetchPage]);

  return {
    data: pages,
    isLoading,
    isError,
    error,
    fetchNextPage,
    fetchPreviousPage,
    hasNextPage,
    hasPreviousPage,
    isFetchingNextPage,
    isFetchingPreviousPage,
  };
}

/**
 * Hook for real-time data with validation
 */
export function useValidatedSubscription<T>(
  subscriptionKey: string,
  subscribe: (callback: (data: unknown) => void) => () => void,
  responseSchema: z.ZodSchema<T>,
  options: ValidatedApiOptions = {}
) {
  const { enabled = true, onSuccess, onError } = options;
  const [data, setData] = useState<T | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const { handleError } = useApiErrorHandler();

  React.useEffect(() => {
    if (!enabled) return;

    setIsConnected(true);

    const unsubscribe = subscribe((rawData: unknown) => {
      try {
        const validatedData = validateApiResponse(responseSchema, rawData, subscriptionKey);
        setData(validatedData);
        setError(null);
        onSuccess?.(validatedData);
      } catch (err) {
        const apiError = handleError(err, subscriptionKey);
        setError(apiError);
        onError?.(apiError);
      }
    });

    return () => {
      setIsConnected(false);
      unsubscribe();
    };
  }, [enabled, subscribe, responseSchema, subscriptionKey, onSuccess, onError, handleError]);

  return {
    data,
    isConnected,
    error,
  };
}