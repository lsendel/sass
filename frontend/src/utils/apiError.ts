import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'
import type { SerializedError } from '@reduxjs/toolkit'

export interface ParsedApiError {
  status?: number
  message: string
}

export function parseApiError(err: unknown): ParsedApiError {
  // RTK Query errors
  const fbq = err as FetchBaseQueryError
  if (typeof fbq === 'object' && fbq !== null && 'status' in fbq) {
    const status = typeof fbq.status === 'number' ? fbq.status : undefined
    // Try to read common message shapes
    const data = (fbq as { data?: unknown }).data
    const rawMessage =
      (data &&
        typeof data === 'object' &&
        data !== null &&
        ((data as Record<string, unknown>).message ??
          (data as Record<string, unknown>).error ??
          (data as Record<string, unknown>).detail)) ??
      undefined

    const message =
      (typeof rawMessage === 'string'
        ? rawMessage
        : rawMessage != null
          ? String(rawMessage)
          : undefined) ??
      (status === 401
        ? 'Unauthorized'
        : status === 403
          ? 'Forbidden'
          : 'Request failed')

    if (status !== undefined) {
      return { status, message }
    } else {
      return { message }
    }
  }

  // SerializedError or generic
  const se = err as SerializedError
  if (se && typeof se === 'object' && ('message' in se || 'name' in se)) {
    return { message: se.message! ?? 'Unexpected error' } as ParsedApiError
  }

  return { message: 'Unexpected error' }
}
