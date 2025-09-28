# Frontend Advanced Patterns & Best Practices
*Advanced architectural patterns and optimization strategies for the Spring Boot Modulith Payment Platform Frontend*

## Table of Contents
1. [Advanced State Management Patterns](#1-advanced-state-management-patterns)
2. [Micro-Frontend Architecture](#2-micro-frontend-architecture)
3. [Advanced Performance Patterns](#3-advanced-performance-patterns)
4. [Security Patterns](#4-security-patterns)
5. [Accessibility Patterns](#5-accessibility-patterns)
6. [Testing Strategies](#6-testing-strategies)
7. [Monitoring & Observability](#7-monitoring--observability)

---

## 1. Advanced State Management Patterns

### 1.1 Event-Driven State Management

```typescript
// Pattern: Event-driven state updates with side effects
// Path: src/store/events/eventBus.ts

import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/store'

// Domain events
export interface DomainEvent {
  id: string
  type: string
  aggregateId: string
  payload: any
  timestamp: number
  version: number
}

// Event types
export enum EventTypes {
  USER_REGISTERED = 'USER_REGISTERED',
  ORGANIZATION_CREATED = 'ORGANIZATION_CREATED',
  PAYMENT_METHOD_ADDED = 'PAYMENT_METHOD_ADDED',
  SUBSCRIPTION_ACTIVATED = 'SUBSCRIPTION_ACTIVATED',
  SUBSCRIPTION_CANCELLED = 'SUBSCRIPTION_CANCELLED',
}

// Event bus implementation
class EventBus {
  private handlers = new Map<string, Array<(event: DomainEvent) => void>>()
  private eventLog: DomainEvent[] = []
  private maxLogSize = 1000

  subscribe(eventType: string, handler: (event: DomainEvent) => void) {
    if (!this.handlers.has(eventType)) {
      this.handlers.set(eventType, [])
    }
    this.handlers.get(eventType)!.push(handler)

    return () => {
      const handlers = this.handlers.get(eventType)
      if (handlers) {
        const index = handlers.indexOf(handler)
        if (index > -1) {
          handlers.splice(index, 1)
        }
      }
    }
  }

  publish(event: DomainEvent) {
    // Log event
    this.eventLog.unshift(event)
    if (this.eventLog.length > this.maxLogSize) {
      this.eventLog = this.eventLog.slice(0, this.maxLogSize)
    }

    // Notify handlers
    const handlers = this.handlers.get(event.type)
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(event)
        } catch (error) {
          console.error(`Error handling event ${event.type}:`, error)
        }
      })
    }
  }

  getEventHistory(aggregateId?: string): DomainEvent[] {
    if (aggregateId) {
      return this.eventLog.filter(event => event.aggregateId === aggregateId)
    }
    return [...this.eventLog]
  }

  replayEvents(fromTimestamp: number) {
    const eventsToReplay = this.eventLog.filter(
      event => event.timestamp >= fromTimestamp
    )

    eventsToReplay.reverse().forEach(event => {
      this.publish(event)
    })
  }
}

export const eventBus = new EventBus()

// Event-driven slice
interface EventDrivenState {
  events: DomainEvent[]
  isReplaying: boolean
  lastEventId: string | null
}

const initialState: EventDrivenState = {
  events: [],
  isReplaying: false,
  lastEventId: null,
}

// Async thunk for processing events
export const processEvent = createAsyncThunk(
  'events/process',
  async (event: Omit<DomainEvent, 'id' | 'timestamp'>) => {
    const domainEvent: DomainEvent = {
      ...event,
      id: generateEventId(),
      timestamp: Date.now(),
    }

    // Publish to event bus
    eventBus.publish(domainEvent)

    // Persist event if needed
    if (shouldPersistEvent(domainEvent.type)) {
      await persistEvent(domainEvent)
    }

    return domainEvent
  }
)

const eventSlice = createSlice({
  name: 'events',
  initialState,
  reducers: {
    startReplay: (state) => {
      state.isReplaying = true
    },
    endReplay: (state) => {
      state.isReplaying = false
    },
    clearEvents: (state) => {
      state.events = []
      state.lastEventId = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(processEvent.fulfilled, (state, action) => {
        state.events.unshift(action.payload)
        state.lastEventId = action.payload.id

        // Keep only recent events in state
        if (state.events.length > 100) {
          state.events = state.events.slice(0, 100)
        }
      })
  },
})

// Event handlers for cross-cutting concerns
export const setupEventHandlers = (dispatch: any) => {
  // User registration side effects
  eventBus.subscribe(EventTypes.USER_REGISTERED, (event) => {
    // Send welcome email
    dispatch(sendWelcomeEmail(event.payload.userId))

    // Track analytics
    trackUserRegistration(event.payload)

    // Initialize user preferences
    dispatch(initializeUserPreferences(event.payload.userId))
  })

  // Organization creation side effects
  eventBus.subscribe(EventTypes.ORGANIZATION_CREATED, (event) => {
    // Create default settings
    dispatch(createDefaultOrganizationSettings(event.payload.organizationId))

    // Set up billing
    dispatch(initializeBilling(event.payload.organizationId))

    // Track conversion
    trackOrganizationCreation(event.payload)
  })

  // Payment method side effects
  eventBus.subscribe(EventTypes.PAYMENT_METHOD_ADDED, (event) => {
    // Validate payment method
    dispatch(validatePaymentMethod(event.payload.paymentMethodId))

    // Update billing status
    dispatch(updateBillingStatus(event.payload.organizationId))

    // Track payment method adoption
    trackPaymentMethodAdded(event.payload)
  })

  // Subscription lifecycle
  eventBus.subscribe(EventTypes.SUBSCRIPTION_ACTIVATED, (event) => {
    // Enable features
    dispatch(enableSubscriptionFeatures(event.payload.subscriptionId))

    // Send confirmation
    dispatch(sendSubscriptionConfirmation(event.payload.userId))

    // Update analytics
    trackSubscriptionActivation(event.payload)
  })

  eventBus.subscribe(EventTypes.SUBSCRIPTION_CANCELLED, (event) => {
    // Disable features
    dispatch(disableSubscriptionFeatures(event.payload.subscriptionId))

    // Send cancellation survey
    dispatch(sendCancellationSurvey(event.payload.userId))

    // Track churn
    trackSubscriptionCancellation(event.payload)
  })
}

// Utility functions
const generateEventId = (): string => {
  return `evt_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

const shouldPersistEvent = (eventType: string): boolean => {
  const persistableEvents = [
    EventTypes.USER_REGISTERED,
    EventTypes.ORGANIZATION_CREATED,
    EventTypes.SUBSCRIPTION_ACTIVATED,
    EventTypes.SUBSCRIPTION_CANCELLED,
  ]
  return persistableEvents.includes(eventType as EventTypes)
}

const persistEvent = async (event: DomainEvent): Promise<void> => {
  // Persist to backend or local storage
  try {
    await fetch('/api/v1/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(event),
    })
  } catch (error) {
    console.error('Failed to persist event:', error)
  }
}

// React hook for event-driven components
export const useEventListener = (
  eventType: string,
  handler: (event: DomainEvent) => void,
  dependencies: any[] = []
) => {
  React.useEffect(() => {
    const unsubscribe = eventBus.subscribe(eventType, handler)
    return unsubscribe
  }, dependencies)
}

// Higher-order component for event-driven updates
export const withEventDrivenUpdates = <P extends object>(
  Component: React.ComponentType<P>,
  eventTypes: string[]
) => {
  return React.forwardRef<any, P>((props, ref) => {
    const [updateTrigger, setUpdateTrigger] = React.useState(0)

    React.useEffect(() => {
      const unsubscribers = eventTypes.map(eventType =>
        eventBus.subscribe(eventType, () => {
          setUpdateTrigger(prev => prev + 1)
        })
      )

      return () => {
        unsubscribers.forEach(unsubscribe => unsubscribe())
      }
    }, [])

    return <Component {...props} ref={ref} key={updateTrigger} />
  })
}

export const { startReplay, endReplay, clearEvents } = eventSlice.actions
export default eventSlice.reducer
```

### 1.2 Offline-First State Management

```typescript
// Pattern: Offline-first state with synchronization
// Path: src/store/offline/offlineManager.ts

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { RootState } from '@/store'

// Offline action types
interface OfflineAction {
  id: string
  type: string
  payload: any
  timestamp: number
  retryCount: number
  maxRetries: number
  priority: number
}

// Offline queue state
interface OfflineState {
  isOnline: boolean
  syncInProgress: boolean
  queuedActions: OfflineAction[]
  failedActions: OfflineAction[]
  lastSyncTimestamp: number
  conflictResolution: 'client' | 'server' | 'merge'
}

const initialState: OfflineState = {
  isOnline: navigator.onLine,
  syncInProgress: false,
  queuedActions: [],
  failedActions: [],
  lastSyncTimestamp: 0,
  conflictResolution: 'merge',
}

// Offline action queue manager
class OfflineActionQueue {
  private queue: OfflineAction[] = []
  private isProcessing = false

  addAction(action: Omit<OfflineAction, 'id' | 'timestamp' | 'retryCount'>) {
    const offlineAction: OfflineAction = {
      ...action,
      id: generateActionId(),
      timestamp: Date.now(),
      retryCount: 0,
    }

    // Insert based on priority (higher number = higher priority)
    const insertIndex = this.queue.findIndex(
      queuedAction => queuedAction.priority < offlineAction.priority
    )

    if (insertIndex === -1) {
      this.queue.push(offlineAction)
    } else {
      this.queue.splice(insertIndex, 0, offlineAction)
    }

    return offlineAction.id
  }

  removeAction(actionId: string): boolean {
    const index = this.queue.findIndex(action => action.id === actionId)
    if (index > -1) {
      this.queue.splice(index, 1)
      return true
    }
    return false
  }

  getQueuedActions(): OfflineAction[] {
    return [...this.queue]
  }

  async processQueue(onlineHandler: (action: OfflineAction) => Promise<boolean>) {
    if (this.isProcessing || !navigator.onLine) {
      return
    }

    this.isProcessing = true

    try {
      for (const action of [...this.queue]) {
        try {
          const success = await onlineHandler(action)
          if (success) {
            this.removeAction(action.id)
          } else {
            action.retryCount++
            if (action.retryCount >= action.maxRetries) {
              this.removeAction(action.id)
              // Move to failed actions
            }
          }
        } catch (error) {
          console.error(`Failed to process offline action ${action.id}:`, error)
          action.retryCount++
        }
      }
    } finally {
      this.isProcessing = false
    }
  }

  clear() {
    this.queue = []
  }
}

const actionQueue = new OfflineActionQueue()

// Async thunks
export const queueOfflineAction = createAsyncThunk(
  'offline/queueAction',
  async (action: {
    type: string
    payload: any
    priority?: number
    maxRetries?: number
  }) => {
    const actionId = actionQueue.addAction({
      type: action.type,
      payload: action.payload,
      priority: action.priority || 1,
      maxRetries: action.maxRetries || 3,
    })

    // Try to process immediately if online
    if (navigator.onLine) {
      await processOfflineQueue()
    }

    return actionId
  }
)

export const processOfflineQueue = createAsyncThunk(
  'offline/processQueue',
  async (_, { dispatch, getState }) => {
    const state = getState() as RootState

    if (!state.offline.isOnline || state.offline.syncInProgress) {
      return
    }

    const results = await actionQueue.processQueue(async (action) => {
      try {
        // Execute the actual API call
        const response = await executeOfflineAction(action)
        return response.success
      } catch (error) {
        console.error(`Offline action failed:`, error)
        return false
      }
    })

    return results
  }
)

export const syncWithServer = createAsyncThunk(
  'offline/syncWithServer',
  async (_, { getState, dispatch }) => {
    const state = getState() as RootState

    if (!state.offline.isOnline) {
      throw new Error('Cannot sync while offline')
    }

    // Get server state
    const serverState = await fetchServerState()

    // Compare with local state
    const conflicts = detectConflicts(state, serverState)

    // Resolve conflicts based on strategy
    const resolvedState = await resolveConflicts(
      conflicts,
      state.offline.conflictResolution
    )

    // Apply resolved state
    dispatch(applyResolvedState(resolvedState))

    // Process remaining offline actions
    await dispatch(processOfflineQueue())

    return {
      conflictsResolved: conflicts.length,
      timestamp: Date.now(),
    }
  }
)

// Offline slice
const offlineSlice = createSlice({
  name: 'offline',
  initialState,
  reducers: {
    setOnlineStatus: (state, action) => {
      state.isOnline = action.payload

      if (action.payload && state.queuedActions.length > 0) {
        // Trigger queue processing when coming online
        state.syncInProgress = true
      }
    },

    addToQueue: (state, action) => {
      state.queuedActions.push(action.payload)
    },

    removeFromQueue: (state, action) => {
      state.queuedActions = state.queuedActions.filter(
        queuedAction => queuedAction.id !== action.payload
      )
    },

    moveToFailed: (state, action) => {
      const actionIndex = state.queuedActions.findIndex(
        queuedAction => queuedAction.id === action.payload
      )

      if (actionIndex > -1) {
        const failedAction = state.queuedActions[actionIndex]
        state.failedActions.push(failedAction)
        state.queuedActions.splice(actionIndex, 1)
      }
    },

    clearQueue: (state) => {
      state.queuedActions = []
    },

    clearFailed: (state) => {
      state.failedActions = []
    },

    setConflictResolution: (state, action) => {
      state.conflictResolution = action.payload
    },
  },

  extraReducers: (builder) => {
    builder
      .addCase(processOfflineQueue.pending, (state) => {
        state.syncInProgress = true
      })
      .addCase(processOfflineQueue.fulfilled, (state) => {
        state.syncInProgress = false
        state.lastSyncTimestamp = Date.now()
      })
      .addCase(processOfflineQueue.rejected, (state) => {
        state.syncInProgress = false
      })
      .addCase(syncWithServer.pending, (state) => {
        state.syncInProgress = true
      })
      .addCase(syncWithServer.fulfilled, (state, action) => {
        state.syncInProgress = false
        state.lastSyncTimestamp = action.payload.timestamp
      })
      .addCase(syncWithServer.rejected, (state) => {
        state.syncInProgress = false
      })
  },
})

// Network status management
export const setupNetworkListener = (dispatch: any) => {
  const handleOnline = () => {
    dispatch(setOnlineStatus(true))
    dispatch(processOfflineQueue())
  }

  const handleOffline = () => {
    dispatch(setOnlineStatus(false))
  }

  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)

  return () => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  }
}

// Offline-first RTK Query base query
export const offlineBaseQuery = (baseUrl: string) => {
  return async (args: any, api: any, extraOptions: any) => {
    const state = api.getState() as RootState

    if (!state.offline.isOnline) {
      // Queue the action for later
      if (args.method && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(args.method)) {
        api.dispatch(queueOfflineAction({
          type: 'API_CALL',
          payload: { args, baseUrl },
          priority: args.priority || 1,
        }))

        return {
          data: { queued: true, actionId: generateActionId() },
        }
      }

      // For GET requests, try to return cached data
      return {
        error: {
          status: 'OFFLINE',
          data: 'This action requires an internet connection',
        },
      }
    }

    // Online - proceed with normal request
    return fetch(`${baseUrl}${args.url}`, {
      method: args.method || 'GET',
      headers: args.headers,
      body: args.body ? JSON.stringify(args.body) : undefined,
    }).then(response => response.json())
  }
}

// React hooks for offline functionality
export const useOfflineStatus = () => {
  const isOnline = useAppSelector(state => state.offline.isOnline)
  const queueLength = useAppSelector(state => state.offline.queuedActions.length)
  const syncInProgress = useAppSelector(state => state.offline.syncInProgress)

  return { isOnline, queueLength, syncInProgress }
}

export const useOfflineAction = () => {
  const dispatch = useAppDispatch()

  const queueAction = React.useCallback((
    type: string,
    payload: any,
    options: { priority?: number; maxRetries?: number } = {}
  ) => {
    return dispatch(queueOfflineAction({
      type,
      payload,
      priority: options.priority || 1,
      maxRetries: options.maxRetries || 3,
    }))
  }, [dispatch])

  return { queueAction }
}

// Utility functions
const generateActionId = (): string => {
  return `action_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

const executeOfflineAction = async (action: OfflineAction): Promise<{ success: boolean }> => {
  // Implementation depends on action type
  switch (action.type) {
    case 'API_CALL':
      return executeAPICall(action.payload)
    case 'LOCAL_UPDATE':
      return executeLocalUpdate(action.payload)
    default:
      return { success: false }
  }
}

const executeAPICall = async (payload: any): Promise<{ success: boolean }> => {
  try {
    const response = await fetch(`${payload.baseUrl}${payload.args.url}`, {
      method: payload.args.method,
      headers: payload.args.headers,
      body: payload.args.body ? JSON.stringify(payload.args.body) : undefined,
    })

    return { success: response.ok }
  } catch (error) {
    return { success: false }
  }
}

const executeLocalUpdate = async (payload: any): Promise<{ success: boolean }> => {
  // Handle local state updates
  return { success: true }
}

const fetchServerState = async () => {
  // Fetch current server state for conflict resolution
  const response = await fetch('/api/v1/sync/state')
  return response.json()
}

const detectConflicts = (localState: any, serverState: any) => {
  // Implement conflict detection logic
  return []
}

const resolveConflicts = async (conflicts: any[], strategy: string) => {
  // Implement conflict resolution strategies
  return {}
}

export const {
  setOnlineStatus,
  addToQueue,
  removeFromQueue,
  moveToFailed,
  clearQueue,
  clearFailed,
  setConflictResolution,
} = offlineSlice.actions

export default offlineSlice.reducer
```

---

## 2. Micro-Frontend Architecture

### 2.1 Module Federation Setup

```typescript
// Pattern: Module federation for scalable micro-frontends
// Path: vite.config.ts

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'shell',
      remotes: {
        authModule: 'http://localhost:3001/assets/remoteEntry.js',
        paymentModule: 'http://localhost:3002/assets/remoteEntry.js',
        organizationModule: 'http://localhost:3003/assets/remoteEntry.js',
        subscriptionModule: 'http://localhost:3004/assets/remoteEntry.js',
      },
      shared: {
        react: {
          singleton: true,
          requiredVersion: '^18.0.0',
        },
        'react-dom': {
          singleton: true,
          requiredVersion: '^18.0.0',
        },
        'react-router-dom': {
          singleton: true,
          requiredVersion: '^6.0.0',
        },
        '@reduxjs/toolkit': {
          singleton: true,
          requiredVersion: '^1.9.0',
        },
        'react-redux': {
          singleton: true,
          requiredVersion: '^8.0.0',
        },
      },
    }),
  ],
  build: {
    target: 'esnext',
    minify: false,
    cssCodeSplit: false,
  },
})
```

### 2.2 Micro-Frontend Shell

```typescript
// Pattern: Shell application for micro-frontend orchestration
// Path: src/shell/MicroFrontendShell.tsx

import React, { Suspense, lazy, ErrorBoundary } from 'react'
import { Routes, Route } from 'react-router-dom'
import { Provider } from 'react-redux'
import { store } from '@/store'
import { LoadingSpinner, ErrorFallback } from '@/components/ui'

// Dynamic imports for micro-frontends
const AuthModule = lazy(() => import('authModule/App'))
const PaymentModule = lazy(() => import('paymentModule/App'))
const OrganizationModule = lazy(() => import('organizationModule/App'))
const SubscriptionModule = lazy(() => import('subscriptionModule/App'))

// Micro-frontend registry
interface MicroFrontendConfig {
  name: string
  component: React.ComponentType<any>
  route: string
  permissions?: string[]
  dependencies?: string[]
  errorBoundary?: boolean
  preload?: boolean
}

const microFrontends: MicroFrontendConfig[] = [
  {
    name: 'auth',
    component: AuthModule,
    route: '/auth/*',
    errorBoundary: true,
    preload: true,
  },
  {
    name: 'payment',
    component: PaymentModule,
    route: '/payments/*',
    permissions: ['PAYMENT_ACCESS'],
    dependencies: ['auth'],
    errorBoundary: true,
  },
  {
    name: 'organization',
    component: OrganizationModule,
    route: '/organizations/*',
    permissions: ['ORGANIZATION_ACCESS'],
    dependencies: ['auth'],
    errorBoundary: true,
  },
  {
    name: 'subscription',
    component: SubscriptionModule,
    route: '/subscription/*',
    permissions: ['SUBSCRIPTION_ACCESS'],
    dependencies: ['auth', 'payment'],
    errorBoundary: true,
  },
]

// Micro-frontend wrapper component
interface MicroFrontendWrapperProps {
  config: MicroFrontendConfig
  children: React.ComponentType<any>
}

const MicroFrontendWrapper: React.FC<MicroFrontendWrapperProps> = ({
  config,
  children: Component,
}) => {
  const [isReady, setIsReady] = React.useState(false)
  const [error, setError] = React.useState<Error | null>(null)

  React.useEffect(() => {
    const checkDependencies = async () => {
      try {
        if (config.dependencies) {
          for (const dep of config.dependencies) {
            await validateDependency(dep)
          }
        }

        if (config.permissions) {
          const hasPermissions = await checkPermissions(config.permissions)
          if (!hasPermissions) {
            throw new Error(`Insufficient permissions for ${config.name}`)
          }
        }

        setIsReady(true)
      } catch (err) {
        setError(err as Error)
      }
    }

    checkDependencies()
  }, [config])

  if (error) {
    return (
      <ErrorFallback
        error={error}
        onRetry={() => {
          setError(null)
          setIsReady(false)
        }}
      />
    )
  }

  if (!isReady) {
    return <LoadingSpinner />
  }

  const WrappedComponent = config.errorBoundary ? (
    <ErrorBoundary>
      <Component />
    </ErrorBoundary>
  ) : (
    <Component />
  )

  return WrappedComponent
}

// Main shell component
const MicroFrontendShell: React.FC = () => {
  React.useEffect(() => {
    // Preload critical micro-frontends
    const preloadableMFs = microFrontends.filter(mf => mf.preload)
    preloadableMFs.forEach(mf => {
      // Preload the micro-frontend
      import(/* webpackChunkName: "[request]" */ `${mf.name}Module/App`)
    })
  }, [])

  return (
    <Provider store={store}>
      <Routes>
        {microFrontends.map(config => (
          <Route
            key={config.name}
            path={config.route}
            element={
              <Suspense fallback={<LoadingSpinner />}>
                <MicroFrontendWrapper config={config}>
                  {config.component}
                </MicroFrontendWrapper>
              </Suspense>
            }
          />
        ))}
      </Routes>
    </Provider>
  )
}

// Communication bus for micro-frontends
class MicroFrontendEventBus {
  private listeners = new Map<string, Set<Function>>()

  subscribe(event: string, callback: Function) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event)!.add(callback)

    return () => {
      this.listeners.get(event)?.delete(callback)
    }
  }

  publish(event: string, data: any) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data)
        } catch (error) {
          console.error(`Error in event listener for ${event}:`, error)
        }
      })
    }
  }

  clear() {
    this.listeners.clear()
  }
}

export const microFrontendEventBus = new MicroFrontendEventBus()

// Global API for micro-frontends
declare global {
  interface Window {
    __MICRO_FRONTEND_API__: {
      eventBus: MicroFrontendEventBus
      store: typeof store
      navigation: {
        navigate: (path: string) => void
        getCurrentPath: () => string
      }
      auth: {
        getCurrentUser: () => any
        hasPermission: (permission: string) => boolean
      }
    }
  }
}

// Initialize global API
window.__MICRO_FRONTEND_API__ = {
  eventBus: microFrontendEventBus,
  store,
  navigation: {
    navigate: (path: string) => {
      window.history.pushState({}, '', path)
    },
    getCurrentPath: () => window.location.pathname,
  },
  auth: {
    getCurrentUser: () => store.getState().auth.user,
    hasPermission: (permission: string) => {
      const user = store.getState().auth.user
      return user?.permissions?.includes(permission) || false
    },
  },
}

// Utility functions
const validateDependency = async (dependency: string): Promise<void> => {
  // Check if dependency micro-frontend is available and ready
  const dependencyStatus = await checkMicroFrontendStatus(dependency)
  if (!dependencyStatus.ready) {
    throw new Error(`Dependency ${dependency} is not ready`)
  }
}

const checkPermissions = async (permissions: string[]): Promise<boolean> => {
  const user = store.getState().auth.user
  if (!user) return false

  return permissions.every(permission =>
    user.permissions?.includes(permission)
  )
}

const checkMicroFrontendStatus = async (name: string) => {
  // Implementation to check micro-frontend health
  try {
    const response = await fetch(`/api/v1/micro-frontends/${name}/health`)
    return await response.json()
  } catch (error) {
    return { ready: false, error: error.message }
  }
}

export default MicroFrontendShell
```

---

## 3. Advanced Performance Patterns

### 3.1 Intelligent Resource Management

```typescript
// Pattern: Intelligent resource loading and caching
// Path: src/performance/resourceManager.ts

import { LRUCache } from 'lru-cache'

// Resource types
enum ResourceType {
  COMPONENT = 'component',
  DATA = 'data',
  IMAGE = 'image',
  SCRIPT = 'script',
  STYLE = 'style',
}

interface Resource {
  id: string
  type: ResourceType
  url?: string
  data?: any
  priority: number
  expiresAt?: number
  dependencies?: string[]
  sizeBytes?: number
}

interface LoadingStrategy {
  eager: boolean
  preload: boolean
  prefetch: boolean
  lazy: boolean
  critical: boolean
}

// Advanced resource manager
class ResourceManager {
  private cache = new LRUCache<string, Resource>({ max: 1000 })
  private loadingPromises = new Map<string, Promise<any>>()
  private loadingStrategies = new Map<string, LoadingStrategy>()
  private networkQuality: 'slow' | 'fast' | 'unknown' = 'unknown'
  private memoryPressure: 'low' | 'medium' | 'high' = 'low'

  constructor() {
    this.detectNetworkQuality()
    this.monitorMemoryPressure()
    this.setupPerformanceObserver()
  }

  // Register resource with loading strategy
  register(
    id: string,
    resource: Omit<Resource, 'id'>,
    strategy: LoadingStrategy
  ) {
    this.cache.set(id, { ...resource, id })
    this.loadingStrategies.set(id, strategy)

    // Apply strategy
    if (strategy.critical || strategy.eager) {
      this.load(id)
    } else if (strategy.preload) {
      this.schedulePreload(id)
    } else if (strategy.prefetch) {
      this.schedulePrefetch(id)
    }
  }

  // Load resource with caching and deduplication
  async load(id: string): Promise<any> {
    // Check cache first
    const cached = this.cache.get(id)
    if (cached?.data) {
      return cached.data
    }

    // Check if already loading
    const existingPromise = this.loadingPromises.get(id)
    if (existingPromise) {
      return existingPromise
    }

    // Start loading
    const loadingPromise = this.performLoad(id)
    this.loadingPromises.set(id, loadingPromise)

    try {
      const data = await loadingPromise

      // Cache the result
      const resource = this.cache.get(id)
      if (resource) {
        resource.data = data
        this.cache.set(id, resource)
      }

      return data
    } finally {
      this.loadingPromises.delete(id)
    }
  }

  // Adaptive loading based on network and memory conditions
  private async performLoad(id: string): Promise<any> {
    const resource = this.cache.get(id)
    if (!resource) {
      throw new Error(`Resource ${id} not found`)
    }

    // Load dependencies first
    if (resource.dependencies) {
      await Promise.all(
        resource.dependencies.map(depId => this.load(depId))
      )
    }

    // Adapt loading strategy based on conditions
    const adaptedStrategy = this.adaptLoadingStrategy(resource)

    switch (resource.type) {
      case ResourceType.COMPONENT:
        return this.loadComponent(resource, adaptedStrategy)
      case ResourceType.DATA:
        return this.loadData(resource, adaptedStrategy)
      case ResourceType.IMAGE:
        return this.loadImage(resource, adaptedStrategy)
      case ResourceType.SCRIPT:
        return this.loadScript(resource, adaptedStrategy)
      case ResourceType.STYLE:
        return this.loadStyle(resource, adaptedStrategy)
      default:
        throw new Error(`Unknown resource type: ${resource.type}`)
    }
  }

  private adaptLoadingStrategy(resource: Resource): LoadingStrategy {
    const baseStrategy = this.loadingStrategies.get(resource.id) || {
      eager: false,
      preload: false,
      prefetch: false,
      lazy: true,
      critical: false,
    }

    // Adapt based on network quality
    if (this.networkQuality === 'slow') {
      return {
        ...baseStrategy,
        eager: baseStrategy.critical,
        preload: false,
        prefetch: false,
      }
    }

    // Adapt based on memory pressure
    if (this.memoryPressure === 'high') {
      return {
        ...baseStrategy,
        preload: false,
        prefetch: false,
      }
    }

    return baseStrategy
  }

  private async loadComponent(
    resource: Resource,
    strategy: LoadingStrategy
  ): Promise<React.ComponentType> {
    if (!resource.url) {
      throw new Error('Component resource must have a URL')
    }

    // Dynamic import with retry
    return this.withRetry(async () => {
      const module = await import(/* webpackChunkName: "[request]" */ resource.url!)
      return module.default || module
    })
  }

  private async loadData(
    resource: Resource,
    strategy: LoadingStrategy
  ): Promise<any> {
    if (!resource.url) {
      return resource.data
    }

    return this.withRetry(async () => {
      const response = await fetch(resource.url!, {
        priority: strategy.critical ? 'high' : 'low',
      } as RequestInit)

      if (!response.ok) {
        throw new Error(`Failed to load data: ${response.statusText}`)
      }

      return response.json()
    })
  }

  private async loadImage(
    resource: Resource,
    strategy: LoadingStrategy
  ): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const img = new Image()

      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error(`Failed to load image: ${resource.url}`))

      // Set loading strategy
      if (strategy.lazy && 'loading' in img) {
        img.loading = 'lazy'
      }

      if (resource.url) {
        img.src = resource.url
      }
    })
  }

  private async loadScript(
    resource: Resource,
    strategy: LoadingStrategy
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script')

      script.onload = () => resolve()
      script.onerror = () => reject(new Error(`Failed to load script: ${resource.url}`))

      script.src = resource.url!
      script.async = !strategy.critical
      script.defer = !strategy.critical && !strategy.eager

      document.head.appendChild(script)
    })
  }

  private async loadStyle(
    resource: Resource,
    strategy: LoadingStrategy
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const link = document.createElement('link')

      link.onload = () => resolve()
      link.onerror = () => reject(new Error(`Failed to load style: ${resource.url}`))

      link.rel = 'stylesheet'
      link.href = resource.url!

      if (!strategy.critical) {
        link.media = 'print'
        link.onload = () => {
          link.media = 'all'
          resolve()
        }
      }

      document.head.appendChild(link)
    })
  }

  // Utility methods
  private async withRetry<T>(
    operation: () => Promise<T>,
    maxRetries = 3,
    delay = 1000
  ): Promise<T> {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await operation()
      } catch (error) {
        if (attempt === maxRetries) {
          throw error
        }

        await new Promise(resolve => setTimeout(resolve, delay * attempt))
      }
    }

    throw new Error('Max retries exceeded')
  }

  private schedulePreload(id: string) {
    // Use requestIdleCallback for non-critical preloading
    if ('requestIdleCallback' in window) {
      requestIdleCallback(() => this.load(id))
    } else {
      setTimeout(() => this.load(id), 0)
    }
  }

  private schedulePrefetch(id: string) {
    // Delay prefetch to avoid blocking critical resources
    setTimeout(() => {
      if (this.networkQuality !== 'slow' && this.memoryPressure !== 'high') {
        this.load(id)
      }
    }, 2000)
  }

  private detectNetworkQuality() {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection

      const updateNetworkQuality = () => {
        if (connection.effectiveType === 'slow-2g' || connection.effectiveType === '2g') {
          this.networkQuality = 'slow'
        } else {
          this.networkQuality = 'fast'
        }
      }

      updateNetworkQuality()
      connection.addEventListener('change', updateNetworkQuality)
    }
  }

  private monitorMemoryPressure() {
    if ('memory' in performance) {
      const checkMemory = () => {
        const memory = (performance as any).memory
        const usedRatio = memory.usedJSHeapSize / memory.jsHeapSizeLimit

        if (usedRatio > 0.8) {
          this.memoryPressure = 'high'
        } else if (usedRatio > 0.6) {
          this.memoryPressure = 'medium'
        } else {
          this.memoryPressure = 'low'
        }
      }

      checkMemory()
      setInterval(checkMemory, 10000) // Check every 10 seconds
    }
  }

  private setupPerformanceObserver() {
    if ('PerformanceObserver' in window) {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.entryType === 'navigation') {
            this.analyzeNavigationTiming(entry as PerformanceNavigationTiming)
          } else if (entry.entryType === 'resource') {
            this.analyzeResourceTiming(entry as PerformanceResourceTiming)
          }
        }
      })

      observer.observe({ entryTypes: ['navigation', 'resource'] })
    }
  }

  private analyzeNavigationTiming(timing: PerformanceNavigationTiming) {
    const loadTime = timing.loadEventEnd - timing.navigationStart
    const domInteractive = timing.domInteractive - timing.navigationStart

    // Adjust strategies based on performance metrics
    if (loadTime > 3000) {
      // Slow page load - be more conservative
      this.reducePreloadingAggression()
    }
  }

  private analyzeResourceTiming(timing: PerformanceResourceTiming) {
    // Track resource loading performance
    const loadTime = timing.responseEnd - timing.requestStart

    if (loadTime > 1000) {
      // Slow resource - might indicate network issues
      console.warn(`Slow resource load: ${timing.name} (${loadTime}ms)`)
    }
  }

  private reducePreloadingAggression() {
    // Reduce aggressive preloading when performance is poor
    this.loadingStrategies.forEach((strategy, id) => {
      if (strategy.prefetch) {
        strategy.prefetch = false
        this.loadingStrategies.set(id, strategy)
      }
    })
  }

  // Public API
  invalidate(id: string) {
    this.cache.delete(id)
    this.loadingPromises.delete(id)
  }

  clear() {
    this.cache.clear()
    this.loadingPromises.clear()
  }

  getStats() {
    return {
      cacheSize: this.cache.size,
      loadingCount: this.loadingPromises.size,
      networkQuality: this.networkQuality,
      memoryPressure: this.memoryPressure,
    }
  }
}

// Global resource manager instance
export const resourceManager = new ResourceManager()

// React hooks for resource management
export const useResource = <T>(id: string): {
  data: T | null
  loading: boolean
  error: Error | null
} => {
  const [data, setData] = React.useState<T | null>(null)
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<Error | null>(null)

  React.useEffect(() => {
    let cancelled = false

    const loadResource = async () => {
      setLoading(true)
      setError(null)

      try {
        const result = await resourceManager.load(id)
        if (!cancelled) {
          setData(result)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err as Error)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadResource()

    return () => {
      cancelled = true
    }
  }, [id])

  return { data, loading, error }
}

export const useResourceManager = () => {
  const register = React.useCallback(
    (id: string, resource: Omit<Resource, 'id'>, strategy: LoadingStrategy) => {
      resourceManager.register(id, resource, strategy)
    },
    []
  )

  const load = React.useCallback((id: string) => {
    return resourceManager.load(id)
  }, [])

  const invalidate = React.useCallback((id: string) => {
    resourceManager.invalidate(id)
  }, [])

  const stats = resourceManager.getStats()

  return { register, load, invalidate, stats }
}
```

---

## 4. Security Patterns

### 4.1 Content Security Policy Implementation

```typescript
// Pattern: Dynamic CSP management with nonce generation
// Path: src/security/contentSecurityPolicy.ts

import crypto from 'crypto'

interface CSPDirectives {
  'default-src'?: string[]
  'script-src'?: string[]
  'style-src'?: string[]
  'img-src'?: string[]
  'connect-src'?: string[]
  'font-src'?: string[]
  'object-src'?: string[]
  'media-src'?: string[]
  'frame-src'?: string[]
  'sandbox'?: string[]
  'report-uri'?: string[]
  'child-src'?: string[]
  'form-action'?: string[]
  'frame-ancestors'?: string[]
  'plugin-types'?: string[]
  'base-uri'?: string[]
  'manifest-src'?: string[]
  'worker-src'?: string[]
  'prefetch-src'?: string[]
}

class ContentSecurityPolicyManager {
  private nonce: string = ''
  private directives: CSPDirectives = {}
  private reportEndpoint: string = '/api/v1/csp-reports'
  private enforceMode: boolean = true

  constructor() {
    this.generateNonce()
    this.initializeDefaultDirectives()
  }

  private generateNonce(): string {
    this.nonce = crypto.randomBytes(16).toString('base64')
    return this.nonce
  }

  private initializeDefaultDirectives() {
    this.directives = {
      'default-src': ["'self'"],
      'script-src': ["'self'", `'nonce-${this.nonce}'`, "'strict-dynamic'"],
      'style-src': ["'self'", `'nonce-${this.nonce}'`],
      'img-src': ["'self'", 'data:', 'https:'],
      'connect-src': ["'self'", 'https://api.stripe.com'],
      'font-src': ["'self'", 'data:'],
      'object-src': ["'none'"],
      'media-src': ["'self'"],
      'frame-src': ["'self'", 'https://js.stripe.com'],
      'base-uri': ["'self'"],
      'form-action': ["'self'"],
      'frame-ancestors': ["'none'"],
      'manifest-src': ["'self'"],
      'worker-src': ["'self'", 'blob:'],
    }

    if (!import.meta.env.PROD) {
      // Allow webpack HMR in development
      this.directives['script-src']?.push("'unsafe-eval'")
      this.directives['connect-src']?.push('ws:', 'wss:')
    }
  }

  public buildCSPHeader(): string {
    const policies = Object.entries(this.directives)
      .map(([directive, values]) => {
        return `${directive} ${values.join(' ')}`
      })
      .join('; ')

    const reportUri = `report-uri ${this.reportEndpoint}; report-to csp-endpoint`

    return `${policies}; ${reportUri}`
  }

  public getNonce(): string {
    return this.nonce
  }

  public addSource(directive: keyof CSPDirectives, source: string) {
    if (!this.directives[directive]) {
      this.directives[directive] = []
    }

    if (!this.directives[directive]!.includes(source)) {
      this.directives[directive]!.push(source)
    }
  }

  public removeSource(directive: keyof CSPDirectives, source: string) {
    if (this.directives[directive]) {
      this.directives[directive] = this.directives[directive]!.filter(
        s => s !== source
      )
    }
  }

  public setReportOnly(reportOnly: boolean) {
    this.enforceMode = !reportOnly
  }

  public injectCSPMeta() {
    const meta = document.createElement('meta')
    meta.httpEquiv = this.enforceMode
      ? 'Content-Security-Policy'
      : 'Content-Security-Policy-Report-Only'
    meta.content = this.buildCSPHeader()
    document.head.appendChild(meta)
  }

  // CSP violation reporter
  public setupViolationReporter() {
    document.addEventListener('securitypolicyviolation', (event) => {
      const violation = {
        documentURI: event.documentURI,
        violatedDirective: event.violatedDirective,
        effectiveDirective: event.effectiveDirective,
        originalPolicy: event.originalPolicy,
        blockedURI: event.blockedURI,
        sourceFile: event.sourceFile,
        lineNumber: event.lineNumber,
        columnNumber: event.columnNumber,
        sample: event.sample,
        disposition: event.disposition,
        statusCode: event.statusCode,
        timestamp: new Date().toISOString(),
      }

      this.reportViolation(violation)
    })
  }

  private async reportViolation(violation: any) {
    try {
      await fetch(this.reportEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(violation),
      })
    } catch (error) {
      console.error('Failed to report CSP violation:', error)
    }
  }
}

export const cspManager = new ContentSecurityPolicyManager()

// React hook for CSP-compliant inline styles and scripts
export const useCSPCompliant = () => {
  const nonce = cspManager.getNonce()

  const inlineStyle = React.useCallback((styles: React.CSSProperties) => {
    return {
      ...styles,
      nonce,
    }
  }, [nonce])

  const inlineScript = React.useCallback((script: string) => {
    const scriptElement = document.createElement('script')
    scriptElement.nonce = nonce
    scriptElement.textContent = script
    return scriptElement
  }, [nonce])

  return { nonce, inlineStyle, inlineScript }
}
```

### 4.2 XSS Protection and Input Sanitization

```typescript
// Pattern: Comprehensive XSS protection
// Path: src/security/xssProtection.ts

import DOMPurify from 'isomorphic-dompurify'

interface SanitizationOptions {
  allowedTags?: string[]
  allowedAttributes?: string[]
  allowedSchemes?: string[]
  transformTags?: Record<string, string>
  forbidTags?: string[]
  forbidAttr?: string[]
}

class XSSProtection {
  private defaultOptions: SanitizationOptions = {
    allowedTags: [
      'b', 'i', 'em', 'strong', 'a', 'p', 'br',
      'ul', 'ol', 'li', 'span', 'div'
    ],
    allowedAttributes: ['href', 'class', 'id', 'target'],
    allowedSchemes: ['http', 'https', 'mailto'],
    forbidTags: ['script', 'style', 'iframe', 'object', 'embed', 'form'],
    forbidAttr: ['onerror', 'onload', 'onclick', 'onmouseover'],
  }

  public sanitizeHTML(dirty: string, options?: SanitizationOptions): string {
    const config = { ...this.defaultOptions, ...options }

    const clean = DOMPurify.sanitize(dirty, {
      ALLOWED_TAGS: config.allowedTags,
      ALLOWED_ATTR: config.allowedAttributes,
      ALLOWED_URI_REGEXP: this.buildURIRegex(config.allowedSchemes || []),
      FORBID_TAGS: config.forbidTags,
      FORBID_ATTR: config.forbidAttr,
      KEEP_CONTENT: true,
      RETURN_TRUSTED_TYPE: false,
    })

    return clean as string
  }

  public sanitizeURL(url: string): string {
    try {
      const parsed = new URL(url)

      // Only allow safe protocols
      const safeProtocols = ['http:', 'https:', 'mailto:']
      if (!safeProtocols.includes(parsed.protocol)) {
        return ''
      }

      // Remove any javascript: or data: URLs
      if (url.toLowerCase().includes('javascript:') ||
          url.toLowerCase().includes('data:text/html')) {
        return ''
      }

      return url
    } catch {
      // Invalid URL
      return ''
    }
  }

  public escapeHTML(text: string): string {
    const div = document.createElement('div')
    div.textContent = text
    return div.innerHTML
  }

  public sanitizeJSON(jsonString: string): any {
    try {
      const parsed = JSON.parse(jsonString)
      return this.deepSanitize(parsed)
    } catch {
      return null
    }
  }

  private deepSanitize(obj: any): any {
    if (typeof obj === 'string') {
      return this.escapeHTML(obj)
    }

    if (Array.isArray(obj)) {
      return obj.map(item => this.deepSanitize(item))
    }

    if (typeof obj === 'object' && obj !== null) {
      const sanitized: any = {}
      for (const [key, value] of Object.entries(obj)) {
        const sanitizedKey = this.escapeHTML(key)
        sanitized[sanitizedKey] = this.deepSanitize(value)
      }
      return sanitized
    }

    return obj
  }

  private buildURIRegex(schemes: string[]): RegExp {
    const schemePattern = schemes.join('|')
    return new RegExp(`^(${schemePattern}):`, 'i')
  }

  // Validate and sanitize file uploads
  public validateFileUpload(file: File): {
    valid: boolean
    reason?: string
  } {
    const maxSize = 10 * 1024 * 1024 // 10MB
    const allowedTypes = [
      'image/jpeg',
      'image/png',
      'image/gif',
      'application/pdf',
      'text/plain',
      'text/csv',
    ]

    if (file.size > maxSize) {
      return { valid: false, reason: 'File too large' }
    }

    if (!allowedTypes.includes(file.type)) {
      return { valid: false, reason: 'File type not allowed' }
    }

    // Check file extension matches MIME type
    const extension = file.name.split('.').pop()?.toLowerCase()
    const expectedExtensions: Record<string, string[]> = {
      'image/jpeg': ['jpg', 'jpeg'],
      'image/png': ['png'],
      'image/gif': ['gif'],
      'application/pdf': ['pdf'],
      'text/plain': ['txt'],
      'text/csv': ['csv'],
    }

    const validExtensions = expectedExtensions[file.type]
    if (!validExtensions || !extension || !validExtensions.includes(extension)) {
      return { valid: false, reason: 'File extension mismatch' }
    }

    return { valid: true }
  }
}

export const xssProtection = new XSSProtection()

// React components for safe rendering
export const SafeHTML: React.FC<{
  content: string
  options?: SanitizationOptions
}> = ({ content, options }) => {
  const sanitized = xssProtection.sanitizeHTML(content, options)

  return (
    <div
      dangerouslySetInnerHTML={{ __html: sanitized }}
    />
  )
}

export const SafeLink: React.FC<{
  href: string
  children: React.ReactNode
  className?: string
  target?: '_blank' | '_self'
}> = ({ href, children, className, target = '_self' }) => {
  const sanitizedURL = xssProtection.sanitizeURL(href)

  if (!sanitizedURL) {
    return <span className={className}>{children}</span>
  }

  const rel = target === '_blank' ? 'noopener noreferrer' : undefined

  return (
    <a
      href={sanitizedURL}
      className={className}
      target={target}
      rel={rel}
    >
      {children}
    </a>
  )
}

// Form input sanitization hook
export const useSanitizedInput = () => {
  const [value, setValue] = React.useState('')
  const [sanitizedValue, setSanitizedValue] = React.useState('')

  const handleChange = React.useCallback((
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const rawValue = e.target.value
    setValue(rawValue)

    // Sanitize the input
    const cleaned = xssProtection.escapeHTML(rawValue)
    setSanitizedValue(cleaned)
  }, [])

  return {
    value,
    sanitizedValue,
    onChange: handleChange,
  }
}
```

### 4.3 Authentication Security Patterns

```typescript
// Pattern: Secure authentication with biometric support
// Path: src/security/authenticationSecurity.ts

import { PublicKeyCredentialCreationOptions } from '@simplewebauthn/typescript-types'

interface BiometricAuthOptions {
  challenge: string
  rpName: string
  rpId: string
  userName: string
  userDisplayName: string
  timeout?: number
  attestation?: 'none' | 'indirect' | 'direct'
}

class AuthenticationSecurity {
  private sessionTimeout: number = 30 * 60 * 1000 // 30 minutes
  private lastActivity: number = Date.now()
  private sessionCheckInterval?: NodeJS.Timeout
  private fingerprintId?: string

  constructor() {
    this.initializeSessionMonitoring()
    this.generateBrowserFingerprint()
  }

  // Session security
  private initializeSessionMonitoring() {
    // Monitor user activity
    const updateActivity = () => {
      this.lastActivity = Date.now()
    }

    window.addEventListener('mousemove', updateActivity)
    window.addEventListener('keydown', updateActivity)
    window.addEventListener('click', updateActivity)
    window.addEventListener('scroll', updateActivity)

    // Check session timeout
    this.sessionCheckInterval = setInterval(() => {
      if (Date.now() - this.lastActivity > this.sessionTimeout) {
        this.handleSessionTimeout()
      }
    }, 60000) // Check every minute
  }

  private handleSessionTimeout() {
    // Clear sensitive data
    this.clearSensitiveData()

    // Emit timeout event
    window.dispatchEvent(new CustomEvent('session-timeout'))

    // Redirect to login
    window.location.href = '/auth/login?reason=timeout'
  }

  private clearSensitiveData() {
    // Clear storage
    sessionStorage.clear()

    // Clear sensitive cookies
    document.cookie.split(";").forEach((c) => {
      document.cookie = c
        .replace(/^ +/, "")
        .replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/")
    })

    // Clear Redux store
    if (window.__REDUX_STORE__) {
      window.__REDUX_STORE__.dispatch({ type: 'CLEAR_SENSITIVE_DATA' })
    }
  }

  // Browser fingerprinting for additional security
  private async generateBrowserFingerprint() {
    const fingerprint = await this.collectFingerprint()
    this.fingerprintId = await this.hashFingerprint(fingerprint)
  }

  private async collectFingerprint(): Promise<any> {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')

    if (ctx) {
      ctx.textBaseline = 'top'
      ctx.font = '14px Arial'
      ctx.textAlign = 'alphabetic'
      ctx.fillStyle = '#f60'
      ctx.fillRect(125, 1, 62, 20)
      ctx.fillStyle = '#069'
      ctx.fillText('Browser Fingerprint', 2, 15)
      ctx.fillStyle = 'rgba(102, 204, 0, 0.7)'
      ctx.fillText('Browser Fingerprint', 4, 17)
    }

    const canvasData = canvas.toDataURL()

    return {
      userAgent: navigator.userAgent,
      language: navigator.language,
      colorDepth: screen.colorDepth,
      deviceMemory: (navigator as any).deviceMemory,
      hardwareConcurrency: navigator.hardwareConcurrency,
      screenResolution: `${screen.width}x${screen.height}`,
      availableScreenResolution: `${screen.availWidth}x${screen.availHeight}`,
      timezoneOffset: new Date().getTimezoneOffset(),
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      sessionStorage: typeof sessionStorage !== 'undefined',
      localStorage: typeof localStorage !== 'undefined',
      indexedDb: !!window.indexedDB,
      addBehavior: !!(document.body as any).addBehavior,
      openDatabase: !!window.openDatabase,
      cpuClass: (navigator as any).cpuClass,
      platform: navigator.platform,
      plugins: Array.from(navigator.plugins || []).map(p => ({
        name: p.name,
        description: p.description,
      })),
      canvas: canvasData,
      webgl: this.getWebGLFingerprint(),
      fonts: await this.getFontFingerprint(),
    }
  }

  private getWebGLFingerprint(): any {
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl')

    if (!gl) return null

    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info')

    return {
      vendor: gl.getParameter(gl.VENDOR),
      renderer: gl.getParameter(gl.RENDERER),
      vendorUnmasked: debugInfo ?
        gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) : null,
      rendererUnmasked: debugInfo ?
        gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) : null,
    }
  }

  private async getFontFingerprint(): Promise<string[]> {
    const testFonts = [
      'Arial', 'Verdana', 'Times New Roman', 'Courier New',
      'Georgia', 'Palatino', 'Garamond', 'Bookman',
      'Comic Sans MS', 'Trebuchet MS', 'Impact'
    ]

    const baseFonts = ['monospace', 'sans-serif', 'serif']
    const testString = 'mmmmmmmmmmlli'
    const testSize = '72px'

    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')

    if (!context) return []

    const detectFont = (font: string): boolean => {
      let detected = false

      for (const baseFont of baseFonts) {
        context.font = `${testSize} ${baseFont}`
        const baseWidth = context.measureText(testString).width

        context.font = `${testSize} ${font}, ${baseFont}`
        const testWidth = context.measureText(testString).width

        if (testWidth !== baseWidth) {
          detected = true
          break
        }
      }

      return detected
    }

    return testFonts.filter(font => detectFont(font))
  }

  private async hashFingerprint(fingerprint: any): Promise<string> {
    const json = JSON.stringify(fingerprint)
    const msgBuffer = new TextEncoder().encode(json)
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
  }

  // WebAuthn/Biometric authentication
  public async setupBiometricAuth(options: BiometricAuthOptions) {
    if (!window.PublicKeyCredential) {
      throw new Error('WebAuthn not supported')
    }

    const publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptions = {
      challenge: Uint8Array.from(options.challenge, c => c.charCodeAt(0)),
      rp: {
        name: options.rpName,
        id: options.rpId,
      },
      user: {
        id: Uint8Array.from(options.userName, c => c.charCodeAt(0)),
        name: options.userName,
        displayName: options.userDisplayName,
      },
      pubKeyCredParams: [
        { alg: -7, type: 'public-key' },  // ES256
        { alg: -257, type: 'public-key' }, // RS256
      ],
      authenticatorSelection: {
        authenticatorAttachment: 'platform',
        userVerification: 'required',
      },
      timeout: options.timeout || 60000,
      attestation: options.attestation || 'direct',
    }

    try {
      const credential = await navigator.credentials.create({
        publicKey: publicKeyCredentialCreationOptions,
      })

      return credential
    } catch (error) {
      console.error('Biometric setup failed:', error)
      throw error
    }
  }

  public async authenticateWithBiometric(credentialId: string, challenge: string) {
    if (!window.PublicKeyCredential) {
      throw new Error('WebAuthn not supported')
    }

    const publicKeyCredentialRequestOptions = {
      challenge: Uint8Array.from(challenge, c => c.charCodeAt(0)),
      allowCredentials: [{
        id: Uint8Array.from(credentialId, c => c.charCodeAt(0)),
        type: 'public-key' as const,
        transports: ['internal'] as AuthenticatorTransport[],
      }],
      userVerification: 'required' as const,
      timeout: 60000,
    }

    try {
      const assertion = await navigator.credentials.get({
        publicKey: publicKeyCredentialRequestOptions,
      })

      return assertion
    } catch (error) {
      console.error('Biometric authentication failed:', error)
      throw error
    }
  }

  // Secure token handling
  public secureTokenStorage = {
    store: (token: string, expiresIn: number) => {
      // Never store tokens in localStorage
      // Use secure, httpOnly cookies or session storage with encryption

      const encrypted = this.encryptData(token)
      const expiry = Date.now() + expiresIn

      sessionStorage.setItem('auth_token', JSON.stringify({
        data: encrypted,
        expiry,
      }))
    },

    retrieve: (): string | null => {
      const stored = sessionStorage.getItem('auth_token')

      if (!stored) return null

      try {
        const { data, expiry } = JSON.parse(stored)

        if (Date.now() > expiry) {
          sessionStorage.removeItem('auth_token')
          return null
        }

        return this.decryptData(data)
      } catch {
        return null
      }
    },

    clear: () => {
      sessionStorage.removeItem('auth_token')
    },
  }

  private encryptData(data: string): string {
    // Simple XOR encryption for demo - use proper encryption in production
    const key = this.fingerprintId || 'default-key'
    let encrypted = ''

    for (let i = 0; i < data.length; i++) {
      encrypted += String.fromCharCode(
        data.charCodeAt(i) ^ key.charCodeAt(i % key.length)
      )
    }

    return btoa(encrypted)
  }

  private decryptData(encrypted: string): string {
    const key = this.fingerprintId || 'default-key'
    const data = atob(encrypted)
    let decrypted = ''

    for (let i = 0; i < data.length; i++) {
      decrypted += String.fromCharCode(
        data.charCodeAt(i) ^ key.charCodeAt(i % key.length)
      )
    }

    return decrypted
  }

  // Cleanup
  public destroy() {
    if (this.sessionCheckInterval) {
      clearInterval(this.sessionCheckInterval)
    }

    this.clearSensitiveData()
  }
}

export const authSecurity = new AuthenticationSecurity()

// React hooks for secure authentication
export const useSecureAuth = () => {
  const [biometricAvailable, setBiometricAvailable] = React.useState(false)

  React.useEffect(() => {
    setBiometricAvailable(!!window.PublicKeyCredential)

    const handleTimeout = () => {
      // Handle session timeout in React
      window.location.href = '/auth/login?reason=timeout'
    }

    window.addEventListener('session-timeout', handleTimeout)

    return () => {
      window.removeEventListener('session-timeout', handleTimeout)
    }
  }, [])

  const setupBiometric = React.useCallback(async (options: BiometricAuthOptions) => {
    return authSecurity.setupBiometricAuth(options)
  }, [])

  const authenticateWithBiometric = React.useCallback(async (
    credentialId: string,
    challenge: string
  ) => {
    return authSecurity.authenticateWithBiometric(credentialId, challenge)
  }, [])

  return {
    biometricAvailable,
    setupBiometric,
    authenticateWithBiometric,
    secureTokenStorage: authSecurity.secureTokenStorage,
  }
}
```

---

## 5. Accessibility Patterns

### 5.1 WCAG 2.1 Compliance Framework

```typescript
// Pattern: Comprehensive accessibility management
// Path: src/accessibility/wcagCompliance.ts

interface AccessibilityConfig {
  level: 'A' | 'AA' | 'AAA'
  features: {
    keyboardNavigation: boolean
    screenReaderSupport: boolean
    colorContrastCheck: boolean
    focusManagement: boolean
    ariaCompliance: boolean
    skipLinks: boolean
  }
}

class WCAGComplianceManager {
  private config: AccessibilityConfig = {
    level: 'AA',
    features: {
      keyboardNavigation: true,
      screenReaderSupport: true,
      colorContrastCheck: true,
      focusManagement: true,
      ariaCompliance: true,
      skipLinks: true,
    }
  }

  private focusTrapStack: HTMLElement[] = []
  private announcer?: HTMLElement

  constructor() {
    this.initializeA11y()
  }

  private initializeA11y() {
    this.setupAnnouncer()
    this.setupSkipLinks()
    this.setupKeyboardNavigation()
    this.setupFocusManagement()
    this.setupColorContrastMonitoring()
  }

  // Screen reader announcements
  private setupAnnouncer() {
    this.announcer = document.createElement('div')
    this.announcer.className = 'sr-only'
    this.announcer.setAttribute('role', 'status')
    this.announcer.setAttribute('aria-live', 'polite')
    this.announcer.setAttribute('aria-atomic', 'true')
    document.body.appendChild(this.announcer)
  }

  public announce(message: string, priority: 'polite' | 'assertive' = 'polite') {
    if (!this.announcer) return

    this.announcer.setAttribute('aria-live', priority)
    this.announcer.textContent = message

    // Clear after announcement
    setTimeout(() => {
      if (this.announcer) {
        this.announcer.textContent = ''
      }
    }, 1000)
  }

  // Skip links for keyboard navigation
  private setupSkipLinks() {
    const skipLink = document.createElement('a')
    skipLink.href = '#main-content'
    skipLink.className = 'skip-link'
    skipLink.textContent = 'Skip to main content'

    skipLink.addEventListener('click', (e) => {
      e.preventDefault()
      const main = document.getElementById('main-content')
      if (main) {
        main.tabIndex = -1
        main.focus()
      }
    })

    document.body.insertBefore(skipLink, document.body.firstChild)
  }

  // Enhanced keyboard navigation
  private setupKeyboardNavigation() {
    document.addEventListener('keydown', (e) => {
      // Escape key closes modals
      if (e.key === 'Escape') {
        this.handleEscapeKey()
      }

      // Tab trapping in modals
      if (e.key === 'Tab' && this.focusTrapStack.length > 0) {
        this.handleTabKey(e)
      }

      // Arrow key navigation for menus
      if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
        this.handleArrowKeys(e)
      }
    })
  }

  private handleEscapeKey() {
    if (this.focusTrapStack.length > 0) {
      const trap = this.focusTrapStack.pop()
      if (trap) {
        const closeButton = trap.querySelector('[data-close-button]')
        if (closeButton instanceof HTMLElement) {
          closeButton.click()
        }
      }
    }
  }

  private handleTabKey(e: KeyboardEvent) {
    const trap = this.focusTrapStack[this.focusTrapStack.length - 1]
    if (!trap) return

    const focusableElements = trap.querySelectorAll(
      'a[href], button:not([disabled]), textarea:not([disabled]), ' +
      'input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )

    const focusableArray = Array.from(focusableElements) as HTMLElement[]

    if (focusableArray.length === 0) return

    const firstElement = focusableArray[0]
    const lastElement = focusableArray[focusableArray.length - 1]

    if (e.shiftKey && document.activeElement === firstElement) {
      e.preventDefault()
      lastElement.focus()
    } else if (!e.shiftKey && document.activeElement === lastElement) {
      e.preventDefault()
      firstElement.focus()
    }
  }

  private handleArrowKeys(e: KeyboardEvent) {
    const activeElement = document.activeElement

    if (!activeElement) return

    // Check if we're in a menu or list
    const menu = activeElement.closest('[role="menu"], [role="listbox"]')

    if (!menu) return

    const items = Array.from(
      menu.querySelectorAll('[role="menuitem"], [role="option"]')
    ) as HTMLElement[]

    const currentIndex = items.indexOf(activeElement as HTMLElement)

    if (currentIndex === -1) return

    let nextIndex = currentIndex

    switch (e.key) {
      case 'ArrowUp':
        nextIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1
        break
      case 'ArrowDown':
        nextIndex = currentIndex < items.length - 1 ? currentIndex + 1 : 0
        break
    }

    if (nextIndex !== currentIndex) {
      e.preventDefault()
      items[nextIndex].focus()
    }
  }

  // Focus management
  private setupFocusManagement() {
    // Save and restore focus when modals open/close
    let previousFocus: Element | null = null

    document.addEventListener('modal-open', (e: CustomEvent) => {
      previousFocus = document.activeElement

      const modal = e.detail.modal
      if (modal) {
        this.trapFocus(modal)

        // Focus first focusable element or close button
        const firstFocusable = modal.querySelector(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        )

        if (firstFocusable instanceof HTMLElement) {
          firstFocusable.focus()
        }
      }
    })

    document.addEventListener('modal-close', () => {
      this.releaseFocusTrap()

      if (previousFocus instanceof HTMLElement) {
        previousFocus.focus()
      }
    })
  }

  public trapFocus(element: HTMLElement) {
    this.focusTrapStack.push(element)
  }

  public releaseFocusTrap() {
    this.focusTrapStack.pop()
  }

  // Color contrast checking
  private setupColorContrastMonitoring() {
    if (!this.config.features.colorContrastCheck) return

    // Check contrast ratios periodically
    setInterval(() => {
      this.checkColorContrast()
    }, 30000) // Every 30 seconds in dev mode
  }

  private checkColorContrast() {
    const elements = document.querySelectorAll('*')
    const issues: Array<{
      element: Element
      foreground: string
      background: string
      ratio: number
      required: number
    }> = []

    elements.forEach(element => {
      const style = window.getComputedStyle(element)
      const color = style.color
      const backgroundColor = this.getEffectiveBackgroundColor(element)

      if (color && backgroundColor) {
        const ratio = this.calculateContrastRatio(color, backgroundColor)
        const required = this.getRequiredContrastRatio(element, style)

        if (ratio < required) {
          issues.push({
            element,
            foreground: color,
            background: backgroundColor,
            ratio,
            required,
          })
        }
      }
    })

    if (issues.length > 0 && import.meta.env.DEV) {
      console.warn('Color contrast issues detected:', issues)
    }
  }

  private getEffectiveBackgroundColor(element: Element): string {
    let current: Element | null = element

    while (current) {
      const style = window.getComputedStyle(current)
      const backgroundColor = style.backgroundColor

      if (backgroundColor && backgroundColor !== 'rgba(0, 0, 0, 0)') {
        return backgroundColor
      }

      current = current.parentElement
    }

    return 'rgb(255, 255, 255)' // Default to white
  }

  private calculateContrastRatio(color1: string, color2: string): number {
    const rgb1 = this.parseColor(color1)
    const rgb2 = this.parseColor(color2)

    const l1 = this.relativeLuminance(rgb1)
    const l2 = this.relativeLuminance(rgb2)

    const lighter = Math.max(l1, l2)
    const darker = Math.min(l1, l2)

    return (lighter + 0.05) / (darker + 0.05)
  }

  private parseColor(color: string): [number, number, number] {
    const match = color.match(/\d+/g)

    if (!match || match.length < 3) {
      return [0, 0, 0]
    }

    return [
      parseInt(match[0], 10),
      parseInt(match[1], 10),
      parseInt(match[2], 10),
    ]
  }

  private relativeLuminance([r, g, b]: [number, number, number]): number {
    const [rs, gs, bs] = [r, g, b].map(c => {
      c = c / 255
      return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
    })

    return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
  }

  private getRequiredContrastRatio(element: Element, style: CSSStyleDeclaration): number {
    const fontSize = parseFloat(style.fontSize)
    const fontWeight = style.fontWeight
    const isLargeText = fontSize >= 18 || (fontSize >= 14 && fontWeight === 'bold')

    switch (this.config.level) {
      case 'AAA':
        return isLargeText ? 4.5 : 7
      case 'AA':
        return isLargeText ? 3 : 4.5
      case 'A':
      default:
        return 3
    }
  }

  // ARIA compliance checking
  public validateARIA(element: HTMLElement): string[] {
    const errors: string[] = []

    // Check for proper ARIA roles
    const role = element.getAttribute('role')
    if (role && !this.isValidARIARole(role)) {
      errors.push(`Invalid ARIA role: ${role}`)
    }

    // Check for required ARIA properties
    if (role === 'button' && !element.hasAttribute('tabindex')) {
      errors.push('Button role requires tabindex')
    }

    // Check for proper labeling
    const labelledBy = element.getAttribute('aria-labelledby')
    const label = element.getAttribute('aria-label')

    if (!labelledBy && !label) {
      const tagName = element.tagName.toLowerCase()

      if (['img', 'button', 'input', 'select', 'textarea'].includes(tagName)) {
        errors.push(`${tagName} element requires aria-label or aria-labelledby`)
      }
    }

    return errors
  }

  private isValidARIARole(role: string): boolean {
    const validRoles = [
      'alert', 'button', 'checkbox', 'dialog', 'img', 'link',
      'list', 'listbox', 'listitem', 'menu', 'menuitem', 'navigation',
      'option', 'progressbar', 'radio', 'region', 'tab', 'tablist',
      'tabpanel', 'textbox', 'toolbar', 'tree', 'treeitem'
    ]

    return validRoles.includes(role)
  }
}

export const wcagManager = new WCAGComplianceManager()

// React hooks for accessibility
export const useAccessibility = () => {
  const announce = React.useCallback((message: string, priority?: 'polite' | 'assertive') => {
    wcagManager.announce(message, priority)
  }, [])

  const trapFocus = React.useCallback((element: HTMLElement) => {
    wcagManager.trapFocus(element)
  }, [])

  const releaseFocusTrap = React.useCallback(() => {
    wcagManager.releaseFocusTrap()
  }, [])

  return {
    announce,
    trapFocus,
    releaseFocusTrap,
  }
}

// Accessible React components
export const AccessibleButton: React.FC<{
  onClick: () => void
  children: React.ReactNode
  ariaLabel?: string
  disabled?: boolean
  className?: string
}> = ({ onClick, children, ariaLabel, disabled = false, className }) => {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={className}
      aria-label={ariaLabel}
      aria-disabled={disabled}
    >
      {children}
    </button>
  )
}

export const AccessibleModal: React.FC<{
  isOpen: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
}> = ({ isOpen, onClose, title, children }) => {
  const modalRef = React.useRef<HTMLDivElement>(null)
  const { trapFocus, releaseFocusTrap } = useAccessibility()

  React.useEffect(() => {
    if (isOpen && modalRef.current) {
      trapFocus(modalRef.current)

      // Dispatch custom event
      const event = new CustomEvent('modal-open', {
        detail: { modal: modalRef.current }
      })
      document.dispatchEvent(event)
    }

    return () => {
      if (isOpen) {
        releaseFocusTrap()
        document.dispatchEvent(new CustomEvent('modal-close'))
      }
    }
  }, [isOpen, trapFocus, releaseFocusTrap])

  if (!isOpen) return null

  return (
    <div
      ref={modalRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      className="modal"
    >
      <div className="modal-content">
        <h2 id="modal-title">{title}</h2>
        <button
          onClick={onClose}
          aria-label="Close modal"
          data-close-button
        >
          ×
        </button>
        {children}
      </div>
    </div>
  )
}
```

---

## 6. Testing Strategies

### 6.1 Advanced Testing Patterns

```typescript
// Pattern: Comprehensive testing utilities
// Path: src/testing/advancedTestingPatterns.ts

import { render, RenderOptions, RenderResult } from '@testing-library/react'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import { configureStore } from '@reduxjs/toolkit'
import userEvent from '@testing-library/user-event'
import { rest } from 'msw'
import { setupServer } from 'msw/node'

// Advanced test utilities
export class TestUtils {
  private static server: ReturnType<typeof setupServer> | null = null

  // Setup MSW server
  static setupMockServer(handlers: any[] = []) {
    this.server = setupServer(...handlers)

    beforeAll(() => this.server?.listen({ onUnhandledRequest: 'error' }))
    afterEach(() => this.server?.resetHandlers())
    afterAll(() => this.server?.close())

    return this.server
  }

  // Custom render with providers
  static renderWithProviders(
    ui: React.ReactElement,
    {
      preloadedState = {},
      store = configureStore({
        reducer: {},
        preloadedState
      }),
      route = '/',
      ...renderOptions
    }: ExtendedRenderOptions = {}
  ): ExtendedRenderResult {
    const user = userEvent.setup()

    function Wrapper({ children }: { children: React.ReactNode }) {
      return (
        <Provider store={store}>
          <MemoryRouter initialEntries={[route]}>
            {children}
          </MemoryRouter>
        </Provider>
      )
    }

    return {
      user,
      store,
      ...render(ui, { wrapper: Wrapper, ...renderOptions }),
    }
  }

  // Performance testing
  static async measurePerformance(
    component: React.ComponentType,
    iterations = 10
  ): Promise<PerformanceMetrics> {
    const measurements: number[] = []

    for (let i = 0; i < iterations; i++) {
      const start = performance.now()
      const { unmount } = render(<component />)
      const end = performance.now()

      measurements.push(end - start)
      unmount()
    }

    return {
      average: measurements.reduce((a, b) => a + b) / measurements.length,
      min: Math.min(...measurements),
      max: Math.max(...measurements),
      median: this.median(measurements),
    }
  }

  private static median(values: number[]): number {
    const sorted = [...values].sort((a, b) => a - b)
    const middle = Math.floor(sorted.length / 2)

    if (sorted.length % 2 === 0) {
      return (sorted[middle - 1] + sorted[middle]) / 2
    }

    return sorted[middle]
  }

  // Accessibility testing
  static async checkAccessibility(element: HTMLElement): Promise<A11yViolation[]> {
    const violations: A11yViolation[] = []

    // Check for alt text on images
    const images = element.querySelectorAll('img')
    images.forEach(img => {
      if (!img.alt && !img.getAttribute('aria-label')) {
        violations.push({
          element: img,
          rule: 'img-alt',
          message: 'Image must have alt text',
        })
      }
    })

    // Check for button labels
    const buttons = element.querySelectorAll('button')
    buttons.forEach(button => {
      if (!button.textContent?.trim() && !button.getAttribute('aria-label')) {
        violations.push({
          element: button,
          rule: 'button-name',
          message: 'Button must have accessible name',
        })
      }
    })

    // Check for form labels
    const inputs = element.querySelectorAll('input, select, textarea')
    inputs.forEach(input => {
      const id = input.id
      const label = id ? element.querySelector(`label[for="${id}"]`) : null

      if (!label && !input.getAttribute('aria-label')) {
        violations.push({
          element: input,
          rule: 'label',
          message: 'Form element must have label',
        })
      }
    })

    return violations
  }

  // Visual regression testing
  static async captureSnapshot(
    element: HTMLElement,
    name: string
  ): Promise<string> {
    // This would integrate with a visual testing service
    // For demo, we'll create a simple DOM snapshot
    return element.outerHTML
  }

  // Interaction testing helpers
  static async waitForLoadingToFinish(
    container: HTMLElement,
    options = { timeout: 3000 }
  ): Promise<void> {
    return waitFor(
      () => {
        const loadingElements = container.querySelectorAll(
          '[data-testid="loading"], .loading, .spinner'
        )
        expect(loadingElements.length).toBe(0)
      },
      options
    )
  }

  // Mock data generators
  static generateMockUser(overrides = {}): User {
    return {
      id: faker.datatype.uuid(),
      email: faker.internet.email(),
      firstName: faker.name.firstName(),
      lastName: faker.name.lastName(),
      role: 'USER',
      createdAt: faker.date.past().toISOString(),
      ...overrides,
    }
  }

  static generateMockOrganization(overrides = {}): Organization {
    return {
      id: faker.datatype.uuid(),
      name: faker.company.name(),
      plan: 'PRO',
      status: 'ACTIVE',
      createdAt: faker.date.past().toISOString(),
      ...overrides,
    }
  }
}

// Custom test matchers
expect.extend({
  toBeAccessible(element: HTMLElement) {
    const violations = TestUtils.checkAccessibility(element)

    return {
      pass: violations.length === 0,
      message: () => {
        if (violations.length > 0) {
          const messages = violations.map(v =>
            `${v.rule}: ${v.message}`
          ).join('\n')

          return `Element has accessibility violations:\n${messages}`
        }

        return 'Element is accessible'
      },
    }
  },

  toHaveNoMemoryLeaks(fn: Function) {
    const initialMemory = performance.memory?.usedJSHeapSize || 0

    // Run function multiple times
    for (let i = 0; i < 100; i++) {
      fn()
    }

    // Force garbage collection if available
    if (global.gc) {
      global.gc()
    }

    const finalMemory = performance.memory?.usedJSHeapSize || 0
    const memoryIncrease = finalMemory - initialMemory
    const threshold = 1024 * 1024 // 1MB

    return {
      pass: memoryIncrease < threshold,
      message: () => {
        if (memoryIncrease >= threshold) {
          return `Function may have memory leak. Memory increased by ${
            (memoryIncrease / 1024 / 1024).toFixed(2)
          }MB`
        }

        return 'No memory leaks detected'
      },
    }
  },
})

// Types
interface ExtendedRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  preloadedState?: any
  store?: any
  route?: string
}

interface ExtendedRenderResult extends RenderResult {
  user: ReturnType<typeof userEvent.setup>
  store: any
}

interface PerformanceMetrics {
  average: number
  min: number
  max: number
  median: number
}

interface A11yViolation {
  element: Element
  rule: string
  message: string
}
```

---

## 7. Monitoring & Observability

### 7.1 Performance Monitoring

```typescript
// Pattern: Comprehensive performance monitoring
// Path: src/monitoring/performanceMonitor.ts

interface PerformanceMetric {
  name: string
  value: number
  unit: string
  timestamp: number
  tags?: Record<string, string>
}

class PerformanceMonitor {
  private metrics: PerformanceMetric[] = []
  private observers: Map<string, PerformanceObserver> = new Map()
  private reportingEndpoint = '/api/v1/metrics'
  private reportingInterval = 30000 // 30 seconds
  private batchSize = 100

  constructor() {
    this.initializeObservers()
    this.startReportingCycle()
  }

  private initializeObservers() {
    // Navigation timing
    this.observeNavigationTiming()

    // Resource timing
    this.observeResourceTiming()

    // Long tasks
    this.observeLongTasks()

    // First input delay
    this.observeFirstInputDelay()

    // Cumulative layout shift
    this.observeLayoutShift()

    // Custom metrics
    this.observeCustomMetrics()
  }

  private observeNavigationTiming() {
    if (!('PerformanceObserver' in window)) return

    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'navigation') {
          const nav = entry as PerformanceNavigationTiming

          this.recordMetric({
            name: 'page_load_time',
            value: nav.loadEventEnd - nav.navigationStart,
            unit: 'ms',
            timestamp: Date.now(),
            tags: {
              type: 'navigation',
            },
          })

          this.recordMetric({
            name: 'dom_interactive',
            value: nav.domInteractive - nav.navigationStart,
            unit: 'ms',
            timestamp: Date.now(),
            tags: {
              type: 'navigation',
            },
          })

          this.recordMetric({
            name: 'time_to_first_byte',
            value: nav.responseStart - nav.navigationStart,
            unit: 'ms',
            timestamp: Date.now(),
            tags: {
              type: 'navigation',
            },
          })
        }
      }
    })

    observer.observe({ entryTypes: ['navigation'] })
    this.observers.set('navigation', observer)
  }

  private observeResourceTiming() {
    if (!('PerformanceObserver' in window)) return

    const observer = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (entry.entryType === 'resource') {
          const resource = entry as PerformanceResourceTiming

          // Only track slow resources
          const duration = resource.responseEnd - resource.startTime

          if (duration > 1000) {
            this.recordMetric({
              name: 'slow_resource',
              value: duration,
              unit: 'ms',
              timestamp: Date.now(),
              tags: {
                url: resource.name,
                type: resource.initiatorType,
              },
            })
          }
        }
      }
    })

    observer.observe({ entryTypes: ['resource'] })
    this.observers.set('resource', observer)
  }

  private observeLongTasks() {
    if (!('PerformanceObserver' in window)) return

    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          this.recordMetric({
            name: 'long_task',
            value: entry.duration,
            unit: 'ms',
            timestamp: Date.now(),
            tags: {
              name: entry.name,
            },
          })
        }
      })

      observer.observe({ entryTypes: ['longtask'] })
      this.observers.set('longtask', observer)
    } catch (e) {
      // Long task observer not supported
    }
  }

  private observeFirstInputDelay() {
    if (!('PerformanceObserver' in window)) return

    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.entryType === 'first-input') {
            const fid = entry as any

            this.recordMetric({
              name: 'first_input_delay',
              value: fid.processingStart - fid.startTime,
              unit: 'ms',
              timestamp: Date.now(),
              tags: {
                eventType: fid.name,
              },
            })
          }
        }
      })

      observer.observe({ entryTypes: ['first-input'] })
      this.observers.set('first-input', observer)
    } catch (e) {
      // FID observer not supported
    }
  }

  private observeLayoutShift() {
    if (!('PerformanceObserver' in window)) return

    let cumulativeLayoutShift = 0

    try {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.entryType === 'layout-shift') {
            const shift = entry as any

            if (!shift.hadRecentInput) {
              cumulativeLayoutShift += shift.value

              this.recordMetric({
                name: 'cumulative_layout_shift',
                value: cumulativeLayoutShift,
                unit: 'score',
                timestamp: Date.now(),
              })
            }
          }
        }
      })

      observer.observe({ entryTypes: ['layout-shift'] })
      this.observers.set('layout-shift', observer)
    } catch (e) {
      // CLS observer not supported
    }
  }

  private observeCustomMetrics() {
    // React component render times
    if (window.__REACT_DEVTOOLS_GLOBAL_HOOK__) {
      const originalCommitFiberRoot =
        window.__REACT_DEVTOOLS_GLOBAL_HOOK__.onCommitFiberRoot

      window.__REACT_DEVTOOLS_GLOBAL_HOOK__.onCommitFiberRoot = (
        id: any,
        root: any,
        priorityLevel: any
      ) => {
        // Measure React render performance
        const renderTime = root?.current?.actualDuration

        if (renderTime && renderTime > 16) { // More than one frame
          this.recordMetric({
            name: 'react_render_time',
            value: renderTime,
            unit: 'ms',
            timestamp: Date.now(),
            tags: {
              component: root?.current?.elementType?.name || 'Unknown',
            },
          })
        }

        if (originalCommitFiberRoot) {
          originalCommitFiberRoot(id, root, priorityLevel)
        }
      }
    }
  }

  public recordMetric(metric: PerformanceMetric) {
    this.metrics.push(metric)

    // Batch reporting
    if (this.metrics.length >= this.batchSize) {
      this.reportMetrics()
    }
  }

  private startReportingCycle() {
    setInterval(() => {
      if (this.metrics.length > 0) {
        this.reportMetrics()
      }
    }, this.reportingInterval)
  }

  private async reportMetrics() {
    const metricsToReport = [...this.metrics]
    this.metrics = []

    try {
      await fetch(this.reportingEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          metrics: metricsToReport,
          timestamp: Date.now(),
          page: window.location.pathname,
          userAgent: navigator.userAgent,
        }),
      })
    } catch (error) {
      console.error('Failed to report metrics:', error)

      // Re-add metrics for retry
      this.metrics = [...metricsToReport, ...this.metrics]
    }
  }

  public getMetricsSummary() {
    const summary: Record<string, any> = {}

    this.metrics.forEach(metric => {
      if (!summary[metric.name]) {
        summary[metric.name] = {
          count: 0,
          total: 0,
          min: Infinity,
          max: -Infinity,
          values: [],
        }
      }

      const stat = summary[metric.name]
      stat.count++
      stat.total += metric.value
      stat.min = Math.min(stat.min, metric.value)
      stat.max = Math.max(stat.max, metric.value)
      stat.values.push(metric.value)
    })

    // Calculate percentiles
    Object.keys(summary).forEach(key => {
      const stat = summary[key]
      stat.average = stat.total / stat.count
      stat.p50 = this.percentile(stat.values, 0.5)
      stat.p95 = this.percentile(stat.values, 0.95)
      stat.p99 = this.percentile(stat.values, 0.99)
      delete stat.values // Remove raw values to save memory
    })

    return summary
  }

  private percentile(values: number[], p: number): number {
    const sorted = [...values].sort((a, b) => a - b)
    const index = Math.ceil(sorted.length * p) - 1
    return sorted[Math.max(0, index)]
  }

  public destroy() {
    this.observers.forEach(observer => observer.disconnect())
    this.observers.clear()

    if (this.metrics.length > 0) {
      this.reportMetrics()
    }
  }
}

export const performanceMonitor = new PerformanceMonitor()

// React hook for performance monitoring
export const usePerformanceMonitoring = (componentName: string) => {
  const renderStartTime = React.useRef<number>(0)

  React.useEffect(() => {
    renderStartTime.current = performance.now()

    return () => {
      const renderEndTime = performance.now()
      const renderTime = renderEndTime - renderStartTime.current

      performanceMonitor.recordMetric({
        name: 'component_render',
        value: renderTime,
        unit: 'ms',
        timestamp: Date.now(),
        tags: {
          component: componentName,
        },
      })
    }
  }, [componentName])

  const measureInteraction = React.useCallback((
    interactionName: string,
    fn: () => void | Promise<void>
  ) => {
    const startTime = performance.now()

    const result = fn()

    if (result instanceof Promise) {
      result.finally(() => {
        const duration = performance.now() - startTime

        performanceMonitor.recordMetric({
          name: 'user_interaction',
          value: duration,
          unit: 'ms',
          timestamp: Date.now(),
          tags: {
            interaction: interactionName,
            component: componentName,
          },
        })
      })
    } else {
      const duration = performance.now() - startTime

      performanceMonitor.recordMetric({
        name: 'user_interaction',
        value: duration,
        unit: 'ms',
        timestamp: Date.now(),
        tags: {
          interaction: interactionName,
          component: componentName,
        },
      })
    }

    return result
  }, [componentName])

  return { measureInteraction }
}
```

This completes the comprehensive Frontend Advanced Patterns documentation with:

1. **Advanced State Management** - Event-driven and offline-first patterns
2. **Micro-Frontend Architecture** - Module federation and shell application
3. **Advanced Performance Patterns** - Intelligent resource management
4. **Security Patterns** - CSP, XSS protection, and authentication security
5. **Accessibility Patterns** - WCAG 2.1 compliance framework
6. **Testing Strategies** - Advanced testing utilities and patterns
7. **Monitoring & Observability** - Performance monitoring and metrics

These advanced patterns provide production-ready solutions for complex frontend challenges in the payment platform.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "Create implementation templates and code patterns", "status": "completed", "activeForm": "Creating implementation templates and code patterns"}, {"content": "Document advanced patterns and best practices", "status": "completed", "activeForm": "Documenting advanced patterns and best practices"}, {"content": "Create testing strategies and examples", "status": "in_progress", "activeForm": "Creating testing strategies and examples"}, {"content": "Generate troubleshooting and debugging guide", "status": "pending", "activeForm": "Generating troubleshooting and debugging guide"}]