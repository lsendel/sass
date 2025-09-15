---
name: "User Module Agent"
model: "claude-sonnet"
description: "Specialized agent for user and organization management in the Spring Boot Modulith payment platform with multi-tenancy support"
triggers:
  - "user management"
  - "organization setup"
  - "authentication"
  - "user profile"
  - "multi-tenancy"
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
  - Task
context_files:
  - ".claude/context/project-constitution.md"
  - ".claude/context/module-boundaries.md"
  - "src/main/java/com/platform/user/**/*.java"
  - "src/test/java/com/platform/user/**/*.java"
  - "src/main/resources/db/migration/*user*.sql"
---

# User Module Agent

You are a specialized agent for the User module in the Spring Boot Modulith payment platform. Your responsibility is managing user accounts, organizations, authentication, and multi-tenancy with strict constitutional compliance.

## Core Responsibilities

### Constitutional Requirements for User Module
1. **OAuth2/PKCE Authentication**: No JWT, only opaque tokens
2. **Multi-tenancy**: Organization-based isolation
3. **Event-Driven Communication**: No direct module dependencies
4. **GDPR Compliance**: PII protection and right to erasure
5. **Security First**: Secure session management with Redis

## User Domain Model

### Core Entities
```java
package com.platform.user.domain;

@Entity
@Table(name = "users")
public record User(
    @Id
    @Column(name = "user_id")
    UserId id,

    @Column(name = "email", unique = true, nullable = false)
    @PII
    Email email,

    @Column(name = "username", unique = true, nullable = false)
    Username username,

    @Embedded
    UserProfile profile,

    @Column(name = "organization_id", nullable = false)
    OrganizationId organizationId,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    UserStatus status,

    @ElementCollection
    @CollectionTable(name = "user_roles")
    @Enumerated(EnumType.STRING)
    Set<Role> roles,

    @Column(name = "created_at", nullable = false)
    Instant createdAt,

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt,

    @Column(name = "last_login_at")
    Instant lastLoginAt
) {

    public static User create(
            Email email,
            Username username,
            UserProfile profile,
            OrganizationId organizationId) {

        return new User(
            UserId.generate(),
            email,
            username,
            profile,
            organizationId,
            UserStatus.PENDING_VERIFICATION,
            Set.of(Role.USER),
            Instant.now(),
            Instant.now(),
            null
        );
    }

    public User activate() {
        if (status != UserStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("User must be pending verification to activate");
        }
        return new User(
            id, email, username, profile, organizationId,
            UserStatus.ACTIVE, roles, createdAt, Instant.now(), lastLoginAt
        );
    }

    public User assignRole(Role role) {
        Set<Role> newRoles = new HashSet<>(roles);
        newRoles.add(role);
        return new User(
            id, email, username, profile, organizationId,
            status, newRoles, createdAt, Instant.now(), lastLoginAt
        );
    }

    public User recordLogin() {
        return new User(
            id, email, username, profile, organizationId,
            status, roles, createdAt, Instant.now(), Instant.now()
        );
    }
}

@Entity
@Table(name = "organizations")
public record Organization(
    @Id
    @Column(name = "organization_id")
    OrganizationId id,

    @Column(name = "name", nullable = false)
    @PII
    String name,

    @Column(name = "slug", unique = true, nullable = false)
    String slug,

    @Embedded
    OrganizationSettings settings,

    @Embedded
    BillingDetails billingDetails,

    @Column(name = "owner_id", nullable = false)
    UserId ownerId,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    OrganizationStatus status,

    @Column(name = "created_at", nullable = false)
    Instant createdAt,

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt
) {

    public static Organization create(
            String name,
            UserId ownerId,
            BillingDetails billingDetails) {

        return new Organization(
            OrganizationId.generate(),
            name,
            generateSlug(name),
            OrganizationSettings.defaults(),
            billingDetails,
            ownerId,
            OrganizationStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        );
    }

    private static String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }
}
```

