package com.platform.payment.api;

import com.platform.payment.internal.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling incoming webhooks from Stripe.
 *
 * <p>This controller provides a public endpoint to receive real-time event notifications from
 * Stripe. It is responsible for verifying the authenticity of webhook requests using the Stripe
 * signature and delegating the event processing to the internal {@link PaymentService}.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
public class StripeWebhookController {

  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

  private final PaymentService paymentService;
  private final String webhookSecret;

  /**
   * Constructs the controller with the payment service and webhook secret.
   *
   * @param paymentService The service responsible for processing payment events.
   * @param webhookSecret The secret key used to verify the signature of incoming webhooks.
   */
  public StripeWebhookController(
      PaymentService paymentService, @Value("${stripe.webhook.secret}") String webhookSecret) {
    this.paymentService = paymentService;
    this.webhookSecret = webhookSecret;
  }

  /**
   * Handles incoming webhook events from Stripe.
   *
   * @param payload The raw JSON payload of the webhook request.
   * @param sigHeader The value of the "Stripe-Signature" header for verification.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} indicating the result of the webhook processing.
   */
  @PostMapping
  public ResponseEntity<String> handleWebhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String sigHeader,
      HttpServletRequest request) {
    Event event;
    try {
      event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
      logger.warn("Failed to verify Stripe webhook signature: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
    } catch (Exception e) {
      logger.error("Error parsing Stripe webhook", e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
    }

    try {
      processStripeEvent(event);
      logger.info(
          "Successfully processed Stripe webhook: {} of type: {}", event.getId(), event.getType());
      return ResponseEntity.ok("Webhook processed successfully");
    } catch (Exception e) {
      logger.error(
          "Error processing Stripe webhook: {} of type: {}", event.getId(), event.getType(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Webhook processing failed");
    }
  }

  /**
   * Processes a verified Stripe event by routing it to the appropriate handler based on its type.
   *
   * @param event The verified {@link Event} object from Stripe.
   */
  private void processStripeEvent(Event event) {
    String eventType = event.getType();
    Map<String, Object> eventData = new java.util.HashMap<>();
    try {
      if (event.getData() != null && event.getData().getObject() != null) {
        eventData.put("type", eventType);
        eventData.put("id", event.getId());
      }
    } catch (Exception e) {
      logger.warn("Failed to parse event data: {}", e.getMessage());
    }

    logger.debug("Processing Stripe event: {} of type: {}", event.getId(), eventType);

    switch (eventType) {
        // Payment Intent events
      case "payment_intent.succeeded" -> {
        logger.info("Payment succeeded: {}", getPaymentIntentId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }
      case "payment_intent.payment_failed" -> {
        logger.warn("Payment failed: {}", getPaymentIntentId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }
      case "payment_intent.canceled" -> {
        logger.info("Payment canceled: {}", getPaymentIntentId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }
      case "payment_intent.processing" -> {
        logger.info("Payment processing: {}", getPaymentIntentId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }
      case "payment_intent.requires_action" -> {
        logger.info("Payment requires action: {}", getPaymentIntentId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }

        // Payment Method events
      case "payment_method.attached" -> {
        logger.info("Payment method attached: {}", getPaymentMethodId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }
      case "payment_method.detached" -> {
        logger.info("Payment method detached: {}", getPaymentMethodId(eventData));
        paymentService.processWebhookEvent(event.getId(), eventType, eventData);
      }

        // Customer events
      case "customer.created" -> logger.info("Customer created: {}", getCustomerId(eventData));
      case "customer.updated" -> logger.info("Customer updated: {}", getCustomerId(eventData));
      case "customer.deleted" -> logger.warn("Customer deleted: {}", getCustomerId(eventData));

        // Invoice events (for subscription billing)
      case "invoice.created" -> logger.info("Invoice created: {}", getInvoiceId(eventData));
      case "invoice.payment_succeeded" -> logger.info("Invoice payment succeeded: {}", getInvoiceId(eventData));
      case "invoice.payment_failed" -> logger.warn("Invoice payment failed: {}", getInvoiceId(eventData));
      case "invoice.finalized" -> logger.info("Invoice finalized: {}", getInvoiceId(eventData));

        // Subscription events
      case "customer.subscription.created" -> logger.info("Subscription created: {}", getSubscriptionId(eventData));
      case "customer.subscription.updated" -> logger.info("Subscription updated: {}", getSubscriptionId(eventData));
      case "customer.subscription.deleted" -> logger.warn("Subscription deleted: {}", getSubscriptionId(eventData));

        // Charge events (for disputes and refunds)
      case "charge.succeeded" -> logger.info("Charge succeeded: {}", getChargeId(eventData));
      case "charge.failed" -> logger.warn("Charge failed: {}", getChargeId(eventData));
      case "charge.dispute.created" -> logger.warn("Charge dispute created: {}", getChargeId(eventData));

        // Setup Intent events (for saving payment methods)
      case "setup_intent.succeeded" -> logger.info("Setup intent succeeded: {}", getSetupIntentId(eventData));
      case "setup_intent.setup_failed" -> logger.warn("Setup intent failed: {}", getSetupIntentId(eventData));
      default -> logger.debug("Received unhandled Stripe webhook event type: {}", eventType);
    }
  }

  private String getPaymentIntentId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "payment_intent");
  }

  private String getPaymentMethodId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "payment_method");
  }

  private String getCustomerId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "customer");
  }

  private String getInvoiceId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "invoice");
  }

  private String getSubscriptionId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "subscription");
  }

  private String getChargeId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "charge");
  }

  private String getSetupIntentId(Map<String, Object> eventData) {
    return getIdFromEventData(eventData, "setup_intent");
  }

  private String getIdFromEventData(Map<String, Object> eventData, String objectType) {
    try {
      Map<?, ?> objectData = (Map<?, ?>) eventData.get("object");
      if (objectData != null) {
        Object id = objectData.get("id");
        return id != null ? id.toString() : "unknown";
      }
    } catch (Exception e) {
      logger.warn("Failed to extract {} ID from event data", objectType, e);
    }
    return "unknown";
  }

  /**
   * A simple health check endpoint for the webhook controller.
   *
   * @return A {@link ResponseEntity} with a success message.
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Webhook endpoint is healthy");
  }
}
