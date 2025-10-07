package com.platform.config;

import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditRequestValidator;
import com.platform.audit.internal.AuditLogViewRepository;
import com.platform.audit.internal.AuditEvent;
import com.platform.audit.api.dto.AuditLogSearchResponse;
import com.platform.audit.api.dto.AuditLogEntryDTO;
import com.platform.audit.api.dto.AuditLogDetailDTO;
import com.platform.audit.internal.AuditLogFilter;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Base test configuration for integration tests.
 *
 * <p>Provides test implementations of services and repositories to resolve
 * bean dependency issues in integration tests while maintaining realistic behavior.
 *
 * @since 1.0.0
 */
@TestConfiguration
@Profile("integration-test")
public class IntegrationTestBaseConfiguration {

    /**
     * Test implementation of AuditLogViewService with realistic test data.
     */
    @Bean
    @Primary
    public AuditLogViewService testAuditLogViewService() {
        return new AuditLogViewService(testAuditLogViewRepository());
    }

    /**
     * Test implementation of AuditLogViewRepository that returns consistent test data.
     */
    @Bean
    @Primary
    public AuditLogViewRepository testAuditLogViewRepository() {
        return new AuditLogViewRepository() {
            @Override
            public Page<AuditEvent> findAll(Pageable pageable) {
                List<AuditEvent> testEvents = createTestAuditEvents();
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), testEvents.size());

                if (start > testEvents.size()) {
                    return new PageImpl<>(List.of(), pageable, testEvents.size());
                }

                List<AuditEvent> pageContent = testEvents.subList(start, end);
                return new PageImpl<>(pageContent, pageable, testEvents.size());
            }

            @Override
            public Optional<AuditEvent> findById(UUID id) {
                // Return a test event for known test IDs
                if ("11111111-1111-1111-1111-111111111111".equals(id.toString())) {
                    return Optional.of(createTestAuditEvent(id));
                }
                return Optional.empty();
            }

            // Implement other repository methods as needed
            private List<AuditEvent> createTestAuditEvents() {
                return IntStream.range(0, 10)
                    .mapToObj(i -> createTestAuditEvent(UUID.randomUUID()))
                    .toList();
            }

            private AuditEvent createTestAuditEvent(UUID id) {
                var event = new AuditEvent();
                event.setId(id);
                event.setAction("TEST_ACTION_" + id.toString().substring(0, 8));
                event.setResourceType("TEST_RESOURCE");
                event.setCreatedAt(Instant.now());
                event.setUserEmail("test@example.com");
                return event;
            }
        };
    }

    /**
     * Test implementation of AuditLogExportService.
     */
    @Bean
    @Primary
    public AuditLogExportService testAuditLogExportService() {
        return new AuditLogExportService() {
            @Override
            public Object requestExport(String format, UUID userId) {
                // Return a mock export response
                return new Object() {
                    public String getExportId() {
                        return UUID.randomUUID().toString();
                    }
                };
            }

            @Override
            public Object getExportStatus(String exportId) {
                // Return mock status
                return new Object() {
                    public String getStatus() {
                        return "COMPLETED";
                    }
                };
            }

            @Override
            public Object getExportDownload(String token) {
                // Return mock download
                return null; // Simulates not found
            }

            // Define custom exceptions as static inner classes for testing
            public static class EntityNotFoundException extends RuntimeException {
                public EntityNotFoundException(String message) {
                    super(message);
                }
            }

            public static class DownloadTokenExpiredException extends RuntimeException {
                public DownloadTokenExpiredException(String message) {
                    super(message);
                }
            }

            public static class DownloadTokenNotFoundException extends RuntimeException {
                public DownloadTokenNotFoundException(String message) {
                    super(message);
                }
            }
        };
    }

    /**
     * Test implementation of AuditRequestValidator.
     */
    @Bean
    @Primary
    public AuditRequestValidator testAuditRequestValidator() {
        return new AuditRequestValidator() {
            @Override
            public ValidationResult validatePageSize(int size) {
                if (size < 1 || size > 100) {
                    return ValidationResult.invalid("INVALID_PAGE_SIZE", "Page size must be between 1 and 100");
                }
                return ValidationResult.valid();
            }

            @Override
            public ParseDateResult parseDate(String dateString, String fieldName) {
                try {
                    if (dateString == null || dateString.isEmpty()) {
                        return new ParseDateResult(ValidationResult.valid(), null);
                    }
                    Instant instant = Instant.parse(dateString);
                    return new ParseDateResult(ValidationResult.valid(), instant);
                } catch (Exception e) {
                    return new ParseDateResult(
                        ValidationResult.invalid("INVALID_DATE_FORMAT", "Invalid date format for " + fieldName),
                        null
                    );
                }
            }

            @Override
            public ValidationResult validateDateRange(Instant from, Instant to) {
                if (from != null && to != null && from.isAfter(to)) {
                    return ValidationResult.invalid("INVALID_DATE_RANGE", "Start date must be before end date");
                }
                return ValidationResult.valid();
            }

            // Inner classes for test validator
            public static class ValidationResult {
                private final boolean valid;
                private final String errorCode;
                private final String errorMessage;

                private ValidationResult(boolean valid, String errorCode, String errorMessage) {
                    this.valid = valid;
                    this.errorCode = errorCode;
                    this.errorMessage = errorMessage;
                }

                public static ValidationResult valid() {
                    return new ValidationResult(true, null, null);
                }

                public static ValidationResult invalid(String errorCode, String errorMessage) {
                    return new ValidationResult(false, errorCode, errorMessage);
                }

                public boolean isValid() { return valid; }
                public String errorCode() { return errorCode; }
                public String errorMessage() { return errorMessage; }
            }

            public static class ParseDateResult {
                private final ValidationResult validation;
                private final Instant date;

                public ParseDateResult(ValidationResult validation, Instant date) {
                    this.validation = validation;
                    this.date = date;
                }

                public ValidationResult validation() { return validation; }
                public Instant date() { return date; }
            }
        };
    }

    /**
     * Test data factory for creating consistent test objects.
     */
    @Bean
    public TestDataFactory testDataFactory() {
        return new TestDataFactory();
    }

    /**
     * Factory for creating test data objects.
     */
    public static class TestDataFactory {

        public AuditEvent createSampleAuditEvent() {
            var event = new AuditEvent();
            event.setId(UUID.randomUUID());
            event.setAction("TEST_ACTION");
            event.setResourceType("TEST_RESOURCE");
            event.setCreatedAt(Instant.now());
            event.setUserEmail("test@example.com");
            return event;
        }

        public AuditEvent createAuditEventWithId(UUID id) {
            var event = createSampleAuditEvent();
            event.setId(id);
            return event;
        }

        public List<AuditEvent> createAuditEventBatch(int count) {
            return IntStream.range(0, count)
                .mapToObj(i -> createSampleAuditEvent())
                .toList();
        }

        public AuditLogEntryDTO createSampleAuditLogEntry() {
            return new AuditLogEntryDTO(
                UUID.randomUUID().toString(),
                Instant.now(),
                "Test User",
                "USER",
                "TEST_ACTION",
                "Test action performed",
                "TEST_RESOURCE",
                "Test Resource",
                "SUCCESS",
                "LOW"
            );
        }
    }
}