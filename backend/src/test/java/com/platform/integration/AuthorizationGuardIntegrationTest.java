package com.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatus;
import com.platform.payment.api.PaymentManagementService;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.api.OrganizationManagementService;
import com.platform.user.api.UserDto.OrganizationResponse;
import com.platform.user.api.UserDto.UserStatistics;
import com.platform.user.api.UserManagementServiceInterface;
import com.platform.subscription.api.SubscriptionManagementService;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatisticsResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationGuardIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OrganizationManagementService organizationManagementService;
  @MockBean private PaymentManagementService paymentManagementService;
  @MockBean private UserManagementServiceInterface userManagementService;
  @MockBean private SubscriptionManagementService subscriptionManagementService;

  private static final UUID MEMBER_ORG = UUID.randomUUID();
  private static final UUID OTHER_ORG = UUID.randomUUID();

  private PlatformUserPrincipal memberPrincipal;
  private PlatformUserPrincipal adminPrincipal;
  private PlatformUserPrincipal outsiderPrincipal;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    memberPrincipal = PlatformUserPrincipal.organizationMember(
        UUID.randomUUID(), "member@example.com", "Member",
        MEMBER_ORG, "member-org", "MEMBER");
    adminPrincipal = PlatformUserPrincipal.organizationMember(
        UUID.randomUUID(), "admin@example.com", "Admin",
        MEMBER_ORG, "member-org", "ADMIN");
    outsiderPrincipal = PlatformUserPrincipal.organizationMember(
        UUID.randomUUID(), "other@example.com", "Other",
        OTHER_ORG, "other-org", "MEMBER");

    reset(
        organizationManagementService,
        paymentManagementService,
        userManagementService,
        subscriptionManagementService);
  }

  @Test
  void organizationEndpoint_deniesCrossTenantAccess() throws Exception {
    UUID targetOrg = MEMBER_ORG;

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/organizations/" + targetOrg)
                    .with(withPrincipal(outsiderPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());

    verifyNoInteractions(organizationManagementService);
  }

  @Test
  void organizationEndpoint_allowsMemberAccess() throws Exception {
    UUID targetOrg = MEMBER_ORG;
    OrganizationResponse response =
        new OrganizationResponse(
            targetOrg,
            "Test Org",
            "test-org",
            UUID.randomUUID(),
            Map.of("plan", "pro"),
            Instant.now(),
            Instant.now());

    when(organizationManagementService.findById(targetOrg)).thenReturn(Optional.of(response));

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/organizations/" + targetOrg)
                    .with(withPrincipal(memberPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    Map<String, Object> body =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertThat(body.get("id")).isEqualTo(targetOrg.toString());
    assertThat(body.get("name")).isEqualTo("Test Org");
  }

  @Test
  void paymentEndpoint_enforcesOrganizationMembership() throws Exception {
    UUID targetOrg = MEMBER_ORG;

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/payments/organizations/" + targetOrg)
                    .with(withPrincipal(outsiderPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());

    verifyNoInteractions(paymentManagementService);
  }

  @Test
  void paymentEndpoint_requiresAdminRole() throws Exception {
    UUID targetOrg = MEMBER_ORG;

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/payments/organizations/" + targetOrg)
                    .with(withPrincipal(memberPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());

    verifyNoInteractions(paymentManagementService);
  }

  @Test
  void paymentEndpoint_allowsOrganizationMember() throws Exception {
    UUID targetOrg = MEMBER_ORG;
    PaymentResponse paymentResponse =
        new PaymentResponse(
            UUID.randomUUID(),
            targetOrg,
            "pi_123",
            BigDecimal.TEN,
            "USD",
            PaymentStatus.SUCCEEDED,
            "pm_123",
            "Invoice",
            Instant.now());

    when(paymentManagementService.getOrganizationPayments(targetOrg))
        .thenReturn(List.of(paymentResponse));

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/payments/organizations/" + targetOrg)
                    .with(withPrincipal(adminPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    List<Map<String, Object>> body =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertThat(body).isNotEmpty();
    assertThat(body.get(0).get("stripePaymentIntentId")).isEqualTo("pi_123");
  }

  @Test
  void subscriptionStatistics_requiresAdminRole() throws Exception {
    UUID targetOrg = MEMBER_ORG;

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/subscriptions/organizations/" + targetOrg + "/statistics")
                    .with(withPrincipal(memberPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());

    verifyNoInteractions(subscriptionManagementService);
  }

  @Test
  void subscriptionStatistics_allowsAdminRole() throws Exception {
    UUID targetOrg = MEMBER_ORG;
    SubscriptionStatisticsResponse response =
        new SubscriptionStatisticsResponse(
            SubscriptionStatus.ACTIVE,
            5L,
            BigDecimal.valueOf(5000, 2),
            BigDecimal.valueOf(1000, 2),
            Instant.now());

    when(subscriptionManagementService.getSubscriptionStatistics(targetOrg)).thenReturn(response);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/subscriptions/organizations/" + targetOrg + "/statistics")
                    .with(withPrincipal(adminPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    Map<String, Object> body =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertThat(body.get("totalInvoices")).isEqualTo(5);
  }

  @Test
  void organizationMemberRemoval_requiresAdminRole() throws Exception {
    UUID targetOrg = MEMBER_ORG;
    UUID targetUser = UUID.randomUUID();

    MvcResult result =
        mockMvc
            .perform(
                delete("/api/v1/organizations/" + targetOrg + "/members/" + targetUser)
                    .with(withPrincipal(memberPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());

    verifyNoInteractions(organizationManagementService);
  }

  @Test
  void organizationMemberRemoval_allowsAdminRole() throws Exception {
    UUID targetOrg = MEMBER_ORG;
    UUID targetUser = UUID.randomUUID();

    MvcResult result =
        mockMvc
            .perform(
                delete("/api/v1/organizations/" + targetOrg + "/members/" + targetUser)
                    .with(withPrincipal(adminPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
  }

  @Test
  void userStatistics_requiresAdminRole() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/users/statistics")
                    .with(withPrincipal(memberPrincipal)))
            .andReturn();

    int status = result.getResponse().getStatus();
    System.out.println("member stats status=" + status);
    assertThat(status).isEqualTo(HttpStatus.FORBIDDEN.value());

    verifyNoInteractions(userManagementService);
  }

  @Test
  void userStatistics_allowsAdminRole() throws Exception {
    UserStatistics stats =
        new UserStatistics(42, 35, 5, Map.of("google", 10L, "github", 5L), 12.5);
    when(userManagementService.getUserStatistics()).thenReturn(stats);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/users/statistics")
                    .with(withPrincipal(adminPrincipal)))
            .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    Map<String, Object> body =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    assertThat(body.get("totalUsers")).isEqualTo(42);
    @SuppressWarnings("unchecked")
    Map<String, Object> providerCounts = (Map<String, Object>) body.get("usersByProvider");
    assertThat(providerCounts.get("google")).isEqualTo(10);
  }

  private RequestPostProcessor withPrincipal(PlatformUserPrincipal principal) {
    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities());
    return authentication(authenticationToken);
  }
}
