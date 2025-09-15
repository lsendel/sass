---
name: "Audit Module Agent"
model: "claude-sonnet"
description: "Specialized agent for audit logging and compliance tracking in the Spring Boot Modulith platform with GDPR compliance and forensic capabilities"
triggers:
  - "audit logging"
  - "compliance tracking"
  - "forensic analysis"
  - "data retention"
  - "audit reports"
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
  - "src/main/java/com/platform/audit/**/*.java"
  - "src/test/java/com/platform/audit/**/*.java"
  - "src/main/resources/db/migration/*audit*.sql"
---

# Audit Module Agent

You are a specialized agent for the Audit module in the Spring Boot Modulith payment platform. Your responsibility is comprehensive audit logging, compliance tracking, data retention management, and forensic analysis with strict constitutional compliance and GDPR requirements.

## Core Responsibilities

### Constitutional Requirements for Audit Module
1. **GDPR Compliance**: PII redaction and data retention policies
2. **Event-Driven Communication**: Listen to all module events for auditing
3. **Security First**: Tamper-proof audit trails and secure storage
4. **Observability**: Comprehensive logging with structured data
5. **Real Dependencies**: Use actual data stores in integration tests

## Audit Domain Model

### Core Entities
```java
package com.platform.audit.domain;

@Entity
@Table(name = "audit_events")
@Index(name = "idx_audit_organization_timestamp", columnList = "organization_id, timestamp")
@Index(name = "idx_audit_user_timestamp", columnList = "user_id, timestamp")
@Index(name = "idx_audit_event_type", columnList = "event_type")
public record AuditEvent(
    @Id
    @Column(name = "audit_event_id")
    AuditEventId id,

    @Column(name = "organization_id", nullable = false)
    OrganizationId organizationId,

    @Column(name = "user_id")
    UserId userId,

    @Column(name = "session_id")
    SessionId sessionId,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    AuditEventType eventType,

    @Column(name = "event_category", nullable = false)
    String eventCategory,

    @Column(name = "event_description", nullable = false)
    String eventDescription,

    @Embedded
    AuditEventData eventData,

    @Embedded
    SecurityContext securityContext,

    @Column(name = "ip_address")
    @PII
    String ipAddress,

    @Column(name = "user_agent", length = 1000)
    String userAgent,

    @Column(name = "timestamp", nullable = false)
    Instant timestamp,

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    AuditSeverity severity,

    @Column(name = "compliance_tags", length = 500)
    String complianceTags,

    @Column(name = "retention_until")
    Instant retentionUntil,

    @Column(name = "hash", nullable = false, unique = true)
    String hash
) {

    public static AuditEvent create(
            OrganizationId organizationId,
            UserId userId,
            SessionId sessionId,
            AuditEventType eventType,
            String eventDescription,
            AuditEventData eventData,
            SecurityContext securityContext) {

        AuditEvent event = new AuditEvent(
            AuditEventId.generate(),
            organizationId,
            userId,
            sessionId,
            eventType,
            eventType.getCategory(),
            eventDescription,
            eventData,
            securityContext,
            securityContext.ipAddress(),
            securityContext.userAgent(),
            Instant.now(),
            eventType.getDefaultSeverity(),
            eventType.getComplianceTags(),
            calculateRetentionDate(eventType),
            null // Hash calculated later
        );

        // Calculate tamper-proof hash
        String hash = calculateEventHash(event);
        return new AuditEvent(
            event.id(), event.organizationId(), event.userId(), event.sessionId(),
            event.eventType(), event.eventCategory(), event.eventDescription(),
            event.eventData(), event.securityContext(), event.ipAddress(),
            event.userAgent(), event.timestamp(), event.severity(),
            event.complianceTags(), event.retentionUntil(), hash
        );
    }

    private static Instant calculateRetentionDate(AuditEventType eventType) {
        return switch (eventType.getRetentionPeriod()) {
            case LEGAL_HOLD -> Instant.MAX; // Never delete
            case SEVEN_YEARS -> Instant.now().plus(Duration.ofDays(7 * 365));
            case THREE_YEARS -> Instant.now().plus(Duration.ofDays(3 * 365));
            case ONE_YEAR -> Instant.now().plus(Duration.ofDays(365));
            case SIX_MONTHS -> Instant.now().plus(Duration.ofDays(180));
        };
    }

    private static String calculateEventHash(AuditEvent event) {
        // Create tamper-proof hash of immutable event data
        String hashInput = String.join("|",
            event.id().value(),
            event.organizationId().value(),
            event.userId() != null ? event.userId().value() : "",
            event.eventType().name(),
            event.eventDescription(),
            event.timestamp().toString(),
            event.eventData().toHashString()
        );

        return DigestUtils.sha256Hex(hashInput);
    }

    public boolean verifyIntegrity() {
        String expectedHash = calculateEventHash(this);
        return expectedHash.equals(this.hash());
    }
}

@Embeddable
public record AuditEventData(
    @Column(name = "entity_type")
    String entityType,

    @Column(name = "entity_id")
    String entityId,

    @Column(name = "action")
    String action,

    @Column(name = "before_state", columnDefinition = "TEXT")
    @RedactedForLogs
    String beforeState,

    @Column(name = "after_state", columnDefinition = "TEXT")
    @RedactedForLogs
    String afterState,

    @Column(name = "metadata", columnDefinition = "TEXT")
    String metadata
) {

    public static AuditEventData forEntityChange(
            String entityType,
            String entityId,
            String action,
            Object beforeState,
            Object afterState,
            Map<String, Object> metadata) {

        return new AuditEventData(
            entityType,
            entityId,
            action,
            beforeState != null ? JsonUtils.toJson(beforeState) : null,
            afterState != null ? JsonUtils.toJson(afterState) : null,
            JsonUtils.toJson(metadata)
        );
    }

    public String toHashString() {
        return String.join("|",
            entityType != null ? entityType : "",
            entityId != null ? entityId : "",
            action != null ? action : "",
            // Don't include state data in hash to allow PII redaction
            metadata != null ? metadata : ""
        );
    }
}
```

