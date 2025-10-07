# Clean Code Refactoring Guide

## Automated Refactoring Scripts

### Backend Refactoring Script

```bash
#!/bin/bash
# backend-refactor.sh - Automated refactoring for backend clean code issues

echo "Starting backend clean code refactoring..."

# 1. Extract Constants for Magic Values
echo "Step 1: Extracting constants..."

cat > backend/src/main/java/com/platform/shared/constants/PaymentConstants.java << 'EOF'
package com.platform.shared.constants;

import java.time.Duration;

public final class PaymentConstants {
    private PaymentConstants() {}

    // URLs
    public static final String PAYMENT_RETURN_URL = "${payment.return.url:https://localhost:3000/payment/return}";

    // Timeouts
    public static final Duration DEFAULT_UPDATE_INTERVAL = Duration.ofSeconds(30);
    public static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(1);
    public static final Duration STATISTICS_PERIOD = Duration.ofDays(30);

    // Payment Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    // Webhook Events
    public static final String EVENT_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
    public static final String EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    public static final String EVENT_PAYMENT_CANCELED = "payment_intent.canceled";
}
EOF

# 2. Create Parameter Objects
echo "Step 2: Creating parameter objects..."

cat > backend/src/main/java/com/platform/payment/api/PaymentRequest.java << 'EOF'
package com.platform.payment.api;

import java.util.Map;
import java.util.UUID;
import com.platform.shared.types.Money;

public record PaymentRequest(
    UUID organizationId,
    Money amount,
    String currency,
    String description,
    Map<String, String> metadata
) {
    public PaymentRequest {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
        if (amount == null || amount.isNegative()) {
            throw new IllegalArgumentException("Valid amount is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
    }
}
EOF

cat > backend/src/main/java/com/platform/payment/api/BillingUpdateRequest.java << 'EOF'
package com.platform.payment.api;

import com.platform.payment.internal.PaymentMethod;

public record BillingUpdateRequest(
    String displayName,
    String billingName,
    String billingEmail,
    PaymentMethod.BillingAddress billingAddress
) {}
EOF

# 3. Extract Service Methods
echo "Step 3: Extracting service methods..."

cat > backend/src/main/java/com/platform/payment/internal/PaymentValidator.java << 'EOF'
package com.platform.payment.internal;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.platform.shared.security.TenantContext;

@Component
public class PaymentValidator {

    public void validateOrganizationAccess(UUID organizationId) {
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("Authentication required");
        }

        if (!TenantContext.belongsToOrganization(organizationId)) {
            throw new SecurityException("Access denied to organization: " + organizationId);
        }
    }

    public void validatePaymentMethodId(String paymentMethodId) {
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new IllegalArgumentException("Payment method ID cannot be null or blank");
        }
    }
}
EOF

cat > backend/src/main/java/com/platform/payment/internal/StripeCustomerService.java << 'EOF'
package com.platform.payment.internal;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import com.platform.user.internal.Organization;
import org.springframework.stereotype.Service;

@Service
public class StripeCustomerService {

    public String getOrCreateCustomer(Organization organization) throws StripeException {
        CustomerSearchParams searchParams = CustomerSearchParams.builder()
            .setQuery("metadata['organization_id']:'" + organization.getId() + "'")
            .build();

        CustomerSearchResult searchResult = Customer.search(searchParams);

        if (!searchResult.getData().isEmpty()) {
            return searchResult.getData().get(0).getId();
        }

        return createNewCustomer(organization);
    }

    private String createNewCustomer(Organization organization) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
            .setName(organization.getName())
            .setDescription("Customer for organization: " + organization.getName())
            .putMetadata("organization_id", organization.getId().toString())
            .build();

        Customer customer = Customer.create(params);
        return customer.getId();
    }
}
EOF

echo "Backend refactoring complete!"
```

### Frontend Refactoring Script