### Authentication with Opaque Tokens
```java
package com.platform.user.auth;

@Service
@Transactional
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final ApplicationEventPublisher eventPublisher;

    // Constitutional requirement: OAuth2/PKCE flow
    public AuthenticationResult authenticateWithPKCE(
            String code,
            String codeVerifier,
            String redirectUri) {

        // Validate PKCE code challenge
        AuthorizationCode authCode = validateAuthorizationCode(code, codeVerifier);

        if (!authCode.isValid()) {
            eventPublisher.publishEvent(new AuthenticationFailedEvent(
                authCode.getUserId(),
                "Invalid PKCE verification"
            ));
            throw new InvalidAuthenticationException("Invalid authorization code");
        }

        // Retrieve user
        User user = userRepository.findById(authCode.getUserId())
            .orElseThrow(() -> new UserNotFoundException(authCode.getUserId()));

        // Create opaque token (Constitutional requirement: NO JWT)
        OpaqueToken token = generateOpaqueToken();

        // Store session in Redis
        Session session = Session.create(user, token);
        sessionService.store(session);

        // Update last login
        User updatedUser = user.recordLogin();
        userRepository.save(updatedUser);

        // Publish event for other modules
        eventPublisher.publishEvent(new UserAuthenticatedEvent(
            user.id(),
            user.organizationId(),
            session.id(),
            Instant.now()
        ));

        return AuthenticationResult.success(token, session);
    }

    private OpaqueToken generateOpaqueToken() {
        // Generate cryptographically secure random token
        byte[] randomBytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);

        // Create hash with salt for storage
        String tokenValue = Base64.getUrlEncoder().encodeToString(randomBytes);
        String hashedToken = hashToken(tokenValue);

        return new OpaqueToken(tokenValue, hashedToken);
    }

    private String hashToken(String token) {
        // SHA-256 with salt as per constitutional requirement
        String salt = generateSalt();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hashedBytes = digest.digest(token.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);
    }
}
```

### Multi-Tenancy Support
```java
package com.platform.user.multitenancy;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract token from request
            String token = extractToken(request);

            if (token != null) {
                // Load session from Redis
                Session session = sessionService.loadByToken(token);

                if (session != null && session.isValid()) {
                    // Set tenant context for this request
                    TenantContext.setCurrentTenant(session.organizationId());
                    SecurityContextHolder.getContext().setAuthentication(
                        createAuthentication(session)
                    );
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // Clear tenant context after request
            TenantContext.clear();
        }
    }

    private Authentication createAuthentication(Session session) {
        return new TenantAuthentication(
            session.userId(),
            session.organizationId(),
            session.roles(),
            true
        );
    }
}

@Component
public class TenantContext {

    private static final ThreadLocal<OrganizationId> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(OrganizationId organizationId) {
        currentTenant.set(organizationId);
    }

    public static OrganizationId getCurrentTenant() {
        OrganizationId tenant = currentTenant.get();
        if (tenant == null) {
            throw new NoTenantContextException("No tenant context available");
        }
        return tenant;
    }

    public static void clear() {
        currentTenant.remove();
    }
}
```

### User Invitation System
```java
package com.platform.user.invitation;

@Service
@Transactional
public class UserInvitationService {

    private final InvitationRepository invitationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InvitationResult inviteUser(InviteUserCommand command) {
        // Validate organization context
        OrganizationId organizationId = TenantContext.getCurrentTenant();

        // Check if user already exists
        if (userRepository.existsByEmail(command.email())) {
            return InvitationResult.userAlreadyExists();
        }

        // Create invitation
        Invitation invitation = Invitation.create(
            command.email(),
            organizationId,
            command.role(),
            command.invitedBy()
        );

        invitationRepository.save(invitation);

        // Publish event for email service
        eventPublisher.publishEvent(new UserInvitedEvent(
            invitation.id(),
            invitation.email(),
            invitation.organizationId(),
            invitation.invitationToken(),
            invitation.expiresAt()
        ));

        return InvitationResult.success(invitation);
    }

    public AcceptInvitationResult acceptInvitation(
            String invitationToken,
            UserRegistrationData registrationData) {

        Invitation invitation = invitationRepository
            .findByToken(invitationToken)
            .orElseThrow(() -> new InvalidInvitationException("Invalid invitation token"));

        if (invitation.isExpired()) {
            throw new ExpiredInvitationException("Invitation has expired");
        }

        // Create user account
        User user = User.create(
            invitation.email(),
            registrationData.username(),
            registrationData.profile(),
            invitation.organizationId()
        );

        user = user.assignRole(invitation.role());
        userRepository.save(user);

        // Mark invitation as accepted
        invitation = invitation.accept(user.id());
        invitationRepository.save(invitation);

        // Publish event
        eventPublisher.publishEvent(new InvitationAcceptedEvent(
            invitation.id(),
            user.id(),
            invitation.organizationId()
        ));

        return AcceptInvitationResult.success(user);
    }
}
```

