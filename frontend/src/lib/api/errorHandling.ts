// import { z } from 'zod';

import { ApiValidationError, ApiResponseError } from './validation';

import { ApiErrorResponseSchema } from '@/types/api';

/**
 * Comprehensive error handling for API responses with user-friendly messages
 */

export interface ErrorInfo {
  code: string;
  message: string;
  userMessage: string;
  isRetryable: boolean;
  severity: 'low' | 'medium' | 'high' | 'critical';
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly info: ErrorInfo,
    public readonly originalError?: Error
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * Maps API error codes to user-friendly messages
 */
const ERROR_MESSAGE_MAP: Record<string, Partial<ErrorInfo>> = {
  // Authentication errors
  'AUTH_001': {
    userMessage: 'Invalid email or password. Please try again.',
    isRetryable: true,
    severity: 'medium',
  },
  'AUTH_002': {
    userMessage: 'Your account has been locked due to too many failed login attempts. Please try again later.',
    isRetryable: false,
    severity: 'high',
  },
  'AUTH_003': {
    userMessage: 'Your session has expired. Please log in again.',
    isRetryable: false,
    severity: 'medium',
  },
  'AUTH_004': {
    userMessage: 'You do not have permission to access this resource.',
    isRetryable: false,
    severity: 'high',
  },
  'AUTH_005': {
    userMessage: 'Please verify your email address before continuing.',
    isRetryable: false,
    severity: 'medium',
  },

  // Payment errors
  'PAY_001': {
    userMessage: 'Your payment could not be processed. Please check your payment details and try again.',
    isRetryable: true,
    severity: 'high',
  },
  'PAY_002': {
    userMessage: 'Your card was declined. Please try a different payment method.',
    isRetryable: true,
    severity: 'high',
  },
  'PAY_003': {
    userMessage: 'Insufficient funds. Please check your account balance.',
    isRetryable: false,
    severity: 'medium',
  },
  'PAY_004': {
    userMessage: 'Your payment method has expired. Please update your payment information.',
    isRetryable: false,
    severity: 'high',
  },
  'PAY_005': {
    userMessage: 'This payment requires additional authentication. Please complete the verification process.',
    isRetryable: true,
    severity: 'medium',
  },

  // Subscription errors
  'SUB_001': {
    userMessage: 'Your subscription could not be created. Please try again.',
    isRetryable: true,
    severity: 'high',
  },
  'SUB_002': {
    userMessage: 'Your subscription has reached its user limit. Please upgrade your plan.',
    isRetryable: false,
    severity: 'medium',
  },
  'SUB_003': {
    userMessage: 'Your subscription is past due. Please update your payment information.',
    isRetryable: false,
    severity: 'high',
  },

  // Organization errors
  'ORG_001': {
    userMessage: 'Organization not found or you do not have access.',
    isRetryable: false,
    severity: 'medium',
  },
  'ORG_002': {
    userMessage: 'You have reached the maximum number of organizations allowed.',
    isRetryable: false,
    severity: 'medium',
  },

  // Validation errors
  'VAL_001': {
    userMessage: 'Please check your input and try again.',
    isRetryable: true,
    severity: 'low',
  },
  'VAL_002': {
    userMessage: 'Password does not meet security requirements.',
    isRetryable: true,
    severity: 'medium',
  },
  'VAL_003': {
    userMessage: 'This email address is already in use.',
    isRetryable: false,
    severity: 'medium',
  },

  // System errors
  'SYS_001': {
    userMessage: 'A temporary system error occurred. Please try again in a few minutes.',
    isRetryable: true,
    severity: 'high',
  },
  'SYS_002': {
    userMessage: 'The service is temporarily unavailable. Please try again later.',
    isRetryable: true,
    severity: 'critical',
  },
  'SYS_003': {
    userMessage: 'Your request could not be processed due to a server error.',
    isRetryable: true,
    severity: 'high',
  },

  // Rate limiting
  'RATE_001': {
    userMessage: 'Too many requests. Please wait a moment and try again.',
    isRetryable: true,
    severity: 'medium',
  },
};

/**
 * Maps HTTP status codes to error information
 */
const HTTP_STATUS_MAP: Record<number, Partial<ErrorInfo>> = {
  400: {
    code: 'BAD_REQUEST',
    userMessage: 'Invalid request. Please check your input and try again.',
    isRetryable: true,
    severity: 'medium',
  },
  401: {
    code: 'UNAUTHORIZED',
    userMessage: 'Please log in to access this feature.',
    isRetryable: false,
    severity: 'medium',
  },
  403: {
    code: 'FORBIDDEN',
    userMessage: 'You do not have permission to perform this action.',
    isRetryable: false,
    severity: 'high',
  },
  404: {
    code: 'NOT_FOUND',
    userMessage: 'The requested resource was not found.',
    isRetryable: false,
    severity: 'medium',
  },
  409: {
    code: 'CONFLICT',
    userMessage: 'This action conflicts with existing data. Please refresh and try again.',
    isRetryable: true,
    severity: 'medium',
  },
  422: {
    code: 'VALIDATION_ERROR',
    userMessage: 'Please check your input and try again.',
    isRetryable: true,
    severity: 'medium',
  },
  429: {
    code: 'RATE_LIMIT',
    userMessage: 'Too many requests. Please wait a moment and try again.',
    isRetryable: true,
    severity: 'medium',
  },
  500: {
    code: 'INTERNAL_ERROR',
    userMessage: 'A server error occurred. Our team has been notified.',
    isRetryable: true,
    severity: 'high',
  },
  502: {
    code: 'BAD_GATEWAY',
    userMessage: 'Service temporarily unavailable. Please try again in a few minutes.',
    isRetryable: true,
    severity: 'high',
  },
  503: {
    code: 'SERVICE_UNAVAILABLE',
    userMessage: 'Service is temporarily down for maintenance. Please try again later.',
    isRetryable: true,
    severity: 'critical',
  },
  504: {
    code: 'TIMEOUT',
    userMessage: 'Request timed out. Please try again.',
    isRetryable: true,
    severity: 'high',
  },
};

/**
 * Processes any error and returns structured error information
 */
export function processApiError(error: unknown): ErrorInfo {
  // Handle API validation errors
  if (error instanceof ApiValidationError) {
    return {
      code: 'VALIDATION_ERROR',
      message: error.message,
      userMessage: 'The server response was invalid. Please try again or contact support.',
      isRetryable: true,
      severity: 'high',
    };
  }

  // Handle API response errors
  if (error instanceof ApiResponseError) {
    const statusInfo = HTTP_STATUS_MAP[error.status];

    // Try to parse error response for specific error code
    let specificError: ErrorInfo | null = null;
    try {
      const errorResponse = ApiErrorResponseSchema.parse(error.data);
      const mappedError = ERROR_MESSAGE_MAP[errorResponse.error.code];

      if (mappedError) {
        specificError = {
          code: errorResponse.error.code,
          message: errorResponse.error.message,
          userMessage: mappedError.userMessage || errorResponse.error.message,
          isRetryable: mappedError.isRetryable ?? true,
          severity: mappedError.severity || 'medium',
        };
      }
    } catch {
      // Could not parse error response strictly. Try a lenient shape check
      try {
        const data: any = (error as any).data;
        const code: string | undefined = data?.error?.code;
        const message: string | undefined = data?.error?.message;
        if (code) {
          const mappedError = ERROR_MESSAGE_MAP[code];
          if (mappedError) {
            specificError = {
              code,
              message: message || mappedError.userMessage || code,
              userMessage: mappedError.userMessage || message || 'An error occurred.',
              isRetryable: mappedError.isRetryable ?? true,
              severity: mappedError.severity || 'medium',
            };
          }
        }
      } catch {
        // Fallback to HTTP status mapping
      }
    }

    return specificError || {
      code: statusInfo?.code || 'HTTP_ERROR',
      message: error.message,
      userMessage: statusInfo?.userMessage || 'An error occurred. Please try again.',
      isRetryable: statusInfo?.isRetryable ?? true,
      severity: statusInfo?.severity || 'medium',
    };
  }

  // Handle network errors
  if (error instanceof TypeError && error.message.includes('fetch')) {
    return {
      code: 'NETWORK_ERROR',
      message: error.message,
      userMessage: 'Unable to connect to the server. Please check your internet connection.',
      isRetryable: true,
      severity: 'high',
    };
  }

  // Handle generic errors
  const message = error instanceof Error ? error.message : 'Unknown error occurred';
  return {
    code: 'UNKNOWN_ERROR',
    message,
    userMessage: 'An unexpected error occurred. Please try again.',
    isRetryable: true,
    severity: 'medium',
  };
}

/**
 * Creates a user-facing ApiError from any error
 */
export function createApiError(error: unknown): ApiError {
  const info = processApiError(error);
  const originalError = error instanceof Error ? error : undefined;

  return new ApiError(info.message, info, originalError);
}

/**
 * Determines if an error should trigger a retry
 */
export function shouldRetry(error: ErrorInfo, attemptCount: number, maxRetries = 3): boolean {
  if (attemptCount >= maxRetries) {
    return false;
  }

  if (!error.isRetryable) {
    return false;
  }

  // Don't retry certain error types on first attempt
  const noImmediateRetry = ['AUTH_001', 'VAL_001', 'VAL_002'];
  if (attemptCount === 0 && noImmediateRetry.includes(error.code)) {
    return false;
  }

  return true;
}

/**
 * Calculates retry delay with exponential backoff
 */
export function getRetryDelay(attemptCount: number, baseDelay = 1000): number {
  return baseDelay * Math.pow(2, attemptCount) + Math.random() * 1000;
}

/**
 * React hook for error handling with retry logic
 */
export function useApiErrorHandler() {
  const handleError = (error: unknown, context?: string) => {
    const apiError = createApiError(error);

    // Log error for debugging
    console.error(`API Error${context ? ` in ${context}` : ''}:`, {
      code: apiError.info.code,
      message: apiError.info.message,
      userMessage: apiError.info.userMessage,
      severity: apiError.info.severity,
      originalError: apiError.originalError,
    });

    // Send to error reporting service in production
    if (process.env.NODE_ENV === 'production' && apiError.info.severity === 'critical') {
      // TODO: Integrate with error reporting service (e.g., Sentry)
      console.error('Critical error reported:', apiError);
    }

    return apiError;
  };

  const createRetryHandler = (
    operation: () => Promise<any>,
    maxRetries = 3,
    baseDelay = 1000
  ) => {
    return async (attemptCount = 0): Promise<any> => {
      try {
        return await operation();
      } catch (error) {
        const apiError = handleError(error);

        if (shouldRetry(apiError.info, attemptCount, maxRetries)) {
          const delay = getRetryDelay(attemptCount, baseDelay);
          await new Promise(resolve => setTimeout(resolve, delay));
          return createRetryHandler(operation, maxRetries, baseDelay)(attemptCount + 1);
        }

        throw apiError;
      }
    };
  };

  return {
    handleError,
    createRetryHandler,
    shouldRetry,
    getRetryDelay,
  };
}
