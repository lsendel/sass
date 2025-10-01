import { useEffect, useCallback } from 'react';
import { UseFormSetValue, FieldValues, Path } from 'react-hook-form';
import { toast } from 'sonner';

/**
 * Configuration options for the form draft hook
 */
export interface UseFormDraftOptions {
  /** Duration in milliseconds before draft expires (default: 24 hours) */
  expiryDuration?: number;
  /** Whether to show toast notifications (default: true) */
  showNotifications?: boolean;
  /** Callback when draft is restored */
  onDraftRestored?: () => void;
  /** Callback when draft is saved */
  onDraftSaved?: () => void;
}

/**
 * Stored draft data structure
 */
interface DraftData<T> {
  data: Partial<T>;
  timestamp: number;
}

const DEFAULT_EXPIRY_DURATION = 86400000; // 24 hours in milliseconds

/**
 * Custom hook for managing form drafts in localStorage with automatic save and restore.
 *
 * Features:
 * - Automatic draft loading on mount
 * - Expiry checking with configurable duration
 * - Type-safe draft management
 * - Toast notifications for user feedback
 * - Error handling with logging
 *
 * @template T - The form data type extending FieldValues
 * @param storageKey - Unique key for localStorage
 * @param isOpen - Whether the form is open/visible
 * @param setValue - React Hook Form's setValue function
 * @param options - Configuration options
 *
 * @example
 * ```typescript
 * const { saveDraft, clearDraft } = useFormDraft<CreateOrgFormData>(
 *   'createOrgDraft',
 *   isModalOpen,
 *   setValue,
 *   { expiryDuration: 3600000 } // 1 hour
 * );
 *
 * // Save draft on form change
 * useEffect(() => {
 *   if (formData) saveDraft(formData);
 * }, [formData]);
 *
 * // Clear draft on successful submit
 * const onSubmit = async (data) => {
 *   await createMutation(data);
 *   clearDraft();
 * };
 * ```
 */
export function useFormDraft<T extends FieldValues>(
  storageKey: string,
  isOpen: boolean,
  setValue: UseFormSetValue<T>,
  options: UseFormDraftOptions = {}
) {
  const {
    expiryDuration = DEFAULT_EXPIRY_DURATION,
    showNotifications = true,
    onDraftRestored,
    onDraftSaved,
  } = options;

  /**
   * Restore draft from localStorage on mount
   */
  useEffect(() => {
    if (!isOpen) return;

    const draftJson = localStorage.getItem(storageKey);
    if (!draftJson) return;

    try {
      const draft: DraftData<T> = JSON.parse(draftJson);
      const isExpired = Date.now() - draft.timestamp > expiryDuration;

      if (isExpired) {
        localStorage.removeItem(storageKey);
        return;
      }

      // Restore each field from draft
      Object.entries(draft.data).forEach(([key, value]) => {
        setValue(key as Path<T>, value as any);
      });

      if (showNotifications) {
        toast.success('Draft restored from your last session');
      }
      onDraftRestored?.();
    } catch (error) {
      console.error(`Failed to restore draft from ${storageKey}:`, error);
      localStorage.removeItem(storageKey);

      if (showNotifications) {
        toast.error('Failed to restore draft');
      }
    }
  }, [isOpen, setValue, storageKey, expiryDuration, showNotifications, onDraftRestored]);

  /**
   * Save draft to localStorage
   */
  const saveDraft = useCallback(
    (data: Partial<T>) => {
      try {
        const draft: DraftData<T> = {
          data,
          timestamp: Date.now(),
        };
        localStorage.setItem(storageKey, JSON.stringify(draft));
        onDraftSaved?.();
      } catch (error) {
        console.error(`Failed to save draft to ${storageKey}:`, error);
      }
    },
    [storageKey, onDraftSaved]
  );

  /**
   * Clear draft from localStorage
   */
  const clearDraft = useCallback(() => {
    try {
      localStorage.removeItem(storageKey);
    } catch (error) {
      console.error(`Failed to clear draft from ${storageKey}:`, error);
    }
  }, [storageKey]);

  /**
   * Check if draft exists
   */
  const hasDraft = useCallback((): boolean => {
    const draftJson = localStorage.getItem(storageKey);
    if (!draftJson) return false;

    try {
      const draft: DraftData<T> = JSON.parse(draftJson);
      const isExpired = Date.now() - draft.timestamp > expiryDuration;
      return !isExpired;
    } catch {
      return false;
    }
  }, [storageKey, expiryDuration]);

  return {
    saveDraft,
    clearDraft,
    hasDraft,
  };
}
