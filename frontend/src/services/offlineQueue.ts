import { openDB, DBSchema, IDBPDatabase } from 'idb'

interface QueuedRequest {
  id: string
  url: string
  method: string
  headers: Record<string, string>
  body?: any
  timestamp: number
  retryCount: number
  maxRetries: number
}

interface OfflineQueueDB extends DBSchema {
  requests: {
    key: string
    value: QueuedRequest
    indexes: {
      'by-timestamp': number
      'by-retry-count': number
    }
  }
}

export class OfflineQueue {
  private db: IDBPDatabase<OfflineQueueDB> | null = null
  private isOnline: boolean = navigator.onLine
  private syncInProgress = false
  private listeners = new Set<(online: boolean) => void>()

  constructor() {
    this.init()
    this.setupEventListeners()
  }

  private async init() {
    this.db = await openDB<OfflineQueueDB>('offline-queue', 1, {
      upgrade(db) {
        const store = db.createObjectStore('requests', {
          keyPath: 'id'
        })
        store.createIndex('by-timestamp', 'timestamp')
        store.createIndex('by-retry-count', 'retryCount')
      }
    })
  }

  private setupEventListeners() {
    window.addEventListener('online', () => this.handleOnline())
    window.addEventListener('offline', () => this.handleOffline())

    // Periodic sync check
    setInterval(() => {
      if (this.isOnline && !this.syncInProgress) {
        this.processQueue()
      }
    }, 30000) // Every 30 seconds
  }

  private handleOnline() {
    console.log('[OfflineQueue] Connection restored')
    this.isOnline = true
    this.notifyListeners(true)
    this.processQueue()
  }

  private handleOffline() {
    console.log('[OfflineQueue] Connection lost')
    this.isOnline = false
    this.notifyListeners(false)
  }

  public onStatusChange(callback: (online: boolean) => void) {
    this.listeners.add(callback)
    return () => this.listeners.delete(callback)
  }

  private notifyListeners(online: boolean) {
    this.listeners.forEach(callback => callback(online))
  }

  public async queueRequest(
    url: string,
    options: RequestInit,
    maxRetries = 3
  ): Promise<string> {
    if (!this.db) await this.init()

    const request: QueuedRequest = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      url,
      method: options.method || 'GET',
      headers: options.headers as Record<string, string> || {},
      body: options.body,
      timestamp: Date.now(),
      retryCount: 0,
      maxRetries
    }

    await this.db!.add('requests', request)
    console.log('[OfflineQueue] Request queued:', request.id)

    // Try to process immediately if online
    if (this.isOnline) {
      this.processQueue()
    }

    return request.id
  }

  public async processQueue(): Promise<void> {
    if (!this.db || this.syncInProgress || !this.isOnline) return

    this.syncInProgress = true

    try {
      const tx = this.db.transaction('requests', 'readonly')
      const requests = await tx.store.index('by-timestamp').getAll()

      console.log(`[OfflineQueue] Processing ${requests.length} queued requests`)

      for (const request of requests) {
        if (request.retryCount >= request.maxRetries) {
          console.log(`[OfflineQueue] Max retries reached for ${request.id}`)
          await this.removeRequest(request.id)
          continue
        }

        try {
          const response = await fetch(request.url, {
            method: request.method,
            headers: request.headers,
            body: request.body
          })

          if (response.ok) {
            console.log(`[OfflineQueue] Request ${request.id} succeeded`)
            await this.removeRequest(request.id)

            // Notify success
            this.notifyRequestSuccess(request, response)
          } else if (response.status >= 400 && response.status < 500) {
            // Client error, don't retry
            console.log(`[OfflineQueue] Request ${request.id} failed with client error`)
            await this.removeRequest(request.id)
            this.notifyRequestFailure(request, new Error(`HTTP ${response.status}`))
          } else {
            // Server error, retry
            await this.incrementRetryCount(request.id)
          }
        } catch (error) {
          console.error(`[OfflineQueue] Request ${request.id} failed:`, error)
          await this.incrementRetryCount(request.id)
        }

        // Add delay between requests
        await new Promise(resolve => setTimeout(resolve, 100))
      }
    } finally {
      this.syncInProgress = false
    }
  }

  private async removeRequest(id: string): Promise<void> {
    if (!this.db) return
    await this.db.delete('requests', id)
  }

  private async incrementRetryCount(id: string): Promise<void> {
    if (!this.db) return

    const tx = this.db.transaction('requests', 'readwrite')
    const request = await tx.store.get(id)

    if (request) {
      request.retryCount++
      await tx.store.put(request)
    }
  }

  private notifyRequestSuccess(request: QueuedRequest, response: Response) {
    window.dispatchEvent(new CustomEvent('offline-queue-success', {
      detail: { request, response }
    }))
  }

  private notifyRequestFailure(request: QueuedRequest, error: Error) {
    window.dispatchEvent(new CustomEvent('offline-queue-failure', {
      detail: { request, error }
    }))
  }

  public async getQueuedRequests(): Promise<QueuedRequest[]> {
    if (!this.db) await this.init()
    return this.db!.getAll('requests')
  }

  public async clearQueue(): Promise<void> {
    if (!this.db) await this.init()
    await this.db!.clear('requests')
  }

  public isOnlineStatus(): boolean {
    return this.isOnline
  }
}

// Singleton instance
export const offlineQueue = new OfflineQueue()

// Hook for React components
import { useState, useEffect } from 'react'

export function useOfflineStatus() {
  const [isOnline, setIsOnline] = useState(offlineQueue.isOnlineStatus())
  const [queueLength, setQueueLength] = useState(0)

  useEffect(() => {
    const updateQueueLength = async () => {
      const requests = await offlineQueue.getQueuedRequests()
      setQueueLength(requests.length)
    }

    const unsubscribe = offlineQueue.onStatusChange((online) => {
      setIsOnline(online)
      updateQueueLength()
    })

    // Check queue length periodically
    const interval = setInterval(updateQueueLength, 5000)

    updateQueueLength()

    return () => {
      unsubscribe()
      clearInterval(interval)
    }
  }, [])

  return { isOnline, queueLength }
}