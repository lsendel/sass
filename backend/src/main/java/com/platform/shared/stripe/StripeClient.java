package com.platform.shared.stripe;

import java.util.UUID;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;

public interface StripeClient {
  String getOrCreateCustomer(UUID organizationId, String organizationName) throws StripeException;

  PaymentMethod retrievePaymentMethod(String paymentMethodId) throws StripeException;

  void attachPaymentMethodToCustomer(String paymentMethodId, String customerId)
      throws StripeException;

  Subscription createSubscription(SubscriptionCreateParams params) throws StripeException;

  Subscription retrieveSubscription(String subscriptionId) throws StripeException;

  Subscription updateSubscription(String subscriptionId, SubscriptionUpdateParams params)
      throws StripeException;
}
