package com.platform.shared.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing monetary values with currency.
 * Immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    private Money(final BigDecimal amount, final Currency currency) {
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /**
     * Creates a new Money instance.
     *
     * @param amount the monetary amount
     * @param currencyCode the ISO 4217 currency code
     * @return a new Money instance
     */
    public static Money of(final BigDecimal amount, final String currencyCode) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Creates a new Money instance in USD.
     *
     * @param amount the monetary amount
     * @return a new Money instance in USD
     */
    public static Money usd(final BigDecimal amount) {
        return of(amount, "USD");
    }

    /**
     * Gets the amount.
     *
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Gets the currency.
     *
     * @return the currency
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Gets the currency code.
     *
     * @return the ISO 4217 currency code
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    /**
     * Adds another Money instance to this one.
     *
     * @param other the money to add
     * @return a new Money instance with the sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(final Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money instance from this one.
     *
     * @param other the money to subtract
     * @return a new Money instance with the difference
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money subtract(final Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplies this Money by a factor.
     *
     * @param factor the multiplication factor
     * @return a new Money instance with the product
     */
    public Money multiply(final BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    /**
     * Checks if this money is zero.
     *
     * @return true if the amount is zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if this money is positive.
     *
     * @return true if the amount is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this money is negative.
     *
     * @return true if the amount is negative
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}
