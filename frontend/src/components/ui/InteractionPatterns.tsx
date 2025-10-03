import React, { useState, useEffect, useRef } from 'react'
import { clsx } from 'clsx'
import {
  ChevronDownIcon,
  MagnifyingGlassIcon,
  XMarkIcon,
  CheckIcon,
  ArrowUpIcon,
} from '@heroicons/react/24/outline'

/**
 * Enhanced Dropdown with search, multi-select, and keyboard navigation
 */
interface DropdownOption {
  value: string
  label: string
  disabled?: boolean
  icon?: React.ComponentType<{ className?: string }>
}

interface EnhancedDropdownProps {
  options: DropdownOption[]
  value?: string | string[]
  onChange: (value: string | string[]) => void
  placeholder?: string
  searchable?: boolean
  multiSelect?: boolean
  disabled?: boolean
  className?: string
  maxHeight?: string
}

export const EnhancedDropdown: React.FC<EnhancedDropdownProps> = ({
  options,
  value,
  onChange,
  placeholder = 'Select an option',
  searchable = false,
  multiSelect = false,
  disabled = false,
  className,
  maxHeight = 'max-h-60',
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [focusedIndex, setFocusedIndex] = useState(-1)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)

  const filteredOptions = options.filter(option =>
    option.label.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const selectedValues = Array.isArray(value) ? value : value ? [value] : []

  const handleToggle = () => {
    if (disabled) return
    setIsOpen(!isOpen)
    if (!isOpen && searchable) {
      setTimeout(() => searchRef.current?.focus(), 100)
    }
  }

  const handleSelect = (optionValue: string) => {
    if (multiSelect) {
      const newValues = selectedValues.includes(optionValue)
        ? selectedValues.filter(v => v !== optionValue)
        : [...selectedValues, optionValue]
      onChange(newValues)
    } else {
      onChange(optionValue)
      setIsOpen(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setFocusedIndex(prev =>
          prev < filteredOptions.length - 1 ? prev + 1 : 0
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setFocusedIndex(prev =>
          prev > 0 ? prev - 1 : filteredOptions.length - 1
        )
        break
      case 'Enter':
        e.preventDefault()
        if (focusedIndex >= 0) {
          handleSelect(filteredOptions[focusedIndex].value)
        }
        break
      case 'Escape':
        setIsOpen(false)
        break
    }
  }

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getDisplayText = () => {
    if (selectedValues.length === 0) return placeholder
    if (multiSelect) {
      if (selectedValues.length === 1) {
        return (
          options.find(opt => opt.value === selectedValues[0])?.label ||
          selectedValues[0]
        )
      }
      return `${selectedValues.length} selected`
    }
    return (
      options.find(opt => opt.value === selectedValues[0])?.label ||
      selectedValues[0]
    )
  }

  return (
    <div className={clsx('relative', className)} ref={dropdownRef}>
      <button
        type="button"
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        className={clsx(
          'w-full flex items-center justify-between px-3 py-2 bg-white border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors',
          disabled
            ? 'bg-gray-50 text-gray-400 cursor-not-allowed'
            : 'hover:border-gray-400',
          isOpen ? 'border-blue-500 ring-2 ring-blue-500' : 'border-gray-300'
        )}
      >
        <span className={clsx(selectedValues.length === 0 && 'text-gray-500')}>
          {getDisplayText()}
        </span>
        <ChevronDownIcon
          className={clsx(
            'h-4 w-4 transition-transform',
            isOpen && 'rotate-180'
          )}
        />
      </button>

      {isOpen && (
        <div
          className={clsx(
            'absolute z-10 mt-1 w-full bg-white border border-gray-300 rounded-md shadow-lg',
            maxHeight,
            'overflow-auto'
          )}
        >
          {searchable && (
            <div className="p-2 border-b border-gray-200">
              <div className="relative">
                <MagnifyingGlassIcon className="absolute left-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  ref={searchRef}
                  type="text"
                  value={searchTerm}
                  onChange={e => setSearchTerm(e.target.value)}
                  placeholder="Search options..."
                  className="w-full pl-8 pr-3 py-1 border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
              </div>
            </div>
          )}

          <div className="py-1">
            {filteredOptions.length === 0 ? (
              <div className="px-3 py-2 text-gray-500 text-sm">
                No options found
              </div>
            ) : (
              filteredOptions.map((option, index) => {
                const isSelected = selectedValues.includes(option.value)
                const isFocused = index === focusedIndex

                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() =>
                      !option.disabled && handleSelect(option.value)
                    }
                    disabled={option.disabled}
                    className={clsx(
                      'w-full px-3 py-2 text-left flex items-center justify-between transition-colors',
                      option.disabled
                        ? 'text-gray-400 cursor-not-allowed'
                        : 'hover:bg-gray-100',
                      isFocused && 'bg-blue-50',
                      isSelected && 'bg-blue-100 text-blue-900'
                    )}
                  >
                    <div className="flex items-center">
                      {option.icon && <option.icon className="h-4 w-4 mr-2" />}
                      <span>{option.label}</span>
                    </div>
                    {isSelected && multiSelect && (
                      <CheckIcon className="h-4 w-4 text-blue-600" />
                    )}
                  </button>
                )
              })
            )}
          </div>
        </div>
      )}
    </div>
  )
}

/**
 * Enhanced Command Palette for quick actions
 */
interface CommandPaletteItem {
  id: string
  title: string
  subtitle?: string
  action: () => void
  icon?: React.ComponentType<{ className?: string }>
  keywords?: string[]
  group?: string
}

interface CommandPaletteProps {
  isOpen: boolean
  onClose: () => void
  items: CommandPaletteItem[]
  placeholder?: string
}

export const CommandPalette: React.FC<CommandPaletteProps> = ({
  isOpen,
  onClose,
  items,
  placeholder = 'Type a command or search...',
}) => {
  const [search, setSearch] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)

  const filteredItems = items.filter(item => {
    const searchLower = search.toLowerCase()
    return (
      item.title.toLowerCase().includes(searchLower) ||
      item.subtitle?.toLowerCase().includes(searchLower) ||
      item.keywords?.some(keyword =>
        keyword.toLowerCase().includes(searchLower)
      )
    )
  })

  // Group filtered items
  const groupedItems = filteredItems.reduce(
    (acc, item) => {
      const group = item.group || 'Other'
      if (!acc[group]) acc[group] = []
      acc[group].push(item)
      return acc
    },
    {} as Record<string, CommandPaletteItem[]>
  )

  const handleKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex(prev =>
          prev < filteredItems.length - 1 ? prev + 1 : 0
        )
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex(prev =>
          prev > 0 ? prev - 1 : filteredItems.length - 1
        )
        break
      case 'Enter':
        e.preventDefault()
        if (filteredItems[selectedIndex]) {
          filteredItems[selectedIndex].action()
          onClose()
        }
        break
      case 'Escape':
        onClose()
        break
    }
  }

  useEffect(() => {
    setSelectedIndex(0)
  }, [search])

  useEffect(() => {
    if (isOpen) {
      inputRef.current?.focus()
      setSearch('')
    }
  }, [isOpen])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-screen items-start justify-center p-4 pt-16">
        <div
          className="fixed inset-0 bg-gray-900 bg-opacity-50 transition-opacity"
          onClick={onClose}
        />
        <div className="relative w-full max-w-2xl bg-white rounded-lg shadow-xl">
          {/* Search Input */}
          <div className="flex items-center px-4 py-3 border-b border-gray-200">
            <MagnifyingGlassIcon className="h-5 w-5 text-gray-400 mr-3" />
            <input
              ref={inputRef}
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={placeholder}
              className="flex-1 outline-none text-gray-900"
            />
            <button
              onClick={onClose}
              className="ml-3 p-1 text-gray-400 hover:text-gray-600"
            >
              <XMarkIcon className="h-4 w-4" />
            </button>
          </div>

          {/* Results */}
          <div className="max-h-96 overflow-y-auto p-2">
            {filteredItems.length === 0 ? (
              <div className="px-4 py-8 text-center text-gray-500">
                No results found for "{search}"
              </div>
            ) : (
              Object.entries(groupedItems).map(([group, groupItems]) => (
                <div key={group} className="mb-4">
                  <div className="px-2 py-1 text-xs font-semibold text-gray-500 uppercase tracking-wide">
                    {group}
                  </div>
                  {groupItems.map((item, _index) => {
                    const globalIndex = filteredItems.indexOf(item)
                    const isSelected = globalIndex === selectedIndex

                    return (
                      <button
                        key={item.id}
                        onClick={() => {
                          item.action()
                          onClose()
                        }}
                        className={clsx(
                          'w-full px-3 py-2 text-left rounded-md flex items-center transition-colors',
                          isSelected
                            ? 'bg-blue-100 text-blue-900'
                            : 'hover:bg-gray-100'
                        )}
                      >
                        {item.icon && (
                          <item.icon className="h-5 w-5 mr-3 text-gray-400" />
                        )}
                        <div>
                          <div className="font-medium">{item.title}</div>
                          {item.subtitle && (
                            <div className="text-sm text-gray-500">
                              {item.subtitle}
                            </div>
                          )}
                        </div>
                      </button>
                    )
                  })}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

/**
 * Contextual Action Menu (Right-click menu)
 */
interface ContextMenuAction {
  id: string
  label: string
  action: () => void
  icon?: React.ComponentType<{ className?: string }>
  disabled?: boolean
  destructive?: boolean
  divider?: boolean
}

interface ContextMenuProps {
  isOpen: boolean
  position: { x: number; y: number }
  onClose: () => void
  actions: ContextMenuAction[]
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
  isOpen,
  position,
  onClose,
  actions,
}) => {
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose()
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
      document.addEventListener('keydown', handleEscape)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div
      ref={menuRef}
      className="fixed z-50 bg-white border border-gray-200 rounded-md shadow-lg py-1 min-w-48"
      style={{
        left: position.x,
        top: position.y,
      }}
    >
      {actions.map((action, _index) => (
        <React.Fragment key={action.id}>
          {action.divider ? (
            <div className="border-t border-gray-200 my-1" />
          ) : (
            <button
              onClick={() => {
                if (!action.disabled) {
                  action.action()
                  onClose()
                }
              }}
              disabled={action.disabled}
              className={clsx(
                'w-full px-3 py-2 text-left flex items-center text-sm transition-colors',
                action.disabled
                  ? 'text-gray-400 cursor-not-allowed'
                  : action.destructive
                    ? 'text-red-600 hover:bg-red-50'
                    : 'text-gray-900 hover:bg-gray-100'
              )}
            >
              {action.icon && (
                <action.icon
                  className={clsx(
                    'h-4 w-4 mr-3',
                    action.destructive ? 'text-red-500' : 'text-gray-500'
                  )}
                />
              )}
              {action.label}
            </button>
          )}
        </React.Fragment>
      ))}
    </div>
  )
}

/**
 * Floating Action Button with expandable menu
 */
interface FloatingAction {
  id: string
  label: string
  action: () => void
  icon: React.ComponentType<{ className?: string }>
}

interface FloatingActionButtonProps {
  primaryAction: FloatingAction
  secondaryActions?: FloatingAction[]
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left'
  className?: string
}

export const FloatingActionButton: React.FC<FloatingActionButtonProps> = ({
  primaryAction,
  secondaryActions = [],
  position = 'bottom-right',
  className,
}) => {
  const [isExpanded, setIsExpanded] = useState(false)

  const positionClasses = {
    'bottom-right': 'bottom-6 right-6',
    'bottom-left': 'bottom-6 left-6',
    'top-right': 'top-6 right-6',
    'top-left': 'top-6 left-6',
  }

  const handlePrimaryAction = () => {
    if (secondaryActions.length > 0) {
      setIsExpanded(!isExpanded)
    } else {
      primaryAction.action()
    }
  }

  return (
    <div className={clsx('fixed z-40', positionClasses[position], className)}>
      {/* Secondary Actions */}
      {isExpanded && secondaryActions.length > 0 && (
        <div className="absolute bottom-16 right-0 space-y-2">
          {secondaryActions.map((action, index) => (
            <div
              key={action.id}
              className="flex items-center animate-fade-in-up"
              style={{ animationDelay: `${index * 50}ms` }}
            >
              <span className="mr-3 px-2 py-1 bg-gray-900 text-white text-xs rounded opacity-90">
                {action.label}
              </span>
              <button
                onClick={() => {
                  action.action()
                  setIsExpanded(false)
                }}
                className="w-12 h-12 bg-white border border-gray-300 rounded-full shadow-lg flex items-center justify-center hover:shadow-xl transition-shadow"
              >
                <action.icon className="h-5 w-5 text-gray-600" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Primary Action Button */}
      <button
        onClick={handlePrimaryAction}
        className={clsx(
          'w-14 h-14 bg-blue-600 rounded-full shadow-lg flex items-center justify-center text-white hover:bg-blue-700 hover:shadow-xl transition-all',
          isExpanded && 'rotate-45'
        )}
      >
        <primaryAction.icon className="h-6 w-6" />
      </button>
    </div>
  )
}

/**
 * Scroll-to-top button that appears when scrolling
 */
export const ScrollToTop: React.FC<{ threshold?: number }> = ({
  threshold = 400,
}) => {
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    const handleScroll = () => {
      setIsVisible(window.scrollY > threshold)
    }

    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [threshold])

  const scrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    })
  }

  if (!isVisible) return null

  return (
    <button
      onClick={scrollToTop}
      className="fixed bottom-6 right-20 z-30 w-12 h-12 bg-gray-900 bg-opacity-80 text-white rounded-full shadow-lg hover:bg-opacity-100 transition-opacity flex items-center justify-center"
      title="Scroll to top"
    >
      <ArrowUpIcon className="h-5 w-5" />
    </button>
  )
}
