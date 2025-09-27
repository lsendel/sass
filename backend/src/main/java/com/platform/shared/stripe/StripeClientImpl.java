package com.platform.shared.stripe;

import com.platform.user.internal.Organization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;

@Component
public class StripeClientImpl implements StripeClient {

  private final RequestOptions requestOptions;
  private final StripeCustomerService stripeCustomerService;

  public StripeClientImpl(
      @Value("${stripe.secret-key}") String apiKey,
      StripeCustomerService stripeCustomerService) {
    this.requestOptions = RequestOptions.builder().setApiKey(apiKey).build();
    this.stripeCustomerService = stripeCustomerService;
  }

  @Override
  public String getOrCreateCustomer(Organization organization) throws StripeException {
    return stripeCustomerService.getOrCreateCustomer(organization);
  }

  @Override
  public PaymentMethod retrievePaymentMethod(String paymentMethodId) throws StripeException {
    return PaymentMethod.retrieve(paymentMethodId, requestOptions);
  }

  @Override
  public void attachPaymentMethodToCustomer(String paymentMethodId, String customerId)
      throws StripeException {
    PaymentMethod paymentMethod = retrievePaymentMethod(paymentMethodId);
    paymentMethod.attach(
        PaymentMethodAttachParams.builder().setCustomer(customerId).build(), requestOptions);
  }

  @Override
  public Subscription createSubscription(SubscriptionCreateParams params) throws StripeException {
    return Subscription.create(params, requestOptions);
  }

  @Override
  public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
    return Subscription.retrieve(subscriptionId, requestOptions);
  }

  @Override
  public Subscription updateSubscription(String subscriptionId, SubscriptionUpdateParams params)
      throws StripeException {
    return Subscription.retrieve(subscriptionId, requestOptions).update(params, requestOptions);
  }
}
