package com.platform.subscription.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.audit.internal.AuditService;
import com.platform.payment.internal.Invoice;
import com.platform.shared.security.TenantContext;
import com.platform.shared.stripe.StripeClient;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;

@Service
@Transactional
public class SubscriptionService {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final InvoiceRepository invoiceRepository;
  private final OrganizationRepository organizationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final AuditService auditService;
  private final StripeClient stripeClient;

  public SubscriptionService(
      SubscriptionRepository subscriptionRepository,
      PlanRepository planRepository,
      InvoiceRepository invoiceRepository,
      OrganizationRepository organizationRepository,
      ApplicationEventPublisher eventPublisher,
      AuditService auditService,
      StripeClient stripeClient) {
    this.subscriptionRepository = subscriptionRepository;
    this.planRepository = planRepository;
    this.invoiceRepository = invoiceRepository;
    this.organizationRepository = organizationRepository;
    this.eventPublisher = eventPublisher;
    this.auditService = auditService;
    this.stripeClient = stripeClient;
  }

  public Subscription createSubscription(
      UUID organizationId, UUID planId, String paymentMethodId, boolean trialEligible)
      throws StripeException {
    validateOrganizationAccess(organizationId);

    // Audit subscription creation attempt
    auditService.logEvent(
        "SUBSCRIPTION_CREATION_STARTED",
        "SUBSCRIPTION",
        "PENDING",
        "CREATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "plan_id", planId.toString(),
            "payment_method_id", paymentMethodId != null ? paymentMethodId : "none",
            "trial_eligible", String.valueOf(trialEligible)),
        null,
        "system",
        "SubscriptionService",
        Map.of("action", "subscription_creation_started"));