```bash
#!/bin/bash
# frontend-refactor.sh - Automated refactoring for frontend clean code issues

echo "Starting frontend clean code refactoring..."

# 1. Extract Constants
echo "Step 1: Creating constants file..."

cat > frontend/src/constants/dashboard.ts << 'EOF'
export const DASHBOARD_CONSTANTS = {
  UPDATE_INTERVAL: 30 * 1000, // 30 seconds
  INACTIVITY_TIMEOUT: 5 * 60 * 1000, // 5 minutes

  STATS: {
    ORGANIZATIONS: 'Organizations',
    TOTAL_PAYMENTS: 'Total Payments',
    REVENUE: 'Revenue',
    SUBSCRIPTION: 'Subscription',
  },

  COLORS: {
    PRIMARY: 'bg-primary-50',
    SUCCESS: 'bg-success-50',
    ACCENT: 'bg-accent-50',
    GRAY: 'bg-gray-50',
  },
} as const;
EOF

# 2. Create Custom Hooks
echo "Step 2: Creating custom hooks..."

cat > frontend/src/hooks/useDashboardData.ts << 'EOF'
import { useAppSelector } from '../store/hooks';
import { selectCurrentUser } from '../store/slices/authSlice';
import { useGetUserOrganizationsQuery } from '../store/api/organizationApi';
import { useGetPaymentStatisticsQuery } from '../store/api/paymentApi';
import { useGetSubscriptionStatisticsQuery } from '../store/api/subscriptionApi';

export const useDashboardData = () => {
  const user = useAppSelector(selectCurrentUser);
  const { data: organizations, isLoading: orgsLoading } = useGetUserOrganizationsQuery();

  const primaryOrg = organizations?.[0];
  const organizationId = primaryOrg?.id || '';

  const {
    data: paymentStats,
    isLoading: paymentStatsLoading,
    refetch: refetchPaymentStats
  } = useGetPaymentStatisticsQuery(organizationId, {
    skip: !organizationId,
  });

  const {
    data: subscriptionStats,
    isLoading: subscriptionStatsLoading,
    refetch: refetchSubscriptionStats
  } = useGetSubscriptionStatisticsQuery(organizationId, {
    skip: !organizationId,
  });

  return {
    user,
    organizations,
    primaryOrg,
    paymentStats,
    subscriptionStats,
    isLoading: orgsLoading || paymentStatsLoading || subscriptionStatsLoading,
    refetch: async () => {
      await Promise.all([
        refetchPaymentStats(),
        refetchSubscriptionStats(),
      ]);
    },
  };
};
EOF

# 3. Extract Components
echo "Step 3: Extracting components..."

cat > frontend/src/components/dashboard/DashboardStats.tsx << 'EOF'
import React from 'react';
import StatsCard from '../ui/StatsCard';
import { InlineLoading } from '../ui/LoadingStates';

interface StatsData {
  name: string;
  value: string | number;
  icon: React.ComponentType<{ className?: string }>;
  bgColor: string;
  iconColor: string;
  trend: string;
  isLoading?: boolean;
}

interface DashboardStatsProps {
  stats: StatsData[];
}

export const DashboardStats: React.FC<DashboardStatsProps> = ({ stats }) => {
  return (
    <div className="grid grid-cols-4 gap-3">
      {stats.map((stat) => (
        <StatsCard
          key={stat.name}
          unstyled
          className="p-3 transition-shadow hover:shadow-lg"
          icon={<stat.icon className="w-3 h-3" />}
          iconWrapperClassName={`inline-flex items-center justify-center w-6 h-6 ${stat.bgColor} ${stat.iconColor} rounded-lg mb-2`}
          title={stat.name}
          titleClassName="text-xs font-medium text-gray-600 mb-1"
          value={stat.isLoading ? <InlineLoading size="md" /> : stat.value}
          valueClassName="text-base font-bold text-gray-900 mb-1"
          trend={stat.trend}
          trendClassName="text-xs font-medium text-success-600 bg-success-100 px-1 py-0.5 rounded"
        />
      ))}
    </div>
  );
};
EOF

cat > frontend/src/components/dashboard/RealTimeStatus.tsx << 'EOF'
import React from 'react';

interface RealTimeStatusProps {
  isActive: boolean;
  onRefresh: () => void;
}

export const RealTimeStatus: React.FC<RealTimeStatusProps> = ({ isActive, onRefresh }) => {
  return (
    <div className="text-right">
      <div className="text-xs text-gray-500 mb-1">
        {isActive ? (
          <span className="flex items-center">
            <div className="w-2 h-2 bg-green-500 rounded-full mr-1 animate-pulse" />
            Live updates active
          </span>
        ) : (
          <span className="text-gray-400">Updates paused</span>
        )}
      </div>
      <button
        onClick={onRefresh}
        className="text-xs text-primary-600 hover:text-primary-500 underline"
      >
        Refresh now
      </button>
    </div>
  );
};
EOF

echo "Frontend refactoring complete!"
```

### Running the Refactoring

```bash
#!/bin/bash
# run-refactoring.sh - Execute all refactoring scripts

echo "Starting comprehensive clean code refactoring..."

# Make scripts executable
chmod +x backend-refactor.sh
chmod +x frontend-refactor.sh

# Run backend refactoring
./backend-refactor.sh

# Run frontend refactoring
./frontend-refactor.sh

# Run tests to verify refactoring
echo "Running tests to verify refactoring..."

# Backend tests
cd backend
./gradlew test

# Frontend tests
cd ../frontend
npm run test
npm run typecheck

echo "Refactoring complete! Please review changes and run full test suite."
```

## Manual Refactoring Checklist

### High Priority (Immediate)

- [ ] Break down PaymentService into smaller services
- [ ] Extract Dashboard component into smaller components
- [ ] Replace magic strings with constants
- [ ] Implement parameter objects for methods with 4+ parameters

### Medium Priority (Next Sprint)

- [ ] Implement Strategy pattern for payment status handling
- [ ] Create custom hooks for data fetching
- [ ] Extract validation logic into separate validators
- [ ] Standardize error handling patterns

### Low Priority (Ongoing)

- [ ] Add comprehensive JSDoc/KDoc documentation
- [ ] Improve test coverage to 80%+
- [ ] Implement performance monitoring
- [ ] Create architectural decision records (ADRs)

## Metrics to Track

### Code Quality Metrics

- **Cyclomatic Complexity**: Target < 10 per method
- **Method Length**: Target < 20 lines
- **Class Size**: Target < 200 lines
- **Test Coverage**: Target > 80%

### Performance Metrics

- **Bundle Size**: Monitor with webpack-bundle-analyzer
- **Load Time**: Target < 3s for initial load
- **Time to Interactive**: Target < 5s
- **API Response Time**: Target < 200ms for 95th percentile
