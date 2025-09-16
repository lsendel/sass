import React, { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import { useGetProvidersQuery } from '../../store/api/authApi'
import { useAppSelector } from '../../store/hooks'
import { selectIsAuthenticated } from '../../store/slices/authSlice'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

const LoginPage: React.FC = () => {
  const isAuthenticated = useAppSelector(selectIsAuthenticated)
  const { data: providersData, isLoading, error } = useGetProvidersQuery()

  // Redirect if already authenticated
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  const handleProviderLogin = (provider: string) => {
    const redirectUri = `${window.location.origin}/auth/callback`
    const authUrl = `/api/v1/auth/authorize?provider=${provider}&redirect_uri=${encodeURIComponent(redirectUri)}`
    window.location.href = authUrl
  }

  if (isLoading) {
    return (
      <div className="flex justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-md bg-red-50 p-4">
        <div className="text-sm text-red-700">
          Failed to load authentication providers. Please try again later.
        </div>
      </div>
    )
  }

  const providers = providersData?.providers || []

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-medium text-gray-900 text-center">
          Sign in to your account
        </h3>
        <p className="mt-1 text-sm text-gray-600 text-center">
          Choose your preferred authentication method
        </p>
      </div>

      <div className="space-y-3">
        {providers.map((provider) => (
          <button
            key={provider.name}
            onClick={() => handleProviderLogin(provider.name)}
            className="w-full flex justify-center items-center px-4 py-3 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 transition-colors"
          >
            {provider.iconUrl && (
              <img
                src={provider.iconUrl}
                alt={`${provider.displayName} icon`}
                className="w-5 h-5 mr-3"
              />
            )}
            Continue with {provider.displayName}
          </button>
        ))}
      </div>

      {providers.length === 0 && (
        <div className="text-center text-sm text-gray-500">
          No authentication providers available
        </div>
      )}

      <div className="mt-8">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-300" />
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-white text-gray-500">
              Secure authentication powered by OAuth 2.0
            </span>
          </div>
        </div>
      </div>

      <div className="text-xs text-gray-500 text-center space-y-1">
        <p>
          By signing in, you agree to our Terms of Service and Privacy Policy.
        </p>
        <p>
          Your data is encrypted and securely stored.
        </p>
      </div>
    </div>
  )
}

export default LoginPage