import { EventEmitter } from 'events'

export interface WebSocketMessage {
  type: string
  payload: any
  timestamp: number
  userId?: string
  organizationId?: string
  correlationId?: string
}

export interface ConnectionConfig {
  url: string
  token?: string
  reconnectInterval?: number
  maxReconnectAttempts?: number
  heartbeatInterval?: number
}

export interface PresenceData {
  userId: string
  userName: string
  avatar?: string
  status: 'online' | 'away' | 'busy' | 'offline'
  lastSeen: number
  currentPage?: string
  cursor?: { x: number; y: number }
}

export class WebSocketService extends EventEmitter {
  private socket: WebSocket | null = null
  private config: ConnectionConfig
  private reconnectAttempts = 0
  private heartbeatTimer: NodeJS.Timeout | null = null
  private reconnectTimer: NodeJS.Timeout | null = null
  private isManualClose = false
  private messageQueue: WebSocketMessage[] = []
  private connectionState: 'connecting' | 'connected' | 'disconnected' | 'error' = 'disconnected'

  constructor(config: ConnectionConfig) {
    super()
    this.config = {
      reconnectInterval: 5000,
      maxReconnectAttempts: 10,
      heartbeatInterval: 30000,
      ...config
    }
  }

  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.isManualClose = false
        this.connectionState = 'connecting'

        const wsUrl = this.buildWebSocketUrl()
        console.log('[WebSocket] Connecting to:', wsUrl)

        this.socket = new WebSocket(wsUrl)

        this.socket.onopen = () => {
          console.log('[WebSocket] Connected successfully')
          this.connectionState = 'connected'
          this.reconnectAttempts = 0
          this.startHeartbeat()
          this.processMessageQueue()
          this.emit('connected')
          resolve()
        }

        this.socket.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('[WebSocket] Failed to parse message:', error)
          }
        }

        this.socket.onclose = (event) => {
          console.log('[WebSocket] Connection closed:', event.code, event.reason)
          this.connectionState = 'disconnected'
          this.stopHeartbeat()
          this.emit('disconnected', { code: event.code, reason: event.reason })

          if (!this.isManualClose && this.shouldReconnect()) {
            this.scheduleReconnect()
          }
        }

        this.socket.onerror = (error) => {
          console.error('[WebSocket] Connection error:', error)
          this.connectionState = 'error'
          this.emit('error', error)
          reject(error)
        }

      } catch (error) {
        console.error('[WebSocket] Failed to create connection:', error)
        reject(error)
      }
    })
  }

  public disconnect(): void {
    this.isManualClose = true
    this.stopHeartbeat()
    this.clearReconnectTimer()

    if (this.socket) {
      this.socket.close(1000, 'Manual disconnect')
      this.socket = null
    }

    this.connectionState = 'disconnected'
    this.emit('disconnected', { manual: true })
  }

  public send(message: Omit<WebSocketMessage, 'timestamp'>): void {
    const fullMessage: WebSocketMessage = {
      ...message,
      timestamp: Date.now()
    }

    if (this.isConnected()) {
      try {
        this.socket!.send(JSON.stringify(fullMessage))
      } catch (error) {
        console.error('[WebSocket] Failed to send message:', error)
        this.queueMessage(fullMessage)
      }
    } else {
      this.queueMessage(fullMessage)
    }
  }

  public subscribe(eventType: string, callback: (data: any) => void): () => void {
    this.on(eventType, callback)
    return () => this.off(eventType, callback)
  }

  public isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN
  }

  public getConnectionState(): string {
    return this.connectionState
  }

  private buildWebSocketUrl(): string {
    const baseUrl = this.config.url.replace(/^http/, 'ws')
    const url = new URL(baseUrl)

    if (this.config.token) {
      url.searchParams.set('token', this.config.token)
    }

    return url.toString()
  }

  private handleMessage(message: WebSocketMessage): void {
    console.log('[WebSocket] Received message:', message.type)

    // Handle system messages
    if (message.type === 'heartbeat') {
      this.send({ type: 'heartbeat_ack', payload: {} })
      return
    }

    if (message.type === 'error') {
      console.error('[WebSocket] Server error:', message.payload)
      this.emit('error', message.payload)
      return
    }

    // Emit the message to subscribers
    this.emit(message.type, message.payload, message)
    this.emit('message', message)
  }

  private startHeartbeat(): void {
    this.stopHeartbeat()

    if (this.config.heartbeatInterval) {
      this.heartbeatTimer = setInterval(() => {
        if (this.isConnected()) {
          this.send({ type: 'heartbeat', payload: {} })
        }
      }, this.config.heartbeatInterval)
    }
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private shouldReconnect(): boolean {
    return this.reconnectAttempts < (this.config.maxReconnectAttempts || 10)
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer()

    const delay = Math.min(
      this.config.reconnectInterval! * Math.pow(2, this.reconnectAttempts),
      30000 // Max 30 seconds
    )

    console.log(`[WebSocket] Scheduling reconnect in ${delay}ms (attempt ${this.reconnectAttempts + 1})`)

    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++
      this.connect().catch((error) => {
        console.error('[WebSocket] Reconnect failed:', error)
      })
    }, delay)
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  private queueMessage(message: WebSocketMessage): void {
    this.messageQueue.push(message)

    // Limit queue size to prevent memory issues
    if (this.messageQueue.length > 100) {
      this.messageQueue.shift()
    }
  }

  private processMessageQueue(): void {
    while (this.messageQueue.length > 0 && this.isConnected()) {
      const message = this.messageQueue.shift()!
      try {
        this.socket!.send(JSON.stringify(message))
      } catch (error) {
        console.error('[WebSocket] Failed to send queued message:', error)
        // Put message back at the front of queue
        this.messageQueue.unshift(message)
        break
      }
    }
  }
}

