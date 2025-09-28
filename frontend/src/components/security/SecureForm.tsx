import React, { useCallback, useEffect, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { SecurityValidator, useSecureValidation } from '../../utils/securityUtils';

/**
 * Secure form schema with comprehensive validation.
 */
const createSecureFormSchema = (fields: Record<string, 'email' | 'password' | 'text' | 'number'>) => {
  const schemaFields: Record<string, z.ZodType> = {};

  Object.entries(fields).forEach(([fieldName, fieldType]) => {
    switch (fieldType) {
      case 'email':
        schemaFields[fieldName] = z
          .string()
          .min(1, 'Email is required')
          .max(254, 'Email too long')
          .refine(
            (email) => SecurityValidator.validateEmail(email).isValid,
            'Invalid email format or security violation'
          );
        break;

      case 'password':
        schemaFields[fieldName] = z
          .string()
          .min(12, 'Password must be at least 12 characters')
          .refine(
            (password) => SecurityValidator.validatePassword(password).isValid,
            'Password does not meet security requirements'
          );
        break;

      case 'text':
        schemaFields[fieldName] = z
          .string()
          .min(1, 'Field is required')
          .max(1000, 'Text too long')
          .refine(
            (text) => !SecurityValidator.checkXss(text) && !SecurityValidator.checkSqlInjection(text),
            'Invalid characters detected'
          );
        break;

      case 'number':
        schemaFields[fieldName] = z
          .number()
          .min(0, 'Must be a positive number')
          .max(Number.MAX_SAFE_INTEGER, 'Number too large');
        break;
    }
  });

  return z.object(schemaFields);
};

interface SecureFormProps<T extends Record<string, any>> {
  fields: Record<keyof T, 'email' | 'password' | 'text' | 'number'>;
  onSubmit: (data: T) => Promise<void>;
  onValidationError?: (errors: Record<string, string>) => void;
  className?: string;
  children: (props: {
    register: any;
    control: any;
    errors: any;
    isSubmitting: boolean;
    isValid: boolean;
  }) => React.ReactNode;
}

/**
 * Secure form component with built-in security validation and CSRF protection.
 * Implements clean code principles while maintaining strict security standards.
 */
export const SecureForm = <T extends Record<string, any>>({
  fields,
  onSubmit,
  onValidationError,
  className = '',
  children,
}: SecureFormProps<T>) => {
  const [csrfToken, setCsrfToken] = useState<string>('');
  const [isSecurityValidated, setIsSecurityValidated] = useState(false);

  const schema = React.useMemo(() => createSecureFormSchema(fields), [fields]);

  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting, isValid },
    watch,
    setError,
  } = useForm<T>({
    resolver: zodResolver(schema) as any,
    mode: 'onBlur',
  });

  // Generate CSRF token on mount
  useEffect(() => {
    const token = SecurityValidator.generateCsrfToken();
    setCsrfToken(token);
  }, []);

  // Real-time security validation
  const watchedValues = watch();
  useEffect(() => {
    let hasSecurityIssues = false;

    Object.entries(watchedValues).forEach(([fieldName, value]) => {
      if (typeof value === 'string' && value) {
        if (SecurityValidator.checkXss(value) || SecurityValidator.checkSqlInjection(value)) {
          setError(fieldName as any, {
            type: 'security',
            message: 'Security violation detected in input',
          });
          hasSecurityIssues = true;
        }
      }
    });

    setIsSecurityValidated(!hasSecurityIssues);
  }, [watchedValues, setError]);

  const handleSecureSubmit = useCallback(
    async (data: any) => {
      if (!isSecurityValidated) {
        onValidationError?.({ security: 'Security validation failed' });
        return;
      }

      try {
        // Sanitize all string inputs before submission
        const sanitizedData = { ...data };
        Object.entries(sanitizedData).forEach(([key, value]) => {
          if (typeof value === 'string') {
            sanitizedData[key as keyof T] = SecurityValidator.sanitizeInput(value) as T[keyof T];
          }
        });

        // Add CSRF token to submission
        const dataWithCsrf = {
          ...sanitizedData,
          _csrf: csrfToken,
        };

        await onSubmit(dataWithCsrf);
      } catch (error) {
        console.error('Secure form submission failed:', SecurityValidator.redactSensitiveData(String(error)));
        onValidationError?.({ submission: 'Submission failed. Please try again.' });
      }
    },
    [onSubmit, csrfToken, isSecurityValidated, onValidationError]
  );

  return (
    <form
      onSubmit={handleSubmit(handleSecureSubmit)}
      className={`secure-form ${className}`}
      noValidate
      autoComplete="off"
    >
      {/* CSRF Token Hidden Field */}
      <input type="hidden" name="_csrf" value={csrfToken} />

      {/* Honeypot field for bot detection */}
      <input
        type="text"
        name="website"
        style={{ display: 'none' }}
        tabIndex={-1}
        autoComplete="off"
      />

      {children({
        register,
        control,
        errors,
        isSubmitting,
        isValid: isValid && isSecurityValidated,
      })}
    </form>
  );
};

