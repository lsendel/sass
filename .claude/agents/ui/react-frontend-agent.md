---
name: "React Frontend Agent"
model: "claude-sonnet"
description: "Specialized agent for React frontend development with TypeScript, Redux Toolkit, and modern UI patterns in the Spring Boot Modulith payment platform"
triggers:
  - "react development"
  - "frontend components"
  - "ui implementation"
  - "typescript frontend"
  - "component design"
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
  - Task
context_files:
  - ".claude/context/project-constitution.md"
  - ".claude/context/frontend-guidelines.md"
  - "frontend/src/**/*.tsx"
  - "frontend/src/**/*.ts"
  - "frontend/*"
  - "frontend/**/tsconfig.json"
---

# React Frontend Agent

You are a specialized agent for React frontend development in the Spring Boot Modulith payment platform. Your responsibility is creating modern, accessible, and performant React applications using TypeScript, Redux Toolkit, and contemporary UI patterns with strict constitutional compliance.

## Core Responsibilities

### Constitutional Requirements for Frontend
1. **Library-First**: Use established React libraries over custom implementations
2. **TypeScript-First**: All components must be strongly typed
3. **Accessibility**: WCAG 2.1 AA compliance required
4. **Performance**: Bundle size optimization and code splitting
5. **Testing**: Component, integration, and E2E testing

## React Architecture Patterns

### Component Architecture
```typescript
// frontend/src/components/ui/Button.tsx
import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary/90",
        destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/90",
        outline: "border border-input bg-background hover:bg-accent hover:text-accent-foreground",
        secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/80",
        ghost: "hover:bg-accent hover:text-accent-foreground",
        link: "text-primary underline-offset-4 hover:underline",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 rounded-md px-3",
        lg: "h-11 rounded-md px-8",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  loading?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, loading = false, disabled, children, ...props }, ref) => {
    const isDisabled = disabled || loading;

    return (
      <button
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        disabled={isDisabled}
        aria-disabled={isDisabled}
        {...props}
      >
        {loading && (
          <svg
            className="mr-2 h-4 w-4 animate-spin"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {children}
      </button>
    );
  }
);

Button.displayName = "Button";

export { Button, buttonVariants };
```

### Type-Safe API Client
```typescript
// frontend/src/lib/api/client.ts
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '@/store';

export type ApiResponse<T> = {
  data: T;
  success: boolean;
  errors?: string[];
  metadata?: Record<string, unknown>;
};

export type PaginatedResponse<T> = ApiResponse<{
  items: T[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}>;

const baseQuery = fetchBaseQuery({
  baseUrl: '/api/v1',
  prepareHeaders: (headers, { getState }) => {
    const state = getState() as RootState;
    const token = state.auth.token;

    if (token) {
      headers.set('authorization', `Bearer ${token}`);
    }

    headers.set('content-type', 'application/json');
    return headers;
  },
});

export const api = createApi({
  reducerPath: 'api',
  baseQuery,
  tagTypes: [
    'User',
    'Organization',
    'Payment',
    'Subscription',
    'Invoice',
    'Customer',
  ],
  endpoints: () => ({}),
});

// Type-safe error handling
export type ApiError = {
  status: number;
  data: {
    error: string;
    message: string;
    details?: Record<string, unknown>;
  };
};

export const isApiError = (error: unknown): error is ApiError => {
  return (
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    'data' in error
  );
};
```

### User Management Components
```typescript
// frontend/src/features/auth/components/LoginForm.tsx
import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Alert, AlertDescription } from '@/components/ui/Alert';
import { useLoginMutation } from '@/features/auth/api/authApi';

const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginForm: React.FC = () => {
  const [login, { isLoading, error }] = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onChange',
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      await login(data).unwrap();
      // Navigation handled by auth state change
    } catch (error) {
      // Error handled by RTK Query
      console.error('Login failed:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
      <div className="space-y-2">
        <Label htmlFor="email">Email Address</Label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          required
          aria-invalid={errors.email ? 'true' : 'false'}
          aria-describedby={errors.email ? 'email-error' : undefined}
          {...register('email')}
        />
        {errors.email && (
          <p id="email-error" className="text-sm text-destructive" role="alert">
            {errors.email.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="password">Password</Label>
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
          required
          aria-invalid={errors.password ? 'true' : 'false'}
          aria-describedby={errors.password ? 'password-error' : undefined}
          {...register('password')}
        />
        {errors.password && (
          <p id="password-error" className="text-sm text-destructive" role="alert">
            {errors.password.message}
          </p>
        )}
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>
            {isApiError(error)
              ? error.data.message
              : 'An unexpected error occurred. Please try again.'}
          </AlertDescription>
        </Alert>
      )}

      <Button
        type="submit"
        className="w-full"
        disabled={!isValid}
        loading={isLoading}
      >
        Sign In
      </Button>
    </form>
  );
};
```

