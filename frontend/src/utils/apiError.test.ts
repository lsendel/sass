import { describe, it, expect } from 'vitest'
import { parseApiError, type ParsedApiError } from './apiError'
import type { FetchBaseQueryError, SerializedError } from '@reduxjs/toolkit/query'

describe('parseApiError', () => {
  describe('FetchBaseQueryError handling', () => {
    it('should parse error with status and message', () => {
      const error: FetchBaseQueryError = {
        status: 400,
        data: {
          message: 'Bad request error'
        }
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 400,
        message: 'Bad request error'
      })
    })

    it('should parse error with status and error field', () => {
      const error: FetchBaseQueryError = {
        status: 500,
        data: {
          error: 'Internal server error'
        }
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 500,
        message: 'Internal server error'
      })
    })

    it('should parse error with status and detail field', () => {
      const error: FetchBaseQueryError = {
        status: 422,
        data: {
          detail: 'Validation failed'
        }
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 422,
        message: 'Validation failed'
      })
    })

    it('should handle 401 error with default message', () => {
      const error: FetchBaseQueryError = {
        status: 401,
        data: {}
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 401,
        message: 'Unauthorized'
      })
    })

    it('should handle 403 error with default message', () => {
      const error: FetchBaseQueryError = {
        status: 403,
        data: {}
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 403,
        message: 'Forbidden'
      })
    })

    it('should handle non-numeric status', () => {
      const error = {
        status: 'FETCH_ERROR',
        data: {
          message: 'Network error'
        }
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: undefined,
        message: 'Network error'
      })
    })

    it('should handle non-string message types', () => {
      const error: FetchBaseQueryError = {
        status: 400,
        data: {
          message: 12345
        }
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 400,
        message: '12345'
      })
    })

    it('should fallback to default message when no data message', () => {
      const error: FetchBaseQueryError = {
        status: 500,
        data: {}
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 500,
        message: 'Request failed'
      })
    })

    it('should handle null data', () => {
      const error: FetchBaseQueryError = {
        status: 404,
        data: null
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        status: 404,
        message: 'Request failed'
      })
    })
  })

  describe('SerializedError handling', () => {
    it('should parse SerializedError with message', () => {
      const error: SerializedError = {
        name: 'TypeError',
        message: 'Something went wrong'
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Something went wrong'
      })
    })

    it('should handle SerializedError with only name', () => {
      const error: SerializedError = {
        name: 'NetworkError'
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })

    it('should handle SerializedError with empty message', () => {
      const error: SerializedError = {
        name: 'Error',
        message: ''
      }

      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })
  })

  describe('Generic error handling', () => {
    it('should handle string error', () => {
      const error = 'Simple string error'
      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })

    it('should handle null error', () => {
      const error = null
      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })

    it('should handle undefined error', () => {
      const error = undefined
      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })

    it('should handle empty object', () => {
      const error = {}
      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })

    it('should handle non-object error', () => {
      const error = 123
      const result = parseApiError(error)
      expect(result).toEqual({
        message: 'Unexpected error'
      })
    })
  })
})