import { useEffect, useCallback, useRef, useState } from 'react'

/**
 * Hook for managing focus trap within a container (useful for modals, dropdowns)
 */
export const useFocusTrap = (isActive: boolean = false) => {
  const containerRef = useRef<HTMLElement>(null)

  useEffect(() => {
    if (!isActive || !containerRef.current) return

    const container = containerRef.current
    const focusableElements = container.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    )

    const firstElement = focusableElements[0] as HTMLElement
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault()
          lastElement?.focus()
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault()
          firstElement?.focus()
        }
      }
    }

    // Focus first element when trap activates
    firstElement?.focus()

    document.addEventListener('keydown', handleTabKey)
    return () => document.removeEventListener('keydown', handleTabKey)
  }, [isActive])

  return containerRef
}

/**
 * Hook for managing keyboard navigation in lists/grids
 */
export const useKeyboardNavigation = <T extends HTMLElement = HTMLElement>(
  options: {
    direction?: 'horizontal' | 'vertical' | 'grid'
    loop?: boolean
    onSelect?: (index: number) => void
    disabled?: boolean
  } = {}
) => {
  const { direction = 'vertical', loop = true, onSelect, disabled = false } = options
  const [focusedIndex, setFocusedIndex] = useState(-1)
  const containerRef = useRef<T>(null)
  const itemsRef = useRef<HTMLElement[]>([])

  const updateFocusableItems = useCallback(() => {
    if (!containerRef.current) return

    const focusableElements = Array.from(
      containerRef.current.querySelectorAll(
        '[data-keyboard-nav]:not([disabled]):not([aria-disabled="true"])'
      )
    ) as HTMLElement[]

    itemsRef.current = focusableElements
  }, [])

  const moveFocus = useCallback((newIndex: number) => {
    if (!itemsRef.current.length) return

    let targetIndex = newIndex

    if (loop) {
      if (targetIndex < 0) targetIndex = itemsRef.current.length - 1
      if (targetIndex >= itemsRef.current.length) targetIndex = 0
    } else {
      targetIndex = Math.max(0, Math.min(itemsRef.current.length - 1, targetIndex))
    }

    setFocusedIndex(targetIndex)
    itemsRef.current[targetIndex]?.focus()
  }, [loop])

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (disabled || !itemsRef.current.length) return

    updateFocusableItems()
    const currentIndex = focusedIndex >= 0 ? focusedIndex : 0

    switch (e.key) {
      case 'ArrowDown':
        if (direction === 'vertical' || direction === 'grid') {
          e.preventDefault()
          moveFocus(currentIndex + 1)
        }
        break
      case 'ArrowUp':
        if (direction === 'vertical' || direction === 'grid') {
          e.preventDefault()
          moveFocus(currentIndex - 1)
        }
        break
      case 'ArrowRight':
        if (direction === 'horizontal' || direction === 'grid') {
          e.preventDefault()
          moveFocus(currentIndex + 1)
        }
        break
      case 'ArrowLeft':
        if (direction === 'horizontal' || direction === 'grid') {
          e.preventDefault()
          moveFocus(currentIndex - 1)
        }
        break
      case 'Home':
        e.preventDefault()
        moveFocus(0)
        break
      case 'End':
        e.preventDefault()
        moveFocus(itemsRef.current.length - 1)
        break
      case 'Enter':
      case ' ':
        if (onSelect && focusedIndex >= 0) {
          e.preventDefault()
          onSelect(focusedIndex)
        }
        break
    }
  }, [direction, disabled, focusedIndex, moveFocus, onSelect])

  useEffect(() => {
    updateFocusableItems()
  }, [updateFocusableItems])

  useEffect(() => {
    if (disabled) return

    const container = containerRef.current
    if (container) {
      container.addEventListener('keydown', handleKeyDown)
      return () => container.removeEventListener('keydown', handleKeyDown)
    }
  }, [handleKeyDown, disabled])

  return {
    containerRef,
    focusedIndex,
    setFocusedIndex,
    updateFocusableItems
  }
}

/**
 * Hook for managing screen reader announcements
 */
export const useScreenReader = () => {
  const [announcement, setAnnouncement] = useState('')
  const timeoutRef = useRef<NodeJS.Timeout>()

  const announce = useCallback((message: string, priority: 'polite' | 'assertive' = 'polite') => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    setAnnouncement('')

    // Use timeout to ensure the announcement is read by screen readers
    timeoutRef.current = setTimeout(() => {
      setAnnouncement(message)

      // Clear after announcement
      setTimeout(() => setAnnouncement(''), 1000)
    }, 10)
  }, [])

  const LiveRegion = ({ className = 'sr-only' }: { className?: string }) => (
    <div
      aria-live="polite"
      aria-atomic="true"
      className={className}
    >
      {announcement}
    </div>
  )

  return { announce, LiveRegion }
}

