import { renderHook, waitFor, act } from '@testing-library/react'
import { vi } from 'vitest'

import { useAutoSave } from './useAutoSave'

describe('useAutoSave', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should save data after delay', async () => {
    const mockSave = vi.fn().mockResolvedValue(undefined)
    const data = { name: 'Test', slug: 'test' }

    const { result, rerender } = renderHook(
      ({ data }) => useAutoSave(data, {
        delay: 1000,
        onSave: mockSave,
      }),
      { initialProps: { data: {} } }
    )

    expect(result.current.status).toBe('idle')
    expect(mockSave).not.toHaveBeenCalled()

    // Update data to trigger auto-save
    rerender({ data })

    // Fast forward time to trigger save and run all async operations
    await act(async () => {
      vi.advanceTimersByTime(1000)
      await vi.runOnlyPendingTimersAsync()
    })

    expect(mockSave).toHaveBeenCalledWith(data)
    expect(result.current.status).toBe('saved')
  })

  it('should show saving state during save', async () => {
    let resolveSave: () => void
    const mockSave = vi.fn(() => new Promise<void>(resolve => {
      resolveSave = resolve
    }))

    const data = { name: 'Test' }
    const { result } = renderHook(() =>
      useAutoSave(data, {
        delay: 500,
        onSave: mockSave,
      })
    )

    // Trigger the timer and wait for the save to start
    await act(async () => {
      vi.advanceTimersByTime(500)
      await vi.runOnlyPendingTimersAsync()
    })

    expect(result.current.status).toBe('saving')

    // Resolve the save
    await act(async () => {
      resolveSave!()
      await vi.runAllTimersAsync()
    })

    expect(result.current.status).toBe('saved')
  })

  it('should handle save errors', async () => {
    const mockError = new Error('Save failed')
    const mockSave = vi.fn().mockRejectedValue(mockError)
    const mockOnError = vi.fn()

    const data = { name: 'Test' }
    const { result } = renderHook(() =>
      useAutoSave(data, {
        delay: 500,
        onSave: mockSave,
        onError: mockOnError,
      })
    )

    await act(async () => {
      vi.advanceTimersByTime(500)
      await vi.runAllTimersAsync()
    })

    expect(result.current.status).toBe('error')
    expect(result.current.error).toBe(mockError)
    expect(mockOnError).toHaveBeenCalledWith(mockError)
  })

  it('should retry failed saves', async () => {
    const mockSave = vi.fn()
      .mockRejectedValueOnce(new Error('First fail'))
      .mockResolvedValueOnce(undefined)

    const data = { name: 'Test' }
    const { result } = renderHook(() =>
      useAutoSave(data, {
        delay: 500,
        onSave: mockSave,
      })
    )

    // Initial save attempt
    await act(async () => {
      vi.advanceTimersByTime(500)
      await vi.runAllTimersAsync()
    })

    expect(result.current.status).toBe('error')

    // Retry
    await act(async () => {
      result.current.retry()
      await vi.runAllTimersAsync()
    })

    expect(result.current.status).toBe('saved')
    expect(mockSave).toHaveBeenCalledTimes(2)
  })

  it('should detect unsaved changes', () => {
    const mockSave = vi.fn().mockResolvedValue(undefined)
    const data = { name: 'Test', slug: 'test' }

    const { result, rerender } = renderHook(
      ({ data }) => useAutoSave(data, { onSave: mockSave }),
      { initialProps: { data } }
    )

    // Initially no unsaved changes
    expect(result.current.hasUnsavedChanges).toBe(false)

    // Update data
    rerender({ data: { name: 'Updated', slug: 'updated' } })

    expect(result.current.hasUnsavedChanges).toBe(true)
  })

  it('should not save empty data', async () => {
    const mockSave = vi.fn().mockResolvedValue(undefined)
    const emptyData = {}

    renderHook(() =>
      useAutoSave(emptyData, {
        delay: 500,
        onSave: mockSave,
      })
    )

    await act(async () => {
      vi.advanceTimersByTime(500)
      await vi.runAllTimersAsync()
    })

    expect(mockSave).not.toHaveBeenCalled()
  })

  it('should debounce rapid changes', async () => {
    const mockSave = vi.fn().mockResolvedValue(undefined)

    const { result, rerender } = renderHook(
      ({ data }) => useAutoSave(data, { delay: 1000, onSave: mockSave }),
      { initialProps: { data: { name: 'Test1' } } }
    )

    // Rapid changes
    rerender({ data: { name: 'Test2' } })
    await act(async () => {
      vi.advanceTimersByTime(500)
    })

    rerender({ data: { name: 'Test3' } })
    await act(async () => {
      vi.advanceTimersByTime(500)
    })

    rerender({ data: { name: 'Test4' } })
    await act(async () => {
      vi.advanceTimersByTime(1000)
      await vi.runAllTimersAsync()
    })

    expect(mockSave).toHaveBeenCalledTimes(1)
    expect(mockSave).toHaveBeenCalledWith({ name: 'Test4' })
  })

  it('should provide manual save function', async () => {
    const mockSave = vi.fn().mockResolvedValue(undefined)
    const data = { name: 'Test' }

    const { result } = renderHook(() =>
      useAutoSave(data, {
        delay: 2000,
        onSave: mockSave,
      })
    )

    // Manual save before auto-save delay
    await act(async () => {
      result.current.save()
      await vi.runAllTimersAsync()
    })

    expect(mockSave).toHaveBeenCalledWith(data)
    expect(result.current.status).toBe('saved')
  })
})