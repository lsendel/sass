import React, { useState } from 'react'
import {
  XMarkIcon,
  CreditCardIcon,
  PlusIcon,
  CheckIcon,
  ArrowLeftIcon,
} from '@heroicons/react/24/outline'
import { toast } from 'react-hot-toast'
import { clsx } from 'clsx'

import { logger } from '../../utils/logger'
import {
  useGetOrganizationPaymentMethodsQuery,
  useSetDefaultPaymentMethodMutation,
  useDetachPaymentMethodMutation,
} from '../../store/api/paymentApi'
import LoadingSpinner from '../ui/LoadingSpinner'

import AddPaymentMethodForm from './AddPaymentMethodForm'


interface PaymentMethodsModalProps {
  isOpen: boolean
  onClose: () => void
  organizationId: string
}

const PaymentMethodsModal: React.FC<PaymentMethodsModalProps> = ({
  isOpen,
  onClose,
  organizationId,
}) => {
  const [showAddForm, setShowAddForm] = useState(false)
  const {
    data: paymentMethods,
    isLoading,
    refetch,
  } = useGetOrganizationPaymentMethodsQuery(organizationId, {
    skip: !isOpen,
  })

  const [setDefaultPaymentMethod] = useSetDefaultPaymentMethodMutation()
  const [detachPaymentMethod] = useDetachPaymentMethodMutation()

  const handleSetDefault = async (paymentMethodId: string) => {
    try {
      await setDefaultPaymentMethod({
        paymentMethodId,
        organizationId,
      }).unwrap()

      toast.success('Default payment method updated')
      void refetch()
    } catch (error) {
      logger.error('Failed to set default payment method:', error)
      toast.error('Failed to update default payment method')
    }
  }

  const handleRemove = async (paymentMethodId: string) => {
    if (!confirm('Are you sure you want to remove this payment method?')) {
      return
    }

    try {
      await detachPaymentMethod({
        paymentMethodId,
        organizationId,
      }).unwrap()

      toast.success('Payment method removed')
      void refetch()
    } catch (error) {
      logger.error('Failed to remove payment method:', error)
      toast.error('Failed to remove payment method')
    }
  }

  const handleAddPaymentMethodSuccess = () => {
    setShowAddForm(false)
    void refetch()
  }

  const handleCloseModal = () => {
    setShowAddForm(false)
    onClose()
  }

  if (!isOpen) {return null}

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-screen items-end justify-center px-4 pt-4 pb-20 text-center sm:block sm:p-0">
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={handleCloseModal}
        />

        <span className="hidden sm:inline-block sm:align-middle sm:h-screen">
          &#8203;
        </span>

        <div className="relative inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full sm:p-6">
          <div className="absolute top-0 right-0 pt-4 pr-4">
            <button
              type="button"
              className="bg-white rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              onClick={handleCloseModal}
            >
              <XMarkIcon className="h-6 w-6" />
            </button>
          </div>

          <div className="sm:flex sm:items-start">
            <div className="w-full mt-3 text-center sm:mt-0 sm:text-left">
              {showAddForm ? (
                <div>
                  <div className="flex items-center mb-4">
                    <button
                      type="button"
                      onClick={() => setShowAddForm(false)}
                      className="mr-3 p-1 rounded-full hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
                    >
                      <ArrowLeftIcon className="h-5 w-5 text-gray-600" />
                    </button>
                    <h3 className="text-lg leading-6 font-medium text-gray-900">
                      Add Payment Method
                    </h3>
                  </div>
                  <AddPaymentMethodForm
                    organizationId={organizationId}
                    onSuccess={handleAddPaymentMethodSuccess}
                    onCancel={() => setShowAddForm(false)}
                  />
                </div>
              ) : (
                <>
                  <h3 className="text-lg leading-6 font-medium text-gray-900">
                    Payment Methods
                  </h3>
                  <div className="mt-2">
                    <p className="text-sm text-gray-500">
                      Manage your payment methods for subscriptions and purchases.
                    </p>
                  </div>

                  <div className="mt-6">
                {isLoading ? (
                  <div className="flex justify-center py-8">
                    <LoadingSpinner size="lg" />
                  </div>
                ) : paymentMethods && paymentMethods.length > 0 ? (
                  <div className="space-y-3">
                    {paymentMethods.map(method => (
                      <div
                        key={method.id}
                        className={clsx(
                          'border rounded-lg p-4',
                          method.isDefault
                            ? 'border-primary-500 bg-primary-50'
                            : 'border-gray-200'
                        )}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            <div className="flex-shrink-0">
                              <CreditCardIcon className="h-6 w-6 text-gray-400" />
                            </div>
                            <div>
                              <p className="text-sm font-medium text-gray-900">
                                {method.type === 'CARD'
                                  ? `${method.brand || 'Card'} ending in ••••${method.last4 || '****'}`
                                  : method.type.replace('_', ' ').toLowerCase()
                                }
                              </p>
                              {method.type === 'CARD' && method.brand && method.last4 && (
                                <p className="text-sm text-gray-500">
                                  {method.brand} ••••{method.last4}
                                  {method.expiryMonth && method.expiryYear && (
                                    <span> (Expires {method.expiryMonth}/{method.expiryYear})</span>
                                  )}
                                </p>
                              )}
                              <p className="text-xs text-gray-500 mt-1">
                                Added {new Date(method.createdAt).toLocaleDateString()}
                              </p>
                            </div>
                          </div>

                          <div className="flex items-center space-x-2">
                            {method.isDefault ? (
                              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-primary-100 text-primary-800">
                                <CheckIcon className="mr-1 h-3 w-3" />
                                Default
                              </span>
                            ) : (
                              <button
                                onClick={() => void handleSetDefault(method.id)}
                                className="text-sm text-primary-600 hover:text-primary-500"
                              >
                                Set as default
                              </button>
                            )}

                            {!method.isDefault && (
                              <button
                                onClick={() => void handleRemove(method.id)}
                                className="text-sm text-red-600 hover:text-red-500"
                              >
                                Remove
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <CreditCardIcon className="mx-auto h-12 w-12 text-gray-400" />
                    <h3 className="mt-2 text-sm font-medium text-gray-900">
                      No payment methods
                    </h3>
                    <p className="mt-1 text-sm text-gray-500">
                      Add a payment method to start making payments.
                    </p>
                  </div>
                )}

                    <div className="mt-6">
                      <button
                        type="button"
                        onClick={() => setShowAddForm(true)}
                        className="w-full inline-flex justify-center items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                      >
                        <PlusIcon className="-ml-1 mr-2 h-5 w-5" />
                        Add Payment Method
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default PaymentMethodsModal
