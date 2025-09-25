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

    const { result } = renderHook(() =>
      useAutoSave(data, {
        delay: 1000,
        onSave: mockSave,
      })
    )

    expect(result.current.status).toBe('idle')
    expect(mockSave).not.toHaveBeenCalled()

    // Fast forward time to trigger save
    act(() => {
      vi.advanceTimersByTime(1000)
    })

    await waitFor(() => {
      expect(mockSave).toHaveBeenCalledWith(data)
    })

    await waitFor(() => {
      expect(result.current.status).toBe('saved')
    })
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

    act(() => {
      vi.advanceTimersByTime(500)
    })

    await waitFor(() => {
      expect(result.current.status).toBe('saving')
    })

    // Resolve the save
    act(() => {
      resolveSave!()
    })

    await waitFor(() => {
      expect(result.current.status).toBe('saved')
    })
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

    act(() => {
      vi.advanceTimersByTime(500)
    })

    await waitFor(() => {
      expect(result.current.status).toBe('error')
      expect(result.current.error).toBe(mockError)
      expect(mockOnError).toHaveBeenCalledWith(mockError)
    })
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
    act(() => {
      vi.advanceTimersByTime(500)
    })

    await waitFor(() => {
      expect(result.current.status).toBe('error')
    })

    // Retry
    act(() => {
      result.current.retry()
    })

    await waitFor(() => {
      expect(result.current.status).toBe('saved')
      expect(mockSave).toHaveBeenCalledTimes(2)
    })
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

  it('should not save empty data', () => {
    const mockSave = vi.fn().mockResolvedValue(undefined)
    const emptyData = {}

    renderHook(() =>
      useAutoSave(emptyData, {
        delay: 500,
        onSave: mockSave,
      })
    )

    act(() => {
      vi.advanceTimersByTime(500)
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
    act(() => {
      vi.advanceTimersByTime(500)
    })

    rerender({ data: { name: 'Test3' } })
    act(() => {
      vi.advanceTimersByTime(500)
    })

    rerender({ data: { name: 'Test4' } })
    act(() => {
      vi.advanceTimersByTime(1000)
    })

    await waitFor(() => {
      expect(mockSave).toHaveBeenCalledTimes(1)
      expect(mockSave).toHaveBeenCalledWith({ name: 'Test4' })
    })
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
    act(() => {
      result.current.save()
    })

    await waitFor(() => {
      expect(mockSave).toHaveBeenCalledWith(data)
      expect(result.current.status).toBe('saved')
    })
  })
})