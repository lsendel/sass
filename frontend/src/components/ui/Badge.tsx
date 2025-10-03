import React from 'react'

import { cn } from '@/lib/utils'

/**
 * Badge Component
 *
 * A reusable badge component for displaying labels, statuses, and tags.
 * Supports multiple variants and sizes with consistent styling.
 *
 * @example
 * ```tsx
 * <Badge variant="success">Active</Badge>
 * <Badge variant="error" size="sm">Error</Badge>
 * ```
 */

export interface BadgeProps {
  /** The content to display inside the badge */
  children: React.ReactNode

  /** Visual variant of the badge */
  variant?:
    | 'default'
    | 'success'
    | 'warning'
    | 'error'
    | 'info'
    | 'primary'
    | 'secondary'

  /** Size of the badge */
  size?: 'sm' | 'md' | 'lg'

  /** Additional CSS classes */
  className?: string

  /** Whether the badge should have a dot indicator */
  dot?: boolean

  /** Optional click handler */
  onClick?: () => void
}

export const Badge = React.forwardRef<HTMLSpanElement, BadgeProps>(
  (
    {
      children,
      variant = 'default',
      size = 'md',
      className,
      dot = false,
      onClick,
    },
    ref
  ) => {
    const variantStyles = {
      default: 'bg-gray-100 text-gray-800 border-gray-200',
      primary: 'bg-blue-100 text-blue-800 border-blue-200',
      secondary: 'bg-purple-100 text-purple-800 border-purple-200',
      success: 'bg-green-100 text-green-800 border-green-200',
      warning: 'bg-yellow-100 text-yellow-800 border-yellow-200',
      error: 'bg-red-100 text-red-800 border-red-200',
      info: 'bg-cyan-100 text-cyan-800 border-cyan-200',
    }

    const sizeStyles = {
      sm: 'text-xs px-2 py-0.5',
      md: 'text-sm px-2.5 py-0.5',
      lg: 'text-base px-3 py-1',
    }

    const dotStyles = {
      default: 'bg-gray-500',
      primary: 'bg-blue-500',
      secondary: 'bg-purple-500',
      success: 'bg-green-500',
      warning: 'bg-yellow-500',
      error: 'bg-red-500',
      info: 'bg-cyan-500',
    }

    return (
      <span
        ref={ref}
        className={cn(
          'inline-flex items-center gap-1.5 rounded-full font-medium border',
          'transition-colors duration-150',
          variantStyles[variant],
          sizeStyles[size],
          onClick && 'cursor-pointer hover:opacity-80',
          className
        )}
        onClick={onClick}
        role={onClick ? 'button' : undefined}
        tabIndex={onClick ? 0 : undefined}
        onKeyDown={
          onClick
            ? e => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  onClick()
                }
              }
            : undefined
        }
      >
        {dot && (
          <span
            className={cn('w-1.5 h-1.5 rounded-full', dotStyles[variant])}
            aria-hidden="true"
          />
        )}
        {children}
      </span>
    )
  }
)

Badge.displayName = 'Badge'

export default Badge
