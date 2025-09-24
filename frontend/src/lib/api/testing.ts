import { z } from 'zod';
import { generateMockData, validateApiResponse, ApiValidationError } from './validation';
import { processApiError } from './errorHandling';
import type { ApiSuccessResponse, ApiErrorResponse } from '@/types/api';

/**
 * Testing utilities for API validation and error handling
 */

export interface MockApiOptions {
  includeOptionalFields?: boolean;
  useInvalidData?: boolean;
  responseDelay?: number;
  errorRate?: number; // 0-1, probability of returning an error
}

export interface TestScenario<T> {
  name: string;
  input: unknown;
  expectedValid: boolean;
  expectedData?: T;
  expectedError?: string;
}

/**
 * Creates a mock API response wrapper
 */
export function createMockApiResponse<T>(
  data: T,
  success: boolean = true,
  message?: string
): ApiSuccessResponse<T> | ApiErrorResponse {
  const timestamp = new Date().toISOString();

  if (success) {
    return {
      success: true,
      data,
      message,
      timestamp,
    };
  } else {
    return {
      success: false,
      error: {
        code: 'MOCK_ERROR',
        message: message || 'Mock error for testing',
        details: { mockData: true },
      },
      correlationId: crypto.randomUUID(),
      timestamp,
    };
  }
}

/**
 * Generates mock data for a given schema with realistic values
 */
export function generateRealisticMockData<T>(schema: z.ZodSchema<T>): T {
  // Enhanced mock generator with more realistic data
  function generateValue(schema: z.ZodTypeAny, path: string[] = []): any {
    const fieldName = path[path.length - 1]?.toLowerCase() || '';

    const genUuid = () =>
      'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
      });

    if (schema instanceof z.ZodString) {
      // Generate realistic string values based on field name
      if (fieldName.includes('email')) {
        return 'user@example.com';
      }
      if (fieldName.includes('name') || fieldName.includes('title')) {
        return 'Test Name';
      }
      if (fieldName.includes('id')) {
        return genUuid();
      }
      if (fieldName.includes('url')) {
        return 'https://example.com';
      }
      if (fieldName.includes('phone')) {
        return '+1-555-123-4567';
      }
      if (fieldName.includes('currency')) {
        return 'USD';
      }
      const isTimestampField =
        fieldName.endsWith('at') ||
        fieldName.includes('timestamp') ||
        fieldName.includes('date') ||
        fieldName.includes('time') ||
        fieldName.includes('createdat') ||
        fieldName.includes('updatedat') ||
        fieldName.includes('lastloginat');
      if (isTimestampField) {
        return new Date().toISOString();
      }
      return 'mock-string-value';
    }

    if (schema instanceof z.ZodNumber) {
      if (fieldName.includes('amount') || fieldName.includes('price')) {
        return 1999; // $19.99 in cents
      }
      if (fieldName.includes('count') || fieldName.includes('total')) {
        return Math.floor(Math.random() * 100);
      }
      if (fieldName.includes('year')) {
        return 2024;
      }
      if (fieldName.includes('month')) {
        return Math.floor(Math.random() * 12) + 1;
      }
      return 42;
    }

    if (schema instanceof z.ZodBoolean) {
      return Math.random() > 0.5;
    }

    if (schema instanceof z.ZodDate) {
      return new Date();
    }

    if (schema instanceof z.ZodArray) {
      const itemCount = Math.floor(Math.random() * 3) + 1;
      return Array.from({ length: itemCount }, () =>
        generateValue(schema.element, path)
      );
    }

    if (schema instanceof z.ZodObject) {
      const shape = schema.shape;
      const mock: any = {};

      for (const [key, fieldSchema] of Object.entries(shape)) {
        mock[key] = generateValue(fieldSchema as z.ZodTypeAny, [...path, key]);
      }

      return mock;
    }

    if (schema instanceof z.ZodOptional) {
      // 70% chance to include optional fields
      if (Math.random() > 0.3) {
        return generateValue(schema.unwrap(), path);
      }
      return undefined;
    }

    if (schema instanceof z.ZodNullable) {
      // 80% chance for non-null values
      if (Math.random() > 0.2) {
        return generateValue(schema.unwrap(), path);
      }
      return null;
    }

    if (schema instanceof z.ZodEnum) {
      const options = schema.options;
      return options[Math.floor(Math.random() * options.length)];
    }

    if (schema instanceof z.ZodLiteral) {
      return schema.value;
    }

    if (schema instanceof z.ZodUnion) {
      const options = schema.options;
      const selectedOption = options[Math.floor(Math.random() * options.length)];
      return generateValue(selectedOption, path);
    }

    return null;
  }

  return generateValue(schema);
}

