import { useMemo } from 'react';
import { FieldErrors, FieldValues } from 'react-hook-form';

/**
 * Validation state for a form field
 */
export type ValidationState = 'default' | 'valid' | 'error' | 'warning';

/**
 * Options for field validation state hook
 */
export interface UseFieldValidationStateOptions {
  /** Whether to show valid state for untouched fields */
  showValidUntouched?: boolean;
  /** Custom validation state resolver */
  customResolver?: (hasError: boolean, isTouched: boolean, hasValue: boolean) => ValidationState;
}

/**
 * Custom hook for determining the validation state of form fields.
 *
 * Features:
 * - Determines field state based on errors, touched state, and value
 * - Returns consistent validation states: 'default', 'valid', 'error', 'warning'
 * - Memoized for performance
 * - Type-safe with React Hook Form
 *
 * @template T - The form data type extending FieldValues
 * @param errors - React Hook Form errors object
 * @param dirtyFields - React Hook Form dirtyFields object
 * @param options - Configuration options
 *
 * @example
 * ```typescript
 * const { getFieldState } = useFieldValidationState<FormData>(
 *   errors,
 *   dirtyFields
 * );
 *
 * const nameState = getFieldState('name', watchedName);
 * // Returns: 'default' | 'valid' | 'error'
 *
 * // Use for styling
 * <input
 *   className={cn(
 *     'border-2',
 *     nameState === 'error' && 'border-red-500',
 *     nameState === 'valid' && 'border-green-500'
 *   )}
 * />
 * ```
 */
export function useFieldValidationState<T extends FieldValues>(
  errors: FieldErrors<T>,
  dirtyFields: Partial<Readonly<{ [K in keyof T]?: boolean }>>,
  options: UseFieldValidationStateOptions = {}
) {
  const { showValidUntouched = false, customResolver } = options;

  /**
   * Get the validation state for a specific field
   */
  const getFieldState = useMemo(
    () =>
      (fieldName: keyof T, fieldValue?: any): ValidationState => {
        const hasError = !!errors[fieldName];
        const isTouched = !!dirtyFields[fieldName];
        const hasValue = fieldValue !== undefined && fieldValue !== null && fieldValue !== '';

        // Use custom resolver if provided
        if (customResolver) {
          return customResolver(hasError, isTouched, hasValue);
        }

        // Error state takes priority
        if (hasError) {
          return isTouched ? 'error' : 'default';
        }

        // Valid state only shown for touched fields with values
        if (isTouched && hasValue) {
          return 'valid';
        }

        // Optionally show valid state for untouched fields
        if (showValidUntouched && hasValue) {
          return 'valid';
        }

        return 'default';
      },
    [errors, dirtyFields, showValidUntouched, customResolver]
  );

  /**
   * Get validation class names for Tailwind styling
   */
  const getFieldClassName = useMemo(
    () =>
      (fieldName: keyof T, fieldValue?: any, baseClasses = ''): string => {
        const state = getFieldState(fieldName, fieldValue);
        const stateClasses = {
          default: 'border-gray-300 focus:border-blue-500 focus:ring-blue-500',
          valid: 'border-green-500 focus:border-green-500 focus:ring-green-500',
          error: 'border-red-500 focus:border-red-500 focus:ring-red-500',
          warning: 'border-yellow-500 focus:border-yellow-500 focus:ring-yellow-500',
        };

        return `${baseClasses} ${stateClasses[state]}`.trim();
      },
    [getFieldState]
  );

  /**
   * Check if any field has errors
   */
  const hasErrors = useMemo(() => Object.keys(errors).length > 0, [errors]);

  /**
   * Check if specific field has error
   */
  const hasFieldError = useMemo(
    () => (fieldName: keyof T): boolean => !!errors[fieldName],
    [errors]
  );

  /**
   * Get error message for a field
   */
  const getFieldError = useMemo(
    () =>
      (fieldName: keyof T): string | undefined => {
        const error = errors[fieldName];
        return error?.message as string | undefined;
      },
    [errors]
  );

  /**
   * Check if field is touched/dirty
   */
  const isFieldTouched = useMemo(
    () => (fieldName: keyof T): boolean => !!dirtyFields[fieldName],
    [dirtyFields]
  );

  return {
    getFieldState,
    getFieldClassName,
    hasErrors,
    hasFieldError,
    getFieldError,
    isFieldTouched,
  };
}
