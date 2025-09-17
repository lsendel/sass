import React from 'react'
import { clsx } from 'clsx'

type LoadingSpinnerProps = {
  size?: 'sm' | 'md' | 'lg'
  className?: string
  color?: 'primary' | 'white' | 'gray'
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  className,
  color = 'primary',
}) => {
  return (
    <div
      className={clsx(
        'animate-spin rounded-full border-2 border-solid border-current border-r-transparent',
        {
          'h-4 w-4': size === 'sm',
          'h-6 w-6': size === 'md',
          'h-8 w-8': size === 'lg',
        },
        {
          'text-primary-600': color === 'primary',
          'text-white': color === 'white',
          'text-gray-400': color === 'gray',
        },
        className
      )}
      role="status"
      aria-label="Loading"
    >
      <span className="sr-only">Loading...</span>
    </div>
  )
}

export default LoadingSpinner
