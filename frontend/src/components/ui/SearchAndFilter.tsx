import React, { useState } from 'react'
import {
  MagnifyingGlassIcon,
  XMarkIcon,
  FunnelIcon,
  ChevronDownIcon,
  CalendarDaysIcon,
  CheckIcon,
} from '@heroicons/react/24/outline'
import { clsx } from 'clsx'

// import { useDebounce } from '../../utils/performance'

// Search component with debounced input
interface SearchBoxProps {
  placeholder?: string
  value: string
  onChange: (value: string) => void
  onClear?: () => void
  debounceMs?: number
  className?: string
  size?: 'sm' | 'md' | 'lg'
}

export const SearchBox: React.FC<SearchBoxProps> = ({
  placeholder = 'Search...',
  value,
  onChange,
  onClear,
  debounceMs: _debounceMs = 300,
  className = '',
  size = 'md',
}) => {
  const [localValue, setLocalValue] = useState(value)
  const debouncedValue = localValue // useDebounce(localValue, debounceMs)

  // Update parent when debounced value changes
  React.useEffect(() => {
    onChange(debouncedValue)
  }, [debouncedValue, onChange])

  // Sync with external value changes
  React.useEffect(() => {
    setLocalValue(value)
  }, [value])

  const handleClear = () => {
    setLocalValue('')
    onClear?.()
  }

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base',
  }

  return (
    <div className={clsx('relative', className)}>
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
          <MagnifyingGlassIcon className="h-4 w-4 text-gray-400" />
        </div>
        <input
          type="text"
          value={localValue}
          onChange={e => setLocalValue(e.target.value)}
          placeholder={placeholder}
          className={clsx(
            'block w-full pl-10 pr-10 border border-gray-300 rounded-md focus:ring-primary-500 focus:border-primary-500',
            sizeClasses[size]
          )}
        />
        {localValue && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute inset-y-0 right-0 pr-3 flex items-center hover:text-gray-600"
          >
            <XMarkIcon className="h-4 w-4 text-gray-400" />
          </button>
        )}
      </div>
      {debouncedValue !== localValue && (
        <div className="absolute top-full left-0 mt-1 text-xs text-gray-500">
          Searching...
        </div>
      )}
    </div>
  )
}

// Results counter component
interface ResultsCounterProps {
  total: number
  query?: string
  filtered?: number
  className?: string
}

export const ResultsCounter: React.FC<ResultsCounterProps> = ({
  total,
  query,
  filtered,
  className = '',
}) => {
  if (query && filtered !== undefined) {
    return (
      <div className={clsx('text-sm text-gray-600', className)}>
        {filtered === 0 ? (
          <span className="text-red-600">
            No matches for "<strong>{query}</strong>"
          </span>
        ) : (
          <span>
            <strong>{filtered}</strong> results found
            {query && (
              <span>
                {' '}
                for "<strong>{query}</strong>"
              </span>
            )}
            <span className="text-gray-400"> ({total} total)</span>
          </span>
        )}
      </div>
    )
  }

  return (
    <div className={clsx('text-sm text-gray-600', className)}>
      <strong>{total}</strong> {total === 1 ? 'item' : 'items'}
    </div>
  )
}

// Filter dropdown component
interface FilterOption {
  label: string
  value: string
  count?: number
}

interface FilterDropdownProps {
  label: string
  options: FilterOption[]
  selectedValues: string[]
  onChange: (values: string[]) => void
  placeholder?: string
  className?: string
  maxHeight?: string
}

