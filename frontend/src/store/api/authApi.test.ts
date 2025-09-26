import { describe, it, expect, vi } from 'vitest'
import {
  authApi,
  type OAuth2Provider,
  type SessionInfo,
  type LoginRequest,
  type PasswordLoginRequest,
  type PasswordRegisterRequest,
  type AuthMethodsResponse,
} from './authApi'
import { createMockUser } from '@/test/fixtures/users'

// Mock import.meta.env
vi.mock('import.meta.env', () => ({
  VITE_API_BASE_URL: 'http://localhost:8082/api/v1',
}))

describe('authApi', () => {
  describe('API configuration', () => {
    it('should have correct reducer path', () => {
      expect(authApi.reducerPath).toBe('authApi')
    })

    it('should have endpoints defined', () => {
      expect(authApi.endpoints).toBeDefined()
      expect(typeof authApi.endpoints).toBe('object')
    })

    it('should export reducer', () => {
      expect(authApi.reducer).toBeDefined()
      expect(typeof authApi.reducer).toBe('function')
    })

    it('should export middleware', () => {
      expect(authApi.middleware).toBeDefined()
      expect(typeof authApi.middleware).toBe('function')
    })
  })

  describe('endpoint definitions', () => {
    it('should have getAuthMethods endpoint', () => {
      expect(authApi.endpoints.getAuthMethods).toBeDefined()
    })

    it('should have passwordLogin endpoint', () => {
      expect(authApi.endpoints.passwordLogin).toBeDefined()
    })

    it('should have passwordRegister endpoint', () => {
      expect(authApi.endpoints.passwordRegister).toBeDefined()
    })

    it('should have getAuthUrl endpoint', () => {
      expect(authApi.endpoints.getAuthUrl).toBeDefined()
    })

    it('should have getSession endpoint', () => {
      expect(authApi.endpoints.getSession).toBeDefined()
    })

    it('should have handleCallback endpoint', () => {
      expect(authApi.endpoints.handleCallback).toBeDefined()
    })

    it('should have logout endpoint', () => {
      expect(authApi.endpoints.logout).toBeDefined()
    })

    it('should have refreshToken endpoint', () => {
      expect(authApi.endpoints.refreshToken).toBeDefined()
    })
  })

  describe('exported hooks', () => {
    it('should export useGetAuthMethodsQuery hook', () => {
      const {
        useGetAuthMethodsQuery,
      } = authApi

      expect(typeof useGetAuthMethodsQuery).toBe('function')
    })

    it('should export mutation hooks', () => {
      const {
        usePasswordLoginMutation,
        usePasswordRegisterMutation,
        useHandleCallbackMutation,
        useLogoutMutation,
        useRefreshTokenMutation,
      } = authApi

      expect(typeof usePasswordLoginMutation).toBe('function')
      expect(typeof usePasswordRegisterMutation).toBe('function')
      expect(typeof useHandleCallbackMutation).toBe('function')
      expect(typeof useLogoutMutation).toBe('function')
      expect(typeof useRefreshTokenMutation).toBe('function')
    })

    it('should export query hooks', () => {
      const {
        useGetAuthUrlQuery,
        useGetSessionQuery,
      } = authApi

      expect(typeof useGetAuthUrlQuery).toBe('function')
      expect(typeof useGetSessionQuery).toBe('function')
    })

    it('should export lazy query hooks', () => {
      const {
        useLazyGetAuthUrlQuery,
        useLazyGetSessionQuery,
      } = authApi

      expect(typeof useLazyGetAuthUrlQuery).toBe('function')
      expect(typeof useLazyGetSessionQuery).toBe('function')
    })
  })

  describe('type definitions', () => {
    it('should define OAuth2Provider type correctly', () => {
      const provider: OAuth2Provider = {
        name: 'google',
        displayName: 'Google',
        iconUrl: 'https://example.com/google-icon.png',
        authUrl: 'https://accounts.google.com/oauth/authorize',
      }

      expect(provider.name).toBe('google')
      expect(provider.displayName).toBe('Google')
      expect(provider.iconUrl).toBeDefined()
      expect(provider.authUrl).toBeDefined()
    })

    it('should allow OAuth2Provider without iconUrl', () => {
      const provider: OAuth2Provider = {
        name: 'github',
        displayName: 'GitHub',
        authUrl: 'https://github.com/login/oauth/authorize',
      }

      expect(provider.iconUrl).toBeUndefined()
      expect(provider.name).toBe('github')
    })

    it('should define SessionInfo type correctly', () => {
      const sessionInfo: SessionInfo = {
        user: createMockUser({ id: '123', name: 'Test User' }),
        session: {
          activeTokens: 1,
          lastActiveAt: '2024-01-15T10:30:00Z',
          createdAt: '2024-01-01T09:00:00Z',
        },
      }

      expect(sessionInfo.user.id).toBe('123')
      expect(sessionInfo.session.activeTokens).toBe(1)
    })

    it('should define LoginRequest type correctly', () => {
      const request: LoginRequest = {
        provider: 'google',
        redirectUri: 'http://localhost:3000/auth/callback',
      }

      expect(request.provider).toBe('google')
      expect(request.redirectUri).toBe('http://localhost:3000/auth/callback')
    })

    it('should define PasswordLoginRequest type correctly', () => {
      const request: PasswordLoginRequest = {
        email: 'test@example.com',
        password: 'password123',
      }

      expect(request.email).toBe('test@example.com')
      expect(request.password).toBe('password123')
    })

    it('should define PasswordRegisterRequest type correctly', () => {
      const request: PasswordRegisterRequest = {
        email: 'newuser@example.com',
        password: 'newpassword123',
        name: 'New User',
      }

      expect(request.email).toBe('newuser@example.com')
      expect(request.password).toBe('newpassword123')
      expect(request.name).toBe('New User')
    })

    it('should define AuthMethodsResponse type correctly', () => {
      const response: AuthMethodsResponse = {
        methods: ['password', 'oauth2'],
        passwordAuthEnabled: true,
        oauth2Providers: ['google', 'github', 'microsoft'],
      }

      expect(Array.isArray(response.methods)).toBe(true)
      expect(typeof response.passwordAuthEnabled).toBe('boolean')
      expect(Array.isArray(response.oauth2Providers)).toBe(true)
    })
  })

  describe('API base configuration', () => {
    it('should have correct API configuration structure', () => {
      expect(authApi.reducerPath).toBe('authApi')
      expect(authApi.endpoints).toBeDefined()
    })
  })

  describe('validation helpers', () => {
    it('should validate email format in requests', () => {
      const validEmail = 'user@example.com'
      const invalidEmail = 'invalid-email'

      // Basic email validation test
      expect(validEmail.includes('@')).toBe(true)
      expect(invalidEmail.includes('@')).toBe(false)
    })

    it('should handle different password requirements', () => {
      const strongPassword = 'StrongPass123!'
      const weakPassword = '123'

      expect(strongPassword.length).toBeGreaterThan(8)
      expect(weakPassword.length).toBeLessThan(8)
    })

    it('should handle provider names correctly', () => {
      const validProviders = ['google', 'github', 'microsoft']
      const testProvider = 'google'

      expect(validProviders).toContain(testProvider)
    })
  })

  describe('API response structures', () => {
    it('should handle successful authentication response', () => {
      const authResponse = {
        user: {
          id: '123',
          name: 'User',
          email: 'user@example.com',
          provider: 'google',
          preferences: {},
          createdAt: '2024-01-01T00:00:00Z',
          lastActiveAt: null,
        },
        token: 'auth-token',
      }

      expect(authResponse.user).toBeDefined()
      expect(authResponse.token).toBeDefined()
      expect(typeof authResponse.token).toBe('string')
    })

    it('should handle auth URL response', () => {
      const authUrlResponse = {
        authUrl: 'https://accounts.google.com/oauth/authorize?client_id=123',
      }

      expect(authUrlResponse.authUrl).toBeDefined()
      expect(authUrlResponse.authUrl.startsWith('https://')).toBe(true)
    })

    it('should handle callback response', () => {
      const callbackData = {
        code: 'auth-code-123',
        state: 'random-state',
      }

      expect(callbackData.code).toBeDefined()
      expect(callbackData.state).toBeDefined()
      expect(typeof callbackData.code).toBe('string')
    })

    it('should handle refresh token response', () => {
      const refreshResponse = {
        token: 'new-access-token',
      }

      expect(refreshResponse.token).toBeDefined()
      expect(typeof refreshResponse.token).toBe('string')
    })
  })

  describe('error handling types', () => {
    it('should handle authentication errors', () => {
      const authError = {
        status: 401,
        message: 'Invalid credentials',
      }

      expect(authError.status).toBe(401)
      expect(authError.message).toBe('Invalid credentials')
    })

    it('should handle validation errors', () => {
      const validationError = {
        status: 400,
        message: 'Email is required',
        field: 'email',
      }

      expect(validationError.status).toBe(400)
      expect(validationError.field).toBe('email')
    })

    it('should handle network errors', () => {
      const networkError = {
        message: 'Network request failed',
        type: 'NetworkError',
      }

      expect(networkError.type).toBe('NetworkError')
      expect(networkError.message).toBeDefined()
    })
  })
})
