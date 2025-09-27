package com.platform.testutil;

import java.math.BigDecimal;
import java.util.UUID;

import com.platform.shared.types.Money;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.Subscription;
import com.platform.user.internal.Organization;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRole;

/**
 * Test data builder utility to create test entities without using reflection.
 * Provides fluent API for building test data with proper defaults.
 */
public class TestDataBuilder {

    public static class OrganizationBuilder {
        private String name = "Test Organization";
        private String slug = "test-org";
        private UUID parentId = null;

        public OrganizationBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public OrganizationBuilder withSlug(String slug) {
            this.slug = slug;
            return this;
        }

        public OrganizationBuilder withParentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }

        public Organization build() {
            return new Organization(name, slug, parentId);
        }
    }

    public static class UserBuilder {
        private String email = "test@example.com";
        private String name = "Test User";
        private Organization organization;
        private UserRole role = UserRole.USER;
        private boolean active = true;

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder withOrganization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public UserBuilder withRole(UserRole role) {
            this.role = role;
            return this;
        }

        public UserBuilder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public User build() {
            User user = new User(email, name);
            if (organization != null) {
                user.setOrganization(organization);
            }
            user.setRole(role);
            user.setActive(active);
            return user;
        }
    }

    public static class PlanBuilder {
        private String name = "Test Plan";
        private String stripePriceId = "price_test_123";
        private Money price = new Money(new BigDecimal("29.99"), "USD");
        private Plan.BillingInterval interval = Plan.BillingInterval.MONTH;
        private String slug = "test-plan";
        private boolean active = true;
        private String description = "Test plan description";
        private int trialDays = 0;

        public PlanBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public PlanBuilder withStripePriceId(String stripePriceId) {
            this.stripePriceId = stripePriceId;
            return this;
        }

        public PlanBuilder withPrice(BigDecimal amount, String currency) {
            this.price = new Money(amount, currency);
            return this;
        }

        public PlanBuilder withInterval(Plan.BillingInterval interval) {
            this.interval = interval;
            return this;
        }

        public PlanBuilder withSlug(String slug) {
            this.slug = slug;
            return this;
        }

        public PlanBuilder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public PlanBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PlanBuilder withTrialDays(int trialDays) {
            this.trialDays = trialDays;
            return this;
        }

        public Plan build() {
            Plan plan = new Plan(name, stripePriceId, price, interval);

            // Use reflection safely for test setup only
            try {
                setField(plan, "slug", slug);
                setField(plan, "active", active);
                setField(plan, "description", description);
                setField(plan, "trialDays", trialDays);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set plan fields", e);
            }

            return plan;
        }
    }

    public static class SubscriptionBuilder {
        private UUID organizationId;
        private UUID planId;
        private Subscription.Status status = Subscription.Status.ACTIVE;
        private java.time.LocalDate currentPeriodStart = java.time.LocalDate.now();
        private java.time.LocalDate currentPeriodEnd = java.time.LocalDate.now().plusMonths(1);

        public SubscriptionBuilder withOrganizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public SubscriptionBuilder withPlanId(UUID planId) {
            this.planId = planId;
            return this;
        }

        public SubscriptionBuilder withStatus(Subscription.Status status) {
            this.status = status;
            return this;
        }

        public SubscriptionBuilder withCurrentPeriod(java.time.LocalDate start, java.time.LocalDate end) {
            this.currentPeriodStart = start;
            this.currentPeriodEnd = end;
            return this;
        }

        public Subscription build() {
            return Subscription.createActive(organizationId, planId, currentPeriodStart, currentPeriodEnd);
        }
    }

    // Factory methods
    public static OrganizationBuilder organization() {
        return new OrganizationBuilder();
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static PlanBuilder plan() {
        return new PlanBuilder();
    }

    public static SubscriptionBuilder subscription() {
        return new SubscriptionBuilder();
    }

    // Helper method for reflection (kept internal to builder)
    private static void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName + " on " + obj.getClass().getSimpleName(), e);
        }
    }

    // Additional utility methods for common test scenarios
    public static Organization createTestOrganization(String name, String slug) {
        return organization()
                .withName(name)
                .withSlug(slug)
                .build();
    }

    public static User createTestUser(String email, String name, Organization org) {
        return user()
                .withEmail(email)
                .withName(name)
                .withOrganization(org)
                .build();
    }

    public static User createTestAdmin(String email, String name, Organization org) {
        return user()
                .withEmail(email)
                .withName(name)
                .withOrganization(org)
                .withRole(UserRole.ADMIN)
                .build();
    }

    public static Plan createTestPlan(String name, String slug, BigDecimal amount, String currency) {
        return plan()
                .withName(name)
                .withSlug(slug)
                .withPrice(amount, currency)
                .withActive(true)
                .build();
    }

    public static Plan createTestPlanWithTrial(String name, String slug, BigDecimal amount,
                                              String currency, int trialDays) {
        return plan()
                .withName(name)
                .withSlug(slug)
                .withPrice(amount, currency)
                .withTrialDays(trialDays)
                .withActive(true)
                .build();
    }
}