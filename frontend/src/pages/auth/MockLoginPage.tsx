import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAppDispatch } from '../../store/hooks'
import { setCredentials } from '../../store/slices/authSlice'
import LoadingSpinner from '../../components/ui/LoadingSpinner'

const MockLoginPage: React.FC = () => {
  const [email, setEmail] = useState('demo@example.com')
  const [password, setPassword] = useState('DemoPassword123!')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const navigate = useNavigate()
  const dispatch = useAppDispatch()

  const handleMockLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)

    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1000))

    if (email === 'demo@example.com' && password === 'DemoPassword123!') {
      // Create mock user data
      const mockUser = {
        id: '123e4567-e89b-12d3-a456-426614174000',
        email: 'demo@example.com',
        emailVerified: true,
        role: 'ADMIN' as const,
        firstName: 'Demo',
        lastName: 'User',
        organizationId: 'b48e719b-3116-423e-b114-c9791e296a8d',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        lastLoginAt: '2024-01-01T00:00:00Z'
      }

      const mockToken = 'mock-jwt-token-' + Date.now()

      // Update Redux store
      dispatch(setCredentials({
        user: mockUser,
        token: mockToken
      }))

      // Store in localStorage for persistence
      localStorage.setItem('auth_token', mockToken)
      localStorage.setItem('auth_user', JSON.stringify(mockUser))

      setIsLoading(false)

      // Navigate to dashboard
      navigate('/dashboard')
    } else {
      setError('Invalid credentials. Use demo@example.com / DemoPassword123!')
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Animated gradient background */}
      <div className="absolute inset-0 bg-gradient-to-br from-indigo-900 via-purple-900 to-pink-900 bg-[length:400%_400%] animate-gradient-x"></div>

      {/* Floating shapes for visual interest */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute top-20 left-20 w-64 h-64 bg-white bg-opacity-5 rounded-full blur-xl animate-float"></div>
        <div className="absolute top-40 right-20 w-96 h-96 bg-purple-400 bg-opacity-10 rounded-full blur-2xl animate-float-delayed"></div>
        <div className="absolute bottom-20 left-40 w-80 h-80 bg-pink-400 bg-opacity-10 rounded-full blur-xl animate-float-slow"></div>
      </div>

      <div className="relative min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-sm w-full">
          {/* Main card with glassmorphism effect */}
          <div className="bg-white bg-opacity-10 backdrop-blur-lg rounded-2xl p-6 shadow-2xl border border-white border-opacity-20">
            {/* Header */}
            <div className="text-center mb-8">
              <div className="flex justify-center mb-4">
                <div className="w-12 h-12 bg-gradient-to-tr from-indigo-400 to-purple-500 rounded-xl flex items-center justify-center shadow-lg">
                  <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                </div>
              </div>
              <h1 className="text-2xl font-bold text-white mb-1">
                Payment Platform
              </h1>
              <p className="text-white text-opacity-70 text-sm mb-3">
                Secure subscription management for your business
              </p>

              {/* Mock login badge */}
              <div className="inline-flex items-center px-2 py-1 rounded-full bg-green-500 bg-opacity-15 border border-green-400 border-opacity-20">
                <div className="w-1.5 h-1.5 bg-green-400 rounded-full mr-1.5 animate-pulse"></div>
                <span className="text-green-100 text-xs font-medium">Demo Mode</span>
              </div>
            </div>

            <form onSubmit={handleMockLogin} className="space-y-4">
              {/* Email input */}
              <div className="space-y-2">
                <label htmlFor="email-address" className="text-white text-sm font-medium block">
                  Email Address
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg className="h-4 w-4 text-white text-opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 12a4 4 0 10-8 0 4 4 0 008 0zm0 0v1.5a2.5 2.5 0 005 0V12a9 9 0 10-9 9m4.5-1.206a8.959 8.959 0 01-4.5 1.207" />
                    </svg>
                  </div>
                  <input
                    id="email-address"
                    name="email"
                    type="email"
                    autoComplete="email"
                    required
                    data-testid="email-input"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full pl-9 pr-4 py-2.5 bg-white bg-opacity-10 border border-white border-opacity-20 rounded-lg text-white placeholder-white placeholder-opacity-50 focus:outline-none focus:ring-1 focus:ring-purple-400 focus:border-transparent transition-all duration-200 text-sm"
                    placeholder="Enter your email"
                  />
                </div>
              </div>

              {/* Password input */}
              <div className="space-y-2">
                <label htmlFor="password" className="text-white text-sm font-medium block">
                  Password
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <svg className="h-4 w-4 text-white text-opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </div>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    required
                    data-testid="password-input"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-9 pr-4 py-2.5 bg-white bg-opacity-10 border border-white border-opacity-20 rounded-lg text-white placeholder-white placeholder-opacity-50 focus:outline-none focus:ring-1 focus:ring-purple-400 focus:border-transparent transition-all duration-200 text-sm"
                    placeholder="Enter your password"
                  />
                </div>
              </div>

              {/* Error message */}
              {error && (
                <div className="bg-red-500 bg-opacity-20 border border-red-400 border-opacity-30 rounded-xl p-4">
                  <div className="flex items-center">
                    <svg className="w-5 h-5 text-red-300 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <p className="text-red-100 text-sm">{error}</p>
                  </div>
                </div>
              )}

              {/* Submit button */}
              <button
                type="submit"
                data-testid="submit-button"
                disabled={isLoading}
                className="w-full group relative py-2.5 px-4 border border-transparent text-sm font-medium rounded-lg text-white bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-600 hover:to-indigo-700 focus:outline-none focus:ring-1 focus:ring-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-md hover:shadow-lg"
              >
                {isLoading ? (
                  <div className="flex items-center justify-center">
                    <LoadingSpinner size="sm" className="mr-2" />
                    <span>Signing in...</span>
                  </div>
                ) : (
                  <div className="flex items-center justify-center">
                    <span>Sign in to Platform</span>
                    <svg className="ml-2 w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                    </svg>
                  </div>
                )}
              </button>

              {/* Demo credentials info */}
              <div className="bg-white bg-opacity-5 border border-white border-opacity-10 rounded-lg p-3 text-center">
                <h3 className="text-white text-xs font-medium mb-1.5">Demo Credentials</h3>
                <div className="space-y-0.5">
                  <p className="text-white text-opacity-70 text-xs font-mono">demo@example.com</p>
                  <p className="text-white text-opacity-70 text-xs font-mono">DemoPassword123!</p>
                </div>
                <button
                  type="button"
                  data-testid="login-button"
                  onClick={() => {
                    setEmail('demo@example.com')
                    setPassword('DemoPassword123!')
                  }}
                  className="mt-2 text-xs text-white text-opacity-70 hover:text-opacity-100 underline transition-opacity"
                >
                  Click to use demo credentials
                </button>
              </div>
            </form>
          </div>

          {/* Footer */}
          <div className="text-center mt-8">
            <p className="text-white text-opacity-60 text-sm">
              © 2024 Payment Platform. All rights reserved.
            </p>
          </div>
        </div>
      </div>

    </div>
  )
}

export default MockLoginPage
