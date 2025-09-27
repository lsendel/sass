import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { debounce, throttle } from 'lodash-es'

/**
 * Advanced performance optimization hooks
 */

/**
 * Intelligent memoization hook that tracks dependency changes
 */
export const useSmartMemo = <T>(
  factory: () => T,
  deps: React.DependencyList,
  options: {
    maxAge?: number;
    compareFunction?: (prev: React.DependencyList, next: React.DependencyList) => boolean
  } = {}
): T => {
  const { maxAge = 5000, compareFunction } = options
  const lastComputedRef = useRef<{ value: T; timestamp: number; deps: React.DependencyList }>()

  return useMemo(() => {
    const now = Date.now()
    const cached = lastComputedRef.current

    // Check if we have a cached value and it's still fresh
    if (cached && now - cached.timestamp < maxAge) {
      const depsEqual = compareFunction
        ? compareFunction(cached.deps, deps)
        : deps.every((dep, index) => dep === cached.deps[index])

      if (depsEqual) {
        return cached.value
      }
    }

    // Compute new value
    const value = factory()
    lastComputedRef.current = { value, timestamp: now, deps: [...deps] }
    return value
  }, deps)
}

/**
 * Optimized selector hook with built-in memoization
 */
export const useOptimizedSelector = <TState, TSelected>(
  selector: (state: TState) => TSelected,
  equalityFn?: (left: TSelected, right: TSelected) => boolean
) => {
  const memoizedSelector = useMemo(() => {
    let lastResult: TSelected
    let lastArgs: TState

    return (state: TState): TSelected => {
      if (state !== lastArgs) {
        const result = selector(state)
        if (!equalityFn || !equalityFn(result, lastResult)) {
          lastResult = result
        }
        lastArgs = state
      }
      return lastResult
    }
  }, [selector, equalityFn])

  return useSelector(memoizedSelector)
}

/**
 * Debounced API call hook for search and filters
 */
export const useDebouncedCallback = <T extends (...args: any[]) => any>(
  callback: T,
  delay: number = 300,
  options: { leading?: boolean; trailing?: boolean; maxWait?: number } = {}
): T => {
  const callbackRef = useRef(callback)
  callbackRef.current = callback

  return useMemo(() => {
    const debouncedFn = debounce(
      (...args: Parameters<T>) => callbackRef.current(...args),
      delay,
      options
    )
    return debouncedFn as T
  }, [delay, options.leading, options.trailing, options.maxWait])
}

/**
 * Throttled scroll/resize handler
 */
export const useThrottledCallback = <T extends (...args: any[]) => any>(
  callback: T,
  delay: number = 100
): T => {
  const callbackRef = useRef(callback)
  callbackRef.current = callback

  return useMemo(() => {
    const throttledFn = throttle(
      (...args: Parameters<T>) => callbackRef.current(...args),
      delay
    )
    return throttledFn as T
  }, [delay])
}

/**
 * Virtual scrolling hook for large lists
 */
export const useVirtualScroll = <T>(
  items: T[],
  itemHeight: number,
  containerHeight: number,
  overscan: number = 5
) => {
  const [scrollTop, setScrollTop] = useState(0)

  const visibleRange = useMemo(() => {
    const start = Math.floor(scrollTop / itemHeight)
    const end = Math.min(
      start + Math.ceil(containerHeight / itemHeight) + overscan,
      items.length
    )
    return { start: Math.max(0, start - overscan), end }
  }, [scrollTop, itemHeight, containerHeight, overscan, items.length])

  const visibleItems = useMemo(() =>
    items.slice(visibleRange.start, visibleRange.end).map((item, index) => ({
      item,
      index: visibleRange.start + index
    }))
  , [items, visibleRange.start, visibleRange.end])

  const totalHeight = items.length * itemHeight

  const handleScroll = useThrottledCallback((e: React.UIEvent<HTMLDivElement>) => {
    setScrollTop(e.currentTarget.scrollTop)
  }, 16) // ~60fps

  return {
    visibleItems,
    totalHeight,
    offsetY: visibleRange.start * itemHeight,
    handleScroll
  }
}

/**
 * Intersection Observer hook for lazy loading
 */
export const useIntersectionObserver = (
  options: IntersectionObserverInit = {}
) => {
  const [isIntersecting, setIsIntersecting] = useState(false)
  const targetRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const element = targetRef.current
    if (!element) return

    const observer = new IntersectionObserver(
      ([entry]) => setIsIntersecting(entry.isIntersecting),
      { threshold: 0.1, ...options }
    )

    observer.observe(element)
    return () => observer.disconnect()
  }, [options.threshold, options.rootMargin])

  return { targetRef, isIntersecting }
}

/**
 * Memory leak prevention hook
 */
export const useCleanup = (cleanup: () => void) => {
  const cleanupRef = useRef(cleanup)
  cleanupRef.current = cleanup

  useEffect(() => {
    return () => cleanupRef.current()
  }, [])
}

/**
 * Performance monitoring hook
 */
export const usePerformanceMonitor = (componentName: string) => {
  const renderStartTime = useRef<number>()
  const renderCount = useRef(0)

  useEffect(() => {
    renderStartTime.current = performance.now()
    renderCount.current += 1
  })

  useEffect(() => {
    if (renderStartTime.current) {
      const renderTime = performance.now() - renderStartTime.current

      if (renderTime > 16) { // Longer than one frame
        console.warn(`Slow render in ${componentName}: ${renderTime.toFixed(2)}ms`)
      }

      // Track performance metrics
      if (window.gtag) {
        window.gtag('event', 'component_render_time', {
          component_name: componentName,
          render_time: renderTime,
          render_count: renderCount.current
        })
      }
    }
  })

  const markStart = useCallback((operation: string) => {
    performance.mark(`${componentName}-${operation}-start`)
  }, [componentName])

  const markEnd = useCallback((operation: string) => {
    const startMark = `${componentName}-${operation}-start`
    const endMark = `${componentName}-${operation}-end`
    performance.mark(endMark)

    try {
      performance.measure(`${componentName}-${operation}`, startMark, endMark)
    } catch (e) {
      // Marks might not exist
    }
  }, [componentName])

  return { markStart, markEnd, renderCount: renderCount.current }
}

/**
 * Optimized state update hook that batches updates
 */
export const useBatchedState = <T>(initialState: T) => {
  const [state, setState] = useState(initialState)
  const pendingUpdates = useRef<Partial<T>[]>([])
  const timeoutRef = useRef<NodeJS.Timeout>()

  const batchedSetState = useCallback((update: Partial<T> | ((prev: T) => Partial<T>)) => {
    const updateObj = typeof update === 'function' ? update(state) : update
    pendingUpdates.current.push(updateObj)

    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    timeoutRef.current = setTimeout(() => {
      setState(prev => {
        const allUpdates = pendingUpdates.current
        pendingUpdates.current = []
        return allUpdates.reduce((acc, update) => ({ ...acc, ...update }), prev)
      })
    }, 0)
  }, [state])

  useCleanup(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }
  })

  return [state, batchedSetState] as const
}