/**
 * Security utilities for frontend input validation and sanitization.
 * Implements clean code principles while maintaining strict security standards.
 */

// Security patterns for validation
const SECURITY_PATTERNS = {
  EMAIL: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  PHONE: /^\+?[1-9]\d{1,14}$/, // E.164 format
  CREDIT_CARD: /\b(?:\d[ -]*?){13,19}\b/,
  SSN: /\b\d{3}-?\d{2}-?\d{4}\b/,
  SQL_INJECTION: /(\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|SCRIPT)\b|--|\/\*|\*\/|;|'|"|xp_|sp_)/i,
  XSS: /<script|<\/script|javascript:|on\w+\s*=|eval\(|expression\(/i,
} as const;

// Password strength requirements
const PASSWORD_REQUIREMENTS = {
  MIN_LENGTH: 12,
  REQUIRE_UPPERCASE: true,
  REQUIRE_LOWERCASE: true,
  REQUIRE_NUMBERS: true,
  REQUIRE_SPECIAL_CHARS: true,
  SPECIAL_CHARS: '!@#$%^&*()_+-=[]{}|;:,.<>?',
} as const;

/**
 * Result interface for validation operations.
 */
interface ValidationResult {
  isValid: boolean;
  errors: string[];
  sanitized?: string;
}

/**
 * Configuration for input sanitization.
 */
interface SanitizationConfig {
  removeHtml?: boolean;
  removeControlChars?: boolean;
  normalizeWhitespace?: boolean;
  maxLength?: number;
  allowedChars?: RegExp;
}

/**
 * Validates and sanitizes user input to prevent security vulnerabilities.
 */
export class SecurityValidator {
  /**
   * Validates email address format and security.
   */
  static validateEmail(email: string): ValidationResult {
    const errors: string[] = [];

    if (!email || typeof email !== 'string') {
      errors.push('Email is required');
      return { isValid: false, errors };
    }

    const trimmedEmail = email.trim().toLowerCase();

    if (!SECURITY_PATTERNS.EMAIL.test(trimmedEmail)) {
      errors.push('Invalid email format');
    }

    if (trimmedEmail.length > 254) { // RFC 5321 limit
      errors.push('Email address too long');
    }

    // Check for suspicious patterns
    if (this.containsSuspiciousPatterns(trimmedEmail)) {
      errors.push('Email contains invalid characters');
    }

    return {
      isValid: errors.length === 0,
      errors,
      sanitized: errors.length === 0 ? trimmedEmail : undefined,
    };
  }

  /**
   * Validates password strength according to security policy.
   */
  static validatePassword(password: string): ValidationResult {
    const errors: string[] = [];

    if (!password || typeof password !== 'string') {
      errors.push('Password is required');
      return { isValid: false, errors };
    }

    if (password.length < PASSWORD_REQUIREMENTS.MIN_LENGTH) {
      errors.push(`Password must be at least ${PASSWORD_REQUIREMENTS.MIN_LENGTH} characters long`);
    }

    if (PASSWORD_REQUIREMENTS.REQUIRE_UPPERCASE && !/[A-Z]/.test(password)) {
      errors.push('Password must contain at least one uppercase letter');
    }

    if (PASSWORD_REQUIREMENTS.REQUIRE_LOWERCASE && !/[a-z]/.test(password)) {
      errors.push('Password must contain at least one lowercase letter');
    }

    if (PASSWORD_REQUIREMENTS.REQUIRE_NUMBERS && !/\d/.test(password)) {
      errors.push('Password must contain at least one number');
    }

    if (PASSWORD_REQUIREMENTS.REQUIRE_SPECIAL_CHARS) {
      const specialCharRegex = new RegExp(`[${PASSWORD_REQUIREMENTS.SPECIAL_CHARS.replace(/[[\]\\-]/g, '\\$&')}]`);
      if (!specialCharRegex.test(password)) {
        errors.push('Password must contain at least one special character');
      }
    }

    // Check for common weak passwords
    const commonPasswords = [
      'password123', 'admin123', 'qwerty123', 'letmein123',
      'welcome123', 'password1234', '123456789012'
    ];

    if (commonPasswords.some(common => password.toLowerCase().includes(common))) {
      errors.push('Password contains common weak patterns');
    }

    // Check for keyboard patterns
    if (this.hasKeyboardPatterns(password)) {
      errors.push('Password contains keyboard patterns');
    }

    return { isValid: errors.length === 0, errors };
  }

  /**
   * Sanitizes input to prevent XSS and injection attacks.
   */
  static sanitizeInput(input: string, config: SanitizationConfig = {}): string {
    if (!input || typeof input !== 'string') {
      return '';
    }

    const {
      removeHtml = true,
      removeControlChars = true,
      normalizeWhitespace = true,
      maxLength,
      allowedChars,
    } = config;

    let sanitized = input;

    // Remove HTML tags if specified
    if (removeHtml) {
      sanitized = sanitized.replace(/<[^>]*>/g, '');
    }

    // Remove control characters
    if (removeControlChars) {
      sanitized = sanitized.replace(/[\x00-\x1F\x7F]/g, '');
    }

    // Normalize whitespace
    if (normalizeWhitespace) {
      sanitized = sanitized.replace(/\s+/g, ' ').trim();
    }

    // Apply character allowlist if specified
    if (allowedChars) {
      sanitized = sanitized.replace(new RegExp(`[^${allowedChars.source}]`, 'g'), '');
    }

    // Truncate if max length specified
    if (maxLength && sanitized.length > maxLength) {
      sanitized = sanitized.substring(0, maxLength);
    }

    return sanitized;
  }

  /**
   * Checks input for SQL injection patterns.
   */
  static checkSqlInjection(input: string): boolean {
    if (!input || typeof input !== 'string') {
      return false;
    }

    return SECURITY_PATTERNS.SQL_INJECTION.test(input);
  }

  /**
   * Checks input for XSS patterns.
   */
  static checkXss(input: string): boolean {
    if (!input || typeof input !== 'string') {
      return false;
    }

    return SECURITY_PATTERNS.XSS.test(input);
  }

  /**
   * Redacts sensitive information for logging.
   */
  static redactSensitiveData(input: string): string {
    if (!input || typeof input !== 'string') {
      return input;
    }

    let redacted = input;

    // Redact email addresses
    redacted = redacted.replace(SECURITY_PATTERNS.EMAIL, '[EMAIL_REDACTED]');

    // Redact phone numbers
    redacted = redacted.replace(SECURITY_PATTERNS.PHONE, '[PHONE_REDACTED]');

    // Redact credit card numbers
    redacted = redacted.replace(SECURITY_PATTERNS.CREDIT_CARD, '[CARD_REDACTED]');

    // Redact SSN
    redacted = redacted.replace(SECURITY_PATTERNS.SSN, '[SSN_REDACTED]');

    return redacted;
  }

  /**
   * Validates file upload security.
   */
  static validateFile(file: File): ValidationResult {
    const errors: string[] = [];

    // Check file size (10MB limit)
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      errors.push('File size exceeds 10MB limit');
    }

    // Check file type
    const allowedTypes = [
      'image/jpeg', 'image/png', 'image/gif', 'image/webp',
      'application/pdf', 'text/plain', 'text/csv',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    if (!allowedTypes.includes(file.type)) {
      errors.push('File type not allowed');
    }

    // Check file name for suspicious patterns
    if (this.containsSuspiciousPatterns(file.name)) {
      errors.push('File name contains invalid characters');
    }

    return { isValid: errors.length === 0, errors };
  }

  /**
   * Generates a secure random token for CSRF protection.
   */
  static generateCsrfToken(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Validates CSRF token.
   */
  static validateCsrfToken(token: string, expectedToken: string): boolean {
    if (!token || !expectedToken || typeof token !== 'string' || typeof expectedToken !== 'string') {
      return false;
    }

    // Use constant-time comparison to prevent timing attacks
    if (token.length !== expectedToken.length) {
      return false;
    }

    let result = 0;
    for (let i = 0; i < token.length; i++) {
      result |= token.charCodeAt(i) ^ expectedToken.charCodeAt(i);
    }

    return result === 0;
  }

  /**
   * Checks for suspicious patterns in input.
   */
  private static containsSuspiciousPatterns(input: string): boolean {
    const suspiciousPatterns = [
      /\.\.\//, // Path traversal
      /\x00/, // Null bytes
      /[\x01-\x08\x0B\x0C\x0E-\x1F\x7F]/, // Control characters
      /%[0-9a-fA-F]{2}/, // URL encoded characters
      /\\u[0-9a-fA-F]{4}/, // Unicode escapes
    ];

    return suspiciousPatterns.some(pattern => pattern.test(input));
  }

  /**
   * Checks for keyboard patterns in passwords.
   */
  private static hasKeyboardPatterns(password: string): boolean {
    const keyboardPatterns = [
      'qwerty', 'asdfgh', 'zxcvbn', '123456', 'abcdef',
      'qwertyuiop', 'asdfghjkl', 'zxcvbnm'
    ];

    const lowerPassword = password.toLowerCase();
    return keyboardPatterns.some(pattern =>
      lowerPassword.includes(pattern) ||
      lowerPassword.includes(pattern.split('').reverse().join(''))
    );
  }
}

/**
 * React hook for form validation with security checks.
 */
export const useSecureValidation = () => {
  const validateField = (value: string, type: 'email' | 'password' | 'text', config?: SanitizationConfig) => {
    switch (type) {
      case 'email':
        return SecurityValidator.validateEmail(value);
      case 'password':
        return SecurityValidator.validatePassword(value);
      case 'text':
        const sanitized = SecurityValidator.sanitizeInput(value, config);
        return {
          isValid: !SecurityValidator.checkXss(sanitized) && !SecurityValidator.checkSqlInjection(sanitized),
          errors: [],
          sanitized,
        };
      default:
        return { isValid: false, errors: ['Unknown validation type'] };
    }
  };

  return { validateField };
};

/**
 * Secure storage utilities for sensitive data.
 */
export class SecureStorage {
  /**
   * Stores data securely in sessionStorage with encryption.
   */
  static setSecureItem(key: string, value: string): void {
    try {
      // In production, implement proper encryption
      const encryptedValue = btoa(value); // Basic base64 encoding (replace with real encryption)
      sessionStorage.setItem(key, encryptedValue);
    } catch (error) {
      console.error('Failed to store secure item:', error);
    }
  }

  /**
   * Retrieves and decrypts data from sessionStorage.
   */
  static getSecureItem(key: string): string | null {
    try {
      const encryptedValue = sessionStorage.getItem(key);
      if (!encryptedValue) return null;

      // In production, implement proper decryption
      return atob(encryptedValue); // Basic base64 decoding (replace with real decryption)
    } catch (error) {
      console.error('Failed to retrieve secure item:', error);
      return null;
    }
  }

  /**
   * Removes item from secure storage.
   */
  static removeSecureItem(key: string): void {
    try {
      sessionStorage.removeItem(key);
    } catch (error) {
      console.error('Failed to remove secure item:', error);
    }
  }

  /**
   * Clears all secure storage.
   */
  static clearSecureStorage(): void {
    try {
      sessionStorage.clear();
    } catch (error) {
      console.error('Failed to clear secure storage:', error);
    }
  }
}

/**
 * Content Security Policy utilities.
 */
export const CSP_UTILITIES = {
  /**
   * Generates a nonce for inline scripts.
   */
  generateNonce: (): string => {
    return SecurityValidator.generateCsrfToken();
  },

  /**
   * Validates if a URL is safe for external requests.
   */
  validateExternalUrl: (url: string): boolean => {
    try {
      const parsedUrl = new URL(url);
      const allowedDomains = ['api.stripe.com', 'api.github.com']; // Configure as needed

      return allowedDomains.some(domain => parsedUrl.hostname.endsWith(domain));
    } catch {
      return false;
    }
  },
} as const;