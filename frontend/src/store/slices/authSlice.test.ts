import { describe, it, expect } from 'vitest'
import authReducer, { setCredentials, logout, setLoading } from './authSlice'

describe('authSlice', () => {
  const initialState = {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
  }

  it('should return the initial state', () => {
    expect(authReducer(undefined, { type: 'unknown' })).toEqual(initialState)
  })

  it('should handle setCredentials', () => {
    const user = {
      id: '123',
      name: 'Test User',
      email: 'test@example.com',
      provider: 'google',
      preferences: {},
      createdAt: '2024-01-01T00:00:00Z',
      lastActiveAt: null,
    }
    const token = 'test-token'

    const actual = authReducer(initialState, setCredentials({ user, token }))
    expect(actual.user).toEqual(user)
    expect(actual.token).toEqual(token)
    expect(actual.isAuthenticated).toBe(true)
    expect(actual.error).toBe(null)
  })

  it('should handle logout', () => {
    const authenticatedState = {
      user: {
        id: '123',
        name: 'Test User',
        email: 'test@example.com',
        provider: 'google',
        preferences: {},
        createdAt: '2024-01-01T00:00:00Z',
        lastActiveAt: null,
      },
      token: 'test-token',
      isAuthenticated: true,
      isLoading: false,
      error: null,
    }

    const actual = authReducer(authenticatedState, logout())
    expect(actual.user).toBe(null)
    expect(actual.token).toBe(null)
    expect(actual.isAuthenticated).toBe(false)
    expect(actual.error).toBe(null)
  })

  it('should handle setLoading', () => {
    const actual = authReducer(initialState, setLoading(true))
    expect(actual.isLoading).toBe(true)
    expect(actual.user).toBe(null)
    expect(actual.isAuthenticated).toBe(false)
  })

  it('should handle setLoading false', () => {
    const loadingState = {
      ...initialState,
      isLoading: true,
    }

    const actual = authReducer(loadingState, setLoading(false))
    expect(actual.isLoading).toBe(false)
  })
})