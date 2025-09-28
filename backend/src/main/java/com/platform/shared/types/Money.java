package com.platform.shared.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Money value object with currency support and arithmetic operations. */
@Embeddable
public class Money {

  @NotNull
  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @NotBlank
  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  // JPA constructor
  protected Money() {}

  public Money(BigDecimal amount, String currency) {
    this.amount = validateAmount(amount);
    this.currency = validateCurrency(currency);
  }

  public Money(double amount, String currency) {
    this(BigDecimal.valueOf(amount), currency);
  }

  public Money(long amountInCents, String currency) {
    this(BigDecimal.valueOf(amountInCents, 2), currency);
  }

  // Factory methods
  public static Money of(BigDecimal amount, String currency) {
    return new Money(amount, currency);
  }

  public static Money of(double amount, String currency) {
    return new Money(amount, currency);
  }

  public static Money usd(BigDecimal amount) {
    return new Money(amount, "USD");
  }

  public static Money usd(double amount) {
    return new Money(amount, "USD");
  }

  public static Money zero(String currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  public static final Money ZERO = new Money(BigDecimal.ZERO, "USD");

  // Validation
  private BigDecimal validateAmount(BigDecimal amount) {
    if (amount == null) {
      throw new IllegalArgumentException("Amount cannot be null");
    }
    // Allow negative amounts for refunds and adjustments
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  private String validateCurrency(String currency) {
    if (currency == null || currency.trim().isEmpty()) {
      throw new IllegalArgumentException("Currency cannot be null or empty");
    }

    String normalizedCurrency = currency.trim().toUpperCase();

    try {
      Currency.getInstance(normalizedCurrency);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid currency code: " + currency);
    }

    return normalizedCurrency;
  }

  // Arithmetic operations
  public Money add(Money other) {
    validateSameCurrency(other);
    return new Money(this.amount.add(other.amount), this.currency);
  }

  public Money subtract(Money other) {
    validateSameCurrency(other);
    BigDecimal result = this.amount.subtract(other.amount);
    return new Money(result, this.currency);
  }

  public Money multiply(int factor) {
    if (factor < 0) {
      throw new IllegalArgumentException("Factor cannot be negative");
    }
    return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
  }

  public Money multiply(double factor) {
    if (factor < 0) {
      throw new IllegalArgumentException("Factor cannot be negative");
    }
    return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
  }

  public Money divide(int divisor) {
    if (divisor <= 0) {
      throw new IllegalArgumentException("Divisor must be positive");
    }
    return new Money(
        this.amount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP), this.currency);
  }

  public Money divide(double divisor) {
    if (divisor <= 0) {
      throw new IllegalArgumentException("Divisor must be positive");
    }
    return new Money(
        this.amount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP), this.currency);
  }

  public Money negate() {
    return new Money(this.amount.negate(), this.currency);
  }

  // Comparison operations
  public boolean isZero() {
    return amount.compareTo(BigDecimal.ZERO) == 0;
  }

  public boolean isPositive() {
    return amount.compareTo(BigDecimal.ZERO) > 0;
  }

  public boolean isNegative() {
    return amount.compareTo(BigDecimal.ZERO) < 0;
  }

  public boolean isGreaterThan(Money other) {
    validateSameCurrency(other);
    return this.amount.compareTo(other.amount) > 0;
  }

  public boolean isLessThan(Money other) {
    validateSameCurrency(other);
    return this.amount.compareTo(other.amount) < 0;
  }

  public boolean isEqualTo(Money other) {
    validateSameCurrency(other);
    return this.amount.compareTo(other.amount) == 0;
  }

  public int compareTo(BigDecimal other) {
    return this.amount.compareTo(other);
  }

  private void validateSameCurrency(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot operate on different currencies: %s and %s", this.currency, other.currency));
    }
  }

  // Conversion methods
  public long getAmountInCents() {
    return amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  public int getAmountInCentsAsInt() {
    long cents = getAmountInCents();
    if (cents > Integer.MAX_VALUE) {
      throw new ArithmeticException("Amount too large to fit in int");
    }
    return (int) cents;
  }

  // Currency methods
  public Currency getCurrencyInstance() {
    return Currency.getInstance(currency);
  }

  public String getCurrencySymbol() {
    return getCurrencyInstance().getSymbol();
  }

  // Formatting
  public String format() {
    return String.format("%s %.2f", currency, amount);
  }

  public String formatWithSymbol() {
    return String.format("%s%.2f", getCurrencySymbol(), amount);
  }

  // Getters
  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Money other)) return false;
    return Objects.equals(amount, other.amount) && Objects.equals(currency, other.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  @Override
  public String toString() {
    return format();
  }
}
