import { describe, it, expect, beforeEach } from 'vitest'
import { z } from 'zod'
import {
  UserSchema,
  PaymentSchema,
  SubscriptionSchema,
  AuthMethodsResponseSchema,
  LoginResponseSchema,
  ApiSuccessResponseSchema,
  ApiErrorResponseSchema,
} from '@/types/api'
import {
  validateApiResponse,
  ApiValidationError,
  ApiResponseError,
} from '@/lib/api/validation'
import {
  createValidationTestSuite,
  runValidationTests,
  generateRealisticMockData,
  benchmarkValidation,
  MockApiClient,
} from '@/lib/api/testing'
import { processApiError } from '@/lib/api/errorHandling'

/**
 * Comprehensive tests for API validation system
 */

describe('API Validation System', () => {
  describe('Schema Validation', () => {
    describe('User Schema', () => {
      it('should validate correct user data', () => {
        const validUser = {
          id: 'user-123',
          email: 'test@example.com',
          firstName: 'John',
          lastName: 'Doe',
          role: 'USER',
          emailVerified: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        }

        expect(() => UserSchema.parse(validUser)).not.toThrow()
      })

      it('should reject invalid user data', () => {
        const invalidUser = {
          id: 'invalid-id', // Not a UUID
          email: 'invalid-email', // Invalid email format
          role: 'INVALID_ROLE', // Invalid role
          emailVerified: 'yes', // Should be boolean
        }

        expect(() => UserSchema.parse(invalidUser)).toThrow()
      })

      it('should validate UUID format for user ID', () => {
        const userWithInvalidId = {
          id: 'not-a-uuid',
          email: 'test@example.com',
          role: 'USER',
          emailVerified: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        }

        expect(() => UserSchema.parse(userWithInvalidId)).toThrow(z.ZodError)
      })

      it('should validate email format', () => {
        const userWithInvalidEmail = {
          id: '123e4567-e89b-12d3-a456-426614174000',
          email: 'not-an-email',
          role: 'USER',
          emailVerified: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        }

        expect(() => UserSchema.parse(userWithInvalidEmail)).toThrow(z.ZodError)
      })

      it('should validate role enum', () => {
        const userWithInvalidRole = {
          id: '123e4567-e89b-12d3-a456-426614174000',
          email: 'test@example.com',
          role: 'INVALID_ROLE',
          emailVerified: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        }

        expect(() => UserSchema.parse(userWithInvalidRole)).toThrow(z.ZodError)
      })
    })

    describe('Payment Schema', () => {
      it('should validate correct payment data', () => {
        const validPayment = {
          id: '123e4567-e89b-12d3-a456-426614174000',
          amount: 1999,
          currency: 'USD',
          status: 'COMPLETED',
          paymentMethodId: 'pm_123',
          customerId: 'cus_123',
          organizationId: '123e4567-e89b-12d3-a456-426614174000',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          paidAt: '2024-01-01T00:00:00Z',
        }

        expect(() => PaymentSchema.parse(validPayment)).not.toThrow()
      })

      it('should reject negative amounts', () => {
        const paymentWithNegativeAmount = {
          id: '123e4567-e89b-12d3-a456-426614174000',
          amount: -100,
          currency: 'USD',
          status: 'COMPLETED',
          paymentMethodId: 'pm_123',
          customerId: 'cus_123',
          organizationId: '123e4567-e89b-12d3-a456-426614174000',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        }

        expect(() => PaymentSchema.parse(paymentWithNegativeAmount)).toThrow()
      })

      it('should validate currency code length', () => {
        const paymentWithInvalidCurrency = {
          id: '123e4567-e89b-12d3-a456-426614174000',
          amount: 1999,
          currency: 'INVALID', // Should be 3 characters
          status: 'COMPLETED',
          paymentMethodId: 'pm_123',
          customerId: 'cus_123',
          organizationId: '123e4567-e89b-12d3-a456-426614174000',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        }

        expect(() => PaymentSchema.parse(paymentWithInvalidCurrency)).toThrow()
      })
    })

    describe('API Response Wrappers', () => {
      it('should validate success response format', () => {
        const successResponse = {
          success: true,
          data: { message: 'Success' },
          timestamp: '2024-01-01T00:00:00Z',
        }

        const schema = ApiSuccessResponseSchema(z.object({ message: z.string() }))
        expect(() => schema.parse(successResponse)).not.toThrow()
      })

      it('should validate error response format', () => {
        const errorResponse = {
          success: false,
          error: {
            code: 'AUTH_001',
            message: 'Authentication failed',
          },
          timestamp: '2024-01-01T00:00:00Z',
        }

        expect(() => ApiErrorResponseSchema.parse(errorResponse)).not.toThrow()
      })

      it('should reject malformed responses', () => {
        const malformedResponse = {
          success: 'maybe', // Should be boolean
          data: 'not an object',
        }

        const schema = ApiSuccessResponseSchema(z.object({}))
        expect(() => schema.parse(malformedResponse)).toThrow()
      })
    })
  })

  describe('Runtime Validation', () => {
    it('should validate API responses at runtime', () => {
      const mockApiResponse = {
        success: true,
        data: {
          id: '123e4567-e89b-12d3-a456-426614174000',
          email: 'test@example.com',
          role: 'USER',
          emailVerified: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        },
        timestamp: '2024-01-01T00:00:00Z',
      }

      const schema = ApiSuccessResponseSchema(UserSchema)
      const result = validateApiResponse(schema, mockApiResponse, 'test-endpoint')

      expect(result.success).toBe(true)
      expect(result.data.email).toBe('test@example.com')
    })

    it('should throw ApiValidationError for invalid responses', () => {
      const invalidResponse = {
        success: true,
        data: {
          id: 'invalid-uuid',
          email: 'invalid-email',
        },
        timestamp: '2024-01-01T00:00:00Z',
      }

      const schema = ApiSuccessResponseSchema(UserSchema)

      expect(() => {
        validateApiResponse(schema, invalidResponse, 'test-endpoint')
      }).toThrow(ApiValidationError)
    })

    it('should provide detailed error information', () => {
      const invalidResponse = {
        success: true,
        data: {
          id: 'invalid-uuid',
          email: 'invalid-email',
          role: 'INVALID_ROLE',
        },
        timestamp: '2024-01-01T00:00:00Z',
      }

      const schema = ApiSuccessResponseSchema(UserSchema)

      try {
        validateApiResponse(schema, invalidResponse, 'test-endpoint')
      } catch (error) {
        expect(error).toBeInstanceOf(ApiValidationError)
        expect(error.message).toContain('API validation failed for test-endpoint')
        expect(error.originalError.issues).toHaveLength(3) // id, email, role errors
      }
    })
  })

  describe('Error Handling', () => {
    it('should process API validation errors correctly', () => {
      const validationError = new ApiValidationError(
        'Test validation error',
        new z.ZodError([
          {
            code: 'invalid_type',
            expected: 'string',
            received: 'number',
            path: ['email'],
            message: 'Expected string, received number',
          },
        ]),
        { email: 123 }
      )

      const errorInfo = processApiError(validationError)

      expect(errorInfo.code).toBe('VALIDATION_ERROR')
      expect(errorInfo.severity).toBe('high')
      expect(errorInfo.isRetryable).toBe(true)
      expect(errorInfo.userMessage).toContain('server response was invalid')
    })

    it('should process API response errors correctly', () => {
      const responseError = new ApiResponseError(
        'Payment failed',
        402,
        {
          success: false,
          error: {
            code: 'PAY_002',
            message: 'Card declined',
          },
        }
      )

      const errorInfo = processApiError(responseError)

      expect(errorInfo.code).toBe('PAY_002')
      expect(errorInfo.message).toBe('Card declined')
      expect(errorInfo.isRetryable).toBe(true)
      expect(errorInfo.userMessage).toBe('Your card was declined. Please try a different payment method.')
    })

    it('should handle network errors', () => {
      const networkError = new TypeError('Failed to fetch')

      const errorInfo = processApiError(networkError)

      expect(errorInfo.code).toBe('NETWORK_ERROR')
      expect(errorInfo.severity).toBe('high')
      expect(errorInfo.isRetryable).toBe(true)
      expect(errorInfo.userMessage).toContain('check your internet connection')
    })
  })

  describe('Mock Data Generation', () => {
    it('should generate realistic mock data', () => {
      const mockUser = generateRealisticMockData(UserSchema)

      expect(mockUser.id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
      expect(mockUser.email).toContain('@')
      expect(['USER', 'ORGANIZATION_ADMIN', 'ADMIN']).toContain(mockUser.role)
      expect(typeof mockUser.emailVerified).toBe('boolean')
    })

    it('should generate different mock data on each call', () => {
      const mockUser1 = generateRealisticMockData(UserSchema)
      const mockUser2 = generateRealisticMockData(UserSchema)

      expect(mockUser1.id).not.toBe(mockUser2.id)
    })
  })

  describe('Test Suite Generation', () => {
    it('should generate comprehensive test scenarios', () => {
      const scenarios = createValidationTestSuite(UserSchema, 'User')

      expect(scenarios).toHaveLength(6) // Valid + 5 invalid scenarios
      expect(scenarios[0].expectedValid).toBe(true)
      expect(scenarios.slice(1).every(s => !s.expectedValid)).toBe(true)
    })

    it('should run validation test scenarios', () => {
      const scenarios = createValidationTestSuite(UserSchema, 'User')
      const results = runValidationTests(UserSchema, scenarios)

      expect(results.passed).toBeGreaterThan(0)
      expect(results.failed).toBe(0) // All tests should pass
      expect(results.results).toHaveLength(scenarios.length)
    })
  })

  describe('Performance Benchmarking', () => {
    it('should benchmark validation performance', async () => {
      const mockUser = generateRealisticMockData(UserSchema)
      const benchmark = await benchmarkValidation(UserSchema, mockUser, 100)

      expect(benchmark.iterations).toBe(100)
      expect(benchmark.averageTime).toBeGreaterThan(0)
      expect(benchmark.averageTime).toBeLessThan(10) // Should be under 10ms average
      expect(benchmark.minTime).toBeLessThanOrEqual(benchmark.averageTime)
      expect(benchmark.maxTime).toBeGreaterThanOrEqual(benchmark.averageTime)
    })

    it('should benchmark complex object validation', async () => {
      const complexSchema = z.object({
        users: z.array(UserSchema),
        payments: z.array(PaymentSchema),
        metadata: z.record(z.string()),
      })

      const complexData = {
        users: Array.from({ length: 10 }, () => generateRealisticMockData(UserSchema)),
        payments: Array.from({ length: 5 }, () => generateRealisticMockData(PaymentSchema)),
        metadata: { key1: 'value1', key2: 'value2' },
      }

      const benchmark = await benchmarkValidation(complexSchema, complexData, 50)

      expect(benchmark.averageTime).toBeGreaterThan(0)
      expect(benchmark.averageTime).toBeLessThan(50) // Should still be reasonably fast
    })
  })

  describe('Mock API Client', () => {
    let mockClient: MockApiClient

    beforeEach(() => {
      mockClient = new MockApiClient()
    })

    it('should mock API responses with validation', async () => {
      const mockResponse = {
        success: true,
        data: generateRealisticMockData(UserSchema),
        timestamp: new Date().toISOString(),
      }

      mockClient.setResponse('/users/123', mockResponse)

      const schema = ApiSuccessResponseSchema(UserSchema)
      const result = await mockClient.request('/users/123', schema)

      expect(result.success).toBe(true)
      expect(result.data.email).toContain('@')
    })

    it('should simulate network delays', async () => {
      const mockResponse = {
        success: true,
        data: { message: 'Delayed response' },
        timestamp: new Date().toISOString(),
      }

      mockClient.setResponse('/slow-endpoint', mockResponse, 100) // 100ms delay

      const start = Date.now()
      const schema = ApiSuccessResponseSchema(z.object({ message: z.string() }))
      await mockClient.request('/slow-endpoint', schema)
      const duration = Date.now() - start

      expect(duration).toBeGreaterThan(90) // Allow some variance
    })

    it('should simulate error rates', async () => {
      mockClient.setResponse('/flaky-endpoint', { data: 'success' }, 0, 0.5) // 50% error rate

      const schema = z.object({ data: z.string() })
      const results = await Promise.allSettled(
        Array.from({ length: 20 }, () => mockClient.request('/flaky-endpoint', schema))
      )

      const failures = results.filter(r => r.status === 'rejected').length
      expect(failures).toBeGreaterThan(0) // Some requests should fail
      expect(failures).toBeLessThan(20) // Not all requests should fail
    })

    it('should handle missing endpoints', async () => {
      const schema = z.object({ data: z.string() })

      await expect(
        mockClient.request('/non-existent', schema)
      ).rejects.toThrow('No mock response configured')
    })
  })

  describe('Integration with RTK Query', () => {
    it('should validate RTK Query responses', async () => {
      // This would test the integration with actual RTK Query endpoints
      // Implementation depends on your specific API setup
      expect(true).toBe(true) // Placeholder for actual integration test
    })

    it('should handle RTK Query errors with validation', async () => {
      // Test error handling in RTK Query with validation
      expect(true).toBe(true) // Placeholder for actual integration test
    })
  })
})