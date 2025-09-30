import { useEffect, useRef, useState, useCallback } from 'react'
import { useAppSelector } from '../store/hooks'
import { selectCurrentUser } from '../store/slices/authSlice'
import {
  getWebSocketService,
  getPresenceService,
  getRealTimeDataService,
  initializeWebSocketServices,
  PresenceData
} from '../services/websocketService'

// Hook for real-time data synchronization
export function useRealTimeData<T>(
  resource: string,
  initialData?: T
): {
  data: T | undefined
  isLive: boolean
  lastUpdate: number | null
  error: string | null
  forceRefresh: () => void
} {
  const [data, setData] = useState<T | undefined>(initialData)
  const [isLive, setIsLive] = useState(false)
  const [lastUpdate, setLastUpdate] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const realTimeService = getRealTimeDataService()

  const forceRefresh = useCallback(() => {
    setLastUpdate(Date.now())
  }, [])

  useEffect(() => {
    if (!realTimeService) {
      setError('Real-time service not initialized')
      return
    }

    setIsLive(true)
    setError(null)

    const unsubscribe = realTimeService.subscribe(resource, (update) => {
      console.log(`[RealTimeData] Update for ${resource}:`, update)

      try {
        const { data: newData, action } = update

        switch (action) {
          case 'update':
          case 'create':
            setData(newData)
            break
          case 'delete':
            setData(undefined)
            break
          default:
            setData(newData)
        }

        setLastUpdate(Date.now())
        setError(null)
      } catch (err) {
        console.error(`[RealTimeData] Error processing update for ${resource}:`, err)
        setError(err instanceof Error ? err.message : 'Update processing failed')
      }
    })

    return () => {
      unsubscribe()
      setIsLive(false)
    }
  }, [resource, realTimeService])

  return {
    data,
    isLive,
    lastUpdate,
    error,
    forceRefresh
  }
}

// Hook for presence management
export function usePresence(): {
  onlineUsers: PresenceData[]
  currentUser: PresenceData | null
  updatePresence: (data: Partial<PresenceData>) => void
  updateCursor: (position: { x: number; y: number }) => void
  setStatus: (status: PresenceData['status']) => void
  isConnected: boolean
}
} {
  const [onlineUsers, setOnlineUsers] = useState<PresenceData[]>([])
  const [currentUser, setCurrentUser] = useState<PresenceData | null>(null)
  const [isConnected, setIsConnected] = useState(false)

  const user = useAppSelector(selectCurrentUser)
  const presenceService = getPresenceService()
  const websocketService = getWebSocketService()

  // Initialize presence when user is available
  useEffect(() => {
    if (user && presenceService && !currentUser) {
      const userData = {
        userId: user.id,
        userName: `${user.firstName} ${user.lastName}`.trim() || user.email,
        status: 'online' as const,
        currentPage: window.location.pathname
      }

      presenceService.setCurrentUser(userData)
      setCurrentUser({ ...userData, lastSeen: Date.now() })
    }
  }, [user, presenceService, currentUser])

  // Listen to connection state
  useEffect(() => {
    if (!websocketService) return

    const handleConnected = () => setIsConnected(true)
    const handleDisconnected = () => setIsConnected(false)

    websocketService.on('connected', handleConnected)
    websocketService.on('disconnected', handleDisconnected)

    setIsConnected(websocketService.isConnected())

    return () => {
      websocketService.off('connected', handleConnected)
      websocketService.off('disconnected', handleDisconnected)
    }
  }, [websocketService])

  // Listen to presence updates
  useEffect(() => {
    if (!presenceService) return

    const handlePresenceUpdate = (userData: PresenceData) => {
      setOnlineUsers(prev => {
        const filtered = prev.filter(u => u.userId !== userData.userId)
        return [...filtered, userData]
      })
    }

    const handlePresenceRemove = (userId: string) => {
      setOnlineUsers(prev => prev.filter(u => u.userId !== userId))
    }

    presenceService.on('presenceUpdate', handlePresenceUpdate)
    presenceService.on('presenceRemove', handlePresenceRemove)

    // Get initial presence data
    setOnlineUsers(presenceService.getOnlineUsers())

    return () => {
      presenceService.off('presenceUpdate', handlePresenceUpdate)
      presenceService.off('presenceRemove', handlePresenceRemove)
    }
  }, [presenceService])

  // Update current page when route changes
  useEffect(() => {
    if (presenceService) {
      presenceService.updateCurrentPage(window.location.pathname)
    }
  }, [window.location.pathname, presenceService])

  const updatePresence = useCallback((data: Partial<PresenceData>) => {
    if (presenceService) {
      presenceService.updatePresence(data)

      if (currentUser) {
        setCurrentUser({ ...currentUser, ...data, lastSeen: Date.now() })
      }
    }
  }, [presenceService, currentUser])

  const updateCursor = useCallback((position: { x: number; y: number }) => {
    if (presenceService) {
      presenceService.updateCursor(position)
    }
  }, [presenceService])

  const setStatus = useCallback((status: PresenceData['status']) => {
    if (presenceService) {
      presenceService.setStatus(status)
      updatePresence({ status })
    }
  }, [presenceService, updatePresence])

  return {
    onlineUsers: onlineUsers.filter(u => u.userId !== user?.id), // Exclude current user
    currentUser,
    updatePresence,
    updateCursor,
    setStatus,
    isConnected
  }
}