// Presence management service
export class PresenceService extends EventEmitter {
  private websocket: WebSocketService
  private presenceData = new Map<string, PresenceData>()
  private currentUser: PresenceData | null = null
  private presenceUpdateTimer: NodeJS.Timeout | null = null

  constructor(websocket: WebSocketService) {
    super()
    this.websocket = websocket
    this.setupEventHandlers()
  }

  private setupEventHandlers(): void {
    this.websocket.on('presence_update', (data: PresenceData) => {
      this.presenceData.set(data.userId, data)
      this.emit('presenceUpdate', data)
    })

    this.websocket.on('presence_remove', (data: { userId: string }) => {
      this.presenceData.delete(data.userId)
      this.emit('presenceRemove', data.userId)
    })

    this.websocket.on('connected', () => {
      if (this.currentUser) {
        this.updatePresence(this.currentUser)
      }
    })
  }

  public setCurrentUser(userData: Omit<PresenceData, 'lastSeen'>): void {
    this.currentUser = {
      ...userData,
      lastSeen: Date.now()
    }

    if (this.websocket.isConnected()) {
      this.updatePresence(this.currentUser)
    }

    this.startPresenceUpdates()
  }

  public updatePresence(data: Partial<PresenceData>): void {
    if (!this.currentUser) return

    this.currentUser = {
      ...this.currentUser,
      ...data,
      lastSeen: Date.now()
    }

    this.websocket.send({
      type: 'presence_update',
      payload: this.currentUser
    })
  }

  public updateCursor(position: { x: number; y: number }): void {
    this.updatePresence({ cursor: position })
  }

  public updateCurrentPage(page: string): void {
    this.updatePresence({ currentPage: page })
  }

  public setStatus(status: PresenceData['status']): void {
    this.updatePresence({ status })
  }

  public getPresenceData(): PresenceData[] {
    return Array.from(this.presenceData.values())
  }

  public getUserPresence(userId: string): PresenceData | undefined {
    return this.presenceData.get(userId)
  }

  public getOnlineUsers(): PresenceData[] {
    return this.getPresenceData().filter(user =>
      user.status === 'online' &&
      Date.now() - user.lastSeen < 60000 // Active within last minute
    )
  }

  private startPresenceUpdates(): void {
    this.stopPresenceUpdates()

    // Send presence update every 30 seconds
    this.presenceUpdateTimer = setInterval(() => {
      if (this.currentUser && this.websocket.isConnected()) {
        this.updatePresence({})
      }
    }, 30000)
  }

  private stopPresenceUpdates(): void {
    if (this.presenceUpdateTimer) {
      clearInterval(this.presenceUpdateTimer)
      this.presenceUpdateTimer = null
    }
  }

  private emit(event: string, data: any): void {
    if (this.websocket) {
      this.websocket.emit(event, data)
    }
  }

  public cleanup(): void {
    this.stopPresenceUpdates()
    this.presenceData.clear()
    this.currentUser = null
  }
}

// Real-time data synchronization service
export class RealTimeDataService {
  private websocket: WebSocketService
  private subscriptions = new Map<string, Set<(data: any) => void>>()

  constructor(websocket: WebSocketService) {
    this.websocket = websocket
    this.setupEventHandlers()
  }

  private setupEventHandlers(): void {
    this.websocket.on('data_update', (payload: { resource: string; data: any; action: string }) => {
      const { resource, data, action } = payload
      const callbacks = this.subscriptions.get(resource)

      if (callbacks) {
        callbacks.forEach(callback => {
          try {
            callback({ data, action, resource })
          } catch (error) {
            console.error('[RealTimeData] Callback error:', error)
          }
        })
      }
    })
  }

  public subscribe(resource: string, callback: (update: any) => void): () => void {
    if (!this.subscriptions.has(resource)) {
      this.subscriptions.set(resource, new Set())

      // Send subscription request to server
      this.websocket.send({
        type: 'subscribe',
        payload: { resource }
      })
    }

    this.subscriptions.get(resource)!.add(callback)

    // Return unsubscribe function
    return () => {
      const callbacks = this.subscriptions.get(resource)
      if (callbacks) {
        callbacks.delete(callback)

        if (callbacks.size === 0) {
          this.subscriptions.delete(resource)

          // Send unsubscribe request to server
          this.websocket.send({
            type: 'unsubscribe',
            payload: { resource }
          })
        }
      }
    }
  }

  public publishUpdate(resource: string, data: any, action = 'update'): void {
    this.websocket.send({
      type: 'data_update',
      payload: { resource, data, action }
    })
  }

  public cleanup(): void {
    this.subscriptions.clear()
  }
}

// Export configured instances
let websocketService: WebSocketService | null = null
let presenceService: PresenceService | null = null
let realTimeDataService: RealTimeDataService | null = null

export const initializeWebSocketServices = (config: ConnectionConfig) => {
  websocketService = new WebSocketService(config)
  presenceService = new PresenceService(websocketService)
  realTimeDataService = new RealTimeDataService(websocketService)

  return {
    websocket: websocketService,
    presence: presenceService,
    realTimeData: realTimeDataService
  }
}

export const getWebSocketService = () => websocketService
export const getPresenceService = () => presenceService
export const getRealTimeDataService = () => realTimeDataService