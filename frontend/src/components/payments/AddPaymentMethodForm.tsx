import React, { useState } from 'react'
import {
  useStripe,
  useElements,
  CardElement,
} from '@stripe/react-stripe-js'
import { toast } from 'react-hot-toast'
import {
  CreditCardIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline'
import { clsx } from 'clsx'

import { useCreateSetupIntentMutation } from '../../store/api/paymentApi'
import { Button } from '../ui/button'
import { logger } from '../../utils/logger'


interface AddPaymentMethodFormProps {
  organizationId: string
  onSuccess: () => void
  onCancel: () => void
}

interface FormData {
  name: string
  email: string
  address: {
    line1: string
    line2: string
    city: string
    state: string
    postal_code: string
    country: string
  }
}

const AddPaymentMethodForm: React.FC<AddPaymentMethodFormProps> = ({
  organizationId,
  onSuccess,
  onCancel,
}) => {
  const stripe = useStripe()
  const elements = useElements()
  const [createSetupIntent] = useCreateSetupIntentMutation()

  const [isProcessing, setIsProcessing] = useState(false)
  const [formData, setFormData] = useState<FormData>({
    name: '',
    email: '',
    address: {
      line1: '',
      line2: '',
      city: '',
      state: '',
      postal_code: '',
      country: 'US',
    },
  })
  const [cardError, setCardError] = useState<string>('')
  const [cardComplete, setCardComplete] = useState(false)

  const handleInputChange = (field: string, value: string) => {
    if (field.startsWith('address.')) {
      const addressField = field.replace('address.', '')
      setFormData(prev => ({
        ...prev,
        address: {
          ...prev.address,
          [addressField]: value,
        },
      }))
    } else {
      setFormData(prev => ({
        ...prev,
        [field]: value,
      }))
    }
  }

  const handleCardChange = (event: any) => {
    if (event.error) {
      setCardError(event.error.message)
    } else {
      setCardError('')
    }
    setCardComplete(event.complete)
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()

    if (!stripe || !elements) {
      logger.error('Stripe not loaded')
      toast.error('Payment system not ready. Please try again.')
      return
    }

    const cardElement = elements.getElement(CardElement)
    if (!cardElement) {
      logger.error('Card element not found')
      toast.error('Card information not found. Please refresh and try again.')
      return
    }

    // Validate form
    if (!formData.name.trim() || !formData.email.trim() || !formData.address.line1.trim()) {
      toast.error('Please fill in all required fields')
      return
    }

    if (!cardComplete) {
      toast.error('Please enter complete card information')
      return
    }

    setIsProcessing(true)
    setCardError('')

    try {
      // Create setup intent
      const { client_secret } = await createSetupIntent({
        organizationId,
        usage: 'off_session',
      }).unwrap()

      // Confirm the setup intent with the card
      const { error, setupIntent } = await stripe.confirmCardSetup(client_secret, {
        payment_method: {
          card: cardElement,
          billing_details: {
            name: formData.name,
            email: formData.email,
            address: {
              line1: formData.address.line1,
              ...(formData.address.line2 ? { line2: formData.address.line2 } : {}),
              city: formData.address.city,
              state: formData.address.state,
              postal_code: formData.address.postal_code,
              country: formData.address.country,
            } as any,
          },
        },
      })

      if (error) {
        logger.error('Setup intent confirmation failed:', error)
        setCardError(error.message || 'Failed to add payment method')
        toast.error('Failed to add payment method. Please try again.')
      } else if (setupIntent?.status === 'succeeded') {
        toast.success('Payment method added successfully!')
        onSuccess()
      }
    } catch (error) {
      logger.error('Failed to create setup intent:', error)
      toast.error('Failed to set up payment method. Please try again.')
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Header */}
      <div className="flex items-center space-x-3 pb-4 border-b border-gray-200">
        <div className="w-8 h-8 bg-primary-100 rounded-lg flex items-center justify-center">
          <CreditCardIcon className="w-4 h-4 text-primary-600" />
        </div>
        <div>
          <h3 className="text-lg font-semibold text-gray-900">Add Payment Method</h3>
          <p className="text-sm text-gray-600">
            Securely add a new payment method using Stripe
          </p>
        </div>
      </div>

      {/* Billing Information */}
      <div className="space-y-4">
        <h4 className="text-sm font-medium text-gray-900">Billing Information</h4>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Full Name *
            </label>
            <input
              type="text"
              id="name"
              value={formData.name}
              onChange={(e) => handleInputChange('name', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
              placeholder="Enter full name"
              required
            />
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email Address *
            </label>
            <input
              type="email"
              id="email"
              value={formData.email}
              onChange={(e) => handleInputChange('email', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
              placeholder="Enter email address"
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="address1" className="block text-sm font-medium text-gray-700 mb-1">
            Address Line 1 *
          </label>
          <input
            type="text"
            id="address1"
            value={formData.address.line1}
            onChange={(e) => handleInputChange('address.line1', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
            placeholder="Enter street address"
            required
          />
        </div>

        <div>
          <label htmlFor="address2" className="block text-sm font-medium text-gray-700 mb-1">
            Address Line 2
          </label>
          <input
            type="text"
            id="address2"
            value={formData.address.line2}
            onChange={(e) => handleInputChange('address.line2', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
            placeholder="Apartment, suite, etc. (optional)"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="city" className="block text-sm font-medium text-gray-700 mb-1">
              City *
            </label>
            <input
              type="text"
              id="city"
              value={formData.address.city}
              onChange={(e) => handleInputChange('address.city', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
              placeholder="Enter city"
              required
            />
          </div>

          <div>
            <label htmlFor="state" className="block text-sm font-medium text-gray-700 mb-1">
              State *
            </label>
            <input
              type="text"
              id="state"
              value={formData.address.state}
              onChange={(e) => handleInputChange('address.state', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
              placeholder="Enter state"
              required
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="postal_code" className="block text-sm font-medium text-gray-700 mb-1">
              Postal Code *
            </label>
            <input
              type="text"
              id="postal_code"
              value={formData.address.postal_code}
              onChange={(e) => handleInputChange('address.postal_code', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
              placeholder="Enter postal code"
              required
            />
          </div>

          <div>
            <label htmlFor="country" className="block text-sm font-medium text-gray-700 mb-1">
              Country *
            </label>
            <select
              id="country"
              value={formData.address.country}
              onChange={(e) => handleInputChange('address.country', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="US">United States</option>
              <option value="CA">Canada</option>
              <option value="GB">United Kingdom</option>
              <option value="AU">Australia</option>
              <option value="DE">Germany</option>
              <option value="FR">France</option>
            </select>
          </div>
        </div>
      </div>

      {/* Card Information */}
      <div className="space-y-4">
        <h4 className="text-sm font-medium text-gray-900">Card Information</h4>

        <div className="relative">
          <label
            id="card-details-label"
            className="block text-sm font-medium text-gray-700 mb-2"
          >
            Card Details *
          </label>
          <div
            className={clsx(
              'px-3 py-3 border rounded-md shadow-sm bg-white',
              cardError ? 'border-red-300' : 'border-gray-300',
              'focus-within:ring-1 focus-within:ring-primary-500 focus-within:border-primary-500'
            )}
            role="group"
            aria-labelledby="card-details-label"
          >
            <CardElement
              onChange={handleCardChange}
              options={{
                style: {
                  base: {
                    fontSize: '16px',
                    color: '#374151',
                    fontFamily: 'Inter, system-ui, sans-serif',
                    '::placeholder': {
                      color: '#9CA3AF',
                    },
                  },
                  invalid: {
                    color: '#EF4444',
                  },
                },
                hidePostalCode: true, // We collect this separately
              }}
            />
          </div>

          {cardError && (
            <div className="flex items-center mt-2 text-sm text-red-600">
              <ExclamationTriangleIcon className="w-4 h-4 mr-1" />
              {cardError}
            </div>
          )}

          {cardComplete && !cardError && (
            <div className="flex items-center mt-2 text-sm text-green-600">
              <CheckCircleIcon className="w-4 h-4 mr-1" />
              Card information looks good
            </div>
          )}
        </div>
      </div>

      {/* Security Notice */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <div className="w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
              <svg className="w-3 h-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
            </div>
          </div>
          <div className="ml-3">
            <p className="text-sm text-blue-800">
              <strong>Secure Payment:</strong> Your card information is processed securely by Stripe.
              We never store your card details on our servers.
            </p>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex space-x-3 pt-4 border-t border-gray-200">
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
          disabled={isProcessing}
          className="flex-1"
        >
          Cancel
        </Button>

        <Button
          type="submit"
          disabled={!stripe || isProcessing || !cardComplete}
          className="flex-1"
        >
          {isProcessing ? (
            <div className="flex items-center">
              <svg className="animate-spin -ml-1 mr-3 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Processing...
            </div>
          ) : (
            <div className="flex items-center">
              <CreditCardIcon className="w-4 h-4 mr-2" />
              Add Payment Method
            </div>
          )}
        </Button>
      </div>
    </form>
  )
}

export default AddPaymentMethodForm
