import { FetchBaseQueryError } from '@reduxjs/toolkit/query';
import { SerializedError } from '@reduxjs/toolkit';

export function isFetchBaseQueryError(
  error: unknown
): error is FetchBaseQueryError {
  return typeof error === 'object' && error != null && 'status' in error;
}

export function isErrorWithMessage(
  error: unknown
): error is { message: string } {
  return (
    typeof error === 'object' &&
    error != null &&
    'message' in error &&
    typeof (error as any).message === 'string'
  );
}

export function getErrorMessage(error: unknown): string {
  if (isFetchBaseQueryError(error)) {
    const errData = 'error' in error ? error.error : error.data;
    if (typeof errData === 'string') return errData;
    if (typeof errData === 'object' && errData != null && 'message' in errData) {
      return (errData as any).message;
    }
    return 'An error occurred';
  }

  if (isErrorWithMessage(error)) {
    return error.message;
  }

  return 'An unknown error occurred';
}

export function getErrorStatus(error: unknown): number | string | undefined {
  if (isFetchBaseQueryError(error)) {
    return error.status;
  }
  return undefined;
}
