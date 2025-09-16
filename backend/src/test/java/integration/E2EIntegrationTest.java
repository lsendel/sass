package integration;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.payment.internal.*;
import com.platform.shared.types.Email;
import com.platform.shared.types.Money;
import com.platform.subscription.internal.*;
import com.platform.user.internal.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("E2E Integration Tests - Complete User Journeys")
class E2EIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("platform_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("stripe.secret-key", () -> "sk_test_mock_key");
        registry.add("stripe.webhook.secret", () -> "whsec_mock_secret");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OpaqueTokenStore tokenStore;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Clean up test data in dependency order
        auditEventRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        invoiceRepository.deleteAll();
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle complete user onboarding flow")
    void shouldHandleCompleteUserOnboardingFlow() {
        String userEmail = "onboarding@example.com";
        String orgName = "Test Company";
        String orgSlug = "test-company";

        // Step 1: User signs up (simulated via direct creation)
        User user = new User(new Email(userEmail), "Test User", "google", "google_123");
        userRepository.save(user);
        String token = createTokenForUser(user);

        // Step 2: Create organization
        Map<String, Object> orgRequest = Map.of(
            "name", orgName,
            "slug", orgSlug,
            "settings", Map.of("industry", "technology")
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(orgRequest)
        .when()
            .post("/api/v1/organizations")
        .then()
            .statusCode(201)
            .body("name", equalTo(orgName))
            .body("slug", equalTo(orgSlug));

        // Verify organization was created with owner membership
        Organization org = organizationRepository.findBySlugAndDeletedAtIsNull(orgSlug).orElseThrow();
        assertThat(org.getOwnerId()).isEqualTo(user.getId());

        OrganizationMember ownerMember = memberRepository.findByUserIdAndOrganizationId(user.getId(), org.getId()).orElseThrow();
        assertThat(ownerMember.getRole()).isEqualTo(OrganizationMember.Role.OWNER);

        // Step 3: Get user's organizations
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/organizations")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].name", equalTo(orgName));
    }

    @Test
    @DisplayName("Should handle organization invitation and membership flow")
    void shouldHandleOrganizationInvitationFlow() {
        // Create owner and organization
        User owner = createTestUser("owner@example.com", "Owner User");
        Organization org = createTestOrganization(owner, "Invitation Org", "invitation-org");
        String ownerToken = createTokenForUser(owner);

        // Create invitee user
        User invitee = createTestUser("invitee@example.com", "Invitee User");
        String inviteeToken = createTokenForUser(invitee);

        // Step 1: Owner invites user
        Map<String, Object> inviteRequest = Map.of(
            "email", "invitee@example.com",
            "role", "MEMBER"
        );

        String invitationToken = given()
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(ContentType.JSON)
            .body(inviteRequest)
        .when()
            .post("/api/v1/organizations/{organizationId}/invitations", org.getId())
        .then()
            .statusCode(201)
            .body("email", equalTo("invitee@example.com"))
            .body("role", equalTo("MEMBER"))
            .body("status", equalTo("PENDING"))
            .extract().path("token");

        // Step 2: Invitee accepts invitation
        given()
            .header("Authorization", "Bearer " + inviteeToken)
        .when()
            .post("/api/v1/organizations/invitations/{token}/accept", invitationToken)
        .then()
            .statusCode(200)
            .body("userId", equalTo(invitee.getId().toString()))
            .body("organizationId", equalTo(org.getId().toString()))
            .body("role", equalTo("MEMBER"));

        // Step 3: Verify membership
        given()
            .header("Authorization", "Bearer " + ownerToken)
        .when()
            .get("/api/v1/organizations/{organizationId}/members", org.getId())
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("userEmail", hasItems("owner@example.com", "invitee@example.com"))
            .body("role", hasItems("OWNER", "MEMBER"));

        // Step 4: Invitee can now see organization
        given()
            .header("Authorization", "Bearer " + inviteeToken)
        .when()
            .get("/api/v1/organizations")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].name", equalTo("Invitation Org"));
    }

    @Test
    @DisplayName("Should handle subscription and payment flow")
    void shouldHandleSubscriptionAndPaymentFlow() {
        // Setup: Create user, organization, and plan
        User user = createTestUser("billing@example.com", "Billing User");
        Organization org = createTestOrganization(user, "Billing Org", "billing-org");
        Plan plan = createTestPlan("Pro Plan", "pro", 19.99);
        String token = createTokenForUser(user);

        // Step 1: Create subscription (would fail with mock Stripe, so create directly)
        Subscription subscription = new Subscription(org.getId(), plan.getId(), "sub_test_billing");
        subscription.updateFromStripe(
            Subscription.Status.ACTIVE,
            Instant.now().minusSeconds(86400),
            Instant.now().plusSeconds(86400 * 30),
            null,
            null
        );
        subscriptionRepository.save(subscription);

        // Step 2: Create payment method (simulate attached payment method)
        PaymentMethod paymentMethod = new PaymentMethod(org.getId(), "pm_test_billing", PaymentMethod.Type.CARD);
        paymentMethod.updateCardDetails("4242", "visa", 12, 2025);
        paymentMethod.markAsDefault();
        paymentMethodRepository.save(paymentMethod);

        // Step 3: Create payment for subscription
        Payment payment = new Payment(org.getId(), "pi_test_billing",
                                     new Money(BigDecimal.valueOf(19.99)), "usd", "Subscription payment");
        payment.setSubscriptionId(subscription.getId());
        payment.updateStatus(Payment.Status.SUCCEEDED);
        paymentRepository.save(payment);

        // Step 4: Create invoice
        Invoice invoice = new Invoice(org.getId(), subscription.getId(), "inv_test_billing", "INV-001");
        invoice.updateAmounts(
            new Money(BigDecimal.valueOf(19.99)),
            new Money(BigDecimal.valueOf(2.00)),
            new Money(BigDecimal.valueOf(21.99)),
            "usd"
        );
        invoice.markAsPaid();
        invoiceRepository.save(invoice);

        // Verify: Get subscription shows correct status
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/subscriptions/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"))
            .body("planId", equalTo(plan.getId().toString()));

        // Verify: Get payment methods
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/methods/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].type", equalTo("CARD"))
            .body("[0].isDefault", equalTo(true));

        // Verify: Get payments
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].status", equalTo("SUCCEEDED"))
            .body("[0].amount", equalTo(19.99f));

        // Verify: Get invoices
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/subscriptions/organizations/{organizationId}/invoices", org.getId())
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].status", equalTo("PAID"))
            .body("[0].totalAmount", equalTo(21.99f));

        // Verify: Get statistics
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}/statistics", org.getId())
        .then()
            .statusCode(200)
            .body("totalSuccessfulPayments", equalTo(1))
            .body("totalAmount", equalTo(19.99f));

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/subscriptions/organizations/{organizationId}/statistics", org.getId())
        .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"))
            .body("totalInvoices", equalTo(1))
            .body("totalAmount", equalTo(21.99f));
    }

    @Test
    @DisplayName("Should enforce organization isolation across modules")
    void shouldEnforceOrganizationIsolation() {
        // Create two separate organizations with different users
        User user1 = createTestUser("user1@example.com", "User One");
        User user2 = createTestUser("user2@example.com", "User Two");

        Organization org1 = createTestOrganization(user1, "Org One", "org-one");
        Organization org2 = createTestOrganization(user2, "Org Two", "org-two");

        String token1 = createTokenForUser(user1);
        String token2 = createTokenForUser(user2);

        // Create resources for org1
        Plan plan = createTestPlan("Isolation Plan", "isolation", 9.99);

        Subscription subscription1 = new Subscription(org1.getId(), plan.getId(), "sub_isolation_1");
        subscriptionRepository.save(subscription1);

        Payment payment1 = new Payment(org1.getId(), "pi_isolation_1",
                                      new Money(BigDecimal.valueOf(9.99)), "usd", "Org 1 payment");
        paymentRepository.save(payment1);

        // User 2 should not be able to access org1's resources
        given()
            .header("Authorization", "Bearer " + token2)
        .when()
            .get("/api/v1/subscriptions/organizations/{organizationId}", org1.getId())
        .then()
            .statusCode(403); // Forbidden due to organization isolation

        given()
            .header("Authorization", "Bearer " + token2)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}", org1.getId())
        .then()
            .statusCode(403);

        given()
            .header("Authorization", "Bearer " + token2)
        .when()
            .get("/api/v1/organizations/{organizationId}/members", org1.getId())
        .then()
            .statusCode(403);

        // User 1 should only see their own organization's resources
        given()
            .header("Authorization", "Bearer " + token1)
        .when()
            .get("/api/v1/organizations")
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].name", equalTo("Org One"));
    }

    @Test
    @DisplayName("Should handle plan changes affecting payments and invoices")
    void shouldHandlePlanChangesAffectingPaymentsAndInvoices() {
        // Setup
        User user = createTestUser("plan-change@example.com", "Plan Change User");
        Organization org = createTestOrganization(user, "Plan Change Org", "plan-change-org");

        Plan basicPlan = createTestPlan("Basic Plan", "basic", 9.99);
        Plan proPlan = createTestPlan("Pro Plan", "pro", 19.99);

        String token = createTokenForUser(user);

        // Create initial subscription with basic plan
        Subscription subscription = new Subscription(org.getId(), basicPlan.getId(), "sub_plan_change");
        subscription.updateFromStripe(Subscription.Status.ACTIVE, Instant.now().minusSeconds(86400),
                                     Instant.now().plusSeconds(86400 * 30), null, null);
        subscriptionRepository.save(subscription);

        // Create payment for basic plan
        Payment basicPayment = new Payment(org.getId(), "pi_basic",
                                          new Money(BigDecimal.valueOf(9.99)), "usd", "Basic plan payment");
        basicPayment.setSubscriptionId(subscription.getId());
        basicPayment.updateStatus(Payment.Status.SUCCEEDED);
        paymentRepository.save(basicPayment);

        // Simulate plan change (update subscription)
        subscription.changePlan(proPlan.getId());
        subscriptionRepository.save(subscription);

        // Create payment for pro plan (prorated amount)
        Payment proPayment = new Payment(org.getId(), "pi_pro",
                                        new Money(BigDecimal.valueOf(10.00)), "usd", "Pro plan upgrade proration");
        proPayment.setSubscriptionId(subscription.getId());
        proPayment.updateStatus(Payment.Status.SUCCEEDED);
        paymentRepository.save(proPayment);

        // Verify subscription reflects new plan
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/subscriptions/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .body("planId", equalTo(proPlan.getId().toString()));

        // Verify both payments are tracked
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("description", hasItems("Basic plan payment", "Pro plan upgrade proration"));

        // Verify payment statistics include both payments
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}/statistics", org.getId())
        .then()
            .statusCode(200)
            .body("totalSuccessfulPayments", equalTo(2))
            .body("totalAmount", equalTo(19.99f));
    }

    @Test
    @DisplayName("Should maintain data consistency across module boundaries")
    void shouldMaintainDataConsistencyAcrossModules() {
        User user = createTestUser("consistency@example.com", "Consistency User");
        Organization org = createTestOrganization(user, "Consistency Org", "consistency-org");
        Plan plan = createTestPlan("Consistency Plan", "consistency", 15.99);
        String token = createTokenForUser(user);

        // Create related entities across modules
        Subscription subscription = new Subscription(org.getId(), plan.getId(), "sub_consistency");
        subscription.updateFromStripe(Subscription.Status.ACTIVE, Instant.now().minusSeconds(86400),
                                     Instant.now().plusSeconds(86400 * 30), null, null);
        subscriptionRepository.save(subscription);

        Invoice invoice = new Invoice(org.getId(), subscription.getId(), "inv_consistency", "INV-CONS-001");
        invoice.updateAmounts(new Money(BigDecimal.valueOf(15.99)), new Money(BigDecimal.valueOf(2.00)),
                             new Money(BigDecimal.valueOf(17.99)), "usd");
        invoiceRepository.save(invoice);

        Payment payment = new Payment(org.getId(), "pi_consistency",
                                     new Money(BigDecimal.valueOf(17.99)), "usd", "Consistency test payment");
        payment.setSubscriptionId(subscription.getId());
        payment.setInvoiceId(invoice.getId());
        payment.updateStatus(Payment.Status.SUCCEEDED);
        paymentRepository.save(payment);

        // Mark invoice as paid
        invoice.markAsPaid();
        invoiceRepository.save(invoice);

        // Verify cross-references are maintained
        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(savedPayment.getSubscriptionId()).isEqualTo(subscription.getId());
        assertThat(savedPayment.getInvoiceId()).isEqualTo(invoice.getId());
        assertThat(savedPayment.getOrganizationId()).isEqualTo(org.getId());

        Invoice savedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(savedInvoice.getSubscriptionId()).isEqualTo(subscription.getId());
        assertThat(savedInvoice.getOrganizationId()).isEqualTo(org.getId());
        assertThat(savedInvoice.getStatus()).isEqualTo(Invoice.Status.PAID);

        // Verify API responses include correct relationships
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].subscriptionId", equalTo(subscription.getId().toString()))
            .body("[0].invoiceId", equalTo(invoice.getId().toString()));
    }

    private User createTestUser(String email, String name) {
        User user = new User(new Email(email), name, "google", "test-provider-id");
        return userRepository.save(user);
    }

    private Organization createTestOrganization(User owner, String name, String slug) {
        Organization org = new Organization(name, slug, owner.getId());
        organizationRepository.save(org);

        OrganizationMember member = OrganizationMember.createOwner(owner.getId(), org.getId());
        memberRepository.save(member);

        return org;
    }

    private Plan createTestPlan(String name, String slug, double amount) {
        Plan plan = new Plan(name, slug, "Test plan description",
                            new Money(BigDecimal.valueOf(amount)), "usd", "price_" + slug + "_test");
        plan.updateInterval(Plan.Interval.MONTH, 1);
        return planRepository.save(plan);
    }

    private String createTokenForUser(User user) {
        return tokenStore.createToken(
            user.getId(),
            "127.0.0.1",
            "test-user-agent",
            "google"
        );
    }
}