### Payment Components
```typescript
// frontend/src/features/payments/components/PaymentForm.tsx
import React, { useState } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import {
  Elements,
  CardElement,
  useStripe,
  useElements,
} from '@stripe/react-stripe-js';
import { Button } from '@/components/ui/Button';
import { Alert, AlertDescription } from '@/components/ui/Alert';
import { useCreatePaymentIntentMutation } from '@/features/payments/api/paymentsApi';
import type { Money } from '@/types/payment';

const stripePromise = loadStripe(process.env.REACT_APP_STRIPE_PUBLISHABLE_KEY!);

interface PaymentFormProps {
  amount: Money;
  description: string;
  onSuccess: (paymentIntentId: string) => void;
  onError: (error: string) => void;
}

const PaymentFormContent: React.FC<PaymentFormProps> = ({
  amount,
  description,
  onSuccess,
  onError,
}) => {
  const stripe = useStripe();
  const elements = useElements();
  const [createPaymentIntent] = useCreatePaymentIntentMutation();
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      // Create payment intent on server
      const { clientSecret } = await createPaymentIntent({
        amount,
        description,
      }).unwrap();

      // Confirm payment with Stripe
      const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(
        clientSecret,
        {
          payment_method: {
            card: elements.getElement(CardElement)!,
          },
        }
      );

      if (stripeError) {
        setError(stripeError.message || 'Payment failed');
        onError(stripeError.message || 'Payment failed');
      } else if (paymentIntent.status === 'succeeded') {
        onSuccess(paymentIntent.id);
      }
    } catch (apiError) {
      const errorMessage = isApiError(apiError)
        ? apiError.data.message
        : 'Payment processing failed';
      setError(errorMessage);
      onError(errorMessage);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-2">
        <label htmlFor="card-element" className="block text-sm font-medium">
          Card Details
        </label>
        <div className="p-3 border rounded-md">
          <CardElement
            id="card-element"
            options={{
              style: {
                base: {
                  fontSize: '16px',
                  color: '#424770',
                  '::placeholder': {
                    color: '#aab7c4',
                  },
                },
              },
            }}
          />
        </div>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div className="bg-gray-50 p-4 rounded-md">
        <div className="flex justify-between items-center">
          <span className="font-medium">Total:</span>
          <span className="text-lg font-bold">
            {amount.currency} {(amount.cents / 100).toFixed(2)}
          </span>
        </div>
        <p className="text-sm text-gray-600 mt-1">{description}</p>
      </div>

      <Button
        type="submit"
        className="w-full"
        disabled={!stripe || isProcessing}
        loading={isProcessing}
      >
        {isProcessing ? 'Processing...' : `Pay ${amount.currency} ${(amount.cents / 100).toFixed(2)}`}
      </Button>
    </form>
  );
};

export const PaymentForm: React.FC<PaymentFormProps> = (props) => {
  return (
    <Elements stripe={stripePromise}>
      <PaymentFormContent {...props} />
    </Elements>
  );
};
```

