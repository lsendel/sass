# UI/UX Guidelines Context

This context provides comprehensive UI/UX standards and accessibility requirements for all agents working on the React frontend of the Spring Boot Modulith payment platform.

## Design System Foundation

### React Architecture Standards
**Framework**: React 18+ with TypeScript 5.0+
**State Management**: Redux Toolkit with RTK Query
**Styling**: CSS-in-JS with styled-components
**Component Library**: Custom design system with accessibility-first approach

### Core Design Principles
1. **Accessibility First**: WCAG 2.1 AA compliance mandatory
2. **Performance Driven**: Core Web Vitals optimization
3. **Mobile First**: Responsive design with mobile-first approach
4. **Progressive Enhancement**: Graceful degradation for all features
5. **Inclusive Design**: Usable by users with diverse abilities and contexts

## Accessibility Requirements (WCAG 2.1 AA)

### Mandatory Accessibility Standards
**Level AA Compliance**: All components must meet WCAG 2.1 AA standards

#### 1. Perceivable
```tsx
// Color contrast requirements
const colors = {
  primary: '#1976d2',      // 4.5:1 contrast ratio minimum
  secondary: '#dc004e',    // 4.5:1 contrast ratio minimum
  text: '#212121',         // 7:1 contrast ratio for body text
  background: '#ffffff'
};

// Alternative text for images
<img
  src="/payment-success.svg"
  alt="Payment processed successfully - checkmark icon"
  role="img"
/>

// Text alternatives for complex content
<div role="img" aria-label="Payment flow: card entry, processing, confirmation">
  <PaymentFlowDiagram />
</div>
```

#### 2. Operable
```tsx
// Keyboard navigation support
const PaymentForm = () => {
  const handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
      submitPayment();
    }
  };

  return (
    <form onKeyDown={handleKeyDown}>
      <input
        type="text"
        aria-label="Credit card number"
        tabIndex={0}
      />
      <button
        type="submit"
        aria-label="Submit payment"
        tabIndex={0}
      >
        Pay Now
      </button>
    </form>
  );
};

// Focus management
const Modal = ({ isOpen }: { isOpen: boolean }) => {
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen && modalRef.current) {
      modalRef.current.focus();
    }
  }, [isOpen]);

  return (
    <div
      ref={modalRef}
      role="dialog"
      aria-modal="true"
      tabIndex={-1}
    >
      <ModalContent />
    </div>
  );
};
```

#### 3. Understandable
```tsx
// Clear form validation
const SubscriptionForm = () => {
  const [errors, setErrors] = useState<ValidationErrors>({});

  return (
    <form aria-labelledby="subscription-form-title">
      <h2 id="subscription-form-title">Choose Your Subscription Plan</h2>

      <fieldset>
        <legend>Billing Information</legend>

        <input
          type="email"
          aria-describedby="email-error email-help"
          aria-invalid={errors.email ? 'true' : 'false'}
          aria-required="true"
        />

        <div id="email-help" className="help-text">
          We'll use this email for billing notifications
        </div>

        {errors.email && (
          <div id="email-error" role="alert" className="error-message">
            Please enter a valid email address
          </div>
        )}
      </fieldset>
    </form>
  );
};
```

#### 4. Robust
```tsx
// Semantic HTML with ARIA enhancement
const PricingTier = ({ plan }: { plan: SubscriptionPlan }) => (
  <article role="button" tabIndex={0} aria-describedby={`plan-${plan.id}-details`}>
    <header>
      <h3>{plan.name}</h3>
      <p aria-label={`${plan.price} per month`}>
        ${plan.price}<span aria-hidden="true">/mo</span>
      </p>
    </header>

    <div id={`plan-${plan.id}-details`}>
      <ul role="list">
        {plan.features.map(feature => (
          <li key={feature} role="listitem">{feature}</li>
        ))}
      </ul>
    </div>

    <button aria-label={`Select ${plan.name} plan for ${plan.price} per month`}>
      Choose Plan
    </button>
  </article>
);
```

## Component Architecture Standards

### Component Hierarchy
```tsx
// Atomic Design Pattern
components/
├── atoms/           # Basic building blocks
│   ├── Button/
│   ├── Input/
│   └── Icon/
├── molecules/       # Simple combinations
│   ├── FormField/
│   ├── PriceCard/
│   └── AlertMessage/
├── organisms/       # Complex combinations
│   ├── PaymentForm/
│   ├── SubscriptionTier/
│   └── UserDashboard/
├── templates/       # Page-level layouts
│   ├── AuthLayout/
│   ├── DashboardLayout/
│   └── CheckoutLayout/
└── pages/          # Complete pages
    ├── LoginPage/
    ├── SubscriptionPage/
    └── PaymentPage/
```

