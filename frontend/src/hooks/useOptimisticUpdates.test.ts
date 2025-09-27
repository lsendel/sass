import { renderHook, waitFor, act } from '@testing-library/react'
import { vi } from 'vitest'

import { useOptimisticUpdates, useOptimisticList } from './useOptimisticUpdates'

describe('useOptimisticUpdates', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should add optimistic update and confirm on success', async () => {
    const mockMutation = vi.fn().mockResolvedValue('success')
    const { result } = renderHook(() => useOptimisticUpdates())

    const testData = { id: '1', name: 'Test' }

    let updatePromise: Promise<any>
    act(() => {
      updatePromise = result.current.addOptimisticUpdate(
        testData,
        mockMutation,
        { successMessage: 'Success!' }
      )
    })

    expect(result.current.optimisticUpdates).toHaveLength(1)
    expect(result.current.optimisticUpdates[0].status).toBe('pending')

    // Wait for mutation to be called and completed
    await act(async () => {
      await updatePromise
    })

    expect(mockMutation).toHaveBeenCalledWith(testData)
    expect(result.current.optimisticUpdates[0].status).toBe('confirmed')

    // Should clean up after delay
    await act(async () => {
      vi.advanceTimersByTime(3000)
      await vi.runAllTimersAsync()
    })

    expect(result.current.optimisticUpdates).toHaveLength(0)
  })

  it('should handle errors and provide rollback', async () => {
    const mockError = new Error('Mutation failed')
    const mockMutation = vi.fn().mockRejectedValue(mockError)
    const mockOnError = vi.fn()

    const { result } = renderHook(() => useOptimisticUpdates())

    const testData = { id: '1', name: 'Test' }

    let updateResult: any
    await act(async () => {
      updateResult = await result.current.addOptimisticUpdate(
        testData,
        mockMutation,
        { onError: mockOnError, rollbackDelay: 1000 }
      )
    })

    expect(updateResult).toBeNull()
    expect(result.current.optimisticUpdates[0].status).toBe('failed')

    // Test manual rollback
    const updateId = result.current.optimisticUpdates[0].id
    act(() => {
      result.current.rollbackUpdate(updateId, testData, mockOnError)
    })

    await waitFor(() => {
      expect(result.current.optimisticUpdates[0].status).toBe('rolledBack')
      expect(mockOnError).toHaveBeenCalledWith(
        expect.objectContaining({ message: 'Operation rolled back' }),
        testData
      )
    })
  })

  it('should auto-rollback after delay', async () => {
    const mockMutation = vi.fn().mockRejectedValue(new Error('Failed'))
    const mockOnError = vi.fn()

    const { result } = renderHook(() => useOptimisticUpdates())

    const testData = { id: '1', name: 'Test' }

    await act(async () => {
      try {
        await result.current.addOptimisticUpdate(
          testData,
          mockMutation,
          { onError: mockOnError, rollbackDelay: 1000 }
        )
      } catch (error) {
        // Expected to fail
      }
    })

    expect(result.current.optimisticUpdates[0]?.status).toBe('failed')

    // Fast forward to trigger auto-rollback
    await act(async () => {
      vi.advanceTimersByTime(1000)
      await vi.runAllTimersAsync()
    })

    const rolledBackUpdate = result.current.optimisticUpdates.find(u => u.status === 'rolledBack')
    expect(rolledBackUpdate).toBeDefined()
  })

  it('should provide helper methods', async () => {
    const { result } = renderHook(() => useOptimisticUpdates())

    // Add some updates
    let updatePromise: Promise<any>
    act(() => {
      updatePromise = result.current.addOptimisticUpdate(
        { id: '1' },
        async () => 'success'
      )
    })

    expect(result.current.hasOptimisticUpdates).toBe(true)
    expect(result.current.getPendingUpdates()).toHaveLength(1)
    expect(result.current.getFailedUpdates()).toHaveLength(0)

    // Wait for update to complete to avoid act warnings
    await act(async () => {
      await updatePromise
    })
  })
})

