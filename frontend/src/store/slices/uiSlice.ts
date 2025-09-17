import { createSlice, PayloadAction } from '@reduxjs/toolkit'

export type Theme = 'light' | 'dark'

export type Notification = {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  duration?: number
  actions?: Array<{
    label: string
    action: () => void
  }>
}

export type UiState = {
  theme: Theme
  sidebarOpen: boolean
  loading: {
    global: boolean
    components: Record<string, boolean>
  }
  notifications: Notification[]
  modals: {
    isPaymentMethodModalOpen: boolean
    isSubscriptionModalOpen: boolean
    isInviteUserModalOpen: boolean
  }
}

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

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setTheme: (state, action: PayloadAction<Theme>) => {
      state.theme = action.payload
      // Persist theme to localStorage
      localStorage.setItem('theme', action.payload)
    },
    toggleSidebar: state => {
      state.sidebarOpen = !state.sidebarOpen
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload
    },
    setGlobalLoading: (state, action: PayloadAction<boolean>) => {
      state.loading.global = action.payload
    },
    setComponentLoading: (
      state,
      action: PayloadAction<{ component: string; loading: boolean }>
    ) => {
      state.loading.components[action.payload.component] =
        action.payload.loading
    },
    addNotification: (
      state,
      action: PayloadAction<Omit<Notification, 'id'>>
    ) => {
      const notification: Notification = {
        ...action.payload,
        id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
      }
      state.notifications.push(notification)
    },
    removeNotification: (state, action: PayloadAction<string>) => {
      state.notifications = state.notifications.filter(
        n => n.id !== action.payload
      )
    },
    clearNotifications: state => {
      state.notifications = []
    },
    openModal: (state, action: PayloadAction<keyof UiState['modals']>) => {
      state.modals[action.payload] = true
    },
    closeModal: (state, action: PayloadAction<keyof UiState['modals']>) => {
      state.modals[action.payload] = false
    },
    closeAllModals: state => {
      Object.keys(state.modals).forEach(key => {
        state.modals[key as keyof UiState['modals']] = false
      })
    },
  },
})

export const {
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
} = uiSlice.actions

export default uiSlice.reducer

// Selectors
export const selectTheme = (state: { ui: UiState }) => state.ui.theme
export const selectSidebarOpen = (state: { ui: UiState }) =>
  state.ui.sidebarOpen
export const selectGlobalLoading = (state: { ui: UiState }) =>
  state.ui.loading.global
export const selectComponentLoading =
  (component: string) => (state: { ui: UiState }) =>
    state.ui.loading.components[component] || false
export const selectNotifications = (state: { ui: UiState }) =>
  state.ui.notifications
export const selectModals = (state: { ui: UiState }) => state.ui.modals
