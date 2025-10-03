/**
 * Type Guards for Runtime Type Checking
 *
 * This file provides type guards and utility functions for safe runtime
 * type validation throughout the application.
 *
 * @module typeGuards
 */

import { isAxiosError as axiosIsAxiosError } from 'axios';

import type {
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
  CreateProjectRequest,
  UpdateProjectRequest,
} from './api';

/**
 * Check if value is a non-null object
 */
export function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

/**
 * Check if value has a specific property
 */
export function hasProperty<K extends string>(
  obj: unknown,
  key: K
): obj is Record<K, unknown> {
  return isObject(obj) && key in obj;
}

/**
 * Type guard for CreateOrganizationRequest
 */
export function isCreateOrganizationRequest(
  data: unknown
): data is CreateOrganizationRequest {
  return (
    isObject(data) &&
    hasProperty(data, 'name') &&
    typeof data.name === 'string' &&
    data.name.length > 0 &&
    hasProperty(data, 'slug') &&
    typeof data.slug === 'string' &&
    data.slug.length > 0
  );
}

/**
 * Type guard for UpdateOrganizationRequest
 */
export function isUpdateOrganizationRequest(
  data: unknown
): data is UpdateOrganizationRequest {
  if (!isObject(data)) return false;

  // At least one field must be present
  const hasValidField =
    (hasProperty(data, 'name') && typeof data.name === 'string') ||
    (hasProperty(data, 'description') && (typeof data.description === 'string' || data.description === null)) ||
    (hasProperty(data, 'settings') && isObject(data.settings));

  return hasValidField;
}

/**
 * Type guard for CreateProjectRequest
 */
export function isCreateProjectRequest(
  data: unknown
): data is CreateProjectRequest {
  return (
    isObject(data) &&
    hasProperty(data, 'name') &&
    typeof data.name === 'string' &&
    data.name.length > 0 &&
    hasProperty(data, 'slug') &&
    typeof data.slug === 'string' &&
    data.slug.length > 0 &&
    hasProperty(data, 'workspaceId') &&
    typeof data.workspaceId === 'string'
  );
}

/**
 * Type guard for UpdateProjectRequest
 */
export function isUpdateProjectRequest(
  data: unknown
): data is UpdateProjectRequest {
  if (!isObject(data)) return false;

  // At least one field must be present
  const hasValidField =
    (hasProperty(data, 'name') && typeof data.name === 'string') ||
    (hasProperty(data, 'description') && (typeof data.description === 'string' || data.description === null)) ||
    (hasProperty(data, 'status') && typeof data.status === 'string');

  return hasValidField;
}

/**
 * API Error Response type
 */
export interface ApiErrorResponse {
  message: string;
  code?: string;
  details?: Record<string, unknown>;
  status?: number;
}

/**
 * Type guard for API error responses
 */
export function isApiErrorResponse(data: unknown): data is ApiErrorResponse {
  return (
    isObject(data) &&
    hasProperty(data, 'message') &&
    typeof data.message === 'string'
  );
}

/**
 * Re-export Axios error type guard
 */
export const isAxiosError = axiosIsAxiosError;

/**
 * Safely get error message from unknown error
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (isAxiosError(error)) {
    const data = error.response?.data;
    if (isApiErrorResponse(data)) {
      return data.message;
    }
    return error.message;
  }
  if (isObject(error) && hasProperty(error, 'message')) {
    const msg = error.message;
    if (typeof msg === 'string') {
      return msg;
    }
  }
  return 'An unknown error occurred';
}

/**
 * WebSocket Message base type
 */
export interface WebSocketMessage<T = unknown> {
  type: string;
  payload: T;
  timestamp: number;
  correlationId?: string;
}

/**
 * Type guard for WebSocket messages
 */
export function isWebSocketMessage(data: unknown): data is WebSocketMessage {
  return (
    isObject(data) &&
    hasProperty(data, 'type') &&
    typeof data.type === 'string' &&
    hasProperty(data, 'payload') &&
    hasProperty(data, 'timestamp') &&
    typeof data.timestamp === 'number'
  );
}

/**
 * User Joined Payload
 */
export interface UserJoinedPayload {
  userId: string;
  userName: string;
  avatar?: string;
}

/**
 * Type guard for UserJoinedPayload
 */
export function isUserJoinedPayload(data: unknown): data is UserJoinedPayload {
  return (
    isObject(data) &&
    hasProperty(data, 'userId') &&
    typeof data.userId === 'string' &&
    hasProperty(data, 'userName') &&
    typeof data.userName === 'string'
  );
}

/**
 * User Left Payload
 */
export interface UserLeftPayload {
  userId: string;
}

/**
 * Type guard for UserLeftPayload
 */
export function isUserLeftPayload(data: unknown): data is UserLeftPayload {
  return (
    isObject(data) &&
    hasProperty(data, 'userId') &&
    typeof data.userId === 'string'
  );
}

/**
 * Message Payload
 */
export interface MessagePayload {
  messageId: string;
  content: string;
  senderId: string;
  timestamp: number;
}

/**
 * Type guard for MessagePayload
 */
export function isMessagePayload(data: unknown): data is MessagePayload {
  return (
    isObject(data) &&
    hasProperty(data, 'messageId') &&
    typeof data.messageId === 'string' &&
    hasProperty(data, 'content') &&
    typeof data.content === 'string' &&
    hasProperty(data, 'senderId') &&
    typeof data.senderId === 'string' &&
    hasProperty(data, 'timestamp') &&
    typeof data.timestamp === 'number'
  );
}

/**
 * Check if a string is a valid non-empty string
 */
export function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

/**
 * Check if a value is a valid UUID
 */
export function isUUID(value: unknown): value is string {
  if (typeof value !== 'string') return false;
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  return uuidRegex.test(value);
}

/**
 * Check if a value is a valid email
 */
export function isEmail(value: unknown): value is string {
  if (typeof value !== 'string') return false;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(value);
}

/**
 * Check if a value is a valid ISO date string
 */
export function isISODateString(value: unknown): value is string {
  if (typeof value !== 'string') return false;
  const date = new Date(value);
  return !isNaN(date.getTime()) && date.toISOString() === value;
}

/**
 * Type guard for arrays of a specific type
 */
export function isArrayOf<T>(
  value: unknown,
  guard: (item: unknown) => item is T
): value is T[] {
  return Array.isArray(value) && value.every(guard);
}

/**
 * Safely stringify any value for logging/debugging
 */
export function safeStringify(value: unknown): string {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return Object.prototype.toString.call(value);
  }
}
