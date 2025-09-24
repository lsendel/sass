import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { AutoSaveIndicator, UnsavedChangesWarning } from './AutoSaveComponents'
import type { AutoSaveStatus } from '../../hooks/useAutoSave'

describe('AutoSaveComponents', () => {
  describe('AutoSaveIndicator', () => {
    it('shows saving state with spinner', () => {
      render(
        <AutoSaveIndicator status="saving" />
      )

      expect(screen.getByText('Saving...')).toBeInTheDocument()
      expect(screen.getByRole('img', { hidden: true })).toHaveClass('animate-spin')
    })

    it('shows saved state with timestamp', () => {
      const lastSaved = new Date('2024-01-01T12:00:00Z')
      render(
        <AutoSaveIndicator status="saved" lastSaved={lastSaved} />
      )

      expect(screen.getByText(/Saved 12:00 PM/)).toBeInTheDocument()
    })

    it('shows error state with retry button', () => {
      const mockRetry = jest.fn()
      const error = new Error('Save failed')

      render(
        <AutoSaveIndicator
          status="error"
          error={error}
          onRetry={mockRetry}
        />
      )

      expect(screen.getByText('Save failed')).toBeInTheDocument()
      expect(screen.getByText('retry')).toBeInTheDocument()

      screen.getByText('retry').click()
      expect(mockRetry).toHaveBeenCalled()
    })

    it('shows idle state by default', () => {
      render(
        <AutoSaveIndicator status="idle" />
      )

      expect(screen.getByText('Draft saved')).toBeInTheDocument()
    })
  })

  describe('UnsavedChangesWarning', () => {
    it('renders nothing when there are no unsaved changes', () => {
      render(
        <UnsavedChangesWarning hasUnsavedChanges={false} />
      )

      expect(screen.queryByText(/unsaved changes/)).not.toBeInTheDocument()
    })

    it('shows warning when there are unsaved changes', () => {
      render(
        <UnsavedChangesWarning hasUnsavedChanges={true} />
      )

      expect(screen.getByText(/You have unsaved changes/)).toBeInTheDocument()
    })

    it('shows save now button when onSave is provided', async () => {
      const mockSave = jest.fn().mockResolvedValue(undefined)

      render(
        <UnsavedChangesWarning
          hasUnsavedChanges={true}
          onSave={mockSave}
        />
      )

      const saveButton = screen.getByText('Save now')
      expect(saveButton).toBeInTheDocument()

      saveButton.click()
      expect(mockSave).toHaveBeenCalled()

      await waitFor(() => {
        expect(saveButton).not.toBeDisabled()
      })
    })

    it('shows discard button when onDiscard is provided', () => {
      const mockDiscard = jest.fn()

      render(
        <UnsavedChangesWarning
          hasUnsavedChanges={true}
          onDiscard={mockDiscard}
        />
      )

      const discardButton = screen.getByText('Discard changes')
      expect(discardButton).toBeInTheDocument()

      discardButton.click()
      expect(mockDiscard).toHaveBeenCalled()
    })
  })
})