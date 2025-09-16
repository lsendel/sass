package com.platform.payment.internal;

import com.platform.shared.types.Money;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_methods_organization_id", columnList = "organization_id"),
    @Index(name = "idx_payment_methods_stripe_payment_method_id", columnList = "stripe_payment_method_id"),
    @Index(name = "idx_payment_methods_organization_default", columnList = "organization_id, is_default"),
    @Index(name = "idx_payment_methods_deleted_at", columnList = "deleted_at")
})
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "stripe_payment_method_id", nullable = false, unique = true)
    private String stripePaymentMethodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "last_four")
    private String lastFour;

    @Column(name = "brand")
    private String brand;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    @Column(name = "billing_name")
    private String billingName;

    @Column(name = "billing_email")
    private String billingEmail;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "addressLine1", column = @Column(name = "billing_address_line1")),
        @AttributeOverride(name = "addressLine2", column = @Column(name = "billing_address_line2")),
        @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
        @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "billing_country"))
    })
    private BillingAddress billingAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected PaymentMethod() {}

    public PaymentMethod(UUID organizationId, String stripePaymentMethodId, Type type) {
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.stripePaymentMethodId = Objects.requireNonNull(stripePaymentMethodId, "Stripe payment method ID cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    public void updateCardDetails(String lastFour, String brand, Integer expMonth, Integer expYear) {
        if (type != Type.CARD) {
            throw new IllegalStateException("Can only update card details for card payment methods");
        }
        this.lastFour = lastFour;
        this.brand = brand;
        this.expMonth = expMonth;
        this.expYear = expYear;
    }

    public void updateBillingDetails(String billingName, String billingEmail, BillingAddress billingAddress) {
        this.billingName = billingName;
        this.billingEmail = billingEmail;
        this.billingAddress = billingAddress;
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    public void markAsDeleted() {
        this.deletedAt = Instant.now();
        this.isDefault = false;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isExpired() {
        if (type != Type.CARD || expMonth == null || expYear == null) {
            return false;
        }

        Instant now = Instant.now();
        java.time.LocalDate currentDate = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        java.time.LocalDate expiryDate = java.time.LocalDate.of(expYear, expMonth, 1)
            .plusMonths(1)
            .minusDays(1);

        return currentDate.isAfter(expiryDate);
    }

    public String getDisplayName() {
        if (type == Type.CARD && brand != null && lastFour != null) {
            return String.format("%s •••• %s",
                brand.substring(0, 1).toUpperCase() + brand.substring(1).toLowerCase(),
                lastFour);
        }
        return type.getDisplayName();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getStripePaymentMethodId() { return stripePaymentMethodId; }
    public Type getType() { return type; }
    public boolean isDefault() { return isDefault; }
    public String getLastFour() { return lastFour; }
    public String getBrand() { return brand; }
    public Integer getExpMonth() { return expMonth; }
    public Integer getExpYear() { return expYear; }
    public String getBillingName() { return billingName; }
    public String getBillingEmail() { return billingEmail; }
    public BillingAddress getBillingAddress() { return billingAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    public enum Type {
        CARD("Card"),
        BANK_ACCOUNT("Bank Account"),
        SEPA_DEBIT("SEPA Direct Debit"),
        ACH_DEBIT("ACH Direct Debit");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Embeddable
    public static class BillingAddress {
        @Column(name = "address_line1")
        private String addressLine1;

        @Column(name = "address_line2")
        private String addressLine2;

        @Column(name = "city")
        private String city;

        @Column(name = "state")
        private String state;

        @Column(name = "postal_code")
        private String postalCode;

        @Column(name = "country", length = 2)
        private String country;

        protected BillingAddress() {}

        public BillingAddress(String addressLine1, String city, String state, String postalCode, String country) {
            this.addressLine1 = addressLine1;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
            this.country = country;
        }

        // Getters
        public String getAddressLine1() { return addressLine1; }
        public String getAddressLine2() { return addressLine2; }
        public String getCity() { return city; }
        public String getState() { return state; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }

        // Setters
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentMethod that = (PaymentMethod) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
               "id=" + id +
               ", organizationId=" + organizationId +
               ", type=" + type +
               ", isDefault=" + isDefault +
               ", displayName='" + getDisplayName() + '\'' +
               '}';
    }
}