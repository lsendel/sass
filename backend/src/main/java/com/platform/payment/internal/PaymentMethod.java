package com.platform.payment.internal;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a customer's payment method, such as a credit card or bank account.
 *
 * <p>This entity stores details about a payment method, linking it to an organization and a
 * corresponding object in the Stripe payment provider. It includes encrypted fields for sensitive
 * information and supports soft deletion.
 *
 * @see com.platform.user.internal.Organization
 * @see com.platform.shared.security.EncryptedStringConverter
 */
@Entity
@Table(
    name = "payment_methods",
    indexes = {
      @Index(name = "idx_payment_methods_organization_id", columnList = "organization_id"),
      @Index(
          name = "idx_payment_methods_stripe_payment_method_id",
          columnList = "stripe_payment_method_id"),
      @Index(
          name = "idx_payment_methods_organization_default",
          columnList = "organization_id, is_default"),
      @Index(name = "idx_payment_methods_deleted_at", columnList = "deleted_at")
    })
public class PaymentMethod {

  /** The unique identifier for the payment method. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** The ID of the organization this payment method belongs to. */
  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  /** The corresponding ID from the Stripe API. */
  @Column(name = "stripe_payment_method_id", nullable = false, unique = true)
  private String stripePaymentMethodId;

  /** The type of the payment method (e.g., CARD, BANK_ACCOUNT). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type;

  /** A flag indicating whether this is the default payment method for the organization. */
  @Column(name = "is_default", nullable = false)
  private boolean isDefault = false;

  /** The last four digits of the card or bank account number (encrypted). */
  @Convert(converter = com.platform.shared.security.EncryptedStringConverter.class)
  @Column(name = "last_four")
  private String lastFour;

  /** The brand of the card (e.g., "Visa", "Mastercard") (encrypted). */
  @Convert(converter = com.platform.shared.security.EncryptedStringConverter.class)
  @Column(name = "brand")
  private String brand;

  /** The expiration month of the card. */
  @Column(name = "exp_month")
  private Integer expMonth;

  /** The expiration year of the card. */
  @Column(name = "exp_year")
  private Integer expYear;

  /** The name on the payment method (encrypted). */
  @Convert(converter = com.platform.shared.security.EncryptedStringConverter.class)
  @Column(name = "billing_name")
  private String billingName;

  /** The email associated with the billing details (encrypted). */
  @Convert(converter = com.platform.shared.security.EncryptedStringConverter.class)
  @Column(name = "billing_email")
  private String billingEmail;

  /** The billing address associated with the payment method. */
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

  /** The timestamp of when the payment method was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of the last update to the payment method. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** The timestamp of when the payment method was soft-deleted. */
  @Column(name = "deleted_at")
  private Instant deletedAt;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected PaymentMethod() {}

  /**
   * Constructs a new PaymentMethod.
   *
   * @param organizationId the ID of the organization this payment method belongs to
   * @param stripePaymentMethodId the corresponding ID from the Stripe API
   * @param type the type of the payment method (e.g., CARD)
   */
  public PaymentMethod(UUID organizationId, String stripePaymentMethodId, Type type) {
    this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
    this.stripePaymentMethodId =
        Objects.requireNonNull(stripePaymentMethodId, "Stripe payment method ID cannot be null");
    this.type = Objects.requireNonNull(type, "Type cannot be null");
  }

  /**
   * Updates the details for a card-based payment method.
   *
   * @param lastFour the last four digits of the card number
   * @param brand the card brand (e.g., "Visa")
   * @param expMonth the expiration month
   * @param expYear the expiration year
   * @throws IllegalStateException if this method is called on a non-card payment method
   */
  public void updateCardDetails(String lastFour, String brand, Integer expMonth, Integer expYear) {
    if (type != Type.CARD) {
      throw new IllegalStateException("Can only update card details for card payment methods");
    }
    this.lastFour = lastFour;
    this.brand = brand;
    this.expMonth = expMonth;
    this.expYear = expYear;
  }

  /**
   * Updates the billing details associated with this payment method.
   *
   * @param billingName the name on the payment method
   * @param billingEmail the email associated with the billing details
   * @param billingAddress the billing address
   */
  public void updateBillingDetails(
      String billingName, String billingEmail, BillingAddress billingAddress) {
    this.billingName = billingName;
    this.billingEmail = billingEmail;
    this.billingAddress = billingAddress;
  }

  /** Marks this payment method as the default for the organization. */
  public void markAsDefault() {
    this.isDefault = true;
  }