/**
 * Creates a comprehensive test suite for a schema
 */
export function createValidationTestSuite<T>(
  schema: z.ZodSchema<T>,
  suiteName: string
): TestScenario<T>[] {
  const scenarios: TestScenario<T>[] = [];

  // Valid data scenario
  scenarios.push({
    name: `${suiteName} - Valid data`,
    input: generateRealisticMockData(schema),
    expectedValid: true,
  });

  // Invalid data scenarios
  scenarios.push(
    {
      name: `${suiteName} - Null input`,
      input: null,
      expectedValid: false,
      expectedError: 'Invalid input: expected object, received null',
    },
    {
      name: `${suiteName} - Undefined input`,
      input: undefined,
      expectedValid: false,
      expectedError: 'Invalid input: expected object, received undefined',
    },
    {
      name: `${suiteName} - Empty object`,
      input: {},
      expectedValid: false,
    },
    {
      name: `${suiteName} - Invalid type`,
      input: 'invalid-string',
      expectedValid: false,
      expectedError: 'Invalid input: expected object, received string',
    },
    {
      name: `${suiteName} - Array instead of object`,
      input: [],
      expectedValid: false,
      expectedError: 'Invalid input: expected object, received array',
    }
  );

  return scenarios;
}

/**
 * Runs validation test scenarios
 */
export function runValidationTests<T>(
  schema: z.ZodSchema<T>,
  scenarios: TestScenario<T>[],
  verbose: boolean = false
): { passed: number; failed: number; results: any[] } {
  const results: any[] = [];
  let passed = 0;
  let failed = 0;

  for (const scenario of scenarios) {
    try {
      const result = validateApiResponse(schema, scenario.input, scenario.name);

      if (scenario.expectedValid) {
        passed++;
        if (verbose) {
          console.log(`✅ ${scenario.name}: PASSED`);
        }
        results.push({
          scenario: scenario.name,
          status: 'PASSED',
          result,
        });
      } else {
        failed++;
        console.error(`❌ ${scenario.name}: Expected validation to fail but it passed`);
        results.push({
          scenario: scenario.name,
          status: 'FAILED',
          error: 'Expected validation to fail',
          result,
        });
      }
    } catch (error) {
      if (!scenario.expectedValid) {
        const isValidationError = error instanceof ApiValidationError;
        const hasExpectedError = !scenario.expectedError ||
          (error instanceof Error && error.message.includes(scenario.expectedError));

        if (isValidationError && hasExpectedError) {
          passed++;
          if (verbose) {
            console.log(`✅ ${scenario.name}: PASSED (correctly failed validation)`);
          }
          results.push({
            scenario: scenario.name,
            status: 'PASSED',
            error: error.message,
          });
        } else {
          failed++;
          console.error(`❌ ${scenario.name}: Unexpected error: ${error}`);
          results.push({
            scenario: scenario.name,
            status: 'FAILED',
            error: `Unexpected error: ${error}`,
          });
        }
      } else {
        failed++;
        console.error(`❌ ${scenario.name}: Validation failed unexpectedly: ${error}`);
        results.push({
          scenario: scenario.name,
          status: 'FAILED',
          error: error instanceof Error ? error.message : String(error),
        });
      }
    }
  }

  return { passed, failed, results };
}

