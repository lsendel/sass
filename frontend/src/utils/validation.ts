import { z } from 'zod'

import { VALIDATION_MESSAGES, UI_LIMITS } from '@/constants/appConstants'

// Base validation schemas
export const ValidationSchemas = {
  email: z.string().email(VALIDATION_MESSAGES.INVALID_EMAIL),
  
  password: z.string()
    .min(8, VALIDATION_MESSAGES.PASSWORD_TOO_SHORT)
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/, 
      'Password must contain uppercase, lowercase, number, and special character'),
  
  searchText: z.string()
    .max(UI_LIMITS.MAX_SEARCH_LENGTH, VALIDATION_MESSAGES.SEARCH_TOO_LONG)
    .optional(),
  
  dateRange: z.object({
    from: z.date(),
    to: z.date(),
  }).refine(data => data.from < data.to, {
    message: 'Start date must be before end date',
    path: ['from'],
  }),
  
  fileSize: z.number()
    .max(UI_LIMITS.MAX_FILE_SIZE_MB * 1024 * 1024, VALIDATION_MESSAGES.FILE_TOO_LARGE),
} as const

// Domain-specific validation schemas
export const AuditValidationSchemas = {
  exportRequest: z.object({
    format: z.enum(['CSV', 'JSON', 'PDF']),
    dateFrom: z.date().optional(),
    dateTo: z.date().optional(),
    search: ValidationSchemas.searchText,
    actionTypes: z.array(z.string()).max(50).optional(),
    resourceTypes: z.array(z.string()).max(50).optional(),
  }).refine(data => {
    if (data.dateFrom && data.dateTo) {
      return data.dateFrom < data.dateTo
    }
    return true
  }, {
    message: 'Start date must be before end date',
    path: ['dateFrom'],
  }),

  userProfile: z.object({
    email: ValidationSchemas.email,
    name: z.string().min(1, 'Name is required').max(100),
    role: z.enum(['admin', 'user', 'viewer']),
    timezone: z.string().optional(),
  }),
} as const

// Validation result types
export interface ValidationResult<T = any> {
  success: boolean
  data?: T
  errors?: ValidationError[]
}

export interface ValidationError {
  field: string
  message: string
  code: string
}

// Validation utility class
export class ValidationUtils {
  /**
   * Validates data against a Zod schema with improved error handling.
   */
  static validate<T>(schema: z.ZodSchema<T>, data: unknown): ValidationResult<T> {
    try {
      const result = schema.parse(data)
      return {
        success: true,
        data: result,
      }
    } catch (error) {
      if (error instanceof z.ZodError) {
        const errors: ValidationError[] = error.issues.map(err => ({
          field: err.path.join('.'),
          message: err.message,
          code: err.code,
        }))
        
        return {
          success: false,
          errors,
        }
      }
      
      return {
        success: false,
        errors: [{
          field: 'unknown',
          message: 'Validation failed',
          code: 'unknown_error',
        }],
      }
    }
  }

  /**
   * Validates data asynchronously with custom async validators.
   */
  static async validateAsync<T>(
    schema: z.ZodSchema<T>, 
    data: unknown,
    customValidators?: Array<AsyncValidator<T>>
  ): Promise<ValidationResult<T>> {
    // First, run synchronous validation
    const syncResult = this.validate(schema, data)
    if (!syncResult.success) {
      return syncResult
    }

    // Then run async validators if provided
    if (customValidators && syncResult.data) {
      for (const validator of customValidators) {
        const asyncResult = await validator(syncResult.data)
        if (!asyncResult.success) {
          return asyncResult
        }
      }
    }

    return syncResult
  }

  /**
   * Creates a debounced validator for real-time validation.
   */
  static createDebouncedValidator<T>(
    schema: z.ZodSchema<T>,
    delay = 300
  ): (data: unknown) => Promise<ValidationResult<T>> {
    let timeoutId: number | NodeJS.Timeout | null = null
    
    return (data: unknown) => {
      return new Promise((resolve) => {
        if (timeoutId) {
          clearTimeout(timeoutId)
        }
        
        timeoutId = setTimeout(() => {
          resolve(this.validate(schema, data))
        }, delay)
      })
    }
  }

  /**
   * Validates form data with field-level error mapping.
   */
  static validateForm<T extends Record<string, any>>(
    schema: z.ZodSchema<T>, 
    formData: T
  ): FormValidationResult<T> {
    const result = this.validate(schema, formData)
    
    if (result.success) {
      return {
        success: true,
        data: result.data!,
        fieldErrors: {},
      }
    }

    const fieldErrors: Record<string, string> = {}
    result.errors?.forEach(error => {
      fieldErrors[error.field] = error.message
    })

    return {
      success: false,
      fieldErrors,
    }
  }
}

// Form validation result type
export interface FormValidationResult<T = any> {
  success: boolean
  data?: T
  fieldErrors: Record<string, string>
}

// Async validator type
export type AsyncValidator<T> = (data: T) => Promise<ValidationResult<T>>

// Custom React hook for form validation
export function useFormValidation<T extends Record<string, any>>(
  schema: z.ZodSchema<T>,
  initialData: T
) {
  const [data, setData] = React.useState<T>(initialData)
  const [errors, setErrors] = React.useState<Record<string, string>>({})
  const [isValidating, setIsValidating] = React.useState(false)
  const [isValid, setIsValid] = React.useState(false)

  const debouncedValidator = React.useMemo(
    () => ValidationUtils.createDebouncedValidator(schema),
    [schema]
  )

  const validateField = React.useCallback(async (field: keyof T, value: any) => {
    setIsValidating(true)
    
    const fieldData = { ...data, [field]: value }
    const result = await debouncedValidator(fieldData)
    
    setErrors(prev => {
      const newErrors = { ...prev }
      if (result.success) {
        delete newErrors[field as string]
      } else {
        const fieldError = result.errors?.find(e => e.field === field)
        if (fieldError) {
          newErrors[field as string] = fieldError.message
        }
      }
      return newErrors
    })
    
    setIsValid(result.success && Object.keys(errors).length === 0)
    setIsValidating(false)
  }, [data, debouncedValidator, errors])

  const updateField = React.useCallback((field: keyof T, value: any) => {
    const newData = { ...data, [field]: value }
    setData(newData)
    validateField(field, value)
  }, [data, validateField])

  const validateAll = React.useCallback(async () => {
    setIsValidating(true)
    const result = await debouncedValidator(data)
    
    if (result.success) {
      setErrors({})
      setIsValid(true)
    } else {
      const fieldErrors: Record<string, string> = {}
      result.errors?.forEach(error => {
        fieldErrors[error.field] = error.message
      })
      setErrors(fieldErrors)
      setIsValid(false)
    }
    
    setIsValidating(false)
    return result
  }, [data, debouncedValidator])

  return {
    data,
    errors,
    isValidating,
    isValid,
    updateField,
    validateField,
    validateAll,
  }
}

// Export commonly used validators
export const CommonValidators = {
  required: (message = VALIDATION_MESSAGES.REQUIRED_FIELD) => 
    z.string().min(1, message),
  
  email: ValidationSchemas.email,
  
  password: ValidationSchemas.password,
  
  url: z.string().url('Please enter a valid URL'),
  
  phoneNumber: z.string().regex(
    /^\+?[1-9]\d{1,14}$/, 
    'Please enter a valid phone number'
  ),
  
  positiveNumber: z.number().positive('Must be a positive number'),
  
  nonEmptyArray: <T>(schema: z.ZodSchema<T>) => 
    z.array(schema).min(1, 'At least one item is required'),
} as const