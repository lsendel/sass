import React, { forwardRef } from 'react'
import { clsx } from 'clsx'

export interface AccessibleButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
  loadingText?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
  fullWidth?: boolean
  children: React.ReactNode
}

const AccessibleButton = forwardRef<HTMLButtonElement, AccessibleButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      loadingText,
      leftIcon,
      rightIcon,
      fullWidth = false,
      disabled,
      className,
      children,
      ...props
    },
    ref
  ) => {
    const baseClasses = [
      // Base styles
      'inline-flex items-center justify-center font-medium rounded-lg',
      'border border-transparent transition-all duration-200',
      'focus:outline-none focus:ring-3 focus:ring-offset-2',
      'disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none',

      // Ensure minimum touch target size (44px)
      'min-h-[44px] min-w-[44px]',

      // High contrast mode support
      'forced-colors:border forced-colors:border-solid',

      // Reduced motion support
      'motion-reduce:transition-none',
    ]

    // Size variants with proper touch targets
    const sizeClasses = {
      sm: 'px-3 py-2 text-sm min-h-[44px]', // Still meets 44px minimum
      md: 'px-4 py-2.5 text-sm min-h-[48px]', // 48px for better usability
      lg: 'px-6 py-3 text-base min-h-[52px]', // 52px for large buttons
    }

    // Variant styles with proper contrast
    const variantClasses = {
      primary: [
        'bg-blue-600 text-white',
        'hover:bg-blue-700 active:bg-blue-800',
        'focus:ring-blue-500',
        'shadow-sm hover:shadow-md',
      ],
      secondary: [
        'bg-gray-600 text-white',
        'hover:bg-gray-700 active:bg-gray-800',
        'focus:ring-gray-500',
        'shadow-sm hover:shadow-md',
      ],
      outline: [
        'bg-transparent text-blue-600 border-blue-600',
        'hover:bg-blue-50 hover:text-blue-700',
        'active:bg-blue-100',
        'focus:ring-blue-500',
      ],
      ghost: [
        'bg-transparent text-gray-700',
        'hover:bg-gray-100 hover:text-gray-900',
        'active:bg-gray-200',
        'focus:ring-gray-500',
      ],
      danger: [
        'bg-red-600 text-white',
        'hover:bg-red-700 active:bg-red-800',
        'focus:ring-red-500',
        'shadow-sm hover:shadow-md',
      ],
    }

    // Full width option
    const widthClasses = fullWidth ? 'w-full' : ''

    // Loading spinner component
    const LoadingSpinner = () => (
      <svg
        className="animate-spin -ml-1 mr-2 h-4 w-4"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        />
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        />
      </svg>
    )

    const buttonContent = (
      <>
        {isLoading && <LoadingSpinner />}
        {!isLoading && leftIcon && (
          <span className="mr-2" aria-hidden="true">
            {leftIcon}
          </span>
        )}
        <span>{isLoading && loadingText ? loadingText : children}</span>
        {!isLoading && rightIcon && (
          <span className="ml-2" aria-hidden="true">
            {rightIcon}
          </span>
        )}
      </>
    )

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={clsx(
          baseClasses,
          sizeClasses[size],
          variantClasses[variant],
          widthClasses,
          className
        )}
        aria-disabled={disabled || isLoading}
        aria-busy={isLoading}
        {...props}
      >
        {buttonContent}
      </button>
    )
  }
)

AccessibleButton.displayName = 'AccessibleButton'

export default AccessibleButton

// Export variants for convenience
export const PrimaryButton = (
  props: Omit<AccessibleButtonProps, 'variant'>
) => <AccessibleButton variant="primary" {...props} />

export const SecondaryButton = (
  props: Omit<AccessibleButtonProps, 'variant'>
) => <AccessibleButton variant="secondary" {...props} />

export const OutlineButton = (
  props: Omit<AccessibleButtonProps, 'variant'>
) => <AccessibleButton variant="outline" {...props} />

export const GhostButton = (props: Omit<AccessibleButtonProps, 'variant'>) => (
  <AccessibleButton variant="ghost" {...props} />
)

export const DangerButton = (props: Omit<AccessibleButtonProps, 'variant'>) => (
  <AccessibleButton variant="danger" {...props} />
)