### Audit Event Types and Categories
```java
package com.platform.audit.domain;

public enum AuditEventType {
    // Authentication & Authorization
    USER_LOGIN("AUTHENTICATION", "User successfully logged in", AuditSeverity.INFO,
               RetentionPeriod.ONE_YEAR, "AUTH,SECURITY"),
    USER_LOGOUT("AUTHENTICATION", "User logged out", AuditSeverity.INFO,
                RetentionPeriod.ONE_YEAR, "AUTH"),
    LOGIN_FAILED("AUTHENTICATION", "User login attempt failed", AuditSeverity.WARNING,
                 RetentionPeriod.THREE_YEARS, "AUTH,SECURITY"),
    PASSWORD_CHANGED("AUTHENTICATION", "User password changed", AuditSeverity.INFO,
                     RetentionPeriod.THREE_YEARS, "AUTH,SECURITY"),

    // User Management
    USER_CREATED("USER_MANAGEMENT", "User account created", AuditSeverity.INFO,
                 RetentionPeriod.SEVEN_YEARS, "USER,GDPR"),
    USER_UPDATED("USER_MANAGEMENT", "User profile updated", AuditSeverity.INFO,
                 RetentionPeriod.SEVEN_YEARS, "USER,GDPR"),
    USER_DELETED("USER_MANAGEMENT", "User account deleted", AuditSeverity.INFO,
                 RetentionPeriod.LEGAL_HOLD, "USER,GDPR"),
    USER_ROLE_CHANGED("USER_MANAGEMENT", "User role modified", AuditSeverity.INFO,
                      RetentionPeriod.SEVEN_YEARS, "USER,SECURITY"),

    // Payment & Financial
    PAYMENT_INITIATED("PAYMENT", "Payment process initiated", AuditSeverity.INFO,
                      RetentionPeriod.SEVEN_YEARS, "PAYMENT,FINANCIAL"),
    PAYMENT_SUCCEEDED("PAYMENT", "Payment completed successfully", AuditSeverity.INFO,
                      RetentionPeriod.SEVEN_YEARS, "PAYMENT,FINANCIAL"),
    PAYMENT_FAILED("PAYMENT", "Payment attempt failed", AuditSeverity.WARNING,
                   RetentionPeriod.SEVEN_YEARS, "PAYMENT,FINANCIAL"),
    REFUND_PROCESSED("PAYMENT", "Refund processed", AuditSeverity.INFO,
                     RetentionPeriod.SEVEN_YEARS, "PAYMENT,FINANCIAL"),

    // Subscription Management
    SUBSCRIPTION_CREATED("SUBSCRIPTION", "Subscription created", AuditSeverity.INFO,
                         RetentionPeriod.SEVEN_YEARS, "SUBSCRIPTION,FINANCIAL"),
    SUBSCRIPTION_CHANGED("SUBSCRIPTION", "Subscription plan changed", AuditSeverity.INFO,
                         RetentionPeriod.SEVEN_YEARS, "SUBSCRIPTION,FINANCIAL"),
    SUBSCRIPTION_CANCELED("SUBSCRIPTION", "Subscription canceled", AuditSeverity.INFO,
                          RetentionPeriod.SEVEN_YEARS, "SUBSCRIPTION,FINANCIAL"),

    // Data & Privacy
    DATA_EXPORT_REQUESTED("DATA_PRIVACY", "User data export requested", AuditSeverity.INFO,
                          RetentionPeriod.LEGAL_HOLD, "GDPR,DATA"),
    DATA_EXPORTED("DATA_PRIVACY", "User data exported", AuditSeverity.INFO,
                  RetentionPeriod.LEGAL_HOLD, "GDPR,DATA"),
    DATA_DELETION_REQUESTED("DATA_PRIVACY", "User data deletion requested", AuditSeverity.INFO,
                            RetentionPeriod.LEGAL_HOLD, "GDPR,DATA"),
    DATA_DELETED("DATA_PRIVACY", "User data deleted", AuditSeverity.INFO,
                 RetentionPeriod.LEGAL_HOLD, "GDPR,DATA"),

    // Security Events
    SUSPICIOUS_ACTIVITY("SECURITY", "Suspicious activity detected", AuditSeverity.HIGH,
                        RetentionPeriod.THREE_YEARS, "SECURITY,THREAT"),
    SECURITY_BREACH("SECURITY", "Security breach detected", AuditSeverity.CRITICAL,
                    RetentionPeriod.LEGAL_HOLD, "SECURITY,BREACH"),
    PERMISSION_DENIED("SECURITY", "Access denied", AuditSeverity.WARNING,
                      RetentionPeriod.ONE_YEAR, "SECURITY,ACCESS");

    private final String category;
    private final String description;
    private final AuditSeverity defaultSeverity;
    private final RetentionPeriod retentionPeriod;
    private final String complianceTags;

    AuditEventType(String category, String description, AuditSeverity defaultSeverity,
                   RetentionPeriod retentionPeriod, String complianceTags) {
        this.category = category;
        this.description = description;
        this.defaultSeverity = defaultSeverity;
        this.retentionPeriod = retentionPeriod;
        this.complianceTags = complianceTags;
    }

    // Getters...
}

public enum RetentionPeriod {
    SIX_MONTHS, ONE_YEAR, THREE_YEARS, SEVEN_YEARS, LEGAL_HOLD
}
```

