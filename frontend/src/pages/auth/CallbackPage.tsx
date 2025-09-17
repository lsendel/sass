import React, { useEffect, useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../../store/hooks'
import {
  setCredentials,
  setError,
  selectIsAuthenticated,
} from '../../store/slices/authSlice'
import { useHandleCallbackMutation } from '../../store/api/authApi'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { ExclamationTriangleIcon } from '@heroicons/react/24/outline'
import { parseApiError } from '../../utils/apiError'
import { logger } from '../../utils/logger'

const CallbackPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const dispatch = useAppDispatch()
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const [handleCallback] = useHandleCallbackMutation()
  const [error, setLocalError] = useState<string | null>(null)

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get('code')
      const state = searchParams.get('state')
      const error = searchParams.get('error')
      const errorDescription = searchParams.get('error_description')

      // Handle OAuth errors
      if (error) {
        const message = errorDescription || `Authentication failed: ${error}`
        setLocalError(message)
        dispatch(setError(message))
        return
      }

      // Handle missing authorization code
      if (!code) {
        const message = 'Authorization code not found in callback'
        setLocalError(message)
        dispatch(setError(message))
        return
      }

      try {
        // Exchange code for user session
        const result = await handleCallback({
          code,
          state: state || undefined,
        }).unwrap()

        // Set credentials (token handled via httpOnly cookie from server)
        dispatch(
          setCredentials({
            user: result.user,
          })
        )
      } catch (err) {
        const parsed = parseApiError(err)
        logger.error('Callback processing failed:', parsed)
        const message =
          parsed.message || 'Authentication failed. Please try again.'
        setLocalError(message)
        dispatch(setError(message))
      }
    }

    if (!isAuthenticated) {
      processCallback()
    }
  }, [searchParams, handleCallback, dispatch, isAuthenticated])

  // Redirect to dashboard if authenticated
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  // Show error state
  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
        <div className="sm:mx-auto sm:w-full sm:max-w-md">
          <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
            <div className="flex items-center justify-center w-12 h-12 mx-auto bg-red-100 rounded-full">
              <ExclamationTriangleIcon className="w-6 h-6 text-red-600" />
            </div>

            <div className="mt-4 text-center">
              <h3 className="text-lg font-medium text-gray-900">
                Authentication Failed
              </h3>
              <p className="mt-2 text-sm text-gray-600">{error}</p>
            </div>

            <div className="mt-6">
              <button
                onClick={() => (window.location.href = '/auth/login')}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Try Again
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // Show loading state
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <div className="text-center">
            <LoadingSpinner size="lg" className="mx-auto" />
            <h3 className="mt-4 text-lg font-medium text-gray-900">
              Completing sign in...
            </h3>
            <p className="mt-2 text-sm text-gray-600">
              Please wait while we complete your authentication.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default CallbackPage
