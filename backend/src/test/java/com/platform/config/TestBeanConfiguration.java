package com.platform.config;

import com.platform.audit.internal.AuditEventRepository;
import com.platform.audit.internal.AuditLogExportRepository;
import com.platform.audit.internal.AuditLogViewRepository;
import com.platform.audit.internal.ComplianceRepository;
import com.platform.audit.internal.SecurityAnalyticsRepository;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditLogPermissionService;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;

/**
 * Test configuration that provides mocked beans for testing.
 * Extends BaseTestConfiguration to inherit common infrastructure beans.
 * This configuration is activated when running tests to ensure
 * all required dependencies are available in the test context.
 */
@TestConfiguration
@Profile("test")
@TestPropertySource("classpath:application-test.yml")
public class TestBeanConfiguration extends BaseTestConfiguration {

    @MockBean
    private AuditEventRepository auditEventRepository;

    @MockBean
    private AuditLogExportRepository auditLogExportRepository;

    @MockBean
    private AuditLogViewRepository auditLogViewRepository;

    @MockBean
    private ComplianceRepository complianceRepository;

    @MockBean
    private SecurityAnalyticsRepository securityAnalyticsRepository;

    @MockBean
    private AuditLogExportService auditLogExportService;

    @MockBean
    private AuditLogViewService auditLogViewService;

    @MockBean
    private AuditLogPermissionService auditLogPermissionService;

    @MockBean
    private AuditRequestValidator auditRequestValidator;
}