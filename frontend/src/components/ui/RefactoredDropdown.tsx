/**
 * Refactored Dropdown Component
 * 
 * Improvements from original InteractionPatterns.tsx:
 * - Extracted constants to eliminate magic numbers
 * - Broke down into smaller, focused functions
 * - Improved type safety and naming
 * - Added proper error handling
 */

import React, { useState, useEffect, useRef, useCallback } from 'react'
import { clsx } from 'clsx'
import {
  ChevronDownIcon,
  MagnifyingGlassIcon,
  CheckIcon,
} from '@heroicons/react/24/outline'
import { UI_LIMITS } from '@/constants/appConstants'

// Constants for component behavior
const DROPDOWN_CONFIG = {
  MAX_VISIBLE_OPTIONS: 8,
  SEARCH_DEBOUNCE_MS: 300,
  KEYBOARD_NAVIGATION_DELAY: 100,
  FOCUS_TRAP_DELAY: 50,
} as const;

const KEYBOARD_KEYS = {
  ARROW_DOWN: 'ArrowDown',
  ARROW_UP: 'ArrowUp',
  ENTER: 'Enter',
  ESCAPE: 'Escape',
  SPACE: ' ',
  TAB: 'Tab',
} as const;

interface DropdownOption {
  value: string
  label: string
  disabled?: boolean
  icon?: React.ComponentType<{ className?: string }>
  description?: string
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
  error?: string
  'aria-label'?: string
  'data-testid'?: string
}

// Custom hooks for better separation of concerns
const useDropdownState = () => {
  const [isOpen, setIsOpen] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [focusedIndex, setFocusedIndex] = useState(-1)
  
  const resetState = useCallback(() => {
    setSearchTerm('')
    setFocusedIndex(-1)
  }, [])
  
  return {
    isOpen,
    setIsOpen,
    searchTerm,
    setSearchTerm,
    focusedIndex,
    setFocusedIndex,
    resetState,
  }
}

const useDropdownFiltering = (options: DropdownOption[], searchTerm: string) => {
  return React.useMemo(() => {
    if (!searchTerm.trim()) return options
    
    const normalizedSearch = searchTerm.toLowerCase()
    return options.filter(option =>
      option.label.toLowerCase().includes(normalizedSearch) ||
      option.description?.toLowerCase().includes(normalizedSearch)
    )
  }, [options, searchTerm])
}

