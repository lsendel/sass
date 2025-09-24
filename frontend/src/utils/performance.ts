// Performance utilities for React components
import { lazy, Suspense, useState, useEffect, ComponentType } from 'react';
import React from 'react';

// Type definitions
export type LazyComponent<T = any> = ComponentType<T>;

// Lazy loading wrapper
export const lazyLoad = <T extends {}>(
  importFunc: () => Promise<{ default: ComponentType<T> }>
): ComponentType<T> => {
  const Component = lazy(importFunc);

  return (props: T) =>
    React.createElement(
      Suspense,
      {
        fallback: React.createElement('div', {
          className: 'animate-pulse bg-gray-200 h-32 rounded'
        })
      },
      React.createElement(Component, props)
    );
};

// Debounce hook for search/input
export const useDebounce = (value: string, delay: number): string => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
};

// Memoized API calls
export const memoizedFetch = (() => {
  const cache = new Map<string, any>();

  return async (url: string, options?: RequestInit): Promise<any> => {
    const key = `${url}${JSON.stringify(options)}`;
    if (cache.has(key)) return cache.get(key);

    const response = await fetch(url, options);
    const data = await response.json();
    cache.set(key, data);
    return data;
  };
})();

// Performance monitoring
export const measurePerformance = (name: string, fn: () => void): void => {
  const start = performance.now();
  fn();
  const end = performance.now();
  console.log(`${name} took ${end - start} milliseconds`);
};

// Component performance wrapper
export const withPerformanceMonitoring = <T extends {}>(
  Component: ComponentType<T>,
  name: string
): ComponentType<T> => {
  return (props: T) => {
    useEffect(() => {
      const start = performance.now();
      return () => {
        const end = performance.now();
        console.log(`${name} render took ${end - start} milliseconds`);
      };
    });

    return React.createElement(Component, props);
  };
};