    // Check if organization already has an active subscription
    Optional<Subscription> existingSubscription =
        subscriptionRepository.findByOrganizationId(organizationId);
    if (existingSubscription.isPresent() && existingSubscription.get().isActive()) {
      // Audit failed attempt
      auditService.logEvent(
          "SUBSCRIPTION_CREATION_FAILED",
          "SUBSCRIPTION",
          "REJECTED",
          "CREATE",
          Map.of(
              "organization_id", organizationId.toString(),
              "reason", "Organization already has active subscription",
              "existing_subscription_id", existingSubscription.get().getId().toString()),
          null,
          "system",
          "SubscriptionService",
          Map.of("error", "duplicate_subscription"));

      throw new IllegalStateException("Organization already has an active subscription");
    }

    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));

    Plan plan =
        planRepository
            .findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

    if (!plan.isActive()) {
      // Audit failed attempt
      auditService.logEvent(
          "SUBSCRIPTION_CREATION_FAILED",
          "SUBSCRIPTION",
          "REJECTED",
          "CREATE",
          Map.of(
              "organization_id", organizationId.toString(),
              "plan_id", planId.toString(),
              "reason", "Plan is not active"),
          null,
          "system",
          "SubscriptionService",
          Map.of("error", "inactive_plan"));

      throw new IllegalArgumentException("Plan is not active: " + planId);
    }

    // Audit plan selection
    auditService.logEvent(
        "SUBSCRIPTION_PLAN_SELECTED",
        "SUBSCRIPTION",
        planId.toString(),
        "SELECT",
        Map.of(
            "organization_id", organizationId.toString(),
            "plan_id", planId.toString(),
            "plan_name", plan.getName(),
            "plan_price", plan.getPrice().toString()),
        null,
        "system",
        "SubscriptionService",
        null);

    // Create or get Stripe customer
    String customerId =
        stripeClient.getOrCreateCustomer(organization.getId(), organization.getName());

    // Audit Stripe customer creation/retrieval
    auditService.logEvent(
        "STRIPE_CUSTOMER_ASSOCIATED",
        "SUBSCRIPTION",
        customerId,
        "ASSOCIATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "stripe_customer_id", customerId,
            "organization_name", organization.getName()),
        null,
        "system",
        "SubscriptionService",
        null);

    // Attach payment method to customer if provided
    if (paymentMethodId != null) {
      stripeClient.attachPaymentMethodToCustomer(paymentMethodId, customerId);

      // Audit payment method attachment
      auditService.logEvent(
          "PAYMENT_METHOD_ATTACHED",
          "SUBSCRIPTION",
          paymentMethodId,
          "ATTACH",
          Map.of(
              "organization_id", organizationId.toString(),
              "payment_method_id", paymentMethodId,
              "stripe_customer_id", customerId),
          null,
          "system",
          "SubscriptionService",
          null);
    }

    // Create Stripe subscription
    SubscriptionCreateParams.Builder paramsBuilder =
        SubscriptionCreateParams.builder()
            .setCustomer(customerId)
            .addItem(
                SubscriptionCreateParams.Item.builder().setPrice(plan.getStripePriceId()).build())
            .putMetadata("organization_id", organizationId.toString())
            .putMetadata("plan_id", planId.toString());

    if (paymentMethodId != null) {
      paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
    }

    // Add trial if eligible and plan supports it
    if (trialEligible && plan.getTrialDays() != null && plan.getTrialDays() > 0) {
      paramsBuilder.setTrialPeriodDays(plan.getTrialDays().longValue());

      // Audit trial period assignment
      auditService.logEvent(
          "SUBSCRIPTION_TRIAL_ASSIGNED",
          "SUBSCRIPTION",
          "TRIAL",
          "ASSIGN",
          Map.of(
              "organization_id", organizationId.toString(),
              "trial_days", plan.getTrialDays().toString(),
              "plan_id", planId.toString()),
          null,
          "system",
          "SubscriptionService",
          null);
    }

    com.stripe.model.Subscription stripeSubscription =
        stripeClient.createSubscription(paramsBuilder.build());

    // Audit successful Stripe subscription creation
    auditService.logEvent(
        "STRIPE_SUBSCRIPTION_CREATED",
        "SUBSCRIPTION",
        stripeSubscription.getId(),
        "CREATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "stripe_subscription_id", stripeSubscription.getId(),
            "stripe_status", stripeSubscription.getStatus(),
            "plan_id", planId.toString(),
            "customer_id", customerId),
        null,
        "system",
        "SubscriptionService",
        null);

    // Create our subscription record
    Subscription subscription =
        new Subscription(
            organizationId, planId, Subscription.Status.fromString(stripeSubscription.getStatus()));
    subscription.updateStripeSubscriptionId(stripeSubscription.getId());

    updateSubscriptionFromStripe(subscription, stripeSubscription);
    Subscription savedSubscription = subscriptionRepository.save(subscription);

    // Audit successful subscription creation
    auditService.logEvent(
        "SUBSCRIPTION_CREATED",
        "SUBSCRIPTION",
        savedSubscription.getId().toString(),
        "CREATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", savedSubscription.getId().toString(),
            "stripe_subscription_id", stripeSubscription.getId(),
            "plan_id", planId.toString(),
            "status", savedSubscription.getStatus().toString(),
            "trial_eligible", String.valueOf(trialEligible)),
        Map.of(
            "subscription_id", savedSubscription.getId().toString(),
            "status", savedSubscription.getStatus().toString(),
            "created_at", savedSubscription.getCreatedAt().toString()),
        "system",
        "SubscriptionService",
        Map.of("success", "subscription_created"));

    logger.info(
        "Created subscription: {} for organization: {} with plan: {}",
        stripeSubscription.getId(),
        organizationId,
        planId);

    return savedSubscription;
  }

  public Subscription changeSubscriptionPlan(
      UUID organizationId, UUID newPlanId, boolean prorationBehavior) throws StripeException {
    validateOrganizationAccess(organizationId);

    Subscription subscription =
        subscriptionRepository
            .findByOrganizationId(organizationId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No active subscription found for organization: " + organizationId));

    UUID oldPlanId = subscription.getPlanId();

    // Audit plan change attempt
    auditService.logEvent(
        "SUBSCRIPTION_PLAN_CHANGE_STARTED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "UPDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "old_plan_id", oldPlanId.toString(),
            "new_plan_id", newPlanId.toString(),
            "proration_enabled", String.valueOf(prorationBehavior)),
        null,
        "system",
        "SubscriptionService",
        Map.of("action", "plan_change_started"));

    if (!subscription.isActive()) {
      // Audit failed attempt
      auditService.logEvent(
          "SUBSCRIPTION_PLAN_CHANGE_FAILED",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "UPDATE",
          Map.of(
              "organization_id", organizationId.toString(),
              "subscription_id", subscription.getId().toString(),
              "reason", "Subscription is not active",
              "current_status", subscription.getStatus().toString()),
          null,
          "system",
          "SubscriptionService",
          Map.of("error", "inactive_subscription"));

      throw new IllegalStateException("Subscription is not active: " + subscription.getId());
    }

    Plan newPlan =
        planRepository
            .findById(newPlanId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + newPlanId));

    if (!newPlan.isActive()) {
      // Audit failed attempt
      auditService.logEvent(
          "SUBSCRIPTION_PLAN_CHANGE_FAILED",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "UPDATE",
          Map.of(
              "organization_id", organizationId.toString(),
              "subscription_id", subscription.getId().toString(),
              "new_plan_id", newPlanId.toString(),
              "reason", "Target plan is not active"),
          null,
          "system",
          "SubscriptionService",
          Map.of("error", "inactive_target_plan"));

      throw new IllegalArgumentException("Plan is not active: " + newPlanId);
    }

    // Audit target plan validation
    auditService.logEvent(
        "SUBSCRIPTION_NEW_PLAN_VALIDATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "VALIDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "new_plan_id", newPlanId.toString(),
            "new_plan_name", newPlan.getName(),
            "new_plan_price", newPlan.getPrice().toString()),
        null,
        "system",
        "SubscriptionService",
        null);

    // Update Stripe subscription
    com.stripe.model.Subscription stripeSubscription =
        stripeClient.retrieveSubscription(subscription.getStripeSubscriptionId());
    SubscriptionItem subscriptionItem = stripeSubscription.getItems().getData().get(0);

    SubscriptionUpdateParams params =
        SubscriptionUpdateParams.builder()
            .addItem(
                SubscriptionUpdateParams.Item.builder()
                    .setId(subscriptionItem.getId())
                    .setPrice(newPlan.getStripePriceId())
                    .build())
            .setProrationBehavior(
                prorationBehavior
                    ? SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS
                    : SubscriptionUpdateParams.ProrationBehavior.NONE)
            .putMetadata("plan_id", newPlanId.toString())
            .build();

    // Audit Stripe update initiation
    auditService.logEvent(
        "STRIPE_SUBSCRIPTION_UPDATE_INITIATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "UPDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "stripe_subscription_id", subscription.getStripeSubscriptionId(),
            "old_plan_id", oldPlanId.toString(),
            "new_plan_id", newPlanId.toString(),
            "proration_behavior", prorationBehavior ? "CREATE_PRORATIONS" : "NONE"),
        null,
        "system",
        "SubscriptionService",
        null);

    com.stripe.model.Subscription updatedStripeSubscription =
        stripeClient.updateSubscription(subscription.getStripeSubscriptionId(), params);

    // Audit successful Stripe update
    auditService.logEvent(
        "STRIPE_SUBSCRIPTION_UPDATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "UPDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "stripe_subscription_id", subscription.getStripeSubscriptionId(),
            "new_stripe_status", updatedStripeSubscription.getStatus(),
            "old_plan_id", oldPlanId.toString(),
            "new_plan_id", newPlanId.toString()),
        null,
        "system",
        "SubscriptionService",
        null);

    // Update our subscription record
    subscription.changePlan(newPlanId);
    updateSubscriptionFromStripe(subscription, updatedStripeSubscription);
    Subscription savedSubscription = subscriptionRepository.save(subscription);

    // Audit successful plan change
    auditService.logEvent(
        "SUBSCRIPTION_PLAN_CHANGED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "UPDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "old_plan_id", oldPlanId.toString(),
            "new_plan_id", newPlanId.toString(),
            "proration_enabled", String.valueOf(prorationBehavior),
            "new_status", savedSubscription.getStatus().toString()),
        Map.of(
            "subscription_id", savedSubscription.getId().toString(),
            "new_plan_id", newPlanId.toString(),
            "status", savedSubscription.getStatus().toString(),
            "updated_at", savedSubscription.getUpdatedAt().toString()),
        "system",
        "SubscriptionService",
        Map.of("success", "plan_changed"));

    logger.info(
        "Changed subscription plan: {} from {} to {}", subscription.getId(), oldPlanId, newPlanId);

    return savedSubscription;
  }

  public Subscription cancelSubscription(UUID organizationId, boolean immediate, Instant cancelAt)
      throws StripeException {
    validateOrganizationAccess(organizationId);

    Subscription subscription =
        subscriptionRepository
            .findByOrganizationId(organizationId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No active subscription found for organization: " + organizationId));

    // Audit cancellation attempt
    auditService.logEvent(
        "SUBSCRIPTION_CANCELLATION_STARTED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "CANCEL",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "immediate_cancellation", String.valueOf(immediate),
            "scheduled_cancel_at", cancelAt != null ? cancelAt.toString() : "end_of_period",
            "current_status", subscription.getStatus().toString()),
        null,
        "system",
        "SubscriptionService",
        Map.of("action", "cancellation_started"));

    if (subscription.getStatus() == Subscription.Status.CANCELED) {
      // Audit failed attempt
      auditService.logEvent(
          "SUBSCRIPTION_CANCELLATION_FAILED",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "CANCEL",
          Map.of(
              "organization_id", organizationId.toString(),
              "subscription_id", subscription.getId().toString(),
              "reason", "Subscription already canceled"),
          null,
          "system",
          "SubscriptionService",
          Map.of("error", "already_canceled"));

      throw new IllegalStateException("Subscription is already canceled: " + subscription.getId());
    }

    com.stripe.model.Subscription stripeSubscription =
        stripeClient.retrieveSubscription(subscription.getStripeSubscriptionId());

    if (immediate) {
      // Audit immediate cancellation
      auditService.logEvent(
          "SUBSCRIPTION_IMMEDIATE_CANCELLATION",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "CANCEL",
          Map.of(
              "organization_id", organizationId.toString(),
              "subscription_id", subscription.getId().toString(),
              "stripe_subscription_id", subscription.getStripeSubscriptionId(),
              "cancellation_type", "immediate"),
          null,
          "system",
          "SubscriptionService",
          null);

      // Cancel immediately
      stripeSubscription.cancel();
      subscription.cancel();

      // Audit Stripe immediate cancellation
      auditService.logEvent(
          "STRIPE_SUBSCRIPTION_CANCELED_IMMEDIATELY",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "CANCEL",
          Map.of(
              "organization_id", organizationId.toString(),
              "stripe_subscription_id", subscription.getStripeSubscriptionId(),
              "cancellation_timestamp", Instant.now().toString()),
          null,
          "system",
          "SubscriptionService",
          null);

    } else {
      Instant effectiveCancelAt =
          cancelAt != null
              ? cancelAt
              : subscription
                  .getCurrentPeriodEnd()
                  .atStartOfDay(java.time.ZoneOffset.UTC)
                  .toInstant();

      // Audit scheduled cancellation
      auditService.logEvent(
          "SUBSCRIPTION_SCHEDULED_CANCELLATION",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "SCHEDULE_CANCEL",
          Map.of(
              "organization_id", organizationId.toString(),
              "subscription_id", subscription.getId().toString(),
              "stripe_subscription_id", subscription.getStripeSubscriptionId(),
              "cancellation_type", "scheduled",
              "cancel_at", effectiveCancelAt.toString(),
              "current_period_end", subscription.getCurrentPeriodEnd().toString()),
          null,
          "system",
          "SubscriptionService",
          null);

      // Schedule cancellation
      SubscriptionUpdateParams params =
          SubscriptionUpdateParams.builder()
              .setCancelAt(effectiveCancelAt.getEpochSecond())
              .build();
      stripeSubscription.update(params);
      subscription.scheduleCancellation(effectiveCancelAt);

      // Audit Stripe scheduled cancellation
      auditService.logEvent(
          "STRIPE_SUBSCRIPTION_CANCELLATION_SCHEDULED",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "SCHEDULE_CANCEL",
          Map.of(
              "organization_id", organizationId.toString(),
              "stripe_subscription_id", subscription.getStripeSubscriptionId(),
              "scheduled_cancel_at", effectiveCancelAt.toString()),
          null,
          "system",
          "SubscriptionService",
          null);
    }

    Subscription savedSubscription = subscriptionRepository.save(subscription);

    // Audit successful cancellation
    auditService.logEvent(
        "SUBSCRIPTION_CANCELED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "CANCEL",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "cancellation_type", immediate ? "immediate" : "scheduled",
            "final_status", savedSubscription.getStatus().toString(),
            "cancel_at",
                savedSubscription.getCancelAt() != null
                    ? savedSubscription.getCancelAt().toString()
                    : "immediate"),
        Map.of(
            "subscription_id", savedSubscription.getId().toString(),
            "status", savedSubscription.getStatus().toString(),
            "cancel_at",
                savedSubscription.getCancelAt() != null
                    ? savedSubscription.getCancelAt().toString()
                    : "immediate",
            "updated_at", savedSubscription.getUpdatedAt().toString()),
        "system",
        "SubscriptionService",
        Map.of("success", "subscription_canceled"));

    logger.info(
        "Canceled subscription: {} for organization: {}", subscription.getId(), organizationId);

    return savedSubscription;
  }

  public Subscription reactivateSubscription(UUID organizationId) throws StripeException {
    validateOrganizationAccess(organizationId);

    Subscription subscription =
        subscriptionRepository
            .findByOrganizationId(organizationId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No subscription found for organization: " + organizationId));

    // Audit reactivation attempt
    auditService.logEvent(
        "SUBSCRIPTION_REACTIVATION_STARTED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "REACTIVATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "current_status", subscription.getStatus().toString(),
            "cancel_at",
                subscription.getCancelAt() != null
                    ? subscription.getCancelAt().toString()
                    : "none"),
        null,
        "system",
        "SubscriptionService",
        Map.of("action", "reactivation_started"));

    if (subscription.getStatus() != Subscription.Status.CANCELED
        && subscription.getCancelAt() == null) {
      // Audit failed attempt
      auditService.logEvent(
          "SUBSCRIPTION_REACTIVATION_FAILED",
          "SUBSCRIPTION",
          subscription.getId().toString(),
          "REACTIVATE",
          Map.of(
              "organization_id", organizationId.toString(),
              "subscription_id", subscription.getId().toString(),
              "reason", "Subscription is not canceled or scheduled for cancellation",
              "current_status", subscription.getStatus().toString()),
          null,
          "system",
          "SubscriptionService",
          Map.of("error", "not_eligible_for_reactivation"));

      throw new IllegalStateException(
          "Subscription is not canceled or scheduled for cancellation: " + subscription.getId());
    }

    // Audit reactivation validation
    auditService.logEvent(
        "SUBSCRIPTION_REACTIVATION_VALIDATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "VALIDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "current_status", subscription.getStatus().toString(),
            "stripe_subscription_id", subscription.getStripeSubscriptionId()),
        null,
        "system",
        "SubscriptionService",
        null);

    // Remove cancellation from Stripe
    com.stripe.model.Subscription stripeSubscription =
        com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());
    SubscriptionUpdateParams params =
        SubscriptionUpdateParams.builder().putExtraParam("cancel_at", "").build();

    // Audit Stripe reactivation initiation
    auditService.logEvent(
        "STRIPE_SUBSCRIPTION_REACTIVATION_INITIATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "REACTIVATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "stripe_subscription_id", subscription.getStripeSubscriptionId(),
            "action", "remove_cancellation_schedule"),
        null,
        "system",
        "SubscriptionService",
        null);

    com.stripe.model.Subscription updatedStripeSubscription =
        stripeClient.updateSubscription(subscription.getStripeSubscriptionId(), params);

    // Audit successful Stripe reactivation
    auditService.logEvent(
        "STRIPE_SUBSCRIPTION_REACTIVATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "REACTIVATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "stripe_subscription_id", subscription.getStripeSubscriptionId(),
            "new_stripe_status", updatedStripeSubscription.getStatus(),
            "cancellation_removed", "true"),
        null,
        "system",
        "SubscriptionService",
        null);

    // Update our subscription record
    subscription.reactivate();
    updateSubscriptionFromStripe(subscription, updatedStripeSubscription);
    Subscription savedSubscription = subscriptionRepository.save(subscription);

    // Audit successful reactivation
    auditService.logEvent(
        "SUBSCRIPTION_REACTIVATED",
        "SUBSCRIPTION",
        subscription.getId().toString(),
        "REACTIVATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "subscription_id", subscription.getId().toString(),
            "previous_status",
                subscription.getStatus() == Subscription.Status.CANCELED
                    ? "CANCELED"
                    : "SCHEDULED_FOR_CANCELLATION",
            "new_status", savedSubscription.getStatus().toString(),
            "cancel_at_removed", "true"),
        Map.of(
            "subscription_id", savedSubscription.getId().toString(),
            "status", savedSubscription.getStatus().toString(),
            "cancel_at", "null",
            "updated_at", savedSubscription.getUpdatedAt().toString()),
        "system",
        "SubscriptionService",
        Map.of("success", "subscription_reactivated"));

    logger.info(
        "Reactivated subscription: {} for organization: {}", subscription.getId(), organizationId);
    return savedSubscription;
  }

  @Transactional(readOnly = true)
  public Optional<Subscription> findByOrganizationId(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return subscriptionRepository.findByOrganizationId(organizationId);
  }

  @Transactional(readOnly = true)
  public List<Plan> getAvailablePlans() {
    return planRepository.findByActiveOrderByDisplayOrderAsc(true);
  }

  @Transactional(readOnly = true)
  public List<Plan> getPlansByInterval(Plan.BillingInterval interval) {
    return planRepository.findActivePlansByInterval(interval);
  }

  @Transactional(readOnly = true)
  public Optional<Plan> findPlanBySlug(String slug) {
    return planRepository.findBySlugAndActive(slug, true);
  }

  @Transactional(readOnly = true)
  public List<Invoice> getOrganizationInvoices(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
  }

  @Transactional(readOnly = true)
  public SubscriptionStatistics getSubscriptionStatistics(UUID organizationId) {
    validateOrganizationAccess(organizationId);

    Optional<Subscription> subscription =
        subscriptionRepository.findByOrganizationId(organizationId);
    if (subscription.isEmpty()) {
      return new SubscriptionStatistics(null, 0, Money.ZERO, Money.ZERO);
    }

    long totalInvoices = invoiceRepository.countPaidInvoicesByOrganization(organizationId);
    Long totalAmountCents = invoiceRepository.sumPaidInvoiceAmountsByOrganization(organizationId);
    Money totalAmount =
        totalAmountCents != null
            ? new Money(java.math.BigDecimal.valueOf(totalAmountCents, 2), "USD")
            : Money.ZERO;

    Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
    Long recentAmountCents =
        invoiceRepository.sumPaidInvoiceAmountsByOrganizationAndDateRange(
            organizationId, thirtyDaysAgo, Instant.now());
    Money recentAmount =
        recentAmountCents != null
            ? new Money(java.math.BigDecimal.valueOf(recentAmountCents, 2), "USD")
            : Money.ZERO;

    return new SubscriptionStatistics(
        subscription.get().getStatus(), totalInvoices, totalAmount, recentAmount);
  }

  public void processWebhookEvent(
      String stripeEventId, String eventType, Map<String, Object> eventData) {
    logger.info("Processing Stripe subscription webhook: {} of type: {}", stripeEventId, eventType);

    switch (eventType) {
      case "customer.subscription.created" -> handleSubscriptionCreated(eventData);
      case "customer.subscription.updated" -> handleSubscriptionUpdated(eventData);
      case "customer.subscription.deleted" -> handleSubscriptionDeleted(eventData);
      case "invoice.created" -> handleInvoiceCreated(eventData);
      case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(eventData);
      case "invoice.payment_failed" -> handleInvoicePaymentFailed(eventData);
      case "invoice.finalized" -> handleInvoiceFinalized(eventData);
      default -> logger.debug("Unhandled subscription webhook event type: {}", eventType);
    }
  }

  // Helper methods

  // Customer creation is handled by StripeClient

  private void updateSubscriptionFromStripe(
      Subscription subscription, com.stripe.model.Subscription stripeSubscription) {
    Subscription.Status status = mapStripeSubscriptionStatus(stripeSubscription.getStatus());

    // In Stripe API v29+, current period is now at subscription item level
    Instant currentPeriodStart = null;
    Instant currentPeriodEnd = null;

    if (stripeSubscription.getItems() != null
        && !stripeSubscription.getItems().getData().isEmpty()) {
      var firstItem = stripeSubscription.getItems().getData().get(0);
      if (firstItem.getCurrentPeriodStart() != null) {
        currentPeriodStart = Instant.ofEpochSecond(firstItem.getCurrentPeriodStart());
      }
      if (firstItem.getCurrentPeriodEnd() != null) {
        currentPeriodEnd = Instant.ofEpochSecond(firstItem.getCurrentPeriodEnd());
      }
    }

    subscription.updateFromStripe(
        status,
        currentPeriodStart != null ? currentPeriodStart : Instant.now(),
        currentPeriodEnd != null ? currentPeriodEnd : Instant.now().plusSeconds(86400),
        stripeSubscription.getTrialEnd() != null
            ? Instant.ofEpochSecond(stripeSubscription.getTrialEnd())
            : null,
        stripeSubscription.getCancelAt() != null
            ? Instant.ofEpochSecond(stripeSubscription.getCancelAt())
            : null);
  }

  private Subscription.Status mapStripeSubscriptionStatus(String stripeStatus) {
    return switch (stripeStatus) {
      case "active" -> Subscription.Status.ACTIVE;
      case "trialing" -> Subscription.Status.TRIALING;
      case "past_due" -> Subscription.Status.PAST_DUE;
      case "canceled" -> Subscription.Status.CANCELED;
      case "unpaid" -> Subscription.Status.UNPAID;
      case "incomplete" -> Subscription.Status.INCOMPLETE;
      case "incomplete_expired" -> Subscription.Status.INCOMPLETE_EXPIRED;
      default -> Subscription.Status.INCOMPLETE;
    };
  }

  private void handleSubscriptionCreated(Map<String, Object> eventData) {
    // Implementation for subscription created webhook
    logger.debug("Subscription created webhook received");
  }

  private void handleSubscriptionUpdated(Map<String, Object> eventData) {
    String subscriptionId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    subscriptionRepository
        .findByStripeSubscriptionId(subscriptionId)
        .ifPresent(
            subscription -> {
              try {
                com.stripe.model.Subscription stripeSubscription =
                    stripeClient.retrieveSubscription(subscriptionId);
                updateSubscriptionFromStripe(subscription, stripeSubscription);
                subscriptionRepository.save(subscription);
                logger.info("Updated subscription from webhook: {}", subscriptionId);
              } catch (StripeException e) {
                logger.error("Failed to update subscription from webhook: {}", subscriptionId, e);
              }
            });
  }

  private void handleSubscriptionDeleted(Map<String, Object> eventData) {
    String subscriptionId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    subscriptionRepository
        .findByStripeSubscriptionId(subscriptionId)
        .ifPresent(
            subscription -> {
              subscription.cancel();
              subscriptionRepository.save(subscription);
              logger.info("Canceled subscription from webhook: {}", subscriptionId);
            });
  }

  private void handleInvoiceCreated(Map<String, Object> eventData) {
    logger.debug("Invoice created webhook received");
  }

  private void handleInvoicePaymentSucceeded(Map<String, Object> eventData) {
    String invoiceId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    invoiceRepository
        .findByStripeInvoiceId(invoiceId)
        .ifPresent(
            invoice -> {
              invoice.markAsPaid();
              invoiceRepository.save(invoice);
              logger.info("Marked invoice as paid from webhook: {}", invoiceId);
            });
  }

  private void handleInvoicePaymentFailed(Map<String, Object> eventData) {
    String invoiceId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    invoiceRepository
        .findByStripeInvoiceId(invoiceId)
        .ifPresent(
            invoice -> {
              invoice.markAsPaymentFailed();
              invoiceRepository.save(invoice);
              logger.warn("Marked invoice payment as failed from webhook: {}", invoiceId);
            });
  }

  private void handleInvoiceFinalized(Map<String, Object> eventData) {
    logger.debug("Invoice finalized webhook received");
  }

  private void validateOrganizationAccess(UUID organizationId) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("Authentication required");
    }
    // Additional organization access validation would go here
  }

  public record SubscriptionStatistics(
      Subscription.Status status, long totalInvoices, Money totalAmount, Money recentAmount) {}
}