const useKeyboardNavigation = (
  filteredOptions: DropdownOption[],
  focusedIndex: number,
  setFocusedIndex: React.Dispatch<React.SetStateAction<number>>,
  onSelect: (option: DropdownOption) => void,
  onClose: () => void
) => {
  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    switch (event.key) {
      case KEYBOARD_KEYS.ARROW_DOWN:
        event.preventDefault()
        setFocusedIndex((prev: number) =>
          prev < filteredOptions.length - 1 ? prev + 1 : 0
        )
        break
        
      case KEYBOARD_KEYS.ARROW_UP:
        event.preventDefault()
        setFocusedIndex((prev: number) =>
          prev > 0 ? prev - 1 : filteredOptions.length - 1
        )
        break
        
      case KEYBOARD_KEYS.ENTER:
      case KEYBOARD_KEYS.SPACE:
        event.preventDefault()
        if (focusedIndex >= 0 && focusedIndex < filteredOptions.length) {
          const option = filteredOptions[focusedIndex]
          if (!option.disabled) {
            onSelect(option)
          }
        }
        break
        
      case KEYBOARD_KEYS.ESCAPE:
        onClose()
        break
    }
  }, [filteredOptions, focusedIndex, setFocusedIndex, onSelect, onClose])
  
  return { handleKeyDown }
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
  error,
  'aria-label': ariaLabel,
  'data-testid': testId,
}) => {
  const dropdownRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)
  
  const {
    isOpen,
    setIsOpen,
    searchTerm,
    setSearchTerm,
    focusedIndex,
    setFocusedIndex,
    resetState,
  } = useDropdownState()
  
  const filteredOptions = useDropdownFiltering(options, searchTerm)
  
  const handleSelect = useCallback((option: DropdownOption) => {
    if (option.disabled) return
    
    if (multiSelect) {
      const currentValues = Array.isArray(value) ? value : []
      const newValues = currentValues.includes(option.value)
        ? currentValues.filter(v => v !== option.value)
        : [...currentValues, option.value]
      onChange(newValues)
    } else {
      onChange(option.value)
      setIsOpen(false)
      resetState()
    }
  }, [multiSelect, value, onChange, setIsOpen, resetState])
  
  const handleClose = useCallback(() => {
    setIsOpen(false)
    resetState()
  }, [setIsOpen, resetState])
  
  const { handleKeyDown } = useKeyboardNavigation(
    filteredOptions,
    focusedIndex,
    setFocusedIndex,
    handleSelect,
    handleClose
  )
  
  // Click outside handler
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        handleClose()
      }
    }
    
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
    
    return () => {} // Always return cleanup function
  }, [isOpen, handleClose])
  
  // Focus search input when dropdown opens
  useEffect(() => {
    if (isOpen && searchable && searchRef.current) {
      setTimeout(() => searchRef.current?.focus(), DROPDOWN_CONFIG.FOCUS_TRAP_DELAY)
    }
  }, [isOpen, searchable])
  
  const getDisplayText = () => {
    if (multiSelect && Array.isArray(value)) {
      if (value.length === 0) return placeholder
      if (value.length === 1) {
        const option = options.find(opt => opt.value === value[0])
        return option?.label || value[0]
      }
      return `${value.length} items selected`
    }
    
    if (typeof value === 'string') {
      const option = options.find(opt => opt.value === value)
      return option?.label || value
    }
    
    return placeholder
  }
  
  return (
    <div 
      ref={dropdownRef}
      className={clsx('relative', className)}
      data-testid={testId}
    >
      {/* Trigger Button */}
      <button
        type="button"
        onClick={() => !disabled && setIsOpen(!isOpen)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        aria-label={ariaLabel}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        className={clsx(
          'w-full flex items-center justify-between px-3 py-2 text-left',
          'border border-gray-300 rounded-lg shadow-sm',
          'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500',
          'transition-colors duration-200',
          {
            'bg-gray-50 text-gray-400 cursor-not-allowed': disabled,
            'bg-white hover:bg-gray-50 cursor-pointer': !disabled,
            'border-red-500 focus:ring-red-500 focus:border-red-500': error,
          }
        )}
      >
        <span className="truncate">{getDisplayText()}</span>
        <ChevronDownIcon 
          className={clsx(
            'w-5 h-5 text-gray-400 transition-transform duration-200',
            { 'transform rotate-180': isOpen }
          )}
        />
      </button>
      
      {/* Error Message */}
      {error && (
        <p className="mt-1 text-sm text-red-600">{error}</p>
      )}
      
      {/* Dropdown Menu */}
      {isOpen && (
        <div className={clsx(
          'absolute z-50 w-full mt-1 bg-white border border-gray-300 rounded-lg shadow-lg',
          maxHeight,
          'overflow-hidden'
        )}>
          {/* Search Input */}
          {searchable && (
            <div className="p-3 border-b border-gray-200">
              <div className="relative">
                <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  ref={searchRef}
                  type="text"
                  value={searchTerm}
                  onChange={(e) => {
                    setSearchTerm(e.target.value)
                    setFocusedIndex(-1)
                  }}
                  placeholder="Search options..."
                  className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  maxLength={UI_LIMITS.MAX_SEARCH_LENGTH}
                />
              </div>
            </div>
          )}
          
          {/* Options List */}
          <div className="max-h-48 overflow-y-auto">
            {filteredOptions.length === 0 ? (
              <div className="px-3 py-2 text-sm text-gray-500 text-center">
                No options found
              </div>
            ) : (
              filteredOptions.map((option, index) => (
                <OptionItem
                  key={option.value}
                  option={option}
                  isSelected={multiSelect 
                    ? Array.isArray(value) && value.includes(option.value)
                    : value === option.value
                  }
                  isFocused={index === focusedIndex}
                  multiSelect={multiSelect}
                  onClick={() => handleSelect(option)}
                />
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}

// Extracted component for better readability
interface OptionItemProps {
  option: DropdownOption
  isSelected: boolean
  isFocused: boolean
  multiSelect: boolean
  onClick: () => void
}

const OptionItem: React.FC<OptionItemProps> = ({
  option,
  isSelected,
  isFocused,
  multiSelect,
  onClick,
}) => (
  <div
    onClick={onClick}
    className={clsx(
      'px-3 py-2 cursor-pointer flex items-center justify-between',
      'hover:bg-gray-50 transition-colors duration-150',
      {
        'bg-blue-50 text-blue-700': isSelected && !multiSelect,
        'bg-gray-100': isFocused,
        'opacity-50 cursor-not-allowed': option.disabled,
      }
    )}
  >
    <div className="flex items-center space-x-2">
      {option.icon && <option.icon className="w-4 h-4" />}
      <div>
        <div className="font-medium">{option.label}</div>
        {option.description && (
          <div className="text-sm text-gray-500">{option.description}</div>
        )}
      </div>
    </div>
    
    {multiSelect && isSelected && (
      <CheckIcon className="w-4 h-4 text-blue-600" />
    )}
  </div>
)