### TypeScript Component Patterns
```tsx
// Branded types for IDs
type UserId = string & { __brand: 'UserId' };
type OrganizationId = string & { __brand: 'OrganizationId' };
type SubscriptionId = string & { __brand: 'SubscriptionId' };

// Component props with accessibility
interface ButtonProps {
  children: React.ReactNode;
  variant: 'primary' | 'secondary' | 'danger';
  size: 'small' | 'medium' | 'large';
  disabled?: boolean;
  loading?: boolean;
  onClick: () => void;

  // Accessibility props
  'aria-label'?: string;
  'aria-describedby'?: string;
  role?: string;
  tabIndex?: number;
}

// Component implementation with accessibility
const Button: React.FC<ButtonProps> = ({
  children,
  variant,
  size,
  disabled = false,
  loading = false,
  onClick,
  ...accessibilityProps
}) => {
  return (
    <StyledButton
      variant={variant}
      size={size}
      disabled={disabled || loading}
      onClick={onClick}
      {...accessibilityProps}
      aria-busy={loading}
    >
      {loading && <LoadingSpinner aria-hidden="true" />}
      <span className={loading ? 'visually-hidden' : ''}>{children}</span>
    </StyledButton>
  );
};
```

### State Management with RTK Query
```tsx
// API slice with error handling
export const subscriptionApi = createApi({
  reducerPath: 'subscriptionApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/subscriptions',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Subscription'],
  endpoints: (builder) => ({
    getSubscriptions: builder.query<Subscription[], void>({
      query: () => '',
      providesTags: ['Subscription'],
    }),
    createSubscription: builder.mutation<Subscription, CreateSubscriptionRequest>({
      query: (subscription) => ({
        url: '',
        method: 'POST',
        body: subscription,
      }),
      invalidatesTags: ['Subscription'],
    }),
  }),
});
```

## Performance Standards

### Core Web Vitals Requirements
- **Largest Contentful Paint (LCP)**: < 2.5 seconds
- **First Input Delay (FID)**: < 100 milliseconds
- **Cumulative Layout Shift (CLS)**: < 0.1

### Bundle Optimization
```tsx
// Code splitting with React.lazy
const SubscriptionPage = React.lazy(() => import('./pages/SubscriptionPage'));
const PaymentPage = React.lazy(() => import('./pages/PaymentPage'));

// Component with Suspense boundary
const App = () => (
  <Router>
    <Suspense fallback={<LoadingSpinner aria-label="Loading page content" />}>
      <Routes>
        <Route path="/subscription" element={<SubscriptionPage />} />
        <Route path="/payment" element={<PaymentPage />} />
      </Routes>
    </Suspense>
  </Router>
);

// Memoization for expensive calculations
const PriceCalculator = React.memo<PriceCalculatorProps>(({
  plan,
  discounts,
  taxRate
}) => {
  const finalPrice = useMemo(() => {
    return calculatePrice(plan.basePrice, discounts, taxRate);
  }, [plan.basePrice, discounts, taxRate]);

  return (
    <div aria-live="polite" aria-label={`Final price: ${finalPrice}`}>
      ${finalPrice}
    </div>
  );
});
```

### Image Optimization
```tsx
// Responsive images with accessibility
const OptimizedImage: React.FC<{
  src: string;
  alt: string;
  width: number;
  height: number;
}> = ({ src, alt, width, height }) => (
  <picture>
    <source
      media="(min-width: 768px)"
      srcSet={`${src}?w=${width}&f=webp`}
      type="image/webp"
    />
    <source
      media="(min-width: 768px)"
      srcSet={`${src}?w=${width}`}
    />
    <img
      src={`${src}?w=${width / 2}`}
      alt={alt}
      width={width}
      height={height}
      loading="lazy"
      decoding="async"
    />
  </picture>
);
```

## Payment Flow UX Patterns

### Progressive Disclosure
```tsx
const PaymentFlow = () => {
  const [step, setStep] = useState<'plan' | 'payment' | 'confirmation'>('plan');

  return (
    <div role="main" aria-labelledby="payment-flow-title">
      <h1 id="payment-flow-title">Subscription Setup</h1>

      <nav aria-label="Payment flow progress">
        <ol>
          <li aria-current={step === 'plan' ? 'step' : undefined}>
            Choose Plan
          </li>
          <li aria-current={step === 'payment' ? 'step' : undefined}>
            Payment Details
          </li>
          <li aria-current={step === 'confirmation' ? 'step' : undefined}>
            Confirmation
          </li>
        </ol>
      </nav>

      {step === 'plan' && <PlanSelection onNext={() => setStep('payment')} />}
      {step === 'payment' && <PaymentDetails onNext={() => setStep('confirmation')} />}
      {step === 'confirmation' && <Confirmation />}
    </div>
  );
};
```