/**
 * Mock API client for testing
 */
export class MockApiClient {
  private responses: Map<string, any> = new Map();
  private delays: Map<string, number> = new Map();
  private errorRates: Map<string, number> = new Map();

  setResponse(endpoint: string, response: any, delay?: number, errorRate?: number) {
    this.responses.set(endpoint, response);
    if (delay !== undefined) {
      this.delays.set(endpoint, delay);
    }
    if (errorRate !== undefined) {
      this.errorRates.set(endpoint, errorRate);
    }
  }

  async request<T>(endpoint: string, schema: z.ZodSchema<T>): Promise<T> {
    const delay = this.delays.get(endpoint) || 0;
    const errorRate = this.errorRates.get(endpoint) || 0;

    // Simulate network delay
    if (delay > 0) {
      await new Promise(resolve => setTimeout(resolve, delay));
    }

    // Simulate errors
    if (Math.random() < errorRate) {
      throw new Error(`Mock error for endpoint: ${endpoint}`);
    }

    const response = this.responses.get(endpoint);
    if (!response) {
      throw new Error(`No mock response configured for endpoint: ${endpoint}`);
    }

    return validateApiResponse(schema, response, `Mock ${endpoint}`);
  }

  reset() {
    this.responses.clear();
    this.delays.clear();
    this.errorRates.clear();
  }
}

/**
 * Performance testing utility
 */
export async function benchmarkValidation<T>(
  schema: z.ZodSchema<T>,
  data: unknown,
  iterations: number = 1000
): Promise<{
  averageTime: number;
  minTime: number;
  maxTime: number;
  totalTime: number;
  iterations: number;
}> {
  const times: number[] = [];

  for (let i = 0; i < iterations; i++) {
    const start = performance.now();
    try {
      schema.parse(data);
    } catch {
      // Ignore validation errors for benchmarking
    }
    const end = performance.now();
    times.push(end - start);
  }

  const totalTime = times.reduce((sum, time) => sum + time, 0);
  const averageTime = totalTime / iterations;
  const minTime = Math.min(...times);
  const maxTime = Math.max(...times);

  return {
    averageTime,
    minTime,
    maxTime,
    totalTime,
    iterations,
  };
}

/**
 * Integration test helper for API endpoints
 */
export async function testApiEndpoint<TRequest, TResponse>(
  endpoint: (request: TRequest) => Promise<TResponse>,
  requestSchema: z.ZodSchema<TRequest>,
  responseSchema: z.ZodSchema<TResponse>,
  testCases: { name: string; request: TRequest; shouldSucceed: boolean }[]
): Promise<void> {
  for (const testCase of testCases) {
    console.log(`Testing: ${testCase.name}`);

    try {
      // Validate request
      const validatedRequest = requestSchema.parse(testCase.request);

      // Make API call
      const response = await endpoint(validatedRequest);

      // Validate response
      const validatedResponse = responseSchema.parse(response);

      if (testCase.shouldSucceed) {
        console.log(`✅ ${testCase.name}: SUCCESS`);
      } else {
        console.error(`❌ ${testCase.name}: Expected failure but succeeded`);
      }
    } catch (error) {
      if (!testCase.shouldSucceed) {
        console.log(`✅ ${testCase.name}: Correctly failed - ${error}`);
      } else {
        console.error(`❌ ${testCase.name}: Unexpected failure - ${error}`);
        throw error;
      }
    }
  }
}

/**
 * Error handling test utility
 */
export function testErrorHandling(errors: unknown[]): void {
  console.log('Testing error handling...');

  for (const [index, error] of errors.entries()) {
    try {
      const errorInfo = processApiError(error);
      console.log(`Error ${index + 1}:`, {
        code: errorInfo.code,
        severity: errorInfo.severity,
        isRetryable: errorInfo.isRetryable,
        userMessage: errorInfo.userMessage,
      });
    } catch (e) {
      console.error(`Failed to process error ${index + 1}:`, e);
    }
  }
}
