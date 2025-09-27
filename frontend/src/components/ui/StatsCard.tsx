import type { ReactNode } from 'react'
import clsx from 'clsx'

export interface StatsCardProps {
  title?: ReactNode
  value?: ReactNode
  description?: ReactNode
  icon?: ReactNode
  iconWrapperClassName?: string
  trend?: ReactNode
  children?: ReactNode
  footer?: ReactNode
  className?: string
  contentClassName?: string
  unstyled?: boolean
  titleClassName?: string
  valueClassName?: string
  descriptionClassName?: string
  trendClassName?: string
}

export function StatsCard({
  title,
  value,
  description,
  icon,
  trend,
  children,
  footer,
  className,
  contentClassName,
  iconWrapperClassName,
  unstyled = false,
  titleClassName,
  valueClassName,
  descriptionClassName,
  trendClassName,
}: StatsCardProps) {
  const containerClass = clsx(
    unstyled ? undefined : 'bg-white overflow-hidden shadow rounded-lg',
    className
  )

  return (
    <div className={containerClass}>
      <div className={clsx('p-6', contentClassName)}>
        {icon && (
          <div
            className={clsx(
              'mb-4 flex items-center justify-center w-12 h-12 rounded-md bg-primary-50 text-primary-600',
              iconWrapperClassName
            )}
          >
            {icon}
          </div>
        )}
        {title && (
          <p className={clsx('text-sm font-medium text-gray-500', titleClassName)}>
            {title}
          </p>
        )}
        {value && (
          <p className={clsx('mt-2 text-3xl font-semibold text-gray-900', valueClassName)}>
            {value}
          </p>
        )}
        {description && (
          <p className={clsx('mt-2 text-sm text-gray-500', descriptionClassName)}>
            {description}
          </p>
        )}
        {trend && (
          <div
            className={clsx(
              'mt-3 text-xs font-medium text-success-600 bg-success-100 inline-flex items-center px-2 py-1 rounded',
              trendClassName
            )}
          >
            {trend}
          </div>
        )}
        {children && <div className={clsx(value || description ? 'mt-4' : undefined)}>{children}</div>}
      </div>
      {footer && <div className="border-t border-gray-100 bg-gray-50 px-6 py-4">{footer}</div>}
    </div>
  )
}

export default StatsCard
