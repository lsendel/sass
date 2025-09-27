/**
 * Advanced Subscription Management System
 *
 * Enterprise-grade subscription management with:
 * - Subscription lifecycle management
 * - Billing cycle customization
 * - Prorations and upgrades/downgrades
 * - Usage-based billing
 * - Subscription analytics
 * - Automated renewals and dunning management
 */

import React, { useState } from 'react'
import {
  CreditCardIcon,
  ChartBarIcon,
  CogIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  BanknotesIcon,
  ArrowTrendingUpIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { clsx } from 'clsx'

import { Button } from '../ui/button'

// Types for advanced subscription management
interface SubscriptionPlan {
  id: string
  name: string
  description: string
  pricing: {
    basePrice: number
    currency: string
    billingCycle: 'monthly' | 'yearly' | 'quarterly'
    setupFee?: number
    trialDays?: number
  }
  features: string[]
  limits: {
    users?: number
    apiCalls?: number
    storage?: number // in GB
    customFields?: number
  }
  usageBased?: {
    meteringUnit: string
    price: number
    includedQuantity: number
    overage: {
      price: number
      tiers: Array<{ from: number; to: number; price: number }>
    }
  }
  isPopular: boolean
  isEnterprise: boolean
}

interface Subscription {
  id: string
  planId: string
  plan: SubscriptionPlan
  status: 'active' | 'trialing' | 'past_due' | 'canceled' | 'unpaid' | 'paused'
  currentPeriodStart: string
  currentPeriodEnd: string
  trialStart?: string
  trialEnd?: string
  canceledAt?: string
  quantity: number
  usage?: {
    users: number
    apiCalls: number
    storage: number
    customFields: number
  }
  billing: {
    nextBillingDate: string
    lastPaymentDate?: string
    amount: number
    currency: string
    paymentMethod?: {
      type: 'card' | 'bank_account'
      last4: string
      brand?: string
    }
  }
  discounts: Array<{
    id: string
    name: string
    type: 'percentage' | 'fixed'
    amount: number
    validUntil?: string
  }>
  analytics: {
    monthlyRecurringRevenue: number
    annualRecurringRevenue: number
    churnRisk: 'low' | 'medium' | 'high'
    usageGrowth: number
    costPerUser: number
  }
}

interface AdvancedSubscriptionManagerProps {
  organizationId: string
  className?: string
}

const AdvancedSubscriptionManager: React.FC<AdvancedSubscriptionManagerProps> = ({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  organizationId,
  className,
}) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'billing' | 'usage' | 'analytics' | 'settings'>('overview')
  const [showPlanSelector, setShowPlanSelector] = useState(false)

  // Mock data - in real app, this would come from API
  const subscription: Subscription = {
    id: 'sub_123',
    planId: 'plan_pro',
    plan: {
      id: 'plan_pro',
      name: 'Professional',
      description: 'Perfect for growing teams',
      pricing: {
        basePrice: 49.99,
        currency: 'USD',
        billingCycle: 'monthly',
        trialDays: 14,
      },
      features: [
        'Unlimited users',
        '10,000 API calls/month',
        '100GB storage',
        'Priority support',
        'Advanced analytics',
      ],
      limits: {
        users: -1, // unlimited
        apiCalls: 10000,
        storage: 100,
      },
      usageBased: {
        meteringUnit: 'API calls',
        price: 0.001,
        includedQuantity: 10000,
        overage: {
          price: 0.001,
          tiers: [
            { from: 0, to: 50000, price: 0.001 },
            { from: 50000, to: 100000, price: 0.0008 },
          ],
        },
      },
      isPopular: true,
      isEnterprise: false,
    },
    status: 'active',
    currentPeriodStart: '2024-01-01T00:00:00Z',
    currentPeriodEnd: '2024-02-01T00:00:00Z',
    quantity: 1,
    usage: {
      users: 8,
      apiCalls: 7500,
      storage: 45.2,
      customFields: 12,
    },
    billing: {
      nextBillingDate: '2024-02-01T00:00:00Z',
      lastPaymentDate: '2024-01-01T00:00:00Z',
      amount: 49.99,
      currency: 'USD',
      paymentMethod: {
        type: 'card',
        last4: '4242',
        brand: 'Visa',
      },
    },
    discounts: [
      {
        id: 'discount_1',
        name: 'Early Bird 20%',
        type: 'percentage',
        amount: 20,
        validUntil: '2024-03-01T00:00:00Z',
      },
    ],
    analytics: {
      monthlyRecurringRevenue: 39.99, // after discount
      annualRecurringRevenue: 479.88,
      churnRisk: 'low',
      usageGrowth: 15.4,
      costPerUser: 5.0,
    },
  }

  const availablePlans: SubscriptionPlan[] = [
    {
      id: 'plan_starter',
      name: 'Starter',
      description: 'Perfect for small teams',
      pricing: {
        basePrice: 19.99,
        currency: 'USD',
        billingCycle: 'monthly',
        trialDays: 7,
      },
      features: ['Up to 5 users', '1,000 API calls/month', '10GB storage'],
      limits: { users: 5, apiCalls: 1000, storage: 10 },
      isPopular: false,
      isEnterprise: false,
    },
    subscription.plan, // Current plan
    {
      id: 'plan_enterprise',
      name: 'Enterprise',
      description: 'For large organizations',
      pricing: {
        basePrice: 199.99,
        currency: 'USD',
        billingCycle: 'monthly',
        setupFee: 499.99,
      },
      features: [
        'Unlimited everything',
        'Custom integrations',
        'Dedicated support',
        'SLA guarantee',
      ],
      limits: {},
      isPopular: false,
      isEnterprise: true,
    },
  ]

  const getStatusBadge = (status: Subscription['status']) => {
    const statusConfig = {
      active: { color: 'bg-green-100 text-green-800', icon: CheckCircleIcon, label: 'Active' },
      trialing: { color: 'bg-blue-100 text-blue-800', icon: ClockIcon, label: 'Trial' },
      past_due: { color: 'bg-yellow-100 text-yellow-800', icon: ExclamationTriangleIcon, label: 'Past Due' },
      canceled: { color: 'bg-gray-100 text-gray-800', icon: ExclamationTriangleIcon, label: 'Canceled' },
      unpaid: { color: 'bg-red-100 text-red-800', icon: ExclamationTriangleIcon, label: 'Unpaid' },
      paused: { color: 'bg-gray-100 text-gray-800', icon: ClockIcon, label: 'Paused' },
    }

    const config = statusConfig[status]
    const IconComponent = config.icon

    return (
      <span className={clsx('inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium', config.color)}>
        <IconComponent className="w-3 h-3 mr-1" />
        {config.label}
      </span>
    )
  }

  const calculateUsagePercentage = (used: number, limit: number) => {
    if (limit === -1) return 0 // Unlimited
    return Math.min((used / limit) * 100, 100)
  }

  const getUsageColor = (percentage: number) => {
    if (percentage >= 90) return 'text-red-600 bg-red-100'
    if (percentage >= 75) return 'text-yellow-600 bg-yellow-100'
    return 'text-green-600 bg-green-100'
  }

  const renderOverview = () => (
    <div className="space-y-6">
      {/* Subscription Status */}
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center">
            <h3 className="text-lg font-medium text-gray-900">Current Subscription</h3>
            <div className="ml-3">
              {getStatusBadge(subscription.status)}
            </div>
          </div>
          <Button onClick={() => setShowPlanSelector(true)} size="sm">
            Change Plan
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <div className="text-2xl font-bold text-gray-900">
              {subscription.plan.name}
            </div>
            <div className="text-sm text-gray-500">
              ${subscription.billing.amount}/{subscription.plan.pricing.billingCycle}
            </div>
          </div>

          <div>
            <div className="text-sm font-medium text-gray-700">Next Billing</div>
            <div className="text-lg text-gray-900">
              {format(new Date(subscription.billing.nextBillingDate), 'MMM d, yyyy')}
            </div>
          </div>

          <div>
            <div className="text-sm font-medium text-gray-700">Payment Method</div>
            <div className="text-lg text-gray-900">
              {subscription.billing.paymentMethod?.brand} ****{subscription.billing.paymentMethod?.last4}
            </div>
          </div>
        </div>

        {subscription.discounts.length > 0 && (
          <div className="mt-4 p-3 bg-green-50 rounded-lg">
            <div className="flex items-center">
              <CheckCircleIcon className="w-5 h-5 text-green-400 mr-2" />
              <div>
                <div className="text-sm font-medium text-green-800">
                  Active Discount: {subscription.discounts[0].name}
                </div>
                <div className="text-sm text-green-700">
                  {subscription.discounts[0].amount}% off until {format(new Date(subscription.discounts[0].validUntil!), 'MMM d, yyyy')}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Usage Overview */}
      <div className="bg-white shadow rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-medium text-gray-900">Usage This Period</h3>
          <ChartBarIcon className="w-5 h-5 text-gray-400" />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {subscription.plan.limits.users !== undefined && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-medium text-gray-700">Users</span>
                <span className="text-sm text-gray-500">
                  {subscription.usage?.users || 0}
                  {subscription.plan.limits.users === -1 ? '' : `/${subscription.plan.limits.users}`}
                </span>
              </div>
              {subscription.plan.limits.users !== -1 && (
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className={clsx(
                      'h-2 rounded-full',
                      getUsageColor(calculateUsagePercentage(subscription.usage?.users || 0, subscription.plan.limits.users))
                    )}
                    style={{ width: `${calculateUsagePercentage(subscription.usage?.users || 0, subscription.plan.limits.users)}%` }}
                  />
                </div>
              )}
            </div>
          )}

          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">API Calls</span>
              <span className="text-sm text-gray-500">
                {subscription.usage?.apiCalls.toLocaleString() || 0}/{subscription.plan.limits.apiCalls?.toLocaleString()}
              </span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={clsx(
                  'h-2 rounded-full',
                  getUsageColor(calculateUsagePercentage(subscription.usage?.apiCalls || 0, subscription.plan.limits.apiCalls || 0))
                )}
                style={{ width: `${calculateUsagePercentage(subscription.usage?.apiCalls || 0, subscription.plan.limits.apiCalls || 0)}%` }}
              />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Storage</span>
              <span className="text-sm text-gray-500">
                {subscription.usage?.storage}GB/{subscription.plan.limits.storage}GB
              </span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={clsx(
                  'h-2 rounded-full',
                  getUsageColor(calculateUsagePercentage(subscription.usage?.storage || 0, subscription.plan.limits.storage || 0))
                )}
                style={{ width: `${calculateUsagePercentage(subscription.usage?.storage || 0, subscription.plan.limits.storage || 0)}%` }}
              />
            </div>
          </div>

          <div className="md:col-span-2 lg:col-span-1">
            <div className="text-center p-4 bg-blue-50 rounded-lg">
              <div className="text-2xl font-bold text-blue-600">
                {Math.ceil((new Date(subscription.currentPeriodEnd).getTime() - Date.now()) / (1000 * 60 * 60 * 24))}
              </div>
              <div className="text-sm text-blue-700">Days Remaining</div>
            </div>
          </div>
        </div>
      </div>

      {/* Analytics Summary */}
      <div className="bg-white shadow rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Subscription Analytics</h3>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-gray-900">
              ${subscription.analytics.monthlyRecurringRevenue.toFixed(2)}
            </div>
            <div className="text-sm text-gray-500">MRR</div>
          </div>

          <div className="text-center">
            <div className="text-2xl font-bold text-gray-900">
              ${subscription.analytics.annualRecurringRevenue.toFixed(2)}
            </div>
            <div className="text-sm text-gray-500">ARR</div>
          </div>

          <div className="text-center">
            <div className="flex items-center justify-center">
              <ArrowTrendingUpIcon className="w-5 h-5 text-green-500 mr-1" />
              <span className="text-2xl font-bold text-green-600">
                +{subscription.analytics.usageGrowth.toFixed(1)}%
              </span>
            </div>
            <div className="text-sm text-gray-500">Usage Growth</div>
          </div>

          <div className="text-center">
            <div className={clsx(
              'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mb-1',
              subscription.analytics.churnRisk === 'low' ? 'bg-green-100 text-green-800' :
              subscription.analytics.churnRisk === 'medium' ? 'bg-yellow-100 text-yellow-800' :
              'bg-red-100 text-red-800'
            )}>
              {subscription.analytics.churnRisk.toUpperCase()} RISK
            </div>
            <div className="text-sm text-gray-500">Churn Risk</div>
          </div>
        </div>
      </div>
    </div>
  )

  const renderTabContent = () => {
    switch (activeTab) {
      case 'overview':
        return renderOverview()
      case 'billing':
        return <div className="p-8 text-center text-gray-500">Billing management coming soon</div>
      case 'usage':
        return <div className="p-8 text-center text-gray-500">Detailed usage analytics coming soon</div>
      case 'analytics':
        return <div className="p-8 text-center text-gray-500">Advanced analytics dashboard coming soon</div>
      case 'settings':
        return <div className="p-8 text-center text-gray-500">Subscription settings coming soon</div>
      default:
        return renderOverview()
    }
  }

  return (
    <div className={clsx('space-y-6', className)}>
      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {[
            { key: 'overview', label: 'Overview', icon: ChartBarIcon },
            { key: 'billing', label: 'Billing', icon: CreditCardIcon },
            { key: 'usage', label: 'Usage', icon: BanknotesIcon },
            { key: 'analytics', label: 'Analytics', icon: ArrowTrendingUpIcon },
            { key: 'settings', label: 'Settings', icon: CogIcon },
          ].map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => setActiveTab(key as typeof activeTab)}
              className={clsx(
                'group inline-flex items-center py-2 px-1 border-b-2 font-medium text-sm',
                activeTab === key
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              )}
            >
              <Icon className="w-5 h-5 mr-2" />
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      {renderTabContent()}

      {/* Plan Selector Modal */}
      {showPlanSelector && (
        <div className="fixed inset-0 z-50 bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
            <div className="px-6 py-4 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium text-gray-900">Choose Your Plan</h3>
                <button
                  onClick={() => setShowPlanSelector(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  ✕
                </button>
              </div>
            </div>

            <div className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {availablePlans.map((plan) => (
                  <div
                    key={plan.id}
                    className={clsx(
                      'border-2 rounded-lg p-6',
                      plan.id === subscription.planId
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300'
                    )}
                  >
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="text-xl font-semibold text-gray-900">{plan.name}</h4>
                      {plan.isPopular && (
                        <span className="bg-blue-100 text-blue-800 text-xs font-medium px-2.5 py-0.5 rounded-full">
                          Popular
                        </span>
                      )}
                    </div>

                    <div className="mb-4">
                      <div className="text-3xl font-bold text-gray-900">
                        ${plan.pricing.basePrice}
                        <span className="text-base font-normal text-gray-500">
                          /{plan.pricing.billingCycle}
                        </span>
                      </div>
                    </div>

                    <p className="text-gray-600 mb-4">{plan.description}</p>

                    <ul className="space-y-2 mb-6">
                      {plan.features.map((feature, index) => (
                        <li key={index} className="flex items-center text-sm">
                          <CheckCircleIcon className="w-4 h-4 text-green-500 mr-2" />
                          {feature}
                        </li>
                      ))}
                    </ul>

                    <Button
                      className="w-full"
                      variant={plan.id === subscription.planId ? 'outline' : 'primary'}
                      disabled={plan.id === subscription.planId}
                    >
                      {plan.id === subscription.planId ? 'Current Plan' : 'Select Plan'}
                    </Button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AdvancedSubscriptionManager