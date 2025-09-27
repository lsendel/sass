import { useCallback, useMemo, useRef } from 'react';
import { useSelector } from 'react-redux';
import {
  QueryDefinition,
  BaseQueryFn,
  FetchArgs,
  FetchBaseQueryError,
  UseQueryResult
} from '@reduxjs/toolkit/query';

/**
 * Performance-optimized hook for RTK Query with smart caching and deduplication.
 * Reduces unnecessary re-renders and API calls through intelligent memoization.
 */
export const useOptimizedQuery = <T extends Record<string, any>>(
  queryHook: (arg: any, options?: any) => UseQueryResult<T>,
  queryArg: any,
  options: {
    // Performance options
    enableBackground?: boolean;
    cacheTime?: number;
    staleTime?: number;
    refetchOnMount?: boolean;
    refetchOnWindowFocus?: boolean;

    // Optimization options
    selectFromResult?: (result: UseQueryResult<T>) => any;
    debounceMs?: number;

    // Conditional options
    skip?: boolean;
    skipIf?: (arg: any) => boolean;
  } = {}
) => {
  const {
    enableBackground = true,
    cacheTime = 5 * 60 * 1000, // 5 minutes
    staleTime = 30 * 1000, // 30 seconds
    refetchOnMount = true,
    refetchOnWindowFocus = false,
    selectFromResult,
    debounceMs = 0,
    skip = false,
    skipIf,
  } = options;

  // Debounce query arguments to prevent excessive API calls
  const debouncedArg = useDebounce(queryArg, debounceMs);

  // Determine if query should be skipped
  const shouldSkip = useMemo(() => {
    if (skip) return true;
    if (skipIf && skipIf(debouncedArg)) return true;
    return false;
  }, [skip, skipIf, debouncedArg]);

  // Memoize query options to prevent unnecessary re-renders
  const queryOptions = useMemo(() => ({
    refetchOnMount,
    refetchOnWindowFocus,
    skip: shouldSkip,
    selectFromResult: selectFromResult ? (result: UseQueryResult<T>) => ({
      ...result,
      ...selectFromResult(result),
    }) : undefined,
  }), [refetchOnMount, refetchOnWindowFocus, shouldSkip, selectFromResult]);

  // Execute the query
  const result = queryHook(debouncedArg, queryOptions);

  // Background refetch logic
  const backgroundRefetch = useCallback(() => {
    if (enableBackground && !result.isLoading && !result.isFetching) {
      result.refetch();
    }
  }, [enableBackground, result.isLoading, result.isFetching, result.refetch]);

  return {
    ...result,
    backgroundRefetch,
  };
};

/**
 * Custom debounce hook for query parameters.
 */
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = React.useState<T>(value);
  const timeoutRef = useRef<NodeJS.Timeout>();

  React.useEffect(() => {
    if (delay === 0) {
      setDebouncedValue(value);
      return;
    }

    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    timeoutRef.current = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [value, delay]);

  return debouncedValue;
}

/**
 * Hook for optimized pagination queries.
 */
export const useOptimizedPaginationQuery = <T extends { data: any[], total: number }>(
  queryHook: (arg: any) => UseQueryResult<T>,
  baseQuery: { page?: number; limit?: number; [key: string]: any },
  options: {
    prefetchNext?: boolean;
    prefetchPrevious?: boolean;
    maxCachePages?: number;
  } = {}
) => {
  const { prefetchNext = true, prefetchPrevious = false, maxCachePages = 5 } = options;

  const result = queryHook(baseQuery);

  // Prefetch adjacent pages for better UX
  React.useEffect(() => {
    if (result.data && !result.isLoading) {
      const currentPage = baseQuery.page || 1;
      const totalPages = Math.ceil(result.data.total / (baseQuery.limit || 10));

      // Prefetch next page
      if (prefetchNext && currentPage < totalPages) {
        queryHook({ ...baseQuery, page: currentPage + 1 });
      }

      // Prefetch previous page
      if (prefetchPrevious && currentPage > 1) {
        queryHook({ ...baseQuery, page: currentPage - 1 });
      }
    }
  }, [result.data, result.isLoading, baseQuery, prefetchNext, prefetchPrevious, queryHook]);

  return result;
};

/**
 * Hook for optimized infinite scroll queries.
 */
export const useOptimizedInfiniteQuery = <T extends { data: any[], hasMore: boolean }>(
  queryHook: (arg: any) => UseQueryResult<T>,
  baseQuery: any,
  options: {
    getNextPageParam?: (lastPage: T, allPages: T[]) => any;
    threshold?: number;
  } = {}
) => {
  const { getNextPageParam, threshold = 0.8 } = options;
  const [pages, setPages] = React.useState<T[]>([]);
  const [currentQuery, setCurrentQuery] = React.useState(baseQuery);

  const result = queryHook(currentQuery);

  // Append new data to existing pages
  React.useEffect(() => {
    if (result.data && !result.isLoading) {
      setPages(prev => {
        // Check if this is a new page or a refresh
        const isNewPage = currentQuery.page > 1;
        return isNewPage ? [...prev, result.data] : [result.data];
      });
    }
  }, [result.data, result.isLoading, currentQuery.page]);

  // Function to load next page
  const loadNextPage = useCallback(() => {
    if (result.data && getNextPageParam) {
      const nextParams = getNextPageParam(result.data, pages);
      if (nextParams) {
        setCurrentQuery(nextParams);
      }
    }
  }, [result.data, getNextPageParam, pages]);

  // Intersection observer for automatic loading
  const observerRef = useRef<IntersectionObserver>();
  const loadMoreRef = useCallback((node: HTMLElement | null) => {
    if (result.isLoading) return;
    if (observerRef.current) observerRef.current.disconnect();

    observerRef.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && result.data?.hasMore) {
        loadNextPage();
      }
    }, { threshold });

    if (node) observerRef.current.observe(node);
  }, [result.isLoading, result.data?.hasMore, loadNextPage, threshold]);

  return {
    ...result,
    data: pages.flatMap(page => page.data),
    hasMore: result.data?.hasMore || false,
    loadNextPage,
    loadMoreRef,
  };
};