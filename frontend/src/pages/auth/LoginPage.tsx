import React, { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useGetAuthMethodsQuery } from '../../store/api/authApi'
import { useAppSelector } from '../../store/hooks'
import { selectIsAuthenticated } from '../../store/slices/authSlice'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import PasswordLoginForm from '../../components/auth/PasswordLoginForm'

const LoginPage: React.FC = () => {
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const { data: authMethods, isLoading } = useGetAuthMethodsQuery()

  // Redirect if already authenticated
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  const handleLoginSuccess = () => {
    navigate('/dashboard')
  }

  const handleLoginError = (errorMessage: string) => {
    setError(errorMessage)
  }

  if (isLoading) {
    return (
      <div className="flex justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {error && (
        <div className="rounded-md bg-red-50 p-4">
          <div className="text-sm text-red-700">{error}</div>
        </div>
      )}

      {authMethods?.passwordAuthEnabled ? (
        <PasswordLoginForm
          onSuccess={handleLoginSuccess}
          onError={handleLoginError}
        />
      ) : (
        <div className="text-center space-y-4">
          <div>
            <h3 className="text-lg font-medium text-gray-900">
              Authentication Unavailable
            </h3>
            <p className="mt-1 text-sm text-gray-600">
              Password authentication is currently disabled. Please contact
              support.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}

export default LoginPage
