import React from 'react';

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  error?: string;
  label?: string;
}

/**
 * Select Component
 * 
 * Styled select dropdown with error state support.
 * 
 * Features:
 * - Error state styling
 * - Optional label
 * - Consistent styling with other form elements
 * - Proper focus states
 */
export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ error, label, className = '', children, ...props }, ref) => {
    return (
      <div>
        {label && (
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {label}
          </label>
        )}
        <select
          ref={ref}
          className={`
            block w-full px-3 py-2 border rounded-md shadow-sm
            focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500
            disabled:bg-gray-100 disabled:cursor-not-allowed
            ${error 
              ? 'border-red-300 focus:border-red-500 focus:ring-red-500' 
              : 'border-gray-300'
            }
            ${className}
          `}
          {...props}
        >
          {children}
        </select>
        {error && (
          <p className="text-red-600 text-sm mt-1">{error}</p>
        )}
      </div>
    );
  }
);

Select.displayName = 'Select';