import { useState, useEffect, useCallback, useRef } from 'react'

export interface AutoSaveOptions {
  delay?: number
  onSave: (data: any) => Promise<void>
  onError?: (error: Error) => void
}

export type AutoSaveStatus = 'idle' | 'saving' | 'saved' | 'error'

export const useAutoSave = (data: any, options: AutoSaveOptions) => {
  const { delay = 2000, onSave, onError } = options
  const [status, setStatus] = useState<AutoSaveStatus>('idle')
  const [lastSaved, setLastSaved] = useState<Date | null>(null)
  const [error, setError] = useState<Error | null>(null)
  const timeoutRef = useRef<NodeJS.Timeout>()
  const lastSavedRef = useRef<string>()
  const initialDataRef = useRef<string>()

  // Track initial data to detect if there are unsaved changes
  useEffect(() => {
    if (!initialDataRef.current) {
      initialDataRef.current = JSON.stringify(data)
    }
  }, [])

  const save = useCallback(async () => {
    const currentData = JSON.stringify(data)
    if (currentData === lastSavedRef.current) return

    setStatus('saving')
    setError(null)

    try {
      await onSave(data)
      lastSavedRef.current = currentData
      setLastSaved(new Date())
      setStatus('saved')
      setTimeout(() => setStatus('idle'), 2000)
    } catch (error) {
      const err = error as Error
      setStatus('error')
      setError(err)
      onError?.(err)
    }
  }, [data, onSave, onError])

  const retry = useCallback(() => {
    if (status === 'error') {
      save()
    }
  }, [save, status])

  const hasUnsavedChanges = useCallback(() => {
    const currentData = JSON.stringify(data)
    const hasData = currentData !== JSON.stringify({}) && currentData !== initialDataRef.current
    const isNotSaved = currentData !== lastSavedRef.current
    return hasData && isNotSaved && status !== 'saving'
  }, [data, status])

  useEffect(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    // Only auto-save if there's actual content
    const currentData = JSON.stringify(data)
    if (currentData !== JSON.stringify({}) && currentData !== initialDataRef.current) {
      timeoutRef.current = setTimeout(save, delay)
    }

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [data, delay, save])

  return {
    status,
    lastSaved,
    error,
    hasUnsavedChanges: hasUnsavedChanges(),
    save: () => save(),
    retry
  }
}
