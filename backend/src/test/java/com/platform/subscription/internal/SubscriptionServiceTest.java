package com.platform.subscription.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.shared.stripe.StripeClient;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

  @Mock SubscriptionRepository subscriptionRepository;
  @Mock PlanRepository planRepository;
  @Mock InvoiceRepository invoiceRepository;
  @Mock OrganizationRepository organizationRepository;
  @Mock ApplicationEventPublisher eventPublisher;
  @Mock AuditService auditService;
  @Mock StripeClient stripeClient;

  @InjectMocks SubscriptionService subscriptionService;

  private UUID orgId;
  private UUID planId;

  @BeforeEach
  void init() {
    orgId = UUID.randomUUID();
    planId = UUID.randomUUID();
    // Simulate authenticated tenant context
    TenantContext.setTenantInfo(orgId, "acme", UUID.randomUUID());
  }

  @Test
  void createSubscription_throws_whenOrganizationAlreadyHasActive() {
    // Given existing active subscription
    Subscription existingStripe = mock(Subscription.class);
    com.platform.subscription.internal.Subscription existing =
        com.platform.subscription.internal.Subscription.createActive(orgId, planId, null, null);
    try {
      var idField = com.platform.subscription.internal.Subscription.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(existing, UUID.randomUUID());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(existing));

    // When / Then
    assertThrows(
        IllegalStateException.class,
        () -> subscriptionService.createSubscription(orgId, planId, null, false));

    verifyNoInteractions(planRepository, organizationRepository, stripeClient);
  }

  @Test
  void createSubscription_succeeds_andPersists_whenValid_noPaymentMethod() throws Exception {
    // Given
    when(subscriptionRepository.findByOrganizationId(orgId)).thenReturn(Optional.empty());

    Organization org = new Organization("Acme", "acme-demo", (String) null);
    // Set the ID that would be set by JPA
    setField(org, "id", orgId);
    when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

    Plan plan = new Plan("Pro", "price_123", Money.usd(10.00), Plan.BillingInterval.MONTH);
    // Use reflection to set required fields for test
    setField(plan, "slug", "pro-plan");
    setField(plan, "active", true);
    // Simulate persisted IDs via reflection or keep null; service uses plan fields only
    when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

    when(stripeClient.getOrCreateCustomer(any(), any())).thenReturn("cus_123");

    Subscription stripeSub = mock(Subscription.class);
    when(stripeSub.getId()).thenReturn("sub_123");
    when(stripeSub.getStatus()).thenReturn("active");
    when(stripeClient.createSubscription(any(SubscriptionCreateParams.class)))
        .thenReturn(stripeSub);

    ArgumentCaptor<com.platform.subscription.internal.Subscription> savedCaptor =
        ArgumentCaptor.forClass(com.platform.subscription.internal.Subscription.class);
    when(subscriptionRepository.save(any()))
        .thenAnswer(
            invocation -> {
              com.platform.subscription.internal.Subscription sub = invocation.getArgument(0);
              // Simulate JPA setting the ID and timestamps after persistence
              setField(sub, "id", UUID.randomUUID());
              setField(sub, "createdAt", Instant.now());
              setField(sub, "updatedAt", Instant.now());
              return sub;
            });

    // When
    com.platform.subscription.internal.Subscription created =
        subscriptionService.createSubscription(orgId, planId, null, false);

    // Then
    assertNotNull(created);
    assertEquals(orgId, created.getOrganizationId());
    assertEquals(planId, created.getPlanId());
    assertTrue(created.isActive());

    // Verify stripe calls
    verify(stripeClient).getOrCreateCustomer(orgId, "Acme");
    verify(stripeClient, never()).attachPaymentMethodToCustomer(anyString(), anyString());
    verify(stripeClient).createSubscription(any());

    // Verify persistence
    verify(subscriptionRepository, atLeastOnce()).save(savedCaptor.capture());
    com.platform.subscription.internal.Subscription saved = savedCaptor.getValue();
    assertEquals("sub_123", saved.getStripeSubscriptionId());
  }

  private void setField(Object obj, String fieldName, Object value) {
    try {
      Field field = obj.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(obj, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