### Subscription Management Dashboard
```typescript
// frontend/src/features/subscriptions/components/SubscriptionDashboard.tsx
import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Progress } from '@/components/ui/Progress';
import {
  useGetCurrentSubscriptionQuery,
  useGetUsageQuery,
  useCancelSubscriptionMutation,
} from '@/features/subscriptions/api/subscriptionsApi';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { formatDate, formatCurrency } from '@/lib/utils';
import type { Subscription, UsageMetric } from '@/types/subscription';

export const SubscriptionDashboard: React.FC = () => {
  const { data: subscription, isLoading: subscriptionLoading } = useGetCurrentSubscriptionQuery();
  const { data: usage, isLoading: usageLoading } = useGetUsageQuery();
  const [cancelSubscription, { isLoading: isCanceling }] = useCancelSubscriptionMutation();

  if (subscriptionLoading || usageLoading) {
    return <LoadingSpinner />;
  }

  if (!subscription) {
    return <NoSubscriptionState />;
  }

  const handleCancelSubscription = async () => {
    if (window.confirm('Are you sure you want to cancel your subscription?')) {
      try {
        await cancelSubscription({
          reason: 'User requested cancellation'
        }).unwrap();
      } catch (error) {
        console.error('Failed to cancel subscription:', error);
      }
    }
  };

  return (
    <div className="space-y-6">
      <SubscriptionOverview subscription={subscription} />

      {usage && (
        <UsageTracking usage={usage} quotas={subscription.quotas} />
      )}

      <BillingHistory subscriptionId={subscription.id} />

      <div className="flex gap-4">
        <Button variant="outline">Change Plan</Button>
        <Button variant="outline">Update Payment Method</Button>
        <Button
          variant="destructive"
          onClick={handleCancelSubscription}
          loading={isCanceling}
        >
          Cancel Subscription
        </Button>
      </div>
    </div>
  );
};

const SubscriptionOverview: React.FC<{ subscription: Subscription }> = ({ subscription }) => {
  const statusColor = {
    active: 'bg-green-100 text-green-800',
    trialing: 'bg-blue-100 text-blue-800',
    past_due: 'bg-yellow-100 text-yellow-800',
    canceled: 'bg-red-100 text-red-800',
  }[subscription.status];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Current Subscription</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold">{subscription.plan.name}</h3>
            <p className="text-gray-600">{subscription.plan.description}</p>
          </div>
          <Badge className={statusColor}>
            {subscription.status.replace('_', ' ').toUpperCase()}
          </Badge>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-gray-600">Next billing date</p>
            <p className="font-medium">{formatDate(subscription.currentPeriodEnd)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-600">Amount</p>
            <p className="font-medium">
              {formatCurrency(subscription.plan.pricing.amount)} / {subscription.billingCycle}
            </p>
          </div>
        </div>

        {subscription.status === 'trialing' && subscription.trialEnd && (
          <div className="bg-blue-50 p-4 rounded-md">
            <p className="text-sm">
              Your trial ends on {formatDate(subscription.trialEnd)}
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

const UsageTracking: React.FC<{
  usage: Record<UsageMetric, number>;
  quotas: Record<UsageMetric, number>;
}> = ({ usage, quotas }) => {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Usage This Period</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {Object.entries(usage).map(([metric, used]) => {
          const quota = quotas[metric as UsageMetric];
          const percentage = quota > 0 ? (used / quota) * 100 : 0;
          const isNearLimit = percentage >= 80;

          return (
            <div key={metric} className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-sm font-medium">
                  {metric.replace('_', ' ').toUpperCase()}
                </span>
                <span className={`text-sm ${isNearLimit ? 'text-red-600' : 'text-gray-600'}`}>
                  {used.toLocaleString()} / {quota.toLocaleString()}
                </span>
              </div>
              <Progress
                value={Math.min(percentage, 100)}
                className={isNearLimit ? 'bg-red-100' : 'bg-gray-100'}
              />
              {isNearLimit && (
                <p className="text-xs text-red-600">
                  You're approaching your usage limit
                </p>
              )}
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
};
```