describe('useOptimisticList', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should optimistically add items', async () => {
    const initialData = [
      { id: '1', name: 'Item 1' },
      { id: '2', name: 'Item 2' }
    ]

    const { result } = renderHook(() =>
      useOptimisticList(initialData)
    )

    expect(result.current.data).toHaveLength(2)

    const newItem = { id: '3', name: 'Item 3' }
    const mockMutation = vi.fn().mockResolvedValue(newItem)

    await act(async () => {
      const addPromise = result.current.addItem(newItem, mockMutation)
      await addPromise
    })

    // Should immediately show in optimistic list
    expect(result.current.data).toHaveLength(3)
    expect(result.current.data[0]).toEqual(newItem) // Added to beginning
    expect(mockMutation).toHaveBeenCalledWith(newItem)
  })

  it('should optimistically update items', async () => {
    const initialData = [
      { id: '1', name: 'Item 1' },
      { id: '2', name: 'Item 2' }
    ]

    const { result } = renderHook(() =>
      useOptimisticList(initialData)
    )

    const updatedItem = { id: '1', name: 'Updated Item 1' }
    const mockMutation = vi.fn().mockResolvedValue(updatedItem)

    await act(async () => {
      const updatePromise = result.current.updateItem(updatedItem, mockMutation)
      await updatePromise
    })

    // Should immediately reflect in optimistic list
    const updatedItemInList = result.current.data.find(item => item.id === '1')
    expect(updatedItemInList?.name).toBe('Updated Item 1')
    expect(mockMutation).toHaveBeenCalledWith(updatedItem)
  })

  it('should optimistically delete items', async () => {
    const initialData = [
      { id: '1', name: 'Item 1' },
      { id: '2', name: 'Item 2' }
    ]

    const { result } = renderHook(() =>
      useOptimisticList(initialData)
    )

    const itemToDelete = initialData[0]
    const mockMutation = vi.fn().mockResolvedValue(undefined)

    await act(async () => {
      const deletePromise = result.current.deleteItem(itemToDelete, mockMutation)
      await deletePromise
    })

    // Should immediately remove from optimistic list
    expect(result.current.data).toHaveLength(1)
    expect(result.current.data.find(item => item.id === '1')).toBeUndefined()
    expect(mockMutation).toHaveBeenCalledWith('1')
  })

  it('should revert optimistic changes on error', async () => {
    const initialData = [
      { id: '1', name: 'Item 1' },
      { id: '2', name: 'Item 2' }
    ]

    const { result } = renderHook(() =>
      useOptimisticList(initialData)
    )

    const newItem = { id: '3', name: 'Item 3' }
    const mockMutation = vi.fn().mockRejectedValue(new Error('Failed'))

    await act(async () => {
      try {
        const addPromise = result.current.addItem(newItem, mockMutation)
        await addPromise
      } catch (error) {
        // Expected to fail
      }
    })

    expect(mockMutation).toHaveBeenCalled()

    // Should be marked as failed but still visible with error state
    expect(result.current.hasOptimisticUpdates).toBe(true)
    expect(result.current.optimisticUpdates.some(u => u.status === 'failed')).toBe(true)
  })

  it('should update base data when server data changes', () => {
    const initialData = [{ id: '1', name: 'Item 1' }]

    const { result, rerender } = renderHook(
      ({ data }) => useOptimisticList(data),
      { initialProps: { data: initialData } }
    )

    expect(result.current.baseData).toEqual(initialData)

    const newData = [
      { id: '1', name: 'Updated Item 1' },
      { id: '2', name: 'Item 2' }
    ]

    act(() => {
      result.current.setData(newData)
    })

    expect(result.current.baseData).toEqual(newData)
    expect(result.current.data).toEqual(newData)
  })
})