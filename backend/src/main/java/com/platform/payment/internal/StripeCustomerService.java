package com.platform.payment.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.platform.user.internal.Organization;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;

/**
 * Service for managing Stripe customer operations.
 * Separated from main PaymentService for better single responsibility principle.
 */
@Service
public class StripeCustomerService {

    private static final Logger logger = LoggerFactory.getLogger(StripeCustomerService.class);

    /**
     * Gets an existing Stripe customer for the organization or creates a new one.
     *
     * @param organization the organization to get/create a customer for
     * @return the Stripe customer ID
     * @throws StripeException if there's an error communicating with Stripe
     */
    public String getOrCreateCustomer(Organization organization) throws StripeException {
        String existingCustomerId = findExistingCustomer(organization);

        if (existingCustomerId != null) {
            logger.debug("Found existing Stripe customer: {} for organization: {}",
                        existingCustomerId, organization.getId());
            return existingCustomerId;
        }

        return createNewCustomer(organization);
    }

    /**
     * Searches for an existing Stripe customer by organization ID.
     *
     * @param organization the organization to search for
     * @return the customer ID if found, null otherwise
     * @throws StripeException if there's an error communicating with Stripe
     */
    private String findExistingCustomer(Organization organization) throws StripeException {
        CustomerSearchParams searchParams = CustomerSearchParams.builder()
                .setQuery("metadata['organization_id']:'" + organization.getId() + "'")
                .build();

        CustomerSearchResult searchResult = Customer.search(searchParams);

        if (!searchResult.getData().isEmpty()) {
            return searchResult.getData().get(0).getId();
        }

        return null;
    }

    /**
     * Creates a new Stripe customer for the organization.
     *
     * @param organization the organization to create a customer for
     * @return the new customer ID
     * @throws StripeException if there's an error communicating with Stripe
     */
    private String createNewCustomer(Organization organization) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(organization.getName())
                .setDescription("Customer for organization: " + organization.getName())
                .putMetadata("organization_id", organization.getId().toString())
                .build();

        Customer customer = Customer.create(params);

        logger.info("Created new Stripe customer: {} for organization: {}",
                   customer.getId(), organization.getId());

        return customer.getId();
    }

    /**
     * Updates an existing Stripe customer with new organization information.
     *
     * @param customerId the Stripe customer ID to update
     * @param organization the organization with updated information
     * @throws StripeException if there's an error communicating with Stripe
     */
    public void updateCustomer(String customerId, Organization organization) throws StripeException {
        Customer customer = Customer.retrieve(customerId);

        Customer updatedCustomer = customer.update(
                CustomerCreateParams.builder()
                        .setName(organization.getName())
                        .setDescription("Customer for organization: " + organization.getName())
                        .build());

        logger.info("Updated Stripe customer: {} for organization: {}",
                   updatedCustomer.getId(), organization.getId());
    }

    /**
     * Checks if a customer exists in Stripe.
     *
     * @param customerId the customer ID to check
     * @return true if the customer exists, false otherwise
     */
    public boolean customerExists(String customerId) {
        try {
            Customer.retrieve(customerId);
            return true;
        } catch (StripeException e) {
            logger.debug("Customer {} does not exist: {}", customerId, e.getMessage());
            return false;
        }
    }
}