export const FilterDropdown: React.FC<FilterDropdownProps> = ({
  label,
  options,
  selectedValues,
  onChange,
  placeholder = 'All',
  className = '',
  maxHeight = 'max-h-60',
}) => {
  const [isOpen, setIsOpen] = useState(false)

  const handleToggle = (value: string) => {
    const newValues = selectedValues.includes(value)
      ? selectedValues.filter(v => v !== value)
      : [...selectedValues, value]
    onChange(newValues)
  }

  const displayText =
    selectedValues.length === 0
      ? placeholder
      : selectedValues.length === 1
        ? options.find(opt => opt.value === selectedValues[0])?.label
        : `${selectedValues.length} selected`

  return (
    <div className={clsx('relative inline-block text-left', className)}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="inline-flex justify-between items-center w-full px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
      >
        <div className="flex items-center">
          <FunnelIcon className="h-4 w-4 mr-2 text-gray-400" />
          <span>{label}:</span>
          <span className="ml-1 font-normal">{displayText}</span>
        </div>
        <ChevronDownIcon
          className={clsx(
            'h-4 w-4 text-gray-400 transition-transform',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      {isOpen && (
        <div className="absolute right-0 z-10 mt-2 w-56 bg-white rounded-md shadow-lg ring-1 ring-black ring-opacity-5">
          <div className={clsx('py-1 overflow-y-auto', maxHeight)}>
            {options.map(option => (
              <button
                key={option.value}
                type="button"
                onClick={() => handleToggle(option.value)}
                className="w-full flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
              >
                <div className="flex items-center flex-1">
                  <div
                    className={clsx(
                      'w-4 h-4 mr-3 border border-gray-300 rounded flex items-center justify-center',
                      selectedValues.includes(option.value) &&
                        'bg-primary-600 border-primary-600'
                    )}
                  >
                    {selectedValues.includes(option.value) && (
                      <CheckIcon className="h-3 w-3 text-white" />
                    )}
                  </div>
                  <span className="flex-1 text-left">{option.label}</span>
                  {option.count !== undefined && (
                    <span className="text-xs text-gray-400">
                      ({option.count})
                    </span>
                  )}
                </div>
              </button>
            ))}
          </div>
          {selectedValues.length > 0 && (
            <div className="border-t border-gray-200 py-2 px-4">
              <button
                type="button"
                onClick={() => onChange([])}
                className="text-sm text-primary-600 hover:text-primary-500"
              >
                Clear all
              </button>
            </div>
          )}
        </div>
      )}

      {isOpen && (
        <div className="fixed inset-0 z-5" onClick={() => setIsOpen(false)} />
      )}
    </div>
  )
}

// Date range filter component
interface DateRangeFilterProps {
  startDate: string
  endDate: string
  onChange: (startDate: string, endDate: string) => void
  label?: string
  className?: string
}

export const DateRangeFilter: React.FC<DateRangeFilterProps> = ({
  startDate,
  endDate,
  onChange,
  label = 'Date Range',
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false)

  const formatDate = (date: string) => {
    if (!date) return ''
    return new Date(date).toLocaleDateString()
  }

  const displayText =
    startDate || endDate
      ? `${formatDate(startDate)} - ${formatDate(endDate)}`
      : 'All dates'

  const presetRanges = [
    {
      label: 'Last 7 days',
      getValue: () => {
        const end = new Date()
        const start = new Date()
        start.setDate(start.getDate() - 7)
        return {
          start: start.toISOString().split('T')[0],
          end: end.toISOString().split('T')[0],
        }
      },
    },
    {
      label: 'Last 30 days',
      getValue: () => {
        const end = new Date()
        const start = new Date()
        start.setDate(start.getDate() - 30)
        return {
          start: start.toISOString().split('T')[0],
          end: end.toISOString().split('T')[0],
        }
      },
    },
    {
      label: 'Last 3 months',
      getValue: () => {
        const end = new Date()
        const start = new Date()
        start.setMonth(start.getMonth() - 3)
        return {
          start: start.toISOString().split('T')[0],
          end: end.toISOString().split('T')[0],
        }
      },
    },
  ]

  return (
    <div className={clsx('relative inline-block text-left', className)}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="inline-flex justify-between items-center w-full px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
      >
        <div className="flex items-center">
          <CalendarDaysIcon className="h-4 w-4 mr-2 text-gray-400" />
          <span>{label}:</span>
          <span className="ml-1 font-normal truncate max-w-32">
            {displayText}
          </span>
        </div>
        <ChevronDownIcon
          className={clsx(
            'h-4 w-4 text-gray-400 transition-transform ml-2',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      {isOpen && (
        <div className="absolute right-0 z-10 mt-2 w-80 bg-white rounded-md shadow-lg ring-1 ring-black ring-opacity-5">
          <div className="p-4">
            <div className="space-y-4">
              {/* Preset ranges */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-2">
                  Quick Select
                </h4>
                <div className="space-y-1">
                  {presetRanges.map(preset => (
                    <button
                      key={preset.label}
                      type="button"
                      onClick={() => {
                        const range = preset.getValue()
                        onChange(range.start, range.end)
                        setIsOpen(false)
                      }}
                      className="block w-full text-left px-2 py-1 text-sm text-gray-700 hover:bg-gray-100 rounded"
                    >
                      {preset.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Custom range */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-2">
                  Custom Range
                </h4>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs text-gray-600 mb-1">
                      From
                    </label>
                    <input
                      type="date"
                      value={startDate}
                      onChange={e => onChange(e.target.value, endDate)}
                      className="w-full px-2 py-1 text-sm border border-gray-300 rounded focus:ring-primary-500 focus:border-primary-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-600 mb-1">
                      To
                    </label>
                    <input
                      type="date"
                      value={endDate}
                      onChange={e => onChange(startDate, e.target.value)}
                      className="w-full px-2 py-1 text-sm border border-gray-300 rounded focus:ring-primary-500 focus:border-primary-500"
                    />
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex justify-between pt-2 border-t border-gray-200">
                <button
                  type="button"
                  onClick={() => {
                    onChange('', '')
                    setIsOpen(false)
                  }}
                  className="text-sm text-gray-500 hover:text-gray-700"
                >
                  Clear
                </button>
                <button
                  type="button"
                  onClick={() => setIsOpen(false)}
                  className="text-sm text-primary-600 hover:text-primary-500"
                >
                  Done
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {isOpen && (
        <div className="fixed inset-0 z-5" onClick={() => setIsOpen(false)} />
      )}
    </div>
  )
}

// Combined search and filter bar
interface SearchAndFilterBarProps {
  searchValue: string
  onSearchChange: (value: string) => void
  searchPlaceholder?: string
  filters?: React.ReactNode
  resultsCount?: React.ReactNode
  className?: string
}

export const SearchAndFilterBar: React.FC<SearchAndFilterBarProps> = ({
  searchValue,
  onSearchChange,
  searchPlaceholder = 'Search...',
  filters,
  resultsCount,
  className = '',
}) => {
  return (
    <div className={clsx('space-y-4', className)}>
      {/* Search and filters row */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex-1 max-w-md">
          <SearchBox
            value={searchValue}
            onChange={onSearchChange}
            placeholder={searchPlaceholder}
          />
        </div>
        {filters && <div className="flex flex-wrap gap-2">{filters}</div>}
      </div>

      {/* Results counter */}
      {resultsCount && (
        <div className="flex justify-between items-center">{resultsCount}</div>
      )}
    </div>
  )
}
