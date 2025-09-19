# Frontend Development Guide

This guide covers React TypeScript development patterns for the payment platform frontend.

## Project Structure

The frontend follows a feature-based architecture:

```
frontend/src/
├── components/     # Reusable UI components
├── features/       # Feature-specific modules
│   ├── auth/       # Authentication feature
│   ├── payments/   # Payment processing feature
│   ├── users/      # User management feature
│   └── subscriptions/ # Subscription management feature
├── hooks/          # Custom React hooks
├── services/       # API service layer
├── store/          # Redux store configuration
├── types/          # TypeScript type definitions
└── utils/          # Utility functions
```

## Development Workflow

### 1. Component Development

Use TypeScript with strict typing:

```typescript
interface PaymentFormProps {
  onSubmit: (data: PaymentData) => void;
  loading?: boolean;
}

const PaymentForm: React.FC<PaymentFormProps> = ({ onSubmit, loading = false }) => {
  // Component implementation
};
```

### 2. State Management

Use Redux Toolkit with RTK Query:

```typescript
// Store slice
const paymentsSlice = createSlice({
  name: 'payments',
  initialState,
  reducers: {
    // Reducers
  },
});

// API slice
const paymentsApi = createApi({
  reducerPath: 'paymentsApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/payments',
  }),
  endpoints: (builder) => ({
    createPayment: builder.mutation<Payment, CreatePaymentRequest>({
      query: (payment) => ({
        url: '',
        method: 'POST',
        body: payment,
      }),
    }),
  }),
});
```

### 3. Testing

Use Vitest for unit tests and Playwright for E2E:

```typescript
// Unit test
import { render, screen } from '@testing-library/react';
import { PaymentForm } from './PaymentForm';

test('renders payment form', () => {
  render(<PaymentForm onSubmit={jest.fn()} />);
  expect(screen.getByText('Payment Details')).toBeInTheDocument();
});

// E2E test
test('complete payment flow', async ({ page }) => {
  await page.goto('/payments');
  await page.fill('[data-testid="amount"]', '100.00');
  await page.click('[data-testid="submit"]');
  await expect(page.locator('[data-testid="success"]')).toBeVisible();
});
```

## Key Patterns

### API Integration

```typescript
const { data: payments, error, isLoading } = useGetPaymentsQuery();
const [createPayment] = useCreatePaymentMutation();
```

### Error Handling

```typescript
const PaymentComponent = () => {
  const { error } = useGetPaymentsQuery();

  if (error) {
    return <ErrorBoundary error={error} />;
  }

  // Component implementation
};
```