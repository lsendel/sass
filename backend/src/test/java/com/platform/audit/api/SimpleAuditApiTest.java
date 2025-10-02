package com.platform.audit.api;

import com.platform.config.AuditTestConfiguration;
import com.platform.fixtures.AuditTestDataFixtures;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify our test infrastructure works.
 * Spring Security is enabled with proper test configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(AuditTestConfiguration.class)
@WithMockUser(roles = "USER")
@Disabled("Simple test - disabled in favor of integration tests with real databases")
class SimpleAuditApiTest {

    @Test
    void shouldCreateTestData() {
        // Test that our test fixtures work
        var auditEvent = AuditTestDataFixtures.createAuditEvent();
        assertNotNull(auditEvent);

        var auditLogEntry = AuditTestDataFixtures.createAuditLogEntryDTO();
        assertNotNull(auditLogEntry);
        assertNotNull(auditLogEntry.id());

        var auditLogDetail = AuditTestDataFixtures.createAuditLogDetailDTO();
        assertNotNull(auditLogDetail);
        assertNotNull(auditLogDetail.id());
    }

    @Test
    void shouldCreateMultipleAuditEvents() {
        var events = AuditTestDataFixtures.createAuditEvents(5);
        assertEquals(5, events.size());

        // Verify each event is unique
        var uniqueIds = events.stream()
            .map(event -> event.getId())
            .distinct()
            .count();
        assertEquals(5, uniqueIds);
    }

    @Test
    void shouldCreateSearchFilters() {
        var filters = AuditTestDataFixtures.createSearchFilters();
        assertNotNull(filters);
        assertTrue(filters.containsKey("userId"));
        assertTrue(filters.containsKey("action"));
        assertTrue(filters.containsKey("startDate"));
        assertTrue(filters.containsKey("endDate"));
    }
}