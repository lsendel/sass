import { useState, useEffect, useCallback, useRef } from 'react'

export interface RealTimeOptions {
  enabled?: boolean
  interval?: number
  pauseWhenInactive?: boolean
  pauseAfterInactivity?: number
}

export interface RealTimeUpdatesReturn {
  isActive: boolean
  lastUpdate: Date | null
  isPaused: boolean
  updateCount: number
  resumeUpdates: () => void
  pauseUpdates: () => void
  forceUpdate: () => Promise<any>
}

export const useRealTimeUpdates = (
  updateFunction: () => Promise<any>,
  options: RealTimeOptions = {}
): RealTimeUpdatesReturn => {
  const {
    interval = 30000,
    enabled = true,
    pauseWhenInactive = true,
    pauseAfterInactivity = 300000,
  } = options

  const [isActive, setIsActive] = useState(false)
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null)
  const [isPaused, setIsPaused] = useState(false)
  const [updateCount, setUpdateCount] = useState(0)

  const intervalRef = useRef<number | NodeJS.Timeout | null>(null)
  const inactivityTimerRef = useRef<number | NodeJS.Timeout | null>(null)
  const lastActivityRef = useRef<Date>(new Date())

  const clearIntervals = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
    if (inactivityTimerRef.current) {
      clearTimeout(inactivityTimerRef.current)
      inactivityTimerRef.current = null
    }
  }, [])

  const performUpdate = useCallback(async () => {
    if (!enabled || isPaused) return null

    try {
      const result = await updateFunction()
      setLastUpdate(new Date())
      setUpdateCount(prev => prev + 1)
      return result
    } catch (error) {
      console.error('Real-time update failed:', error)
      throw error
    }
  }, [updateFunction, enabled, isPaused])

  const forceUpdate = useCallback(async () => {
    return await performUpdate()
  }, [performUpdate])

  const resumeUpdates = useCallback(() => {
    setIsPaused(false)
    lastActivityRef.current = new Date()
  }, [])

  const pauseUpdates = useCallback(() => {
    setIsPaused(true)
    clearIntervals()
  }, [clearIntervals])

  // Track user activity for inactivity detection
  useEffect(() => {
    if (!pauseWhenInactive) return

    const handleActivity = () => {
      lastActivityRef.current = new Date()
      if (isPaused && enabled) {
        resumeUpdates()
      }
    }

    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click']
    events.forEach(event => {
      document.addEventListener(event, handleActivity, { passive: true })
    })

    return () => {
      events.forEach(event => {
        document.removeEventListener(event, handleActivity)
      })
    }
  }, [pauseWhenInactive, isPaused, enabled, resumeUpdates])

  // Main update interval
  useEffect(() => {
    if (!enabled || isPaused) {
      clearIntervals()
      setIsActive(false)
      return
    }

    setIsActive(true)

    // Set up the main update interval
    intervalRef.current = setInterval(() => void performUpdate(), interval)

    // Set up inactivity detection
    if (pauseWhenInactive && pauseAfterInactivity > 0) {
      const checkInactivity = () => {
        const now = new Date()
        const timeSinceLastActivity = now.getTime() - lastActivityRef.current.getTime()

        if (timeSinceLastActivity > pauseAfterInactivity) {
          setIsPaused(true)
        } else {
          inactivityTimerRef.current = setTimeout(checkInactivity, pauseAfterInactivity / 10)
        }
      }

      inactivityTimerRef.current = setTimeout(checkInactivity, pauseAfterInactivity / 10)
    }

    return clearIntervals
  }, [enabled, isPaused, interval, performUpdate, pauseWhenInactive, pauseAfterInactivity, clearIntervals])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      clearIntervals()
    }
  }, [clearIntervals])

  return {
    isActive,
    lastUpdate,
    isPaused,
    updateCount,
    resumeUpdates,
    pauseUpdates,
    forceUpdate,
  }
}
