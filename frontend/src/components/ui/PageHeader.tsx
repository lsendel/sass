import clsx from 'clsx'
import type { ReactNode } from 'react'

export interface PageHeaderProps {
  title: string
  description?: ReactNode
  actions?: ReactNode
  className?: string
  actionsClassName?: string
}

export function PageHeader({
  title,
  description,
  actions,
  className,
  actionsClassName,
}: PageHeaderProps) {
  return (
    <div className={clsx('md:flex md:items-center md:justify-between', className)}>
      <div className="flex-1 min-w-0">
        <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
          {title}
        </h2>
        {description && (
          <p className="mt-1 text-sm text-gray-500">
            {description}
          </p>
        )}
      </div>
      {actions && (
        <div
          className={clsx(
            'mt-4 flex flex-wrap gap-3 md:mt-0 md:ml-4',
            actionsClassName
          )}
        >
          {actions}
        </div>
      )}
    </div>
  )
}

export default PageHeader
