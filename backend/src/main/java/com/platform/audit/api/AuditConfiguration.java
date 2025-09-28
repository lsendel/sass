package com.platform.audit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.shared.audit.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the audit module.
 *
 * <p>This class is responsible for creating and exposing the {@link AuditService} bean,
 * making it available for dependency injection in other modules. It follows the Spring Modulith
 * pattern of defining beans in an `api` package to be exposed externally.
 * </p>
 */
@Configuration
public class AuditConfiguration {

    /**
     * Creates and configures the {@link AuditService} bean.
     *
     * <p>This bean provides the central service for recording and managing audit events
     * throughout the application.
     * </p>
     *
     * @param auditEventRepository The repository for persisting audit events.
     * @param objectMapper The Jackson object mapper for serializing event details.
     * @param enablePiiRedaction A boolean flag to control the redaction of Personally Identifiable Information (PII)
     *                           in audit logs. Defaults to {@code true}.
     * @param retentionDays The number of days to retain audit logs before they are eligible for archival or deletion.
     *                      Defaults to 2555 days (approximately 7 years).
     * @return A configured instance of {@link com.platform.audit.internal.AuditService}.
     */
    @Bean
    public AuditService auditService(
            AuditEventRepository auditEventRepository,
            ObjectMapper objectMapper,
            @Value("${app.audit.enable-pii-redaction:true}") boolean enablePiiRedaction,
            @Value("${app.audit.retention-days:2555}") int retentionDays) {
        return new com.platform.audit.internal.AuditService(
            auditEventRepository,
            objectMapper,
            enablePiiRedaction,
            retentionDays
        );
    }
}