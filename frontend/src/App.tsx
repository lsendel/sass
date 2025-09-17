import React, { useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { useAppDispatch, useAppSelector } from './store/hooks'
import {
  setCredentials,
  selectIsAuthenticated,
  selectAuthLoading,
} from './store/slices/authSlice'
import { useGetSessionQuery } from './store/api/authApi'
import AuthLayout from './components/layouts/AuthLayout'
import DashboardLayout from './components/layouts/DashboardLayout'
import LoginPage from './pages/auth/LoginPage'
import CallbackPage from './pages/auth/CallbackPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import OrganizationsPage from './pages/organizations/OrganizationsPage'
import OrganizationPage from './pages/organizations/OrganizationPage'
import PaymentsPage from './pages/payments/PaymentsPage'
import SubscriptionPage from './pages/subscription/SubscriptionPage'
import SettingsPage from './pages/settings/SettingsPage'
import LoadingSpinner from './components/ui/LoadingSpinner'
import ErrorBoundary from './components/ui/ErrorBoundary'

const AppContent: React.FC = () => {
  const dispatch = useAppDispatch()
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const authLoading = useAppSelector(selectAuthLoading)

  // Try to restore session on app load
  const { data: sessionData } = useGetSessionQuery(undefined, {
    skip: isAuthenticated,
  })

  useEffect(() => {
    if (sessionData) {
      dispatch(
        setCredentials({
          user: sessionData.user,
          token: localStorage.getItem('auth-token') || '',
        })
      )
    }
  }, [sessionData, dispatch])

  // Show loading spinner while checking authentication
  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <Routes>
      {/* Public routes */}
      <Route path="/auth" element={<AuthLayout />}>
        <Route path="login" element={<LoginPage />} />
        <Route path="callback" element={<CallbackPage />} />
        <Route index element={<Navigate to="/auth/login" replace />} />
      </Route>

      {/* Protected routes */}
      <Route
        path="/*"
        element={
          isAuthenticated ? (
            <DashboardLayout />
          ) : (
            <Navigate to="/auth/login" replace />
          )
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="organizations" element={<OrganizationsPage />} />
        <Route path="organizations/:slug" element={<OrganizationPage />} />
        <Route path="payments" element={<PaymentsPage />} />
        <Route path="subscription" element={<SubscriptionPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      {/* Catch all - redirect to dashboard if authenticated, login if not */}
      <Route
        path="*"
        element={
          <Navigate
            to={isAuthenticated ? '/dashboard' : '/auth/login'}
            replace
          />
        }
      />
    </Routes>
  )
}

const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <div className="App">
        <AppContent />
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#ffffff',
              color: '#374151',
              border: '1px solid #e5e7eb',
              borderRadius: '0.5rem',
              boxShadow:
                '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
            },
            success: {
              iconTheme: {
                primary: '#10b981',
                secondary: '#ffffff',
              },
            },
            error: {
              iconTheme: {
                primary: '#ef4444',
                secondary: '#ffffff',
              },
            },
          }}
        />
      </div>
    </ErrorBoundary>
  )
}

export default App
