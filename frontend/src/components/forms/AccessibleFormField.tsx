import React, { useId } from 'react'
import { clsx } from 'clsx'

export interface AccessibleFormFieldProps {
  label: string
  children: React.ReactElement
  error?: string
  helpText: string
  required?: boolean
  id?: string
  className?: string
  labelClassName?: string
  inputClassName?: string
  errorClassName?: string
  helpClassName?: string
}

const AccessibleFormField: React.FC<AccessibleFormFieldProps> = ({
  label,
  children,
  error,
  helpText,
  required = false,
  id,
  className,
  labelClassName,
  inputClassName,
  errorClassName,
  helpClassName
}) => {
  const fieldId = id || useId()
  const errorId = `${fieldId}-error`
  const helpId = `${fieldId}-help`

  // Calculate describedBy IDs
  const describedByIds = [
    error ? errorId : null,
    helpText ? helpId : null
  ].filter(Boolean).join(' ')

  // Clone the input element with accessibility attributes
  const enhancedInput = React.cloneElement(children as React.ReactElement<any>, {
    id: fieldId,
    'aria-invalid': !!error,
    'aria-describedby': describedByIds || undefined,
    'aria-required': required,
    className: clsx(
      // Base input styles with proper focus indicators
      'block w-full rounded-lg border border-gray-300 px-3 py-2',
      'text-gray-900 placeholder-gray-500',
      'transition-all duration-200',

      // Focus styles for accessibility
      'focus:outline-none focus:ring-3 focus:ring-blue-500 focus:ring-offset-2',
      'focus:border-blue-500',

      // Error styles
      error && [
        'border-red-300 bg-red-50',
        'focus:border-red-500 focus:ring-red-500'
      ],

      // Success styles (when no error and has value)
      !error && (children as any).props?.value && 'border-green-300',

      // High contrast mode
      'forced-colors:border forced-colors:border-solid',

      // Ensure minimum touch target for mobile inputs
      'min-h-[44px]',

      inputClassName,
      (children as any).props?.className
    )
  })

  return (
    <div className={clsx('space-y-1', className)} role="group">
      {/* Label */}
      <label
        htmlFor={fieldId}
        className={clsx(
          'block text-sm font-medium text-gray-700',
          required && 'after:content-["*"] after:text-red-500 after:ml-1',
          labelClassName
        )}
      >
        {label}
        {required && (
          <span className="sr-only">(required)</span>
        )}
      </label>

      {/* Input */}
      {enhancedInput}

      {/* Help text */}
      {helpText && (
        <p
          id={helpId}
          className={clsx(
            'text-sm text-gray-600',
            helpClassName
          )}
        >
          {helpText}
        </p>
      )}

      {/* Error message */}
      {error && (
        <p
          id={errorId}
          role="alert"
          aria-live="polite"
          className={clsx(
            'flex items-center text-sm text-red-600',
            errorClassName
          )}
        >
          <svg
            className="mr-1 h-4 w-4 flex-shrink-0"
            fill="currentColor"
            viewBox="0 0 20 20"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
              clipRule="evenodd"
            />
          </svg>
          {error}
        </p>
      )}
    </div>
  )
}

export default AccessibleFormField

// Specialized field components
export const EmailField: React.FC<Omit<AccessibleFormFieldProps, 'children'> & {
  value?: string
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void
  placeholder?: string
  autoComplete?: string
}> = ({ placeholder = "you@example.com", autoComplete = "email", ...props }) => (
  <AccessibleFormField
    helpText="We'll never share your email with anyone else"
    {...props}
  >
    <input
      type="email"
      placeholder={placeholder}
      autoComplete={autoComplete}
      {...(props.value !== undefined && { value: props.value })}
      {...(props.onChange && { onChange: props.onChange })}
    />
  </AccessibleFormField>
)

export const PasswordField: React.FC<Omit<AccessibleFormFieldProps, 'children'> & {
  value?: string
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void
  placeholder?: string
  autoComplete?: string
  showToggle?: boolean
}> = ({
  placeholder = "Enter your password",
  autoComplete = "current-password",
  showToggle = false,
  ...props
}) => {
  const [showPassword, setShowPassword] = React.useState(false)

  return (
    <AccessibleFormField
      helpText="Password must be at least 8 characters long"
      {...props}
    >
      <div className="relative">
        <input
          type={showToggle && showPassword ? 'text' : 'password'}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className="pr-10"
          {...(props.value !== undefined && { value: props.value })}
          {...(props.onChange && { onChange: props.onChange })}
        />
        {showToggle && (
          <button
            type="button"
            className={clsx(
              'absolute inset-y-0 right-0 flex items-center pr-3',
              'text-gray-400 hover:text-gray-600',
              'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2',
              'min-h-[44px] min-w-[44px]' // Ensure touch target
            )}
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? (
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21" />
              </svg>
            ) : (
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            )}
          </button>
        )}
      </div>
    </AccessibleFormField>
  )
}

export const TextAreaField: React.FC<Omit<AccessibleFormFieldProps, 'children'> & {
  value?: string
  onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void
  placeholder?: string
  rows?: number
  maxLength?: number
}> = ({ rows = 4, maxLength, ...props }) => (
  <AccessibleFormField
    {...props}
    helpText={
      maxLength && props.helpText
        ? `${props.helpText} Maximum ${maxLength} characters.`
        : maxLength
        ? `Maximum ${maxLength} characters.`
        : props.helpText || ''
    }
  >
    <textarea
      rows={rows}
      maxLength={maxLength}
      className="resize-none"
      {...(props.value !== undefined && { value: props.value })}
      {...(props.onChange && { onChange: props.onChange })}
      {...(props.placeholder && { placeholder: props.placeholder })}
    />
  </AccessibleFormField>
)

export const SelectField: React.FC<Omit<AccessibleFormFieldProps, 'children'> & {
  options: Array<{ value: string; label: string; disabled?: boolean }>
  value?: string
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void
  placeholder?: string
}> = ({ options, placeholder = "Select an option...", ...props }) => (
  <AccessibleFormField {...props}>
    <select
      className="cursor-pointer"
      {...(props.value !== undefined && { value: props.value })}
      {...(props.onChange && { onChange: props.onChange })}
    >
      {placeholder && (
        <option value="" disabled>
          {placeholder}
        </option>
      )}
      {options.map((option) => (
        <option
          key={option.value}
          value={option.value}
          disabled={option.disabled}
        >
          {option.label}
        </option>
      ))}
    </select>
  </AccessibleFormField>
)