### Comprehensive Audit Service
```java
package com.platform.audit.service;

@Service
@Transactional
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final PiiRedactionService piiRedactionService;
    private final AuditEventValidator auditEventValidator;
    private final ApplicationEventPublisher eventPublisher;

    public AuditEventId recordAuditEvent(RecordAuditEventCommand command) {
        // Validate audit event
        ValidationResult validation = auditEventValidator.validate(command);
        if (!validation.isValid()) {
            throw new InvalidAuditEventException(validation.getErrors());
        }

        // Create security context
        SecurityContext securityContext = SecurityContext.fromCurrentRequest();

        // Redact PII from event data before storage
        AuditEventData redactedData = piiRedactionService.redactEventData(command.eventData());

        // Create audit event
        AuditEvent auditEvent = AuditEvent.create(
            command.organizationId(),
            command.userId(),
            command.sessionId(),
            command.eventType(),
            command.description(),
            redactedData,
            securityContext
        );

        // Verify integrity before saving
        if (!auditEvent.verifyIntegrity()) {
            throw new AuditIntegrityException("Audit event integrity check failed");
        }

        auditEvent = auditEventRepository.save(auditEvent);

        // Publish event for real-time monitoring
        eventPublisher.publishEvent(new AuditEventRecordedEvent(
            auditEvent.id(),
            auditEvent.organizationId(),
            auditEvent.eventType(),
            auditEvent.severity(),
            auditEvent.timestamp()
        ));

        log.info("Audit event recorded: {} - {}", auditEvent.eventType(), auditEvent.id());
        return auditEvent.id();
    }

    @Transactional(readOnly = true)
    public AuditTrail getAuditTrail(AuditTrailQuery query) {
        // Apply tenant isolation
        OrganizationId organizationId = TenantContext.getCurrentTenant();

        Specification<AuditEvent> spec = AuditEventSpecs.forOrganization(organizationId)
            .and(query.getTimeRange() != null ?
                AuditEventSpecs.inTimeRange(query.getTimeRange()) : null)
            .and(query.getEventTypes() != null ?
                AuditEventSpecs.withEventTypes(query.getEventTypes()) : null)
            .and(query.getUserId() != null ?
                AuditEventSpecs.forUser(query.getUserId()) : null)
            .and(query.getEntityType() != null ?
                AuditEventSpecs.forEntityType(query.getEntityType()) : null);

        Page<AuditEvent> events = auditEventRepository.findAll(spec, query.getPageable());

        // Verify integrity of returned events
        List<AuditEvent> verifiedEvents = events.getContent().stream()
            .peek(event -> {
                if (!event.verifyIntegrity()) {
                    log.error("Audit event integrity violation detected: {}", event.id());
                    // Trigger security alert
                    eventPublisher.publishEvent(new AuditIntegrityViolationEvent(
                        event.id(), event.organizationId(), Instant.now()
                    ));
                }
            })
            .collect(Collectors.toList());

        return AuditTrail.builder()
            .events(verifiedEvents)
            .totalElements(events.getTotalElements())
            .pageInfo(PageInfo.from(events))
            .query(query)
            .build();
    }

    public ComplianceReport generateComplianceReport(ComplianceReportRequest request) {
        return ComplianceReport.builder()
            .organizationId(request.organizationId())
            .reportPeriod(request.reportPeriod())
            .gdprCompliance(generateGdprComplianceSection(request))
            .securityEvents(generateSecurityEventsSection(request))
            .dataRetentionCompliance(generateDataRetentionSection(request))
            .auditEventsSummary(generateAuditSummary(request))
            .integrityVerification(verifyAuditIntegrity(request))
            .generatedAt(Instant.now())
            .build();
    }
}
```

