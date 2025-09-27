import { describe, it, expect, vi } from 'vitest'
import React from 'react'
import { renderHook } from '@testing-library/react'
import { Provider } from 'react-redux'
import type { ReactNode } from 'react'

import { useAppDispatch, useAppSelector } from './hooks'

import {
  createMockStore,
  type MockStore,
} from '@/test/utils/mockStore'
import { createMockUser } from '@/test/fixtures/users'

const createWrapper = (store: MockStore) => {
   
  return ({ children }: { children: ReactNode }) => (
    // @ts-ignore - JSX element
    React.createElement(Provider, { store }, children)
  )
}

describe('Redux hooks', () => {
  describe('useAppSelector', () => {
    it('should select auth state', () => {
      const store = createMockStore({
        auth: {
          isAuthenticated: true,
          user: createMockUser({ id: '123', name: 'Test User' }),
        },
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.auth.isAuthenticated),
        { wrapper }
      )

      expect(result.current).toBe(true)
    })

    it('should select auth user', () => {
      const mockUser = createMockUser({
        id: '456',
        name: 'Hook User',
        email: 'hook@example.com',
        provider: 'github',
      })

      const store = createMockStore({
        auth: { user: mockUser }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.auth.user),
        { wrapper }
      )

      expect(result.current).toEqual(mockUser)
    })

    it('should select auth loading state', () => {
      const store = createMockStore({
        auth: { isLoading: true }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.auth.isLoading),
        { wrapper }
      )

      expect(result.current).toBe(true)
    })

    it('should select ui theme', () => {
      const store = createMockStore({
        ui: { theme: 'dark' }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.ui.theme),
        { wrapper }
      )

      expect(result.current).toBe('dark')
    })

    it('should select ui sidebar state', () => {
      const store = createMockStore({
        ui: { sidebarOpen: true }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.ui.sidebarOpen),
        { wrapper }
      )

      expect(result.current).toBe(true)
    })

    it('should select ui notifications', () => {
      const mockNotifications = [
        {
          id: '1',
          type: 'success' as const,
          title: 'Success',
          message: 'Operation completed',
        }
      ]

      const store = createMockStore({
        ui: { notifications: mockNotifications }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.ui.notifications),
        { wrapper }
      )

      expect(result.current).toEqual(mockNotifications)
    })

    it('should select ui modals state', () => {
      const store = createMockStore({
        ui: {
          modals: {
            isPaymentMethodModalOpen: true,
            isSubscriptionModalOpen: false,
            isInviteUserModalOpen: true,
          }
        }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.ui.modals),
        { wrapper }
      )

      expect(result.current.isPaymentMethodModalOpen).toBe(true)
      expect(result.current.isSubscriptionModalOpen).toBe(false)
      expect(result.current.isInviteUserModalOpen).toBe(true)
    })

    it('should select ui loading states', () => {
      const store = createMockStore({
        ui: {
          loading: {
            global: true,
            components: {
              userProfile: false,
              paymentForm: true,
            }
          }
        }
      })

      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => useAppSelector(state => state.ui.loading),
        { wrapper }
      )

      expect(result.current.global).toBe(true)
      expect(result.current.components.userProfile).toBe(false)
      expect(result.current.components.paymentForm).toBe(true)
    })
  })

  describe('useAppDispatch', () => {
    it('should return dispatch function', () => {
      const store = createMockStore()
      const wrapper = createWrapper(store)

      const { result } = renderHook(() => useAppDispatch(), { wrapper })

      expect(typeof result.current).toBe('function')
    })

    it('should be able to dispatch actions', () => {
      const store = createMockStore()
      const wrapper = createWrapper(store)

      const { result } = renderHook(() => useAppDispatch(), { wrapper })

      // Should not throw when calling dispatch
      expect(() => {
        result.current({ type: 'test/action' })
      }).not.toThrow()
    })

    it('should dispatch auth actions', () => {
      const store = createMockStore()
      const wrapper = createWrapper(store)
      const mockAction = { type: 'auth/setLoading', payload: true }

      const { result } = renderHook(() => useAppDispatch(), { wrapper })

      expect(() => {
        result.current(mockAction)
      }).not.toThrow()
    })

    it('should dispatch ui actions', () => {
      const store = createMockStore()
      const wrapper = createWrapper(store)
      const mockAction = { type: 'ui/setTheme', payload: 'dark' }

      const { result } = renderHook(() => useAppDispatch(), { wrapper })

      expect(() => {
        result.current(mockAction)
      }).not.toThrow()
    })
  })

  describe('hook integration', () => {
    it('should work together for state management', () => {
      const store = createMockStore({
        auth: { isAuthenticated: false },
      })
      const wrapper = createWrapper(store)

      const { result: selectorResult } = renderHook(
        () => useAppSelector(state => state.auth.isAuthenticated),
        { wrapper }
      )

      const { result: dispatchResult } = renderHook(
        () => useAppDispatch(),
        { wrapper }
      )

      expect(selectorResult.current).toBe(false)
      expect(typeof dispatchResult.current).toBe('function')
    })

    it('should handle complex state selection', () => {
      const complexState = {
        auth: {
          user: {
            id: '789',
            name: 'Complex User',
            email: 'complex@example.com',
            provider: 'microsoft',
            preferences: { theme: 'auto', notifications: true },
            createdAt: '2024-01-01T00:00:00Z',
            lastActiveAt: '2024-01-15T10:30:00Z',
          },
          isAuthenticated: true,
          isLoading: false,
        },
        ui: {
          theme: 'dark',
          sidebarOpen: true,
          notifications: [
            { id: '1', type: 'info', title: 'Info', message: 'Test' },
            { id: '2', type: 'warning', title: 'Warning', message: 'Test' },
          ]
        }
      }

      const store = createMockStore(complexState)
      const wrapper = createWrapper(store)

      const { result } = renderHook(
        () => ({
          user: useAppSelector(state => state.auth.user),
          isAuthenticated: useAppSelector(state => state.auth.isAuthenticated),
          theme: useAppSelector(state => state.ui.theme),
          notifications: useAppSelector(state => state.ui.notifications),
        }),
        { wrapper }
      )

      expect(result.current.user?.name).toBe('Complex User')
      expect(result.current.isAuthenticated).toBe(true)
      expect(result.current.theme).toBe('dark')
      expect(result.current.notifications).toHaveLength(2)
    })
  })
})
