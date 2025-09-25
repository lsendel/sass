package com.platform.shared.events;

/**
 * Event published when a payment is successful.
 */
public class PaymentSuccessEvent extends DomainEvent {
    
    private final String paymentId;
    private final String organizationId;
    private final String userId;
    private final String amount;
    private final String currency;
    
    public PaymentSuccessEvent(Object source, String paymentId, String organizationId, String userId, String amount, String currency) {
        super(source, "PAYMENT_SUCCESS", "PAYMENT");
        this.paymentId = paymentId;
        this.organizationId = organizationId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getAmount() {
        return amount;
    }
    
    public String getCurrency() {
        return currency;
    }
}