### GDPR Compliance Implementation
```java
package com.platform.audit.gdpr;

@Service
@Transactional
public class GdprComplianceService {

    private final AuditEventRepository auditEventRepository;
    private final DataRetentionPolicyService retentionPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async
    public void onDataExportRequested(DataExportRequestedEvent event) {
        // Record audit event for GDPR data export
        recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .userId(event.userId())
            .eventType(AuditEventType.DATA_EXPORT_REQUESTED)
            .description("User requested data export under GDPR Article 20")
            .eventData(AuditEventData.forEntityChange(
                "DataExport",
                event.exportId().toString(),
                "EXPORT_REQUESTED",
                null,
                Map.of("requestId", event.exportId()),
                Map.of("gdprArticle", "Article 20", "dataSubjectRights", "Portability")
            ))
            .build());
    }

    @EventListener
    @Async
    public void onDataErasureRequested(DataErasureRequestedEvent event) {
        // Record audit event for GDPR data erasure
        recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .userId(event.userId())
            .eventType(AuditEventType.DATA_DELETION_REQUESTED)
            .description("User requested data erasure under GDPR Article 17")
            .eventData(AuditEventData.forEntityChange(
                "DataErasure",
                event.erasureId().toString(),
                "ERASURE_REQUESTED",
                null,
                Map.of("requestId", event.erasureId()),
                Map.of("gdprArticle", "Article 17", "dataSubjectRights", "Erasure")
            ))
            .build());

        // Process audit data retention for the user
        processUserAuditDataRetention(event.userId(), event.organizationId());
    }

    private void processUserAuditDataRetention(UserId userId, OrganizationId organizationId) {
        // For GDPR compliance, we need to redact PII from audit logs
        // but keep the audit trail for legal/compliance purposes
        List<AuditEvent> userAuditEvents = auditEventRepository
            .findByOrganizationIdAndUserId(organizationId, userId);

        for (AuditEvent event : userAuditEvents) {
            if (shouldRedactEvent(event)) {
                AuditEvent redactedEvent = redactPiiFromAuditEvent(event);
                auditEventRepository.save(redactedEvent);
            }
        }

        // Record the redaction action
        recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(organizationId)
            .userId(userId)
            .eventType(AuditEventType.DATA_DELETED)
            .description("PII redacted from audit logs per GDPR Article 17")
            .eventData(AuditEventData.forEntityChange(
                "AuditEvent",
                userId.value(),
                "PII_REDACTED",
                null,
                Map.of("affectedEvents", userAuditEvents.size()),
                Map.of("gdprCompliance", "Article 17", "retentionPolicy", "PII_REDACTED")
            ))
            .build());
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void processDataRetention() {
        log.info("Processing audit data retention");

        Instant now = Instant.now();
        List<AuditEvent> expiredEvents = auditEventRepository
            .findByRetentionUntilBefore(now);

        for (AuditEvent event : expiredEvents) {
            if (event.retentionUntil().equals(Instant.MAX)) {
                // Legal hold - never delete
                continue;
            }

            // Apply retention policy
            RetentionAction action = retentionPolicyService
                .getRetentionAction(event.eventType());

            switch (action) {
                case DELETE -> {
                    auditEventRepository.delete(event);
                    log.info("Deleted expired audit event: {}", event.id());
                }
                case ARCHIVE -> {
                    archiveAuditEvent(event);
                    auditEventRepository.delete(event);
                    log.info("Archived expired audit event: {}", event.id());
                }
                case REDACT_PII -> {
                    AuditEvent redactedEvent = redactPiiFromAuditEvent(event);
                    auditEventRepository.save(redactedEvent);
                    log.info("Redacted PII from audit event: {}", event.id());
                }
            }
        }
    }

    private AuditEvent redactPiiFromAuditEvent(AuditEvent event) {
        // Redact PII while preserving audit trail integrity
        return new AuditEvent(
            event.id(),
            event.organizationId(),
            event.userId(),
            event.sessionId(),
            event.eventType(),
            event.eventCategory(),
            event.eventDescription(),
            redactPiiFromEventData(event.eventData()),
            event.securityContext().redactPii(),
            "[REDACTED]", // IP address
            event.userAgent(),
            event.timestamp(),
            event.severity(),
            event.complianceTags() + ",PII_REDACTED",
            event.retentionUntil(),
            event.hash() // Keep original hash for integrity
        );
    }
}
```