/**
 * Secure input component with real-time validation.
 */
interface SecureInputProps {
  name: string;
  type: 'email' | 'password' | 'text' | 'number';
  label: string;
  placeholder?: string;
  required?: boolean;
  control: any;
  error?: string;
  className?: string;
}

export const SecureInput: React.FC<SecureInputProps> = ({
  name,
  type,
  label,
  placeholder,
  required = false,
  control,
  error,
  className = '',
}) => {
  const { validateField } = useSecureValidation();
  const [securityStatus, setSecurityStatus] = useState<'safe' | 'warning' | 'danger'>('safe');

  const handleInputChange = useCallback(
    (value: string) => {
      if (!value) {
        setSecurityStatus('safe');
        return;
      }

      const result = validateField(value, type as 'email' | 'password' | 'text');
      if (!result.isValid) {
        setSecurityStatus('danger');
      } else if (SecurityValidator.checkXss(value) || SecurityValidator.checkSqlInjection(value)) {
        setSecurityStatus('danger');
      } else {
        setSecurityStatus('safe');
      }
    },
    [validateField, type]
  );

  return (
    <div className={`secure-input-wrapper ${className}`}>
      <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
        {label}
        {required && <span className="text-red-500 ml-1">*</span>}
      </label>

      <Controller
        name={name}
        control={control}
        render={({ field: { onChange, value, onBlur } }) => (
          <div className="relative">
            <input
              id={name}
              type={type === 'password' ? 'password' : type === 'email' ? 'email' : 'text'}
              placeholder={placeholder}
              value={value || ''}
              onChange={(e) => {
                const newValue = e.target.value;
                onChange(newValue);
                handleInputChange(newValue);
              }}
              onBlur={onBlur}
              className={`
                block w-full px-3 py-2 border rounded-md shadow-sm
                focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500
                ${error ? 'border-red-300' : 'border-gray-300'}
                ${securityStatus === 'danger' ? 'border-red-500 bg-red-50' : ''}
                ${securityStatus === 'warning' ? 'border-yellow-500 bg-yellow-50' : ''}
              `}
              autoComplete={type === 'password' ? 'current-password' : type === 'email' ? 'email' : 'off'}
              spellCheck={false}
              required={required}
            />

            {/* Security indicator */}
            {securityStatus !== 'safe' && (
              <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                <svg
                  className={`h-5 w-5 ${
                    securityStatus === 'danger' ? 'text-red-500' : 'text-yellow-500'
                  }`}
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                    clipRule="evenodd"
                  />
                </svg>
              </div>
            )}
          </div>
        )}
      />

      {/* Error message */}
      {error && (
        <p className="mt-1 text-sm text-red-600" role="alert">
          {error}
        </p>
      )}

      {/* Security warning */}
      {securityStatus === 'danger' && (
        <p className="mt-1 text-sm text-red-600" role="alert">
          Security issue detected in input
        </p>
      )}
    </div>
  );
};

/**
 * Password strength indicator component.
 */
interface PasswordStrengthProps {
  password: string;
  className?: string;
}

export const PasswordStrength: React.FC<PasswordStrengthProps> = ({
  password,
  className = '',
}) => {
  const getStrengthScore = useCallback((password: string): number => {
    if (!password) return 0;

    let score = 0;
    const validation = SecurityValidator.validatePassword(password);

    if (password.length >= 12) score += 1;
    if (password.length >= 16) score += 1;
    if (/[A-Z]/.test(password)) score += 1;
    if (/[a-z]/.test(password)) score += 1;
    if (/\d/.test(password)) score += 1;
    if (/[!@#$%^&*()_+-=[\]{}|;:,.<>?]/.test(password)) score += 1;
    if (validation.isValid) score += 2;

    return Math.min(score, 8);
  }, []);

  const strengthScore = getStrengthScore(password);
  const strengthPercentage = (strengthScore / 8) * 100;

  const getStrengthColor = (score: number): string => {
    if (score <= 2) return 'bg-red-500';
    if (score <= 4) return 'bg-yellow-500';
    if (score <= 6) return 'bg-blue-500';
    return 'bg-green-500';
  };

  const getStrengthLabel = (score: number): string => {
    if (score <= 2) return 'Weak';
    if (score <= 4) return 'Fair';
    if (score <= 6) return 'Good';
    return 'Strong';
  };

  if (!password) return null;

  return (
    <div className={`password-strength ${className}`}>
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs text-gray-600">Password strength:</span>
        <span className={`text-xs font-medium ${
          strengthScore <= 2 ? 'text-red-600' :
          strengthScore <= 4 ? 'text-yellow-600' :
          strengthScore <= 6 ? 'text-blue-600' :
          'text-green-600'
        }`}>
          {getStrengthLabel(strengthScore)}
        </span>
      </div>
      <div className="w-full bg-gray-200 rounded-full h-2">
        <div
          className={`h-2 rounded-full transition-all duration-300 ${getStrengthColor(strengthScore)}`}
          style={{ width: `${strengthPercentage}%` }}
        />
      </div>
    </div>
  );
};