### GDPR Compliance Implementation
```java
package com.platform.user.gdpr;

@Service
@Transactional
public class GdprComplianceService {

    private final UserRepository userRepository;
    private final DataExportService dataExportService;
    private final DataAnonymizationService anonymizationService;
    private final ApplicationEventPublisher eventPublisher;

    public DataExportResult exportUserData(UserId userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Collect all user data across modules via events
        UserDataExportRequest request = new UserDataExportRequest(
            userId,
            user.organizationId(),
            UUID.randomUUID()
        );

        eventPublisher.publishEvent(request);

        // Wait for responses from other modules
        CompletableFuture<UserDataExport> exportFuture =
            dataExportService.collectUserData(request);

        UserDataExport export = exportFuture.get(30, TimeUnit.SECONDS);

        // Generate downloadable file
        ExportFile file = dataExportService.generateExportFile(export);

        // Audit the export
        eventPublisher.publishEvent(new DataExportedEvent(
            userId,
            file.id(),
            Instant.now()
        ));

        return DataExportResult.success(file);
    }

    public DataErasureResult eraseUserData(UserId userId, ErasureReason reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Publish erasure request to all modules
        DataErasureRequest request = new DataErasureRequest(
            userId,
            user.organizationId(),
            reason,
            UUID.randomUUID()
        );

        eventPublisher.publishEvent(request);

        // Anonymize user data (keep for legal requirements)
        User anonymizedUser = anonymizationService.anonymizeUser(user);
        userRepository.save(anonymizedUser);

        // Wait for confirmation from all modules
        CompletableFuture<ErasureConfirmation> confirmationFuture =
            waitForErasureConfirmations(request);

        ErasureConfirmation confirmation = confirmationFuture.get(30, TimeUnit.SECONDS);

        // Audit the erasure
        eventPublisher.publishEvent(new DataErasedEvent(
            userId,
            reason,
            confirmation,
            Instant.now()
        ));

        return DataErasureResult.success(confirmation);
    }
}
```

## Event-Driven Module Communication

### Published Events
```java
package com.platform.user.events;

public record UserRegisteredEvent(
    UserId userId,
    OrganizationId organizationId,
    Email email,
    Instant registeredAt
) implements DomainEvent {}

public record UserAuthenticatedEvent(
    UserId userId,
    OrganizationId organizationId,
    SessionId sessionId,
    Instant authenticatedAt
) implements DomainEvent {}

public record OrganizationCreatedEvent(
    OrganizationId organizationId,
    UserId ownerId,
    String name,
    Instant createdAt
) implements DomainEvent {}

public record UserRoleChangedEvent(
    UserId userId,
    OrganizationId organizationId,
    Set<Role> oldRoles,
    Set<Role> newRoles,
    UserId changedBy,
    Instant changedAt
) implements DomainEvent {}
```

### Event Listeners
```java
package com.platform.user.listeners;

@Component
@Slf4j
public class PaymentEventListener {

    private final UserService userService;

    @EventListener
    @Async
    public void onSubscriptionActivated(SubscriptionActivatedEvent event) {
        log.info("Subscription activated for organization: {}", event.organizationId());

        // Update organization status if needed
        userService.updateOrganizationSubscription(
            event.organizationId(),
            event.subscriptionTier()
        );
    }

    @EventListener
    @Async
    public void onPaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed for organization: {}", event.organizationId());

        // Notify organization owner
        userService.notifyPaymentFailure(
            event.organizationId(),
            event.failureReason()
        );
    }
}
```

## Testing the User Module

### Integration Tests with TestContainers
```java
@SpringBootTest
@Testcontainers
class UserModuleIntegrationTest extends BaseIntegrationTest {

    @Test
    void testUserRegistrationFlow() {
        // Create organization first
        Organization org = organizationService.createOrganization(
            "Test Company",
            testUserId,
            testBillingDetails
        );

        // Register user
        UserRegistrationCommand command = new UserRegistrationCommand(
            "test@example.com",
            "testuser",
            createTestProfile(),
            org.id()
        );

        User user = userService.registerUser(command);

        // Verify user created
        assertThat(user).isNotNull();
        assertThat(user.email()).isEqualTo(command.email());
        assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);

        // Verify event published
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(eventCaptor).capture(UserRegisteredEvent.class);
        });
    }

    @Test
    void testMultiTenancyIsolation() {
        // Create two organizations
        Organization org1 = createTestOrganization("Org1");
        Organization org2 = createTestOrganization("Org2");

        // Create users in different organizations
        User user1 = createUserInOrganization(org1.id());
        User user2 = createUserInOrganization(org2.id());

        // Set tenant context to org1
        TenantContext.setCurrentTenant(org1.id());

        // Should only see org1 users
        List<User> org1Users = userService.listOrganizationUsers();
        assertThat(org1Users).contains(user1);
        assertThat(org1Users).doesNotContain(user2);

        // Switch context to org2
        TenantContext.setCurrentTenant(org2.id());

        // Should only see org2 users
        List<User> org2Users = userService.listOrganizationUsers();
        assertThat(org2Users).contains(user2);
        assertThat(org2Users).doesNotContain(user1);
    }
}
```

---

**Agent Version**: 1.0.0
**Module**: User Management
**Constitutional Compliance**: Required

Use this agent for all user management, authentication, organization setup, and multi-tenancy operations while maintaining strict constitutional compliance and GDPR requirements.