// Hook for live notifications
export function useLiveNotifications(): {
  notifications: Array<{
    id: string
    type: 'info' | 'success' | 'warning' | 'error'
    title: string
    message: string
    timestamp: number
  }>
  clearNotification: (id: string) => void
  clearAll: () => void
} {
  const [notifications, setNotifications] = useState<any[]>([])
  const websocketService = getWebSocketService()

  useEffect(() => {
    if (!websocketService) return

    const handleNotification = (payload: any) => {
      const notification = {
        id: payload.id || Date.now().toString(),
        type: payload.type || 'info',
        title: payload.title || 'Notification',
        message: payload.message || '',
        timestamp: Date.now()
      }

      setNotifications(prev => [...prev, notification])

      // Auto-remove after 5 seconds for non-error notifications
      if (notification.type !== 'error') {
        setTimeout(() => {
          clearNotification(notification.id)
        }, 5000)
      }
    }

    websocketService.on('notification', handleNotification)

    return () => {
      websocketService.off('notification', handleNotification)
    }
  }, [websocketService])

  const clearNotification = useCallback((id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id))
  }, [])

  const clearAll = useCallback(() => {
    setNotifications([])
  }, [])

  return {
    notifications,
    clearNotification,
    clearAll
  }
}

// Hook for collaborative cursor tracking
export function useCollaborativeCursors(): {
  cursors: Array<{
    userId: string
    userName: string
    position: { x: number; y: number }
    color: string
  }>
  updateMyCursor: (position: { x: number; y: number }) => void
} {
  const [cursors, setCursors] = useState<any[]>([])
  const { onlineUsers, updateCursor: updatePresenceCursor } = usePresence()
  const throttleRef = useRef<NodeJS.Timeout>()

  // Generate colors for users
  const getUserColor = useCallback((userId: string): string => {
    const colors = [
      '#3B82F6', '#EF4444', '#10B981', '#F59E0B',
      '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16'
    ]
    const hash = userId.split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0)
      return a & a
    }, 0)
    return colors[Math.abs(hash) % colors.length]
  }, [])

  // Update cursors from presence data
  useEffect(() => {
    const activeCursors = onlineUsers
      .filter(user => user.cursor && user.currentPage === window.location.pathname)
      .map(user => ({
        userId: user.userId,
        userName: user.userName,
        position: user.cursor!,
        color: getUserColor(user.userId)
      }))

    setCursors(activeCursors)
  }, [onlineUsers, getUserColor])

  // Throttled cursor update
  const updateMyCursor = useCallback((position: { x: number; y: number }) => {
    if (throttleRef.current) {
      clearTimeout(throttleRef.current)
    }

    throttleRef.current = setTimeout(() => {
      updatePresenceCursor(position)
    }, 100) // Throttle to 10 updates per second
  }, [updatePresenceCursor])

  // Cleanup throttle on unmount
  useEffect(() => {
    return () => {
      if (throttleRef.current) {
        clearTimeout(throttleRef.current)
      }
    }
  }, [])

  return {
    cursors,
    updateMyCursor
  }
}

