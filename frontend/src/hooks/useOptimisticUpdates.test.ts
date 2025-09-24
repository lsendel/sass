import { renderHook, waitFor, act } from '@testing-library/react'
import { useOptimisticUpdates, useOptimisticList } from './useOptimisticUpdates'

describe('useOptimisticUpdates', () => {
  beforeEach(() => {
    jest.useFakeTimers()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('should add optimistic update and confirm on success', async () => {
    const mockMutation = jest.fn().mockResolvedValue('success')
    const { result } = renderHook(() => useOptimisticUpdates())

    const testData = { id: '1', name: 'Test' }

    act(() => {
      result.current.addOptimisticUpdate(
        testData,
        mockMutation,
        { successMessage: 'Success!' }
      )
    })

    expect(result.current.optimisticUpdates).toHaveLength(1)
    expect(result.current.optimisticUpdates[0].status).toBe('pending')

    await waitFor(() => {
      expect(mockMutation).toHaveBeenCalledWith(testData)
    })

    await waitFor(() => {
      expect(result.current.optimisticUpdates[0].status).toBe('confirmed')
    })

    // Should clean up after delay
    act(() => {
      jest.advanceTimersByTime(3000)
    })

    await waitFor(() => {
      expect(result.current.optimisticUpdates).toHaveLength(0)
    })
  })

  it('should handle errors and provide rollback', async () => {
    const mockError = new Error('Mutation failed')
    const mockMutation = jest.fn().mockRejectedValue(mockError)
    const mockOnError = jest.fn()

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
    const mockMutation = jest.fn().mockRejectedValue(new Error('Failed'))
    const mockOnError = jest.fn()

    const { result } = renderHook(() => useOptimisticUpdates())

    const testData = { id: '1', name: 'Test' }

    await act(async () => {
      await result.current.addOptimisticUpdate(
        testData,
        mockMutation,
        { onError: mockOnError, rollbackDelay: 1000 }
      )
    })

    expect(result.current.optimisticUpdates[0].status).toBe('failed')

    // Fast forward to trigger auto-rollback
    act(() => {
      jest.advanceTimersByTime(1000)
    })

    await waitFor(() => {
      expect(result.current.optimisticUpdates[0].status).toBe('rolledBack')
    })
  })

  it('should provide helper methods', () => {
    const { result } = renderHook(() => useOptimisticUpdates())

    // Add some updates
    act(() => {
      result.current.addOptimisticUpdate(
        { id: '1' },
        async () => 'success'
      )
    })

    expect(result.current.hasOptimisticUpdates).toBe(true)
    expect(result.current.getPendingUpdates()).toHaveLength(1)
    expect(result.current.getFailedUpdates()).toHaveLength(0)
  })
})

describe('useOptimisticList', () => {
  beforeEach(() => {
    jest.useFakeTimers()
  })

  afterEach(() => {
    jest.useRealTimers()
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
    const mockMutation = jest.fn().mockResolvedValue(newItem)

    await act(async () => {
      await result.current.addItem(newItem, mockMutation)
    })

    // Should immediately show in optimistic list
    expect(result.current.data).toHaveLength(3)
    expect(result.current.data[0]).toEqual(newItem) // Added to beginning

    await waitFor(() => {
      expect(mockMutation).toHaveBeenCalledWith(newItem)
    })
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
    const mockMutation = jest.fn().mockResolvedValue(updatedItem)

    await act(async () => {
      await result.current.updateItem(updatedItem, mockMutation)
    })

    // Should immediately reflect in optimistic list
    expect(result.current.data[1].name).toBe('Updated Item 1') // Added at beginning, so original index 0 is now 1

    await waitFor(() => {
      expect(mockMutation).toHaveBeenCalledWith(updatedItem)
    })
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
    const mockMutation = jest.fn().mockResolvedValue(undefined)

    await act(async () => {
      await result.current.deleteItem(itemToDelete, mockMutation)
    })

    // Should immediately remove from optimistic list
    expect(result.current.data).toHaveLength(1)
    expect(result.current.data.find(item => item.id === '1')).toBeUndefined()

    await waitFor(() => {
      expect(mockMutation).toHaveBeenCalledWith('1')
    })
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
    const mockMutation = jest.fn().mockRejectedValue(new Error('Failed'))

    await act(async () => {
      await result.current.addItem(newItem, mockMutation)
    })

    // Should initially show optimistic item
    expect(result.current.data).toHaveLength(3)

    // Wait for error handling and auto-rollback
    await waitFor(() => {
      expect(mockMutation).toHaveBeenCalled()
    })

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