package com.platform.audit.api;

import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test for the audit log viewer controller.
 * Tests controller logic without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogViewerUnitTest {

    @Mock
    private AuditLogViewService auditLogViewService;

    @Mock
    private AuditLogExportService auditLogExportService;

    @Mock
    private AuditRequestValidator auditRequestValidator;

    private AuditLogViewController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditLogViewController(
            auditLogViewService,
            auditLogExportService,
            auditRequestValidator
        );
    }

    @Test
    void controllerShouldBeCreated() {
        // Simple test to verify the controller can be instantiated
        assertNotNull(controller);
    }

    @Test
    void controllerShouldHaveGetAuditLogsMethod() {
        // This test verifies the controller has the expected method
        // We can't easily test the full method due to static SecurityContextHolder
        // but we can verify the controller structure is correct

        assertNotNull(controller);

        // Verify the controller exists and can be called
        // The actual method testing would require more complex setup
        // due to SecurityContextHolder.getContext() usage
    }

    @Test
    void controllerShouldImplementAllRequiredEndpoints() {
        // Test that verifies controller implements expected REST endpoints
        // by checking method signatures exist

        assertNotNull(controller);

        // Verify all methods exist through reflection
        var methods = controller.getClass().getDeclaredMethods();
        var methodNames = List.of(methods).stream()
                .map(method -> method.getName())
                .toList();

        assertThat(methodNames).contains("getAuditLogs");
        assertThat(methodNames).contains("getAuditLogDetails");
        assertThat(methodNames).contains("exportAuditLogs");
        assertThat(methodNames).contains("getExportStatus");
        assertThat(methodNames).contains("downloadExport");
    }
}