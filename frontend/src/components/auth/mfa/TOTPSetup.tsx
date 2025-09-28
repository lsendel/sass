/**
 * TOTP (Time-based One-Time Password) Setup Component
 *
 * Comprehensive setup flow for TOTP MFA using Google Authenticator,
 * Authy, or other TOTP-compatible apps:
 * - QR code display for easy setup
 * - Manual secret key entry option
 * - Verification flow
 * - Backup codes generation
 * - Progressive enhancement for accessibility
 */

import React, { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { QrCodeIcon, ClipboardDocumentIcon, CheckCircleIcon, ExclamationTriangleIcon } from '@heroicons/react/24/outline'
import { clsx } from 'clsx'
import { toast } from 'react-hot-toast'

import { useSetupMFAMethodMutation, useVerifyMFASetupMutation } from '../../../store/api/mfaApi'
import { Button } from '../../ui/button'
import { logger } from '../../../utils/logger'
import type { TOTPSetupForm, MFASetupResponse } from '../../../types/mfa'

// Validation schemas
const totpSetupSchema = z.object({
  name: z.string().min(1, 'Please provide a name for this authenticator').max(50, 'Name must be 50 characters or less'),
  verificationCode: z.string()
    .regex(/^\d{6}$/, 'Verification code must be exactly 6 digits')
    .length(6, 'Verification code must be 6 digits'),
})

interface TOTPSetupProps {
  onComplete: (method: any) => void
  onCancel: () => void
  className?: string
}

type SetupStep = 'configure' | 'scan' | 'verify' | 'backup' | 'complete'

const TOTPSetup: React.FC<TOTPSetupProps> = ({ onComplete, onCancel, className }) => {
  const [currentStep, setCurrentStep] = useState<SetupStep>('configure')
  const [setupData, setSetupData] = useState<MFASetupResponse | null>(null)
  const [backupCodes, setBackupCodes] = useState<string[]>([])
  const [showSecretKey, setShowSecretKey] = useState(false)

  const [setupMFAMethod, { isLoading: isSettingUp }] = useSetupMFAMethodMutation()
  const [verifySetup, { isLoading: isVerifying, error: verifyError }] = useVerifyMFASetupMutation()

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    clearErrors,
  } = useForm<TOTPSetupForm>({
    resolver: zodResolver(totpSetupSchema),
    defaultValues: {
      name: '',
      verificationCode: '',
    },
  })

  const methodName = watch('name')
  const verificationCode = watch('verificationCode')

  // Start TOTP setup process
  const handleStartSetup = async (data: Pick<TOTPSetupForm, 'name'>) => {
    try {
      logger.info('Starting TOTP setup:', { name: data.name })

      const response = await setupMFAMethod({
        type: 'totp',
        name: data.name,
      }).unwrap()

      setSetupData(response)
      setCurrentStep('scan')

      logger.info('TOTP setup initiated successfully')
    } catch (error) {
      logger.error('TOTP setup failed:', error)
      toast.error('Failed to start TOTP setup. Please try again.')
    }
  }

  // Verify TOTP setup
  const handleVerifySetup = async (data: TOTPSetupForm) => {
    if (!setupData?.method.id) {
      toast.error('Setup data not found. Please start over.')
      return
    }

    try {
      logger.info('Verifying TOTP setup:', { methodId: setupData.method.id })

      const response = await verifySetup({
        methodId: setupData.method.id,
        code: data.verificationCode,
      }).unwrap()

      if (response.success) {
        // Check if backup codes were generated
        if (setupData.setupData?.backupCodes) {
          setBackupCodes(setupData.setupData.backupCodes)
          setCurrentStep('backup')
        } else {
          setCurrentStep('complete')
          onComplete(response.method)
        }

        logger.info('TOTP setup verified successfully')
        toast.success('TOTP authenticator setup successfully!')
      }
    } catch (error) {
      logger.error('TOTP verification failed:', error)
      toast.error('Invalid verification code. Please try again.')
    }
  }

  // Copy text to clipboard
  const copyToClipboard = async (text: string, label: string) => {
    try {
      await navigator.clipboard.writeText(text)
      toast.success(`${label} copied to clipboard`)
    } catch (error) {
      logger.error('Clipboard copy failed:', error)
      toast.error('Failed to copy to clipboard')
    }
  }

  // Handle backup codes acknowledgment
  const handleBackupCodesAcknowledged = () => {
    setCurrentStep('complete')
    onComplete(setupData!.method)
  }

  // Auto-focus verification code input when QR is scanned
  useEffect(() => {
    if (currentStep === 'verify') {
      const input = document.getElementById('verificationCode') as HTMLInputElement
      if (input) {
        input.focus()
      }
    }
  }, [currentStep])

  // Auto-format verification code
  useEffect(() => {
    if (verificationCode && verificationCode.length === 6 && !errors.verificationCode) {
      // Auto-submit if code is valid format
      const timer = setTimeout(() => {
        handleSubmit(handleVerifySetup)()
      }, 500)
      return () => clearTimeout(timer)
    }
    return undefined
  }, [verificationCode, errors.verificationCode, handleSubmit])

  const renderStepIndicator = () => {
    const steps = [
      { key: 'configure', label: 'Configure', number: 1 },
      { key: 'scan', label: 'Scan QR', number: 2 },
      { key: 'verify', label: 'Verify', number: 3 },
      { key: 'backup', label: 'Backup', number: 4 },
    ]

    const stepIndex = steps.findIndex(step => step.key === currentStep)

    return (
      <div className="flex items-center justify-between mb-8">
        {steps.map((step, index) => (
          <div key={step.key} className="flex items-center">
            <div
              className={clsx(
                'flex items-center justify-center w-8 h-8 rounded-full text-sm font-medium',
                index <= stepIndex
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-600'
              )}
            >
              {index < stepIndex ? '✓' : step.number}
            </div>
            <span
              className={clsx(
                'ml-2 text-sm font-medium',
                index <= stepIndex ? 'text-blue-600' : 'text-gray-500'
              )}
            >
              {step.label}
            </span>
            {index < steps.length - 1 && (
              <div
                className={clsx(
                  'w-12 h-0.5 mx-4',
                  index < stepIndex ? 'bg-blue-600' : 'bg-gray-200'
                )}
              />
            )}
          </div>
        ))}
      </div>
    )
  }

  const renderConfigureStep = () => (
    <div className="space-y-6">
      <div className="text-center">
        <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <QrCodeIcon className="w-8 h-8 text-blue-600" />
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Setup Authenticator App
        </h3>
        <p className="text-sm text-gray-600 max-w-md mx-auto">
          Configure your TOTP authenticator app like Google Authenticator, Authy, or 1Password to generate secure codes.
        </p>
      </div>

      <form onSubmit={handleSubmit(handleStartSetup)} className="space-y-4">
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
            Authenticator Name
          </label>
          <input
            {...register('name')}
            type="text"
            id="name"
            placeholder="e.g., iPhone Authenticator, Work Phone"
            className={clsx(
              'w-full px-3 py-2 border rounded-lg text-sm transition-colors',
              errors.name
                ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            )}
          />
          {errors.name && (
            <p className="mt-1 text-xs text-red-600">{errors.name.message}</p>
          )}
        </div>

        <div className="flex gap-3">
          <Button type="submit" isLoading={isSettingUp} className="flex-1">
            Continue Setup
          </Button>
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  )

  const renderScanStep = () => {
    if (!setupData?.setupData?.qrCode) {
      return (
        <div className="text-center py-8">
          <ExclamationTriangleIcon className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <p className="text-red-600">QR code not generated. Please try again.</p>
        </div>
      )
    }

    return (
      <div className="space-y-6">
        <div className="text-center">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">
            Scan QR Code
          </h3>
          <p className="text-sm text-gray-600 max-w-md mx-auto">
            Open your authenticator app and scan this QR code to add your account.
          </p>
        </div>

        {/* QR Code Display */}
        <div className="bg-white p-6 border-2 border-gray-200 rounded-lg text-center">
          <img
            src={setupData.setupData.qrCode}
            alt="TOTP QR Code"
            className="mx-auto max-w-full h-auto"
            style={{ maxWidth: '200px' }}
          />
        </div>

        {/* Manual Entry Option */}
        <div className="bg-gray-50 p-4 rounded-lg">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-gray-700">
              Can't scan? Enter manually:
            </span>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setShowSecretKey(!showSecretKey)}
            >
              {showSecretKey ? 'Hide' : 'Show'} Key
            </Button>
          </div>

          {showSecretKey && setupData.setupData.secretKey && (
            <div className="bg-white p-3 border border-gray-200 rounded font-mono text-xs break-all">
              <div className="flex items-center justify-between">
                <code>{setupData.setupData.secretKey}</code>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => setupData.setupData?.secretKey && copyToClipboard(setupData.setupData.secretKey, 'Secret key')}
                >
                  <ClipboardDocumentIcon className="w-4 h-4" />
                </Button>
              </div>
            </div>
          )}
        </div>

        <div className="flex gap-3">
          <Button
            type="button"
            onClick={() => setCurrentStep('verify')}
            className="flex-1"
          >
            I've Added the Account
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => setCurrentStep('configure')}
          >
            Back
          </Button>
        </div>
      </div>
    )
  }

  const renderVerifyStep = () => (
    <div className="space-y-6">
      <div className="text-center">
        <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <CheckCircleIcon className="w-8 h-8 text-green-600" />
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Verify Setup
        </h3>
        <p className="text-sm text-gray-600 max-w-md mx-auto">
          Enter the 6-digit code from your authenticator app to complete setup.
        </p>
      </div>

      <form onSubmit={handleSubmit(handleVerifySetup)} className="space-y-4">
        <div>
          <label htmlFor="verificationCode" className="block text-sm font-medium text-gray-700 mb-1">
            Verification Code
          </label>
          <input
            {...register('verificationCode')}
            type="text"
            id="verificationCode"
            inputMode="numeric"
            pattern="\d{6}"
            maxLength={6}
            placeholder="000000"
            className={clsx(
              'w-full px-4 py-3 border rounded-lg text-center text-lg font-mono tracking-widest transition-colors',
              errors.verificationCode
                ? 'border-red-300 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            )}
            onChange={(e) => {
              const value = e.target.value.replace(/\D/g, '').slice(0, 6)
              setValue('verificationCode', value)
              if (errors.verificationCode && value.length === 6) {
                clearErrors('verificationCode')
              }
            }}
          />
          {errors.verificationCode && (
            <p className="mt-1 text-xs text-red-600">
              {String((errors.verificationCode as any)?.message || 'Invalid verification code')}
            </p>
          )}
          {Boolean(verifyError) && (
            <p className="mt-1 text-xs text-red-600">Invalid code. Please try again.</p>
          )}
        </div>

        <div className="flex gap-3">
          <Button type="submit" isLoading={isVerifying} disabled={verificationCode.length !== 6} className="flex-1">
            Verify & Complete
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => setCurrentStep('scan')}
          >
            Back
          </Button>
        </div>
      </form>

      <div className="text-center">
        <p className="text-xs text-gray-500">
          Codes refresh every 30 seconds. Wait for a new code if this one doesn't work.
        </p>
      </div>
    </div>
  )

  const renderBackupCodesStep = () => (
    <div className="space-y-6">
      <div className="text-center">
        <div className="w-16 h-16 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <ExclamationTriangleIcon className="w-8 h-8 text-yellow-600" />
        </div>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Save Backup Codes
        </h3>
        <p className="text-sm text-gray-600 max-w-md mx-auto">
          Store these backup codes in a secure place. You can use them to access your account if you lose your authenticator.
        </p>
      </div>

      <div className="bg-gray-50 p-4 rounded-lg">
        <div className="flex items-center justify-between mb-3">
          <span className="text-sm font-medium text-gray-700">
            Backup Codes (use once each)
          </span>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => copyToClipboard(backupCodes.join('\n'), 'Backup codes')}
          >
            <ClipboardDocumentIcon className="w-4 h-4 mr-1" />
            Copy All
          </Button>
        </div>

        <div className="grid grid-cols-2 gap-2">
          {backupCodes.map((code, index) => (
            <div
              key={index}
              className="bg-white p-2 border border-gray-200 rounded font-mono text-sm text-center"
            >
              {code}
            </div>
          ))}
        </div>
      </div>

      <div className="bg-blue-50 p-4 rounded-lg">
        <div className="flex">
          <div className="flex-shrink-0">
            <ExclamationTriangleIcon className="h-5 w-5 text-blue-400" />
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-blue-800">Important</h3>
            <div className="mt-2 text-sm text-blue-700">
              <ul className="list-disc list-inside space-y-1">
                <li>Each code can only be used once</li>
                <li>Store them in a secure password manager</li>
                <li>Don't share these codes with anyone</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <Button type="button" onClick={handleBackupCodesAcknowledged} className="w-full">
        I've Saved My Backup Codes
      </Button>
    </div>
  )

  const renderCompleteStep = () => (
    <div className="text-center space-y-6">
      <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto">
        <CheckCircleIcon className="w-8 h-8 text-green-600" />
      </div>
      <div>
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Setup Complete!
        </h3>
        <p className="text-sm text-gray-600">
          Your authenticator "{methodName}" has been configured successfully.
        </p>
      </div>
    </div>
  )

  return (
    <div className={clsx('max-w-md mx-auto', className)}>
      {currentStep !== 'complete' && renderStepIndicator()}

      {currentStep === 'configure' && renderConfigureStep()}
      {currentStep === 'scan' && renderScanStep()}
      {currentStep === 'verify' && renderVerifyStep()}
      {currentStep === 'backup' && renderBackupCodesStep()}
      {currentStep === 'complete' && renderCompleteStep()}
    </div>
  )
}

export default TOTPSetup