  /** Unmarks this payment method as the default. */
  public void unmarkAsDefault() {
    this.isDefault = false;
  }

  /** Marks this payment method as deleted (soft delete). */
  public void markAsDeleted() {
    this.deletedAt = Instant.now();
    this.isDefault = false;
  }

  /**
   * Checks if this payment method has been soft-deleted.
   *
   * @return {@code true} if deleted, {@code false} otherwise
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /**
   * Checks if the card payment method has expired.
   *
   * @return {@code true} if the card is expired, {@code false} otherwise. Returns {@code false} for
   *     non-card payment methods
   */
  public boolean isExpired() {
    if (type != Type.CARD || expMonth == null || expYear == null) {
      return false;
    }
    Instant now = Instant.now();
    java.time.LocalDate currentDate = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
    java.time.LocalDate expiryDate =
        java.time.LocalDate.of(expYear, expMonth, 1).plusMonths(1).minusDays(1);
    return currentDate.isAfter(expiryDate);
  }

  /**
   * Generates a user-friendly display name for the payment method.
   *
   * @return a formatted string (e.g., "Visa •••• 4242") or the type's display name
   */
  public String getDisplayName() {
    if (type == Type.CARD && brand != null && lastFour != null) {
      return String.format(
          "%s •••• %s",
          brand.substring(0, 1).toUpperCase() + brand.substring(1).toLowerCase(), lastFour);
    }
    return type.getDisplayName();
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getStripePaymentMethodId() {
    return stripePaymentMethodId;
  }

  public Type getType() {
    return type;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public String getLastFour() {
    return lastFour;
  }

  public String getBrand() {
    return brand;
  }

  public Integer getExpMonth() {
    return expMonth;
  }

  public Integer getExpYear() {
    return expYear;
  }

  public String getBillingName() {
    return billingName;
  }

  public String getBillingEmail() {
    return billingEmail;
  }

  public BillingAddress getBillingAddress() {
    return billingAddress;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  /** Enumerates the types of payment methods. */
  public enum Type {
    /** A credit or debit card. */
    CARD("Card"),
    /** A bank account. */
    BANK_ACCOUNT("Bank Account"),
    /** A SEPA Direct Debit payment method. */
    SEPA_DEBIT("SEPA Direct Debit"),
    /** An ACH Direct Debit payment method. */
    ACH_DEBIT("ACH Direct Debit");

    private final String displayName;

    Type(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /** An embeddable class representing a billing address. */
  @Embeddable
  public static class BillingAddress {
    /** The first line of the billing address. */
    @Column(name = "address_line1")
    private String addressLine1;

    /** The second line of the billing address. */
    @Column(name = "address_line2")
    private String addressLine2;

    /** The city of the billing address. */
    @Column(name = "city")
    private String city;

    /** The state or province of the billing address. */
    @Column(name = "state")
    private String state;

    /** The postal or ZIP code of the billing address. */
    @Column(name = "postal_code")
    private String postalCode;

    /** The two-letter country code of the billing address. */
    @Column(name = "country", length = 2)
    private String country;

    /**
     * Protected no-argument constructor for JPA.
     *
     * <p>This constructor is required by JPA and should not be used directly.
     */
    protected BillingAddress() {}

    /**
     * Constructs a new BillingAddress.
     *
     * @param addressLine1 the first line of the address
     * @param city the city
     * @param state the state or province
     * @param postalCode the postal or ZIP code
     * @param country the two-letter country code
     */
    public BillingAddress(
        String addressLine1, String city, String state, String postalCode, String country) {
      this.addressLine1 = addressLine1;
      this.city = city;
      this.state = state;
      this.postalCode = postalCode;
      this.country = country;
    }

    // Getters
    public String getAddressLine1() {
      return addressLine1;
    }

    public String getAddressLine2() {
      return addressLine2;
    }

    public String getCity() {
      return city;
    }

    public String getState() {
      return state;
    }

    public String getPostalCode() {
      return postalCode;
    }

    public String getCountry() {
      return country;
    }

    // Setters
    public void setAddressLine2(String addressLine2) {
      this.addressLine2 = addressLine2;
    }
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
    return "PaymentMethod{"
        + "id="
        + id
        + ", organizationId="
        + organizationId
        + ", type="
        + type
        + ", isDefault="
        + isDefault
        + ", displayName='"
        + getDisplayName()
        + '\''
        + '}';
  }
}
