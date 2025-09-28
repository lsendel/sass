import { z } from 'zod';
import type { BaseQueryFn } from '@reduxjs/toolkit/query';
import { fetchBaseQuery } from '@reduxjs/toolkit/query/react';

import { ApiErrorResponseSchema, ApiSuccessResponseSchema } from '@/types/api';

/**
 * Runtime API validation utilities using Zod
 * Provides type-safe API responses with runtime validation
 */

export class ApiValidationError extends Error {
  constructor(
    message: string,
    public readonly originalError: z.ZodError,
    public readonly response: unknown
  ) {
    super(message);
    this.name = 'ApiValidationError';
  }
}

export class ApiResponseError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly data: unknown
  ) {
    super(message);
    this.name = 'ApiResponseError';
  }
}

/**
 * Validates API response data against a Zod schema
 */
export function validateApiResponse<T>(
  schema: z.ZodSchema<T>,
  data: unknown,
  context?: string
): T {
  try {
    return schema.parse(data);
  } catch (error) {
    if (error instanceof z.ZodError) {
      const errorMessage = `API validation failed${context ? ` for ${context}` : ''}: ${error.issues
        .map(issue => `${issue.path.join('.')}: ${issue.message}`)
        .join(', ')}`;

      console.error('API Validation Error:', {
        context,
        issues: error.issues,
        data,
      });

      throw new ApiValidationError(errorMessage, error, data);
    }
    throw error;
  }
}

/**
 * Creates a validated RTK Query base query with automatic response validation
 */
export function createValidatedBaseQuery(
  baseUrl: string,
  options?: Parameters<typeof fetchBaseQuery>[0]
): BaseQueryFn {
  const baseQuery = fetchBaseQuery({
    baseUrl,
    ...options,
  });

  return async (args, api, extraOptions) => {
    const result = await baseQuery(args, api, extraOptions);

    // Handle network errors
    if (result.error) {
      if ('status' in result.error) {
        // HTTP error with status
        throw new ApiResponseError(
          `API request failed with status ${result.error.status}`,
          typeof result.error.status === 'number' ? result.error.status : 500,
          result.error.data
        );
      } else {
        // Network or other error
        throw new Error(`Network error: ${result.error.error}`);
      }
    }

    return result;
  };
}

/**
 * Creates a validated endpoint that automatically validates responses
 */
export function createValidatedEndpoint<TResponse, TRequest = void>(
  responseSchema: z.ZodSchema<TResponse>,
  endpoint: {
    query: (arg: TRequest) => any;
    method?: string;
  }
) {
  return {
    ...endpoint,
    transformResponse: (response: unknown) => {
      // First, try to validate as error response
      try {
        const errorResponse = ApiErrorResponseSchema.parse(response);
        throw new ApiResponseError(
          errorResponse.error.message,
          500, // Default to 500 for API errors
          errorResponse
        );
      } catch (error) {
        if (!(error instanceof z.ZodError)) {
          throw error; // Re-throw if it's an ApiResponseError
        }
        // Not an error response, continue with success validation
      }

      // Validate as success response
      const context = `${endpoint.method || 'GET'} endpoint`;
      const validatedResponse = validateApiResponse(responseSchema, response, context);

      // Extract data field from API success response
      if (validatedResponse && typeof validatedResponse === 'object' && 'data' in validatedResponse) {
        return (validatedResponse as any).data;
      }

      return validatedResponse;
    },
  };
}

/**
 * Wraps a success response schema with the standard API wrapper
 */
export function wrapSuccessResponse<T extends z.ZodTypeAny>(dataSchema: T) {
  return ApiSuccessResponseSchema(dataSchema);
}

/**
 * Type-safe query hook factory with automatic validation
 */
export function createValidatedQuery<TResponse, TRequest = void>(
  responseSchema: z.ZodSchema<TResponse>,
  queryFn: (arg: TRequest) => Promise<unknown>
) {
  return async (arg: TRequest): Promise<TResponse> => {
    try {
      const response = await queryFn(arg);
      return validateApiResponse(responseSchema, response, 'query');
    } catch (error) {
      if (error instanceof ApiValidationError || error instanceof ApiResponseError) {
        throw error;
      }
      throw new Error(`Query failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };
}

/**
 * Type-safe mutation hook factory with automatic validation
 */
export function createValidatedMutation<TResponse, TRequest = void>(
  responseSchema: z.ZodSchema<TResponse>,
  mutationFn: (arg: TRequest) => Promise<unknown>
) {
  return async (arg: TRequest): Promise<TResponse> => {
    try {
      const response = await mutationFn(arg);
      return validateApiResponse(responseSchema, response, 'mutation');
    } catch (error) {
      if (error instanceof ApiValidationError || error instanceof ApiResponseError) {
        throw error;
      }
      throw new Error(`Mutation failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  };
}

/**
 * Development mode response validator for debugging
 */
export function validateResponseInDev<T>(
  schema: z.ZodSchema<T>,
  data: unknown,
  endpoint: string
): T {
  if (process.env.NODE_ENV === 'development') {
    console.group(`🔍 API Response Validation: ${endpoint}`);

    try {
      const result = schema.parse(data);
      console.log('✅ Validation passed');
      console.log('Response data:', result);
      console.groupEnd();
      return result;
      } catch (error) {
        if (error instanceof z.ZodError) {
          console.error('❌ Validation failed');
          console.error('Issues:', error.issues);
          console.error('Raw data:', data);
          console.groupEnd();
          throw new ApiValidationError(
            `API validation failed for ${endpoint}`,
            error,
            data
          );
        }
        console.error('❌ Unexpected parsing error');
        console.groupEnd();
        throw error;
      }
  }

  return schema.parse(data);
}

/**
 * Utility to safely parse API responses with fallback
 */
export function safeParseApiResponse<T>(
  schema: z.ZodSchema<T>,
  data: unknown,
  fallback?: T
): { success: true; data: T } | { success: false; error: z.ZodError; fallback: T | undefined } {
  const result = schema.safeParse(data);

  if (result.success) {
    return { success: true, data: result.data };
  } else {
    console.warn('API response validation failed:', {
      issues: result.error.issues,
      data,
    });

    return {
      success: false,
      error: result.error,
      fallback,
    };
  }
}

/**
 * Mock data generator for development and testing
 */
export function generateMockData<T>(schema: z.ZodSchema<T>): T {
  // This is a simplified mock generator - in a real implementation,
  // you'd use a library like @anatine/zod-mock or faker.js

  if (schema instanceof z.ZodObject) {
    const shape = schema.shape;
    const mock: any = {};

    for (const [key, fieldSchema] of Object.entries(shape)) {
      mock[key] = generateMockValue(fieldSchema as z.ZodTypeAny);
    }

    return mock;
  }

  return generateMockValue(schema);
}

function generateMockValue(schema: z.ZodTypeAny): any {
  if (schema instanceof z.ZodString) {
    return 'mock-string';
  }
  if (schema instanceof z.ZodNumber) {
    return 42;
  }
  if (schema instanceof z.ZodBoolean) {
    return true;
  }
  if (schema instanceof z.ZodArray) {
    return [generateMockValue(schema.element)];
  }
  if (schema instanceof z.ZodOptional) {
    return generateMockValue(schema.unwrap());
  }
  if (schema instanceof z.ZodEnum) {
    return schema.options[0];
  }
  if (schema instanceof z.ZodLiteral) {
    return schema.value;
  }

  return null;
}