/**
 * Hook for managing reduced motion preferences
 */
export const useReducedMotion = () => {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false)

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    setPrefersReducedMotion(mediaQuery.matches)

    const handler = (event: MediaQueryListEvent) => {
      setPrefersReducedMotion(event.matches)
    }

    mediaQuery.addEventListener('change', handler)
    return () => mediaQuery.removeEventListener('change', handler)
  }, [])

  return prefersReducedMotion
}

/**
 * Hook for managing skip links
 */
export const useSkipLinks = () => {
  const skipLinksRef = useRef<HTMLElement[]>([])

  const registerSkipLink = useCallback((element: HTMLElement | null) => {
    if (element) {
      skipLinksRef.current.push(element)
    }
  }, [])

  const SkipLink = ({
    href,
    children
  }: {
    href: string
    children: React.ReactNode
  }) => (
    <a
      href={href}
      className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 bg-blue-600 text-white px-4 py-2 rounded-md z-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
    >
      {children}
    </a>
  )

  return { registerSkipLink, SkipLink }
}

/**
 * Hook for color contrast checking and high contrast mode
 */
export const useHighContrast = () => {
  const [isHighContrast, setIsHighContrast] = useState(false)

  useEffect(() => {
    // Check for Windows High Contrast mode
    const mediaQuery = window.matchMedia('(prefers-contrast: high)')
    setIsHighContrast(mediaQuery.matches)

    const handler = (event: MediaQueryListEvent) => {
      setIsHighContrast(event.matches)
    }

    mediaQuery.addEventListener('change', handler)
    return () => mediaQuery.removeEventListener('change', handler)
  }, [])

  const getContrastClass = useCallback((normalClass: string, highContrastClass: string) => {
    return isHighContrast ? highContrastClass : normalClass
  }, [isHighContrast])

  return { isHighContrast, getContrastClass }
}

/**
 * Hook for managing ARIA attributes dynamically
 */
export const useAriaAttributes = () => {
  const setAriaLabel = useCallback((element: HTMLElement | null, label: string) => {
    if (element) {
      element.setAttribute('aria-label', label)
    }
  }, [])

  const setAriaDescribedBy = useCallback((element: HTMLElement | null, id: string) => {
    if (element) {
      element.setAttribute('aria-describedby', id)
    }
  }, [])

  const setAriaExpanded = useCallback((element: HTMLElement | null, expanded: boolean) => {
    if (element) {
      element.setAttribute('aria-expanded', expanded.toString())
    }
  }, [])

  const setAriaSelected = useCallback((element: HTMLElement | null, selected: boolean) => {
    if (element) {
      element.setAttribute('aria-selected', selected.toString())
    }
  }, [])

  const setAriaDisabled = useCallback((element: HTMLElement | null, disabled: boolean) => {
    if (element) {
      element.setAttribute('aria-disabled', disabled.toString())
    }
  }, [])

  const setRole = useCallback((element: HTMLElement | null, role: string) => {
    if (element) {
      element.setAttribute('role', role)
    }
  }, [])

  return {
    setAriaLabel,
    setAriaDescribedBy,
    setAriaExpanded,
    setAriaSelected,
    setAriaDisabled,
    setRole
  }
}

/**
 * Hook for managing live regions for dynamic content updates
 */
export const useLiveRegion = (type: 'status' | 'alert' = 'status') => {
  const [message, setMessage] = useState('')
  const timeoutRef = useRef<NodeJS.Timeout>()

  const updateLiveRegion = useCallback((newMessage: string, clearAfter = 5000) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    setMessage(newMessage)

    if (clearAfter > 0) {
      timeoutRef.current = setTimeout(() => {
        setMessage('')
      }, clearAfter)
    }
  }, [])

  const clearLiveRegion = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }
    setMessage('')
  }, [])

  const LiveRegion = ({ className = 'sr-only' }: { className?: string }) => (
    <div
      role={type}
      aria-live={type === 'alert' ? 'assertive' : 'polite'}
      aria-atomic="true"
      className={className}
    >
      {message}
    </div>
  )

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  return { updateLiveRegion, clearLiveRegion, LiveRegion, message }
}

/**
 * Comprehensive accessibility provider for the entire app
 */
export const useAccessibilityContext = () => {
  const { announce, LiveRegion: AnnouncementRegion } = useScreenReader()
  const prefersReducedMotion = useReducedMotion()
  const { isHighContrast, getContrastClass } = useHighContrast()
  const { updateLiveRegion, LiveRegion: StatusRegion } = useLiveRegion('status')
  const { SkipLink } = useSkipLinks()

  return {
    announce,
    updateLiveRegion,
    prefersReducedMotion,
    isHighContrast,
    getContrastClass,
    components: {
      AnnouncementRegion,
      StatusRegion,
      SkipLink
    }
  }
}