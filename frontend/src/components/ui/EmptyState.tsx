import React from 'react';

interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
  className?: string;
}

/**
 * EmptyState Component
 * 
 * Displays empty state with icon, title, description and optional action button.
 * Used when lists or collections have no items to show.
 * 
 * Features:
 * - Customizable icon
 * - Optional action button
 * - Centered layout
 * - Responsive design
 */
export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon,
  action,
  className = ''
}) => {
  return (
    <div className={`text-center py-12 ${className}`}>
      {icon && (
        <div className="mx-auto mb-4 flex items-center justify-center">
          {icon}
        </div>
      )}
      
      <h3 className="text-lg font-medium text-gray-900 mb-2">
        {title}
      </h3>
      
      <p className="text-gray-500 max-w-sm mx-auto mb-6">
        {description}
      </p>

      {action && (
        <div className="flex justify-center">
          {action}
        </div>
      )}
    </div>
  );
};