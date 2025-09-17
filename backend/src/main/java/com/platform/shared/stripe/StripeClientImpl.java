package com.platform.shared.stripe;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Subscription;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;

@Component
public class StripeClientImpl implements StripeClient {

  private final RequestOptions requestOptions;

  public StripeClientImpl(@Value("${stripe.secret-key}") String apiKey) {
    this.requestOptions = RequestOptions.builder().setApiKey(apiKey).build();
  }

  @Override
  public String getOrCreateCustomer(UUID organizationId, String organizationName)
      throws StripeException {
    CustomerSearchParams searchParams =
        CustomerSearchParams.builder()
            .setQuery("metadata['organization_id']:'" + organizationId + "'")
            .build();

    CustomerSearchResult searchResult = Customer.search(searchParams, requestOptions);
    if (!searchResult.getData().isEmpty()) {
      return searchResult.getData().get(0).getId();
    }

    CustomerCreateParams params =
        CustomerCreateParams.builder()
            .setName(organizationName)
            .setDescription("Customer for organization: " + organizationName)
            .putMetadata("organization_id", organizationId.toString())
            .build();

    Customer customer = Customer.create(params, requestOptions);
    return customer.getId();
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
