package com.platform.audit.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.platform.audit.internal.AuditEventRepository;
import com.platform.shared.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

/**
 * Configuration that exports the AuditService implementation to other modules.
 * This follows Spring Modulith patterns for cross-module dependencies.
 */
@Configuration
public class AuditConfiguration {

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