### Error Handling UX
```tsx
const PaymentForm = () => {
  const [errors, setErrors] = useState<PaymentErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (paymentData: PaymentFormData) => {
    setIsSubmitting(true);
    setErrors({});

    try {
      await processPayment(paymentData);
    } catch (error) {
      setErrors(parsePaymentErrors(error));

      // Announce error to screen readers
      announceToScreenReader('Payment failed. Please check the errors below.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate>
      {Object.keys(errors).length > 0 && (
        <div role="alert" className="error-summary">
          <h3>Please fix the following errors:</h3>
          <ul>
            {Object.entries(errors).map(([field, message]) => (
              <li key={field}>
                <a href={`#${field}`}>{message}</a>
              </li>
            ))}
          </ul>
        </div>
      )}

      <CardNumberField error={errors.cardNumber} />
      <ExpiryField error={errors.expiry} />
      <CVCField error={errors.cvc} />

      <button
        type="submit"
        disabled={isSubmitting}
        aria-describedby="submit-help"
      >
        {isSubmitting ? 'Processing Payment...' : 'Complete Payment'}
      </button>

      <div id="submit-help" className="help-text">
        Your payment is secured with 256-bit SSL encryption
      </div>
    </form>
  );
};
```

## Testing Standards for UI/UX

### Accessibility Testing with Playwright
```typescript
test.describe('Payment Form Accessibility', () => {
  test('meets WCAG 2.1 AA standards', async ({ page }) => {
    await page.goto('/subscription/payment');

    // Inject axe-core
    await page.addInitScript(() => {
      window.axe = require('axe-core');
    });

    // Run accessibility audit
    const accessibilityResults = await page.evaluate(() => window.axe.run({
      rules: {
        'color-contrast': { enabled: true },
        'keyboard-navigation': { enabled: true },
        'focus-management': { enabled: true }
      }
    }));

    expect(accessibilityResults.violations).toEqual([]);
  });

  test('supports keyboard navigation', async ({ page }) => {
    await page.goto('/subscription/payment');

    // Tab through all interactive elements
    await page.keyboard.press('Tab'); // Card number field
    await page.keyboard.press('Tab'); // Expiry field
    await page.keyboard.press('Tab'); // CVC field
    await page.keyboard.press('Tab'); // Submit button

    // Enter should submit form
    await page.keyboard.press('Enter');

    // Verify form submission
    await expect(page.locator('[data-testid="payment-processing"]')).toBeVisible();
  });

  test('provides screen reader announcements', async ({ page }) => {
    await page.goto('/subscription/payment');

    // Monitor aria-live regions
    const liveRegion = page.locator('[aria-live="polite"]');

    // Trigger error state
    await page.click('[data-testid="submit-payment"]');

    // Verify error announcement
    await expect(liveRegion).toContainText('Please fix the following errors');
  });
});
```

### Performance Testing
```typescript
test.describe('Payment Flow Performance', () => {
  test('meets Core Web Vitals requirements', async ({ page }) => {
    await page.goto('/subscription');

    // Measure Core Web Vitals
    const coreWebVitals = await page.evaluate(() => {
      return new Promise((resolve) => {
        new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const vitals = {
            lcp: 0,
            fid: 0,
            cls: 0
          };

          entries.forEach((entry) => {
            if (entry.entryType === 'largest-contentful-paint') {
              vitals.lcp = entry.startTime;
            } else if (entry.entryType === 'first-input') {
              vitals.fid = entry.processingStart - entry.startTime;
            } else if (entry.entryType === 'layout-shift') {
              vitals.cls += entry.value;
            }
          });

          resolve(vitals);
        }).observe({ entryTypes: ['largest-contentful-paint', 'first-input', 'layout-shift'] });
      });
    });

    expect(coreWebVitals.lcp).toBeLessThan(2500);
    expect(coreWebVitals.fid).toBeLessThan(100);
    expect(coreWebVitals.cls).toBeLessThan(0.1);
  });
});
```

## Agent Integration Patterns

### React Optimization Agent
**Responsibilities**:
- Bundle size optimization recommendations
- Component performance analysis
- React best practices enforcement
- State management optimization

### Accessibility Champion Agent
**Responsibilities**:
- WCAG 2.1 AA compliance validation
- Screen reader compatibility testing
- Keyboard navigation verification
- Color contrast validation

### UX Research Agent
**Responsibilities**:
- User journey analysis
- Conversion funnel optimization
- A/B testing recommendations
- Usability heuristic evaluation

### Visual Design Agent
**Responsibilities**:
- Design system consistency
- Component library maintenance
- Visual regression testing
- Brand guideline compliance

## Constitutional UI/UX Compliance

### Mandatory Requirements Checklist
- [ ] WCAG 2.1 AA compliance for all components
- [ ] Core Web Vitals performance targets met
- [ ] Mobile-first responsive design implemented
- [ ] Keyboard navigation fully supported
- [ ] Screen reader compatibility verified
- [ ] Color contrast ratios meet accessibility standards
- [ ] Error handling with clear user feedback
- [ ] Progressive enhancement principles followed
- [ ] Performance budgets respected
- [ ] Semantic HTML with ARIA enhancement

---

**UI/UX Guidelines Version**: 1.4.0
**Last Updated**: 2024-09-14
**WCAG Compliance**: 2.1 AA Required

This UI/UX guidelines context is the authoritative standard for all frontend development and user experience decisions. All agents must prioritize accessibility and performance in their recommendations and implementations.