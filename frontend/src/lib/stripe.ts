import { loadStripe } from '@stripe/stripe-js'

// This should be your publishable key
const stripePublishableKey =
  import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || 'pk_test_51234567890abcdef'

export const stripePromise = loadStripe(stripePublishableKey)

export default stripePromise
