import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'
import { COLOR, ACCENT_COLOR } from '@/lib/theme'

const buttonVariants = cva(
  'inline-flex items-center justify-center font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:pointer-events-none disabled:opacity-60 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        // Primary button using global COLOR theme
        default: `bg-[${COLOR}] text-white hover:bg-[${COLOR}]/90 focus:ring-[${COLOR}]/20 shadow-sm hover:shadow-md`,

        // Accent button using global ACCENT_COLOR
        accent: `bg-[${ACCENT_COLOR}] text-white hover:bg-[${ACCENT_COLOR}]/90 focus:ring-[${ACCENT_COLOR}]/20 shadow-sm hover:shadow-md`,

        // Destructive actions
        destructive:
          'bg-error-600 text-white hover:bg-error-700 focus:ring-error-200 shadow-sm hover:shadow-md',

        // Outline variant using theme colors
        outline: `border border-[${COLOR}] text-[${COLOR}] hover:bg-[${COLOR}] hover:text-white focus:ring-[${COLOR}]/20`,

        // Secondary button
        secondary:
          'bg-gray-100 text-gray-900 hover:bg-gray-200 focus:ring-gray-200',

        // Ghost button
        ghost: 'text-gray-700 hover:bg-gray-100 focus:ring-gray-200',

        // Link variant
        link: `text-[${COLOR}] hover:text-[${ACCENT_COLOR}] hover:underline focus:ring-[${COLOR}]/20 p-0 h-auto font-medium`,
      },
      size: {
        sm: 'h-8 px-3 text-sm rounded-lg',
        default: 'h-10 px-4 text-sm rounded-lg',
        lg: 'h-12 px-6 text-base rounded-xl',
        icon: 'h-10 w-10 rounded-lg',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
  isLoading?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant,
      size,
      asChild = false,
      isLoading,
      children,
      disabled,
      ...props
    },
    ref
  ) => {
    const Comp = asChild ? Slot : 'button'
    const isDisabled = disabled || isLoading

    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        disabled={isDisabled}
        {...props}
      >
        {isLoading && (
          <svg
            className="animate-spin -ml-1 mr-2 h-4 w-4"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
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
        )}
        {children}
      </Comp>
    )
  }
)
Button.displayName = 'Button'

export { Button, buttonVariants }
