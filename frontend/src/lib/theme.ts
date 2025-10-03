/**
 * Global Theme Configuration
 *
 * This file defines the core visual identity for the application.
 * All components should use these tokens for consistent styling.
 *
 */

// Core Style System - Modern, clean, and professional
export const STYLE = 'modern' as const

// Primary Brand Color - Professional blue for trust and reliability
export const COLOR = '#2563eb' as const // blue-600

// Accent Color - Vibrant cyan for highlights and interactions
export const ACCENT_COLOR = '#06b6d4' as const // cyan-500

// Design Token System
export const theme = {
  // Color Palette
  colors: {
    primary: COLOR,
    accent: ACCENT_COLOR,

    // Semantic colors derived from brand
    success: '#16a34a', // green-600
    warning: '#d97706', // amber-600
    error: '#dc2626', // red-600

    // Neutral palette for modern design
    gray: {
      50: '#f9fafb',
      100: '#f3f4f6',
      200: '#e5e7eb',
      300: '#d1d5db',
      400: '#9ca3af',
      500: '#6b7280',
      600: '#4b5563',
      700: '#374151',
      800: '#1f2937',
      900: '#111827',
    },

    // Background system
    background: {
      primary: '#ffffff',
      secondary: '#f9fafb',
      subtle: '#f3f4f6',
    },
  },

  // Typography Scale - Optimized for readability
  typography: {
    // Font sizes - reduced from current oversized headings
    fontSize: {
      xs: '0.75rem', // 12px
      sm: '0.875rem', // 14px
      base: '1rem', // 16px
      lg: '1.125rem', // 18px
      xl: '1.25rem', // 20px
      '2xl': '1.5rem', // 24px - reduced from 4xl
      '3xl': '1.875rem', // 30px - reduced from massive
      '4xl': '2.25rem', // 36px - for hero sections only
    },

    fontWeight: {
      normal: '400',
      medium: '500',
      semibold: '600',
      bold: '700',
    },

    lineHeight: {
      tight: '1.25',
      normal: '1.5',
      relaxed: '1.75',
    },
  },

  // Spacing Scale - Consistent and logical
  spacing: {
    xs: '0.5rem', // 8px
    sm: '0.75rem', // 12px
    base: '1rem', // 16px
    lg: '1.5rem', // 24px
    xl: '2rem', // 32px
    '2xl': '3rem', // 48px
    '3xl': '4rem', // 64px
  },

  // Icon System - Properly sized for hierarchy
  iconSize: {
    xs: 'w-3 h-3', // 12px - for inline text
    sm: 'w-4 h-4', // 16px - for small buttons and compact areas
    base: 'w-5 h-5', // 20px - for normal buttons and navigation
    lg: 'w-6 h-6', // 24px - for section headers (maximum recommended)
    xl: 'w-8 h-8', // 32px - only for major empty states
  },

  // Border Radius - Modern but not excessive
  borderRadius: {
    sm: '0.375rem', // 6px
    base: '0.5rem', // 8px
    lg: '0.75rem', // 12px
    xl: '1rem', // 16px
    '2xl': '1.5rem', // 24px - maximum for most elements
  },

  // Shadow System - Subtle and professional
  shadow: {
    sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
    base: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
    lg: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
    xl: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
  },

  // Animation System - Smooth and purposeful
  animation: {
    duration: {
      fast: '150ms',
      base: '300ms',
      slow: '500ms',
    },

    easing: {
      ease: 'ease',
      easeIn: 'ease-in',
      easeOut: 'ease-out',
      easeInOut: 'ease-in-out',
    },
  },
} as const

// Component Style Variants
export const styleVariants = {
  // Button variants using theme colors
  button: {
    primary: `bg-[${COLOR}] hover:bg-[${COLOR}]/90 text-white`,
    accent: `bg-[${ACCENT_COLOR}] hover:bg-[${ACCENT_COLOR}]/90 text-white`,
    secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-900',
    outline: `border border-[${COLOR}] text-[${COLOR}] hover:bg-[${COLOR}] hover:text-white`,
  },

  // Card variants - clean and minimal
  card: {
    primary: 'bg-white border border-gray-200 shadow-sm',
    elevated: 'bg-white border border-gray-200 shadow-lg',
    subtle: 'bg-gray-50 border border-gray-100',
  },

  // Text color variants
  text: {
    primary: 'text-gray-900',
    secondary: 'text-gray-600',
    muted: 'text-gray-500',
    accent: `text-[${ACCENT_COLOR}]`,
    brand: `text-[${COLOR}]`,
  },
} as const

// Utility functions for consistent styling
export const getButtonClasses = (
  variant: keyof typeof styleVariants.button = 'primary',
  size: 'sm' | 'base' | 'lg' = 'base'
) => {
  const baseClasses =
    'inline-flex items-center justify-center font-medium rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2'

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    base: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base',
  }

  return `${baseClasses} ${styleVariants.button[variant]} ${sizeClasses[size]}`
}

export const getCardClasses = (
  variant: keyof typeof styleVariants.card = 'primary'
) => {
  const baseClasses = 'rounded-xl p-6'
  return `${baseClasses} ${styleVariants.card[variant]}`
}

export const getIconClasses = (size: keyof typeof theme.iconSize = 'base') => {
  return theme.iconSize[size]
}

// Export theme tokens for Tailwind CSS
export const tailwindTheme = {
  colors: {
    primary: {
      DEFAULT: COLOR,
      50: '#eff6ff',
      100: '#dbeafe',
      200: '#bfdbfe',
      300: '#93c5fd',
      400: '#60a5fa',
      500: '#3b82f6',
      600: COLOR,
      700: '#1d4ed8',
      800: '#1e40af',
      900: '#1e3a8a',
    },
    accent: {
      DEFAULT: ACCENT_COLOR,
      50: '#ecfeff',
      100: '#cffafe',
      200: '#a5f3fc',
      300: '#67e8f9',
      400: '#22d3ee',
      500: ACCENT_COLOR,
      600: '#0891b2',
      700: '#0e7490',
      800: '#155e75',
      900: '#164e63',
    },
  },
} as const