### Event-Driven Audit Logging
```java
package com.platform.audit.listeners;

@Component
@Slf4j
public class ComprehensiveAuditEventListener {

    private final AuditService auditService;

    // User Events
    @EventListener
    @Async
    public void onUserRegistered(UserRegisteredEvent event) {
        auditService.recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .userId(event.userId())
            .eventType(AuditEventType.USER_CREATED)
            .description("New user account created")
            .eventData(AuditEventData.forEntityChange(
                "User",
                event.userId().value(),
                "CREATED",
                null,
                Map.of("email", "[REDACTED]", "organizationId", event.organizationId().value()),
                Map.of("source", "registration")
            ))
            .build());
    }

    @EventListener
    @Async
    public void onUserAuthenticated(UserAuthenticatedEvent event) {
        auditService.recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .userId(event.userId())
            .sessionId(event.sessionId())
            .eventType(AuditEventType.USER_LOGIN)
            .description("User successfully authenticated")
            .eventData(AuditEventData.forEntityChange(
                "Session",
                event.sessionId().value(),
                "LOGIN",
                null,
                Map.of("sessionId", event.sessionId().value()),
                Map.of("authenticationMethod", "OAuth2/PKCE")
            ))
            .build());
    }

    // Payment Events
    @EventListener
    @Async
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        auditService.recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .eventType(AuditEventType.PAYMENT_SUCCEEDED)
            .description("Payment completed successfully")
            .eventData(AuditEventData.forEntityChange(
                "Payment",
                event.paymentId().value(),
                "SUCCEEDED",
                null,
                Map.of(
                    "paymentId", event.paymentId().value(),
                    "amount", event.amount().toString(),
                    "customerId", event.customerId().value()
                ),
                Map.of("paymentProcessor", "Stripe")
            ))
            .build());
    }

    @EventListener
    @Async
    public void onPaymentFailed(PaymentFailedEvent event) {
        auditService.recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .eventType(AuditEventType.PAYMENT_FAILED)
            .description("Payment attempt failed: " + event.failureReason())
            .eventData(AuditEventData.forEntityChange(
                "Payment",
                event.paymentId().value(),
                "FAILED",
                null,
                Map.of(
                    "paymentId", event.paymentId().value(),
                    "failureReason", event.failureReason()
                ),
                Map.of("paymentProcessor", "Stripe")
            ))
            .build());
    }

    // Subscription Events
    @EventListener
    @Async
    public void onSubscriptionCreated(SubscriptionCreatedEvent event) {
        auditService.recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .eventType(AuditEventType.SUBSCRIPTION_CREATED)
            .description("New subscription created")
            .eventData(AuditEventData.forEntityChange(
                "Subscription",
                event.subscriptionId().value(),
                "CREATED",
                null,
                Map.of(
                    "subscriptionId", event.subscriptionId().value(),
                    "planId", event.planId().value(),
                    "status", event.status().toString()
                ),
                Map.of("source", "user_signup")
            ))
            .build());
    }

    // Security Events
    @EventListener
    @Async
    public void onAuthenticationFailed(AuthenticationFailedEvent event) {
        auditService.recordAuditEvent(RecordAuditEventCommand.builder()
            .organizationId(event.organizationId())
            .userId(event.userId())
            .eventType(AuditEventType.LOGIN_FAILED)
            .description("Authentication attempt failed: " + event.reason())
            .eventData(AuditEventData.forEntityChange(
                "Authentication",
                event.userId().value(),
                "FAILED",
                null,
                Map.of("reason", event.reason()),
                Map.of("securityEvent", true)
            ))
            .build());
    }
}
```