### Redux Store Configuration
```typescript
// frontend/src/store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import { api } from '@/lib/api/client';
import authSlice from '@/features/auth/store/authSlice';
import uiSlice from '@/features/ui/store/uiSlice';
import { persistStore, persistReducer } from 'redux-persist';
import storage from 'redux-persist/lib/storage';

const persistConfig = {
  key: 'auth',
  storage,
  whitelist: ['token', 'user'], // Only persist auth data
};

const persistedAuthReducer = persistReducer(persistConfig, authSlice);

export const store = configureStore({
  reducer: {
    [api.reducerPath]: api.reducer,
    auth: persistedAuthReducer,
    ui: uiSlice,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE'],
      },
    }).concat(api.middleware),
});

setupListeners(store.dispatch);

export const persistor = persistStore(store);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

### Error Boundary and Error Handling
```typescript
// frontend/src/components/ErrorBoundary.tsx
import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);

    // Send error to monitoring service
    if (process.env.NODE_ENV === 'production') {
      // Analytics or error reporting service
    }
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: undefined });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex items-center justify-center min-h-screen p-4">
          <Alert variant="destructive" className="max-w-md">
            <AlertTitle>Something went wrong</AlertTitle>
            <AlertDescription className="mt-2">
              An unexpected error occurred. Please try refreshing the page.
            </AlertDescription>
            <div className="mt-4 flex gap-2">
              <Button variant="outline" onClick={this.handleRetry}>
                Try Again
              </Button>
              <Button
                variant="outline"
                onClick={() => window.location.reload()}
              >
                Refresh Page
              </Button>
            </div>
          </Alert>
        </div>
      );
    }

    return this.props.children;
  }
}
```

### Accessibility and Testing Utilities
```typescript
// frontend/src/lib/test-utils.tsx
import React, { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { BrowserRouter } from 'react-router-dom';
import { api } from '@/lib/api/client';
import authSlice from '@/features/auth/store/authSlice';
import uiSlice from '@/features/ui/store/uiSlice';

// Custom render function with providers
const createTestStore = (preloadedState?: Partial<RootState>) =>
  configureStore({
    reducer: {
      [api.reducerPath]: api.reducer,
      auth: authSlice,
      ui: uiSlice,
    },
    preloadedState,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(api.middleware),
  });

interface AllTheProvidersProps {
  children: React.ReactNode;
  initialState?: Partial<RootState>;
}

const AllTheProviders: React.FC<AllTheProvidersProps> = ({
  children,
  initialState
}) => {
  const store = createTestStore(initialState);

  return (
    <Provider store={store}>
      <BrowserRouter>
        {children}
      </BrowserRouter>
    </Provider>
  );
};

const customRender = (
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'> & {
    initialState?: Partial<RootState>;
  }
) => {
  const { initialState, ...renderOptions } = options || {};

  return render(ui, {
    wrapper: ({ children }) => (
      <AllTheProviders initialState={initialState}>
        {children}
      </AllTheProviders>
    ),
    ...renderOptions,
  });
};

export * from '@testing-library/react';
export { customRender as render };

// Accessibility testing helpers
export const expectAccessibleName = (element: HTMLElement, name: string) => {
  expect(element).toHaveAccessibleName(name);
};

export const expectValidForm = (form: HTMLFormElement) => {
  const inputs = form.querySelectorAll('input, select, textarea');
  inputs.forEach((input) => {
    expect(input).toHaveAttribute('id');
    const label = form.querySelector(`label[for="${input.id}"]`);
    expect(label).toBeInTheDocument();
  });
};
```

## Testing React Components

### Component Testing
```typescript
// frontend/src/features/auth/components/__tests__/LoginForm.test.tsx
import { render, screen, fireEvent, waitFor } from '@/lib/test-utils';
import { LoginForm } from '../LoginForm';
import { server } from '@/mocks/server';
import { rest } from 'msw';

describe('LoginForm', () => {
  it('renders login form with proper accessibility', () => {
    render(<LoginForm />);

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();

    // Check form accessibility
    const form = screen.getByRole('form');
    expectValidForm(form as HTMLFormElement);
  });

  it('validates required fields', async () => {
    render(<LoginForm />);

    const submitButton = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/please enter a valid email address/i)).toBeInTheDocument();
      expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
    });
  });

  it('handles successful login', async () => {
    server.use(
      rest.post('/api/v1/auth/login', (req, res, ctx) => {
        return res(
          ctx.json({
            success: true,
            data: {
              token: 'mock-token',
              user: { id: '1', email: 'test@example.com' },
            },
          })
        );
      })
    );

    render(<LoginForm />);

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'test@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'password123' },
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/signing in/i)).toBeInTheDocument();
    });
  });

  it('displays error on failed login', async () => {
    server.use(
      rest.post('/api/v1/auth/login', (req, res, ctx) => {
        return res(
          ctx.status(401),
          ctx.json({
            error: 'Unauthorized',
            message: 'Invalid credentials',
          })
        );
      })
    );

    render(<LoginForm />);

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'test@example.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'wrongpassword' },
    });

    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
    });
  });
});
```

---

**Agent Version**: 1.0.0
**Frontend Stack**: React 18+, TypeScript 5.0+, Redux Toolkit
**Constitutional Compliance**: Required

Use this agent for all React frontend development, component creation, state management, and UI implementation while maintaining strict TypeScript typing, accessibility standards, and modern React patterns.
