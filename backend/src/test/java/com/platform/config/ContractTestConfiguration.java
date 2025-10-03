package com.platform.config;

import com.platform.audit.internal.AuditEvent;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.audit.internal.AuditLogExportRepository;
import com.platform.audit.internal.AuditLogExportRequest;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogViewRepository;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditRequestValidator;
import com.platform.audit.internal.ComplianceRepository;
import com.platform.audit.internal.SecurityAnalyticsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Test configuration for contract tests.
 * Extends BaseTestConfiguration to inherit common infrastructure beans.
 * Mocks repositories but uses real service implementations.
 * This allows contract tests to run without needing full JPA/database setup.
 */
@TestConfiguration
@Profile("contract-test")
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
public class ContractTestConfiguration extends BaseTestConfiguration {

    // Use @MockitoBean to create repository mocks that won't trigger JPA proxy creation
    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @MockitoBean
    private AuditLogViewRepository auditLogViewRepository;

    @MockitoBean
    private AuditLogExportRepository auditLogExportRepository;

    @MockitoBean
    private ComplianceRepository complianceRepository;

    @MockitoBean
    private SecurityAnalyticsRepository securityAnalyticsRepository;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }

    // Mock EntityManagerFactory to prevent JPA repository proxy creation issues
    @Bean
    @Primary
    public EntityManagerFactory entityManagerFactory() {
        return Mockito.mock(EntityManagerFactory.class);
    }

    // Services - use real implementations with mocked repositories
    @Bean
    public AuditLogViewService auditLogViewService(AuditLogViewRepository auditLogViewRepository) {
        return new AuditLogViewService(auditLogViewRepository);
    }

    @Bean
    public AuditLogExportService auditLogExportService(
            AuditLogExportRepository exportRepository,
            AuditLogViewRepository auditLogRepository) {
        return new AuditLogExportService(exportRepository, auditLogRepository);
    }

    @Bean
    public AuditRequestValidator auditRequestValidator() {
        return new AuditRequestValidator();
    }

    /**
     * Configure mock repository behaviors after all beans are created.
     */
    @PostConstruct
    public void configureMocks() {
        // Configure AuditLogViewRepository
        UUID sampleId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        AuditEvent sampleEvent = createMockAuditEvent(sampleId, "user.login");

        // Also configure for ID used in failure test
        UUID failedActionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        AuditEvent failedEvent = createMockAuditEvent(failedActionId, "user.login");
        setFieldValue(failedEvent, "description", "Failed login attempt");
        Map<String, Object> errorDetails = new java.util.HashMap<>();
        errorDetails.put("error", "Invalid credentials");
        errorDetails.put("resourceId", failedActionId.toString());
        setFieldValue(failedEvent, "details", errorDetails);

        // Configure findById for ANY UUID - return sample event
        Mockito.when(auditLogViewRepository.findById(Mockito.any(UUID.class)))
            .thenAnswer(invocation -> {
                UUID id = invocation.getArgument(0);
                // Return sample event for standard ID
                if (id.equals(sampleId)) {
                    return Optional.of(sampleEvent);
                }
                // Return failed event for failure test ID
                if (id.equals(failedActionId)) {
                    return Optional.of(failedEvent);
                }
                // Return empty for non-existent ID
                UUID nonExistentId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
                if (id.equals(nonExistentId)) {
                    return Optional.empty();
                }
                // For other IDs, return empty
                return Optional.empty();
            });

        Mockito.when(auditLogViewRepository.count()).thenReturn(100L);
        Page<AuditEvent> emptyPage = new PageImpl<>(Collections.emptyList());
        Mockito.when(auditLogViewRepository.findAll(Mockito.any(Pageable.class)))
            .thenReturn(emptyPage);

        // Configure AuditLogExportRepository
        configureMockExportRepository();
    }

    private AuditEvent createMockAuditEvent(UUID id, String action) {
        AuditEvent event = new AuditEvent(id, action);
        setFieldValue(event, "id", id);
        setFieldValue(event, "resourceType", "USER");
        setFieldValue(event, "userEmail", "test@example.com");
        setFieldValue(event, "createdAt", java.time.Instant.now());
        setFieldValue(event, "correlationId", "test-correlation-id");
        setFieldValue(event, "ipAddress", "192.168.1.1");
        setFieldValue(event, "userAgent", "Mozilla/5.0");
        setFieldValue(event, "description", "Sample audit event");
        setFieldValue(event, "severity", "LOW");
        setFieldValue(event, "module", "test");
        // Set details map with resourceId
        Map<String, Object> details = new java.util.HashMap<>();
        details.put("resourceId", id.toString());
        setFieldValue(event, "details", details);
        return event;
    }

    private void configureMockExportRepository() {
        // Rate limiting check
        Mockito.when(auditLogExportRepository.countActiveExportsForUser(Mockito.any(UUID.class)))
            .thenReturn(0L);

        // Save export request
        Mockito.when(auditLogExportRepository.save(Mockito.any(AuditLogExportRequest.class)))
            .thenAnswer(invocation -> {
                AuditLogExportRequest request = invocation.getArgument(0);
                if (request.getId() == null) {
                    setFieldValue(request, "id", UUID.randomUUID());
                }
                return request;
            });

        // Mock export status lookups for various test scenarios
        configureMockExportStatuses();
    }

    private void configureMockExportStatuses() {
        // Sample export ID from test
        UUID sampleExportId = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        AuditLogExportRequest sampleExport = createMockExport(sampleExportId, AuditLogExportRequest.ExportStatus.PENDING);

        // Pending export
        UUID pendingId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        AuditLogExportRequest pendingExport = createMockExport(pendingId, AuditLogExportRequest.ExportStatus.PENDING);

        // Processing export
        UUID processingId = UUID.fromString("660e8400-e29b-41d4-a716-446655440002");
        AuditLogExportRequest processingExport = createMockExport(processingId, AuditLogExportRequest.ExportStatus.PROCESSING);
        setFieldValue(processingExport, "progressPercentage", 50.0);

        // Completed export
        UUID completedId = UUID.fromString("660e8400-e29b-41d4-a716-446655440003");
        AuditLogExportRequest completedExport = createMockExport(completedId, AuditLogExportRequest.ExportStatus.COMPLETED);
        setFieldValue(completedExport, "progressPercentage", 100.0);
        setFieldValue(completedExport, "downloadToken", "sample-download-token-123");
        setFieldValue(completedExport, "completedAt", java.time.Instant.now());
        setFieldValue(completedExport, "totalRecords", 100L);
        setFieldValue(completedExport, "filePath", "/tmp/export.csv");
        setFieldValue(completedExport, "fileSizeBytes", 2048L);
        setFieldValue(completedExport, "downloadExpiresAt", java.time.Instant.now().plusSeconds(3600));

        // Failed export
        UUID failedId = UUID.fromString("660e8400-e29b-41d4-a716-446655440004");
        AuditLogExportRequest failedExport = createMockExport(failedId, AuditLogExportRequest.ExportStatus.FAILED);
        setFieldValue(failedExport, "errorMessage", "Export failed due to error");

        // Another processing export for progress test
        UUID processing2Id = UUID.fromString("660e8400-e29b-41d4-a716-446655440005");
        AuditLogExportRequest processing2Export = createMockExport(processing2Id, AuditLogExportRequest.ExportStatus.PROCESSING);
        setFieldValue(processing2Export, "progressPercentage", 75.0);

        // Configure findById to return appropriate mocks
        Mockito.when(auditLogExportRepository.findById(sampleExportId)).thenReturn(Optional.of(sampleExport));
        Mockito.when(auditLogExportRepository.findById(pendingId)).thenReturn(Optional.of(pendingExport));
        Mockito.when(auditLogExportRepository.findById(processingId)).thenReturn(Optional.of(processingExport));
        Mockito.when(auditLogExportRepository.findById(completedId)).thenReturn(Optional.of(completedExport));
        Mockito.when(auditLogExportRepository.findById(failedId)).thenReturn(Optional.of(failedExport));
        Mockito.when(auditLogExportRepository.findById(processing2Id)).thenReturn(Optional.of(processing2Export));

        // Non-existent export
        UUID nonExistentId = UUID.fromString("660e8400-e29b-41d4-a716-446655440999");
        Mockito.when(auditLogExportRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Configure download token lookups
        configureMockDownloadTokens();
    }

    private void configureMockDownloadTokens() {
        String validToken = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz";
        String csvToken = validToken + "_csv";
        String jsonToken = validToken + "_json";
        String pdfToken = validToken + "_pdf";
        String largeToken = validToken + "_large";

        // Create completed exports for download tokens
        AuditLogExportRequest csvExport = createCompletedExport(csvToken, "CSV", "audit_export.csv");
        AuditLogExportRequest jsonExport = createCompletedExport(jsonToken, "JSON", "audit_export.json");
        AuditLogExportRequest pdfExport = createCompletedExport(pdfToken, "PDF", "audit_export.pdf");
        AuditLogExportRequest largeExport = createCompletedExport(largeToken, "CSV", "audit_export_large.csv");
        setFieldValue(largeExport, "totalRecords", 100000L); // Large file

        Mockito.when(auditLogExportRepository.findByDownloadToken(validToken)).thenReturn(Optional.of(csvExport));
        Mockito.when(auditLogExportRepository.findByDownloadToken(csvToken)).thenReturn(Optional.of(csvExport));
        Mockito.when(auditLogExportRepository.findByDownloadToken(jsonToken)).thenReturn(Optional.of(jsonExport));
        Mockito.when(auditLogExportRepository.findByDownloadToken(pdfToken)).thenReturn(Optional.of(pdfExport));
        Mockito.when(auditLogExportRepository.findByDownloadToken(largeToken)).thenReturn(Optional.of(largeExport));

        // Non-existent or invalid tokens
        Mockito.when(auditLogExportRepository.findByDownloadToken(Mockito.startsWith("nonexistent_")))
            .thenReturn(Optional.empty());
        Mockito.when(auditLogExportRepository.findByDownloadToken(Mockito.eq("invalid_token_format")))
            .thenReturn(Optional.empty());
    }

    private AuditLogExportRequest createMockExport(UUID id, AuditLogExportRequest.ExportStatus status) {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        AuditLogExportRequest export = new AuditLogExportRequest(
            userId,
            userId,
            AuditLogExportRequest.ExportFormat.CSV
        );
        setFieldValue(export, "id", id);
        setFieldValue(export, "status", status);
        setFieldValue(export, "createdAt", java.time.Instant.now());
        return export;
    }

    private AuditLogExportRequest createCompletedExport(String token, String format, String filename) {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        AuditLogExportRequest export = new AuditLogExportRequest(
            userId,
            userId,
            AuditLogExportRequest.ExportFormat.valueOf(format)
        );
        setFieldValue(export, "id", UUID.randomUUID());
        setFieldValue(export, "status", AuditLogExportRequest.ExportStatus.COMPLETED);
        setFieldValue(export, "downloadToken", token);
        setFieldValue(export, "completedAt", java.time.Instant.now());
        setFieldValue(export, "totalRecords", 100L);
        setFieldValue(export, "filePath", "/tmp/" + filename);
        setFieldValue(export, "fileSizeBytes", 2048L);
        setFieldValue(export, "downloadExpiresAt", java.time.Instant.now().plusSeconds(3600));
        setFieldValue(export, "createdAt", java.time.Instant.now());
        return export;
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            // Try parent class
            try {
                java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                // Ignore - field might not exist or be settable
            }
        }
    }
}