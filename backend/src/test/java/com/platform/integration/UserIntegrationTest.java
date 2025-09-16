package com.platform.integration;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.shared.types.Email;
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
@DisplayName("User Module Integration Tests")
class UserIntegrationTest {

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

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should get current user profile")
    void shouldGetCurrentUserProfile() {
        User user = createTestUser("profile@example.com", "Profile User");
        String token = createTokenForUser(user);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(user.getId().toString()))
            .body("email", equalTo("profile@example.com"))
            .body("name", equalTo("Profile User"))
            .body("provider", equalTo("google"))
            .body("preferences", notNullValue());
    }

    @Test
    @DisplayName("Should update user profile")
    void shouldUpdateUserProfile() {
        User user = createTestUser("update@example.com", "Update User");
        String token = createTokenForUser(user);

        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Name",
            "preferences", Map.of("theme", "dark", "language", "en")
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .put("/api/v1/users/me/profile")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("name", equalTo("Updated Name"))
            .body("preferences.theme", equalTo("dark"))
            .body("preferences.language", equalTo("en"));
    }

    @Test
    @DisplayName("Should update user preferences only")
    void shouldUpdateUserPreferences() {
        User user = createTestUser("prefs@example.com", "Prefs User");
        String token = createTokenForUser(user);

        Map<String, Object> updateRequest = Map.of(
            "preferences", Map.of("notifications", true, "timezone", "UTC")
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .put("/api/v1/users/me/preferences")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("preferences.notifications", equalTo(true))
            .body("preferences.timezone", equalTo("UTC"));
    }

    @Test
    @DisplayName("Should delete current user")
    void shouldDeleteCurrentUser() {
        User user = createTestUser("delete@example.com", "Delete User");
        String token = createTokenForUser(user);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/v1/users/me")
        .then()
            .statusCode(204);

        // Verify user is soft deleted
        User deletedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should search users by name")
    void shouldSearchUsersByName() {
        createTestUser("search1@example.com", "John Doe");
        createTestUser("search2@example.com", "Jane Doe");
        createTestUser("search3@example.com", "Bob Smith");

        User currentUser = createTestUser("current@example.com", "Current User");
        String token = createTokenForUser(currentUser);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("name", "Doe")
        .when()
            .get("/api/v1/users/search")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(2))
            .body("name", hasItems("John Doe", "Jane Doe"));
    }

    @Test
    @DisplayName("Should create organization")
    void shouldCreateOrganization() {
        User user = createTestUser("org@example.com", "Org User");
        String token = createTokenForUser(user);

        Map<String, Object> createRequest = Map.of(
            "name", "Test Organization",
            "slug", "test-org",
            "settings", Map.of("publicProfile", true)
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(createRequest)
        .when()
            .post("/api/v1/organizations")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("name", equalTo("Test Organization"))
            .body("slug", equalTo("test-org"))
            .body("ownerId", equalTo(user.getId().toString()))
            .body("settings.publicProfile", equalTo(true));
    }

    @Test
    @DisplayName("Should get user organizations")
    void shouldGetUserOrganizations() {
        User user = createTestUser("orgs@example.com", "Orgs User");
        String token = createTokenForUser(user);

        // Create organizations
        Organization org1 = new Organization("Org One", "org-one", user.getId());
        Organization org2 = new Organization("Org Two", "org-two", user.getId());
        organizationRepository.save(org1);
        organizationRepository.save(org2);

        // Add user as member
        memberRepository.save(OrganizationMember.createOwner(user.getId(), org1.getId()));
        memberRepository.save(OrganizationMember.createOwner(user.getId(), org2.getId()));

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/organizations")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(2))
            .body("name", hasItems("Org One", "Org Two"));
    }

    @Test
    @DisplayName("Should invite user to organization")
    void shouldInviteUserToOrganization() {
        User owner = createTestUser("owner@example.com", "Owner User");
        String token = createTokenForUser(owner);

        Organization org = new Organization("Test Org", "test-org", owner.getId());
        organizationRepository.save(org);
        memberRepository.save(OrganizationMember.createOwner(owner.getId(), org.getId()));

        Map<String, Object> inviteRequest = Map.of(
            "email", "invited@example.com",
            "role", "MEMBER"
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(inviteRequest)
        .when()
            .post("/api/v1/organizations/{organizationId}/invitations", org.getId())
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("email", equalTo("invited@example.com"))
            .body("role", equalTo("MEMBER"))
            .body("status", equalTo("PENDING"))
            .body("token", notNullValue());
    }

    @Test
    @DisplayName("Should accept organization invitation")
    void shouldAcceptOrganizationInvitation() {
        User owner = createTestUser("owner2@example.com", "Owner User");
        User invited = createTestUser("invited2@example.com", "Invited User");

        Organization org = new Organization("Test Org 2", "test-org-2", owner.getId());
        organizationRepository.save(org);
        memberRepository.save(OrganizationMember.createOwner(owner.getId(), org.getId()));

        Invitation invitation = Invitation.create(
            org.getId(),
            owner.getId(),
            "invited2@example.com",
            OrganizationMember.Role.MEMBER,
            Instant.now().plusSeconds(3600)
        );
        invitationRepository.save(invitation);

        String token = createTokenForUser(invited);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .post("/api/v1/organizations/invitations/{token}/accept", invitation.getToken())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("userId", equalTo(invited.getId().toString()))
            .body("organizationId", equalTo(org.getId().toString()))
            .body("role", equalTo("MEMBER"));

        // Verify invitation status
        Invitation updatedInvitation = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertThat(updatedInvitation.getStatus()).isEqualTo(Invitation.Status.ACCEPTED);
    }

    @Test
    @DisplayName("Should get organization members")
    void shouldGetOrganizationMembers() {
        User owner = createTestUser("owner3@example.com", "Owner User");
        User member = createTestUser("member@example.com", "Member User");
        String token = createTokenForUser(owner);

        Organization org = new Organization("Test Org 3", "test-org-3", owner.getId());
        organizationRepository.save(org);

        memberRepository.save(OrganizationMember.createOwner(owner.getId(), org.getId()));
        memberRepository.save(new OrganizationMember(member.getId(), org.getId(), OrganizationMember.Role.MEMBER));

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/organizations/{organizationId}/members", org.getId())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(2))
            .body("userEmail", hasItems("owner3@example.com", "member@example.com"))
            .body("role", hasItems("OWNER", "MEMBER"));
    }

    @Test
    @DisplayName("Should remove organization member")
    void shouldRemoveOrganizationMember() {
        User owner = createTestUser("owner4@example.com", "Owner User");
        User member = createTestUser("member2@example.com", "Member User");
        String token = createTokenForUser(owner);

        Organization org = new Organization("Test Org 4", "test-org-4", owner.getId());
        organizationRepository.save(org);

        memberRepository.save(OrganizationMember.createOwner(owner.getId(), org.getId()));
        OrganizationMember memberEntity = new OrganizationMember(member.getId(), org.getId(), OrganizationMember.Role.MEMBER);
        memberRepository.save(memberEntity);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/v1/organizations/{organizationId}/members/{userId}", org.getId(), member.getId())
        .then()
            .statusCode(204);

        // Verify member is removed
        assertThat(memberRepository.findByUserIdAndOrganizationId(member.getId(), org.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should update member role")
    void shouldUpdateMemberRole() {
        User owner = createTestUser("owner5@example.com", "Owner User");
        User member = createTestUser("member3@example.com", "Member User");
        String token = createTokenForUser(owner);

        Organization org = new Organization("Test Org 5", "test-org-5", owner.getId());
        organizationRepository.save(org);

        memberRepository.save(OrganizationMember.createOwner(owner.getId(), org.getId()));
        OrganizationMember memberEntity = new OrganizationMember(member.getId(), org.getId(), OrganizationMember.Role.MEMBER);
        memberRepository.save(memberEntity);

        Map<String, Object> updateRequest = Map.of("role", "ADMIN");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .put("/api/v1/organizations/{organizationId}/members/{userId}/role", org.getId(), member.getId())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("role", equalTo("ADMIN"));
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void shouldRequireAuthenticationForProtectedEndpoints() {
        given()
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(401);

        given()
        .when()
            .get("/api/v1/organizations")
        .then()
            .statusCode(401);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Test", "slug", "test"))
        .when()
            .post("/api/v1/organizations")
        .then()
            .statusCode(401);
    }

    private User createTestUser(String email, String name) {
        User user = new User(new Email(email), name, "google", "test-provider-id");
        return userRepository.save(user);
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