// Hook for WebSocket connection management
export function useWebSocketConnection(): {
  isConnected: boolean
  connectionState: string
  connect: () => Promise<void>
  disconnect: () => void
  lastError: string | null
} {
  const [isConnected, setIsConnected] = useState(false)
  const [connectionState, setConnectionState] = useState('disconnected')
  const [lastError, setLastError] = useState<string | null>(null)

  const user = useAppSelector(selectCurrentUser)
  const websocketService = getWebSocketService()
  const initializationRef = useRef(false)

  // Initialize WebSocket services
  useEffect(() => {
    if (user && !initializationRef.current) {
      initializationRef.current = true

      const config = {
        url: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`,
        token: 'mock-token', // In production, get from auth store
        reconnectInterval: 5000,
        maxReconnectAttempts: 10,
        heartbeatInterval: 30000
      }

      const services = initializeWebSocketServices(config)

      // Set up event listeners
      services.websocket.on('connected', () => {
        setIsConnected(true)
        setConnectionState('connected')
        setLastError(null)
      })

      services.websocket.on('disconnected', () => {
        setIsConnected(false)
        setConnectionState('disconnected')
      })

      services.websocket.on('error', (error: any) => {
        setLastError(error?.message || 'Connection error')
        setConnectionState('error')
      })

      // Auto-connect
      services.websocket.connect().catch((error) => {
        console.error('[WebSocket] Initial connection failed:', error)
        setLastError(error?.message || 'Initial connection failed')
      })
    }
  }, [user])

  // Update connection state
  useEffect(() => {
    if (websocketService) {
      setIsConnected(websocketService.isConnected())
      setConnectionState(websocketService.getConnectionState())
    }
  }, [websocketService])

  const connect = useCallback(async () => {
    if (websocketService) {
      try {
        await websocketService.connect()
        setLastError(null)
      } catch (error) {
        setLastError(error instanceof Error ? error.message : 'Connection failed')
        throw error
      }
    }
  }, [websocketService])

  const disconnect = useCallback(() => {
    if (websocketService) {
      websocketService.disconnect()
    }
  }, [websocketService])

  return {
    isConnected,
    connectionState,
    connect,
    disconnect,
    lastError
  }
}

// Hook for optimistic updates with conflict resolution
export function useOptimisticUpdates<T>(
  resource: string,
  initialData?: T
): {
  data: T | undefined
  updateData: (newData: T, optimistic?: boolean) => Promise<void>
  hasConflict: boolean
  resolveConflict: (resolution: 'local' | 'remote') => void
  isUpdating: boolean
} {
  const [data, setData] = useState<T | undefined>(initialData)
  const [optimisticData, setOptimisticData] = useState<T | undefined>()
  const [remoteData, setRemoteData] = useState<T | undefined>()
  const [hasConflict, setHasConflict] = useState(false)
  const [isUpdating, setIsUpdating] = useState(false)

  const realTimeService = getRealTimeDataService()

  // Listen to real-time updates
  useEffect(() => {
    if (!realTimeService) return

    const unsubscribe = realTimeService.subscribe(resource, (update) => {
      const { data: newData } = update

      if (optimisticData && JSON.stringify(optimisticData) !== JSON.stringify(newData)) {
        // Conflict detected
        setRemoteData(newData)
        setHasConflict(true)
      } else {
        setData(newData)
        setOptimisticData(undefined)
      }
    })

    return unsubscribe
  }, [resource, realTimeService, optimisticData])

  const updateData = useCallback(async (newData: T, optimistic = true) => {
    setIsUpdating(true)

    if (optimistic) {
      setOptimisticData(newData)
      setData(newData) // Show optimistic update immediately
    }

    try {
      // Send update to server via WebSocket
      if (realTimeService) {
        realTimeService.publishUpdate(resource, newData, 'update')
      }
    } catch (error) {
      // Revert optimistic update on error
      if (optimistic) {
        setOptimisticData(undefined)
        setData(data) // Revert to previous data
      }
      throw error
    } finally {
      setIsUpdating(false)
    }
  }, [realTimeService, resource, data])

  const resolveConflict = useCallback((resolution: 'local' | 'remote') => {
    if (resolution === 'local' && optimisticData) {
      setData(optimisticData)
      // Send local version to server
      updateData(optimisticData, false)
    } else if (resolution === 'remote' && remoteData) {
      setData(remoteData)
    }

    setHasConflict(false)
    setOptimisticData(undefined)
    setRemoteData(undefined)
  }, [optimisticData, remoteData, updateData])

  return {
    data: hasConflict ? optimisticData : data,
    updateData,
    hasConflict,
    resolveConflict,
    isUpdating
  }
}