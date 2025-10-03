import { useCallback, useState } from 'react'
import { z } from 'zod'

// Security validation schemas
const emailSchema = z.string().email()
const passwordSchema = z
  .string()
  .min(8)
  .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)

export class SecurityValidator {
  static validateEmail(email: string): boolean {
    try {
      emailSchema.parse(email)
      return true
    } catch {
      return false
    }
  }

  static validatePassword(password: string): boolean {
    try {
      passwordSchema.parse(password)
      return true
    } catch {
      return false
    }
  }

  static sanitizeInput(input: string): string {
    return input.replace(/[<>&"']/g, match => {
      switch (match) {
        case '<':
          return '&lt;'
        case '>':
          return '&gt;'
        case '&':
          return '&amp;'
        case '"':
          return '&quot;'
        case "'":
          return '&#x27;'
        default:
          return match
      }
    })
  }

  static validateCsrfToken(token: string): boolean {
    return token.length >= 32 && /^[a-zA-Z0-9_-]+$/.test(token)
  }
}

interface SecurityValidationState {
  isValid: boolean
  errors: string[]
  warnings: string[]
}

export function useSecureValidation() {
  const [validationState, setValidationState] =
    useState<SecurityValidationState>({
      isValid: true,
      errors: [],
      warnings: [],
    })

  const validateForm = useCallback((formData: Record<string, any>) => {
    const errors: string[] = []
    const warnings: string[] = []

    // Email validation
    if (formData.email && !SecurityValidator.validateEmail(formData.email)) {
      errors.push('Invalid email format')
    }

    // Password validation
    if (
      formData.password &&
      !SecurityValidator.validatePassword(formData.password)
    ) {
      errors.push(
        'Password must be at least 8 characters with uppercase, lowercase, number, and special character'
      )
    }

    // Check for potential XSS
    Object.entries(formData).forEach(([key, value]) => {
      if (
        typeof value === 'string' &&
        /<script|javascript:|on\w+=/i.test(value)
      ) {
        warnings.push(`Potential XSS detected in field: ${key}`)
      }
    })

    const isValid = errors.length === 0
    setValidationState({ isValid, errors, warnings })

    return { isValid, errors, warnings }
  }, [])

  const clearValidation = useCallback(() => {
    setValidationState({ isValid: true, errors: [], warnings: [] })
  }, [])

  return {
    validationState,
    validateForm,
    clearValidation,
  }
}
