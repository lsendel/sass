import React, { useState } from 'react'
import {
  useGetAvailablePlansQuery,
  useCreateSubscriptionMutation,
  useChangeSubscriptionPlanMutation,
} from '../../store/api/subscriptionApi'
import { XMarkIcon, CheckIcon } from '@heroicons/react/24/outline'
import { toast } from 'react-hot-toast'
import LoadingSpinner from '../ui/LoadingSpinner'
import { parseApiError } from '../../utils/apiError'
import { clsx } from 'clsx'

type UpgradePlanModalProps = {
  isOpen: boolean
  onClose: () => void
  currentPlanId?: string
  organizationId?: string
  subscriptionId?: string
}

const UpgradePlanModal: React.FC<UpgradePlanModalProps> = ({
  isOpen,
  onClose,
  currentPlanId,
  organizationId,
  subscriptionId,
}) => {
  const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null)
  const { data: plans, isLoading } = useGetAvailablePlansQuery(undefined, {
    skip: !isOpen,
  })

  const [createSubscription, { isLoading: isCreating }] =
    useCreateSubscriptionMutation()
  const [changeSubscriptionPlan, { isLoading: isChanging }] =
    useChangeSubscriptionPlanMutation()

  const handlePlanSelection = async () => {
    if (!selectedPlanId || !organizationId) {return}

    try {
      if (subscriptionId) {
        // Change existing subscription
        await changeSubscriptionPlan({
          subscriptionId,
          organizationId,
          newPlanId: selectedPlanId,
          prorationBehavior: true,
        }).unwrap()

        toast.success('Subscription plan updated successfully')
      } else {
        // Create new subscription
        await createSubscription({
          organizationId,
          planId: selectedPlanId,
          trialEligible: true,
        }).unwrap()

        toast.success('Subscription created successfully')
      }

      onClose()
    } catch (err) {
      const parsed = parseApiError(err)
      console.error('Failed to update subscription:', parsed)
      toast.error(parsed.message || 'Failed to update subscription')
    }
  }

  if (!isOpen) {return null}

  const sortedPlans = plans
    ? [...plans].sort((a, b) => a.amount - b.amount)
    : []

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-screen items-end justify-center px-4 pt-4 pb-20 text-center sm:block sm:p-0">
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={onClose}
        />

        <span className="hidden sm:inline-block sm:align-middle sm:h-screen">
          &#8203;
        </span>

        <div className="relative inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-4xl sm:w-full sm:p-6">
          <div className="absolute top-0 right-0 pt-4 pr-4">
            <button
              type="button"
              className="bg-white rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              onClick={onClose}
            >
              <XMarkIcon className="h-6 w-6" />
            </button>
          </div>

          <div className="sm:flex sm:items-start">
            <div className="w-full mt-3 text-center sm:mt-0 sm:text-left">
              <h3 className="text-lg leading-6 font-medium text-gray-900">
                {subscriptionId ? 'Change Your Plan' : 'Choose a Plan'}
              </h3>
              <div className="mt-2">
                <p className="text-sm text-gray-500">
                  {subscriptionId
                    ? 'Select a new plan for your subscription. Changes will take effect immediately.'
                    : 'Select a plan to start your subscription.'}
                </p>
              </div>

              {isLoading ? (
                <div className="flex justify-center py-8">
                  <LoadingSpinner size="lg" />
                </div>
              ) : (
                <div className="mt-6">
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {sortedPlans.map(plan => {
                      const isCurrentPlan = plan.id === currentPlanId
                      const isSelected = plan.id === selectedPlanId

                      return (
                        <div
                          key={plan.id}
                          className={clsx(
                            'relative rounded-lg border-2 p-6 cursor-pointer',
                            isCurrentPlan
                              ? 'border-gray-300 bg-gray-50'
                              : isSelected
                                ? 'border-primary-500 bg-primary-50'
                                : 'border-gray-200 hover:border-gray-300'
                          )}
                          onClick={() =>
                            !isCurrentPlan && setSelectedPlanId(plan.id)
                          }
                        >
                          {isCurrentPlan && (
                            <span className="absolute top-4 right-4 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-200 text-gray-800">
                              Current
                            </span>
                          )}

                          <div className="flex items-center justify-between">
                            <h4 className="text-lg font-medium text-gray-900">
                              {plan.name}
                            </h4>
                            {isSelected && (
                              <CheckIcon className="h-5 w-5 text-primary-600" />
                            )}
                          </div>

                          <p className="mt-2 text-sm text-gray-500">
                            {plan.description}
                          </p>

                          <div className="mt-4">
                            <span className="text-3xl font-bold text-gray-900">
                              ${plan.amount.toFixed(0)}
                            </span>
                            <span className="text-gray-500">
                              /{plan.interval.toLowerCase()}
                            </span>
                          </div>

                          {plan.trialDays && plan.trialDays > 0 && (
                            <p className="mt-2 text-sm text-green-600">
                              {plan.trialDays} day free trial
                            </p>
                          )}

                          {plan.features &&
                            Object.keys(plan.features).length > 0 && (
                              <ul className="mt-4 space-y-2">
                                {Object.entries(plan.features)
                                  .slice(0, 3)
                                  .map(([key, value]) => (
                                    <li key={key} className="flex items-start">
                                      <CheckIcon className="flex-shrink-0 h-4 w-4 text-green-500 mt-0.5" />
                                      <span className="ml-2 text-sm text-gray-700">
                                        {typeof value === 'boolean'
                                          ? key.replace(/_/g, ' ')
                                          : `${key}: ${value}`}
                                      </span>
                                    </li>
                                  ))}
                              </ul>
                            )}
                        </div>
                      )
                    })}
                  </div>

                  <div className="mt-6 flex justify-end space-x-3">
                    <button
                      type="button"
                      onClick={onClose}
                      className="inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:text-sm"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      onClick={handlePlanSelection}
                      disabled={
                        !selectedPlanId ||
                        selectedPlanId === currentPlanId ||
                        isCreating ||
                        isChanging
                      }
                      className="inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary-600 text-base font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {isCreating || isChanging ? (
                        <>
                          <LoadingSpinner
                            size="sm"
                            className="mr-2"
                            color="white"
                          />
                          Processing...
                        </>
                      ) : subscriptionId ? (
                        'Change Plan'
                      ) : (
                        'Start Subscription'
                      )}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default UpgradePlanModal
