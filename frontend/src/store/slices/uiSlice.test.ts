import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

import uiReducer, {
  setTheme,
  toggleSidebar,
  setSidebarOpen,
  setGlobalLoading,
  setComponentLoading,
  addNotification,
  removeNotification,
  clearNotifications,
  openModal,
  closeModal,
  closeAllModals,
  selectTheme,
  selectSidebarOpen,
  selectGlobalLoading,
  selectComponentLoading,
  selectNotifications,
  selectModals,
  type UiState,
  type Theme,
  type Notification
} from './uiSlice'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

describe('uiSlice', () => {
  const initialState: UiState = {
    theme: 'light',
    sidebarOpen: false,
    loading: {
      global: false,
      components: {},
    },
    notifications: [],
    modals: {
      isPaymentMethodModalOpen: false,
      isSubscriptionModalOpen: false,
      isInviteUserModalOpen: false,
    },
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('initial state', () => {
    it('should return the initial state', () => {
      expect(uiReducer(undefined, { type: 'unknown' })).toEqual(initialState)
    })
  })

  describe('theme actions', () => {
    it('should handle setTheme to dark', () => {
      const actual = uiReducer(initialState, setTheme('dark'))
      expect(actual.theme).toBe('dark')
      expect(localStorage.setItem).toHaveBeenCalledWith('theme', 'dark')
    })

    it('should handle setTheme to light', () => {
      const darkState = { ...initialState, theme: 'dark' as Theme }
      const actual = uiReducer(darkState, setTheme('light'))
      expect(actual.theme).toBe('light')
      expect(localStorage.setItem).toHaveBeenCalledWith('theme', 'light')
    })
  })

  describe('sidebar actions', () => {
    it('should handle toggleSidebar from closed', () => {
      const actual = uiReducer(initialState, toggleSidebar())
      expect(actual.sidebarOpen).toBe(true)
    })

    it('should handle toggleSidebar from open', () => {
      const openState = { ...initialState, sidebarOpen: true }
      const actual = uiReducer(openState, toggleSidebar())
      expect(actual.sidebarOpen).toBe(false)
    })

    it('should handle setSidebarOpen true', () => {
      const actual = uiReducer(initialState, setSidebarOpen(true))
      expect(actual.sidebarOpen).toBe(true)
    })

    it('should handle setSidebarOpen false', () => {
      const openState = { ...initialState, sidebarOpen: true }
      const actual = uiReducer(openState, setSidebarOpen(false))
      expect(actual.sidebarOpen).toBe(false)
    })
  })

  describe('loading actions', () => {
    it('should handle setGlobalLoading true', () => {
      const actual = uiReducer(initialState, setGlobalLoading(true))
      expect(actual.loading.global).toBe(true)
    })

    it('should handle setGlobalLoading false', () => {
      const loadingState = {
        ...initialState,
        loading: { ...initialState.loading, global: true }
      }
      const actual = uiReducer(loadingState, setGlobalLoading(false))
      expect(actual.loading.global).toBe(false)
    })

    it('should handle setComponentLoading', () => {
      const actual = uiReducer(initialState, setComponentLoading({
        component: 'userProfile',
        loading: true
      }))
      expect(actual.loading.components.userProfile).toBe(true)
    })

    it('should handle multiple component loading states', () => {
      let state = uiReducer(initialState, setComponentLoading({
        component: 'userProfile',
        loading: true
      }))
      state = uiReducer(state, setComponentLoading({
        component: 'paymentForm',
        loading: true
      }))
      state = uiReducer(state, setComponentLoading({
        component: 'userProfile',
        loading: false
      }))

      expect(state.loading.components.userProfile).toBe(false)
      expect(state.loading.components.paymentForm).toBe(true)
    })
  })

  describe('notification actions', () => {
    it('should handle addNotification', () => {
      const notification = {
        type: 'success' as const,
        title: 'Success',
        message: 'Operation completed',
        duration: 5000
      }

      const actual = uiReducer(initialState, addNotification(notification))

      expect(actual.notifications).toHaveLength(1)
      expect(actual.notifications[0]).toMatchObject({
        type: 'success',
        title: 'Success',
        message: 'Operation completed',
        duration: 5000
      })
      expect(actual.notifications[0].id).toBeDefined()
      expect(typeof actual.notifications[0].id).toBe('string')
    })

    it('should handle addNotification without duration', () => {
      const notification = {
        type: 'error' as const,
        title: 'Error',
        message: 'Something went wrong'
      }

      const actual = uiReducer(initialState, addNotification(notification))

      expect(actual.notifications).toHaveLength(1)
      expect(actual.notifications[0]).toMatchObject({
        type: 'error',
        title: 'Error',
        message: 'Something went wrong'
      })
      expect(actual.notifications[0].duration).toBeUndefined()
    })

    it('should handle addNotification with actions', () => {
      const mockAction = vi.fn()
      const notification = {
        type: 'warning' as const,
        title: 'Warning',
        message: 'Action required',
        actions: [{ label: 'Retry', action: mockAction }]
      }

      const actual = uiReducer(initialState, addNotification(notification))

      expect(actual.notifications).toHaveLength(1)
      expect(actual.notifications[0].actions).toHaveLength(1)
      expect(actual.notifications[0].actions![0].label).toBe('Retry')
    })

    it('should generate unique IDs for notifications', () => {
      const notification1 = { type: 'info' as const, title: 'Info 1', message: 'Message 1' }
      const notification2 = { type: 'info' as const, title: 'Info 2', message: 'Message 2' }

      let state = uiReducer(initialState, addNotification(notification1))
      state = uiReducer(state, addNotification(notification2))

      expect(state.notifications).toHaveLength(2)
      expect(state.notifications[0].id).not.toBe(state.notifications[1].id)
    })

    it('should handle removeNotification', () => {
      const stateWithNotifications: UiState = {
        ...initialState,
        notifications: [
          { id: '1', type: 'success', title: 'Success', message: 'Test 1' },
          { id: '2', type: 'error', title: 'Error', message: 'Test 2' },
          { id: '3', type: 'info', title: 'Info', message: 'Test 3' }
        ]
      }

      const actual = uiReducer(stateWithNotifications, removeNotification('2'))

      expect(actual.notifications).toHaveLength(2)
      expect(actual.notifications.find(n => n.id === '2')).toBeUndefined()
      expect(actual.notifications.find(n => n.id === '1')).toBeDefined()
      expect(actual.notifications.find(n => n.id === '3')).toBeDefined()
    })

    it('should handle clearNotifications', () => {
      const stateWithNotifications: UiState = {
        ...initialState,
        notifications: [
          { id: '1', type: 'success', title: 'Success', message: 'Test 1' },
          { id: '2', type: 'error', title: 'Error', message: 'Test 2' }
        ]
      }

      const actual = uiReducer(stateWithNotifications, clearNotifications())
      expect(actual.notifications).toHaveLength(0)
    })
  })

  describe('modal actions', () => {
    it('should handle openModal', () => {
      const actual = uiReducer(initialState, openModal('isPaymentMethodModalOpen'))
      expect(actual.modals.isPaymentMethodModalOpen).toBe(true)
      expect(actual.modals.isSubscriptionModalOpen).toBe(false)
      expect(actual.modals.isInviteUserModalOpen).toBe(false)
    })

    it('should handle multiple openModal calls', () => {
      let state = uiReducer(initialState, openModal('isPaymentMethodModalOpen'))
      state = uiReducer(state, openModal('isSubscriptionModalOpen'))

      expect(state.modals.isPaymentMethodModalOpen).toBe(true)
      expect(state.modals.isSubscriptionModalOpen).toBe(true)
      expect(state.modals.isInviteUserModalOpen).toBe(false)
    })

    it('should handle closeModal', () => {
      const stateWithOpenModals: UiState = {
        ...initialState,
        modals: {
          isPaymentMethodModalOpen: true,
          isSubscriptionModalOpen: true,
          isInviteUserModalOpen: true
        }
      }

      const actual = uiReducer(stateWithOpenModals, closeModal('isPaymentMethodModalOpen'))
      expect(actual.modals.isPaymentMethodModalOpen).toBe(false)
      expect(actual.modals.isSubscriptionModalOpen).toBe(true)
      expect(actual.modals.isInviteUserModalOpen).toBe(true)
    })

    it('should handle closeAllModals', () => {
      const stateWithOpenModals: UiState = {
        ...initialState,
        modals: {
          isPaymentMethodModalOpen: true,
          isSubscriptionModalOpen: true,
          isInviteUserModalOpen: true
        }
      }

      const actual = uiReducer(stateWithOpenModals, closeAllModals())
      expect(actual.modals.isPaymentMethodModalOpen).toBe(false)
      expect(actual.modals.isSubscriptionModalOpen).toBe(false)
      expect(actual.modals.isInviteUserModalOpen).toBe(false)
    })
  })

  describe('selectors', () => {
    const mockState = {
      ui: {
        theme: 'dark' as Theme,
        sidebarOpen: true,
        loading: {
          global: true,
          components: {
            userProfile: true,
            paymentForm: false
          }
        },
        notifications: [
          { id: '1', type: 'success', title: 'Success', message: 'Test' }
        ] as Notification[],
        modals: {
          isPaymentMethodModalOpen: true,
          isSubscriptionModalOpen: false,
          isInviteUserModalOpen: false
        }
      }
    }

    it('should select theme', () => {
      expect(selectTheme(mockState)).toBe('dark')
    })

    it('should select sidebar open state', () => {
      expect(selectSidebarOpen(mockState)).toBe(true)
    })

    it('should select global loading state', () => {
      expect(selectGlobalLoading(mockState)).toBe(true)
    })

    it('should select component loading state', () => {
      expect(selectComponentLoading('userProfile')(mockState)).toBe(true)
      expect(selectComponentLoading('paymentForm')(mockState)).toBe(false)
      expect(selectComponentLoading('nonExistent')(mockState)).toBe(false)
    })

    it('should select notifications', () => {
      const notifications = selectNotifications(mockState)
      expect(notifications).toHaveLength(1)
      expect(notifications[0].title).toBe('Success')
    })

    it('should select modals', () => {
      const modals = selectModals(mockState)
      expect(modals.isPaymentMethodModalOpen).toBe(true)
      expect(modals.isSubscriptionModalOpen).toBe(false)
      expect(modals.isInviteUserModalOpen).toBe(false)
    })
  })
})