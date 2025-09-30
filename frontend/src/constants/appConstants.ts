// Frontend Constants
// Centralized constants to eliminate magic numbers and strings

export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  MIN_PAGE_SIZE: 1,
} as const;

export const TIME_INTERVALS = {
  REAL_TIME_UPDATE_INTERVAL: 30000, // 30 seconds
  INACTIVITY_PAUSE_THRESHOLD: 300000, // 5 minutes
  DEBOUNCE_DELAY: 300,
  AUTO_SAVE_DELAY: 2000,
} as const;

export const UI_LIMITS = {
  MAX_SEARCH_LENGTH: 1000,
  MAX_FILE_SIZE_MB: 10,
  MAX_UPLOAD_FILES: 5,
  TOAST_DURATION: 5000,
} as const;

export const VALIDATION_MESSAGES = {
  REQUIRED_FIELD: 'This field is required',
  INVALID_EMAIL: 'Please enter a valid email address',
  PASSWORD_TOO_SHORT: 'Password must be at least 8 characters',
  FILE_TOO_LARGE: 'File size cannot exceed 10MB',
  SEARCH_TOO_LONG: 'Search text cannot exceed 1000 characters',
} as const;

export const API_ENDPOINTS = {
  AUDIT_LOGS: '/api/audit/logs',
  EXPORT_AUDIT: '/api/audit/export',
  EXPORT_STATUS: '/api/audit/export/{id}/status',
  DOWNLOAD_EXPORT: '/api/audit/export/{token}/download',
} as const;

export const ERROR_CODES = {
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN', 
  RATE_LIMITED: 'RATE_LIMITED',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
} as const;