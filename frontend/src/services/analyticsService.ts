/**
 * Analytics service for sending performance and user metrics
 */

interface AnalyticsEvent {
  type: 'performance' | 'user_action' | 'error'
  data: any
  timestamp: number
  sessionId: string
  userId?: string
}

class AnalyticsService {
  private endpoint: string
  private sessionId: string
  private queue: AnalyticsEvent[] = []
  private flushTimer: number | null = null

  constructor() {
    this.endpoint =
      import.meta.env.VITE_ANALYTICS_ENDPOINT || '/api/v1/analytics'
    this.sessionId = this.generateSessionId()
  }

  private generateSessionId(): string {
    // Use crypto.randomUUID() for cryptographically secure random session IDs
    // Fallback to timestamp-based ID if crypto API is not available
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `${Date.now()}-${crypto.randomUUID()}`
    }
    // Fallback: generate secure random bytes
    const randomBytes = new Uint8Array(16)
    crypto.getRandomValues(randomBytes)
    const randomString = Array.from(randomBytes)
      .map(b => b.toString(36))
      .join('')
      .substr(0, 9)
    return `${Date.now()}-${randomString}`
  }

  async sendMetrics(metrics: any[]): Promise<void> {
    const events: AnalyticsEvent[] = metrics.map(metric => ({
      type: 'performance',
      data: metric,
      timestamp: Date.now(),
      sessionId: this.sessionId,
    }))

    this.queue.push(...events)
    this.scheduleFlush()
  }

  private scheduleFlush(): void {
    if (this.flushTimer) return

    this.flushTimer = window.setTimeout(() => {
      this.flush()
      this.flushTimer = null
    }, 1000)
  }

  private async flush(): Promise<void> {
    if (this.queue.length === 0) return

    const events = [...this.queue]
    this.queue = []

    try {
      await fetch(this.endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ events }),
      })
    } catch (error) {
      console.warn('Analytics send failed:', error)
      // Re-queue events for retry
      this.queue.unshift(...events)
    }
  }
}

export const analyticsService = new AnalyticsService()
