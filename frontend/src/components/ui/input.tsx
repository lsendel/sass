import React, { forwardRef } from 'react'

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: string | undefined
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className = '', error, ...props }, ref) => {
    const baseClasses =
      'w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed'
    const errorClasses = error
      ? 'border-red-500 focus:ring-red-500'
      : 'border-gray-300'

    return (
      <input
        ref={ref}
        className={`${baseClasses} ${errorClasses} ${className}`}
        {...props}
      />
    )
  }
)

Input.displayName = 'Input'
