import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline'
import { usePasswordLoginMutation } from '../../store/api/authApi'
import { useAppDispatch } from '../../store/hooks'
import { setCredentials } from '../../store/slices/authSlice'
import LoadingSpinner from '../ui/LoadingSpinner'
import { parseApiError } from '../../utils/apiError'
import { logger } from '../../utils/logger'

const passwordLoginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
})

type PasswordLoginFormData = z.infer<typeof passwordLoginSchema>

interface PasswordLoginFormProps {
  onSuccess?: () => void
  onError?: (error: string) => void
}

const PasswordLoginForm: React.FC<PasswordLoginFormProps> = ({
  onSuccess,
  onError,
}) => {
  const [showPassword, setShowPassword] = useState(false)
  const dispatch = useAppDispatch()
  const [passwordLogin, { isLoading }] = usePasswordLoginMutation()

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
  } = useForm<PasswordLoginFormData>({
    resolver: zodResolver(passwordLoginSchema),
    mode: 'onBlur',
  })

  const onSubmit = async (data: PasswordLoginFormData) => {
    try {
      const result = await passwordLogin(data).unwrap()

      // Update auth state with user and token
      dispatch(
        setCredentials({
          user: result.user,
          token: result.token,
        })
      )

      onSuccess?.()
    } catch (err) {
      const parsed = parseApiError(err)
      logger.error('Login failed:', parsed)
      if (parsed.status === 429) {
        onError?.('Too many login attempts. Please try again later.')
        return
      }
      if (parsed.status === 401) {
        setError('password', { message: 'Invalid email or password' })
        return
      }
      // Try to map backend message to field when possible
      const msg = parsed.message || 'Login failed. Please try again.'
      if (msg.toLowerCase().includes('email')) {
        setError('email', { message: msg })
      } else if (
        msg.toLowerCase().includes('password') ||
        msg.toLowerCase().includes('credentials')
      ) {
        setError('password', { message: 'Invalid email or password' })
      } else {
        onError?.(msg)
      }
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div>
        <h3 className="text-lg font-medium text-gray-900 text-center">
          Sign in to your account
        </h3>
        <p className="mt-1 text-sm text-gray-600 text-center">
          Enter your email and password to continue
        </p>
      </div>

      <div className="space-y-4">
        {/* Email Field */}
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700"
          >
            Email address
          </label>
          <div className="mt-1">
            <input
              {...register('email')}
              type="email"
              id="email"
              autoComplete="email"
              className={`appearance-none block w-full px-3 py-2 border rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:text-sm ${
                errors.email
                  ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                  : 'border-gray-300 focus:border-primary-500'
              }`}
              placeholder="you@example.com"
            />
            {errors.email && (
              <p className="mt-1 text-sm text-red-600">
                {errors.email.message}
              </p>
            )}
          </div>
        </div>

        {/* Password Field */}
        <div>
          <label
            htmlFor="password"
            className="block text-sm font-medium text-gray-700"
          >
            Password
          </label>
          <div className="mt-1 relative">
            <input
              {...register('password')}
              type={showPassword ? 'text' : 'password'}
              id="password"
              autoComplete="current-password"
              className={`appearance-none block w-full px-3 py-2 pr-10 border rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:text-sm ${
                errors.password
                  ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                  : 'border-gray-300 focus:border-primary-500'
              }`}
              placeholder="Enter your password"
            />
            <button
              type="button"
              className="absolute inset-y-0 right-0 pr-3 flex items-center"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? (
                <EyeSlashIcon className="h-5 w-5 text-gray-400 hover:text-gray-600" />
              ) : (
                <EyeIcon className="h-5 w-5 text-gray-400 hover:text-gray-600" />
              )}
            </button>
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">
                {errors.password.message}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Submit Button */}
      <div>
        <button
          type="submit"
          disabled={isLoading}
          className="w-full flex justify-center items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {isLoading ? (
            <>
              <LoadingSpinner size="sm" className="mr-2" />
              Signing in...
            </>
          ) : (
            'Sign in'
          )}
        </button>
      </div>

      {/* Footer */}
      <div className="text-xs text-gray-500 text-center space-y-1">
        <p>
          By signing in, you agree to our Terms of Service and Privacy Policy.
        </p>
        <p>Your data is encrypted and securely stored.</p>
      </div>
    </form>
  )
}

export default PasswordLoginForm