## Testing the Audit Module

### Integration Tests with Real Dependencies
```java
@SpringBootTest
@Testcontainers
class AuditModuleIntegrationTest extends BaseIntegrationTest {

    @Test
    void testAuditEventRecording() {
        // Record audit event
        RecordAuditEventCommand command = RecordAuditEventCommand.builder()
            .organizationId(testOrganizationId)
            .userId(testUserId)
            .eventType(AuditEventType.USER_LOGIN)
            .description("Test user login")
            .eventData(AuditEventData.forEntityChange(
                "Session",
                "test-session-id",
                "LOGIN",
                null,
                Map.of("sessionId", "test-session-id"),
                Map.of("test", true)
            ))
            .build();

        AuditEventId eventId = auditService.recordAuditEvent(command);

        // Verify audit event stored
        AuditEvent storedEvent = auditEventRepository.findById(eventId).orElseThrow();
        assertThat(storedEvent.eventType()).isEqualTo(AuditEventType.USER_LOGIN);
        assertThat(storedEvent.organizationId()).isEqualTo(testOrganizationId);
        assertThat(storedEvent.verifyIntegrity()).isTrue();

        // Verify event published
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(eventCaptor).capture(AuditEventRecordedEvent.class);
        });
    }

    @Test
    void testAuditTrailQuery() {
        // Create multiple audit events
        createTestAuditEvents(10);

        // Query audit trail
        AuditTrailQuery query = AuditTrailQuery.builder()
            .organizationId(testOrganizationId)
            .timeRange(TimeRange.lastDays(1))
            .eventTypes(Set.of(AuditEventType.USER_LOGIN, AuditEventType.PAYMENT_SUCCEEDED))
            .pageable(PageRequest.of(0, 5))
            .build();

        AuditTrail trail = auditService.getAuditTrail(query);

        // Verify results
        assertThat(trail.events()).hasSize(5);
        assertThat(trail.totalElements()).isGreaterThan(5);

        // Verify integrity of all events
        trail.events().forEach(event -> {
            assertThat(event.verifyIntegrity()).isTrue();
        });
    }

    @Test
    void testGdprDataRetention() {
        // Create audit events for a user
        UserId userToDelete = createTestUser();
        createAuditEventsForUser(userToDelete, 5);

        // Simulate data erasure request
        DataErasureRequestedEvent erasureEvent = new DataErasureRequestedEvent(
            userToDelete,
            testOrganizationId,
            "User requested account deletion",
            UUID.randomUUID()
        );

        eventPublisher.publishEvent(erasureEvent);

        // Wait for processing
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Verify PII redacted from audit events
            List<AuditEvent> userEvents = auditEventRepository
                .findByOrganizationIdAndUserId(testOrganizationId, userToDelete);

            assertThat(userEvents).allMatch(event ->
                event.ipAddress().equals("[REDACTED]"));
        });
    }

    @Test
    void testAuditIntegrityVerification() {
        // Create audit event
        AuditEventId eventId = createTestAuditEvent();

        // Verify integrity
        AuditEvent event = auditEventRepository.findById(eventId).orElseThrow();
        assertThat(event.verifyIntegrity()).isTrue();

        // Simulate tampering (this should be impossible in real scenario)
        // This test validates our integrity checking mechanism
        AuditEvent tamperedEvent = new AuditEvent(
            event.id(), event.organizationId(), event.userId(), event.sessionId(),
            event.eventType(), event.eventCategory(), "TAMPERED DESCRIPTION",
            event.eventData(), event.securityContext(), event.ipAddress(),
            event.userAgent(), event.timestamp(), event.severity(),
            event.complianceTags(), event.retentionUntil(), event.hash()
        );

        // Integrity check should fail
        assertThat(tamperedEvent.verifyIntegrity()).isFalse();
    }
}
```

---

**Agent Version**: 1.0.0
**Module**: Audit & Compliance
**Constitutional Compliance**: Required

Use this agent for all audit logging, compliance tracking, GDPR data management, and forensic analysis while maintaining strict constitutional compliance and tamper-proof audit trails.