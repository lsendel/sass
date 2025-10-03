package com.platform.fixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for test data fixtures - no Spring context required.
 */
class TestDataValidationTest {

    @Test
    void shouldCreateValidAuditEvent() {
        var auditEvent = AuditTestDataFixtures.createAuditEvent();

        assertNotNull(auditEvent);
        assertNotNull(auditEvent.getId());
        assertNotNull(auditEvent.getCreatedAt());
        assertEquals("192.168.1.1", auditEvent.getIpAddress());
        assertNotNull(auditEvent.getDetails());
        assertFalse(auditEvent.getDetails().isEmpty());
    }

    @Test
    void shouldCreateValidAuditLogEntryDTO() {
        var dto = AuditTestDataFixtures.createAuditLogEntryDTO();

        assertNotNull(dto);
        assertNotNull(dto.id());
        assertNotNull(dto.timestamp());
        assertEquals("user123", dto.actorName());
        assertEquals("LOGIN", dto.actionType());
        assertEquals("SUCCESS", dto.outcome());
    }

    @Test
    void shouldCreateValidAuditLogDetailDTO() {
        var dto = AuditTestDataFixtures.createAuditLogDetailDTO();

        assertNotNull(dto);
        assertNotNull(dto.id());
        assertNotNull(dto.timestamp());
        assertEquals("user123", dto.actorName());
        assertEquals("USER_LOGIN", dto.actionType());
        assertNotNull(dto.metadata());
    }

    @Test
    void shouldCreateMultipleUniqueEvents() {
        var events = AuditTestDataFixtures.createAuditEvents(3);

        assertEquals(3, events.size());

        // Check that all events have unique IDs
        var uniqueIds = events.stream()
            .map(event -> event.getId().toString())
            .distinct()
            .count();
        assertEquals(3, uniqueIds);
    }

    @Test
    void shouldCreateSearchFilters() {
        var filters = AuditTestDataFixtures.createSearchFilters();

        assertNotNull(filters);
        assertEquals(4, filters.size());
        assertTrue(filters.containsKey("userId"));
        assertTrue(filters.containsKey("action"));
        assertTrue(filters.containsKey("startDate"));
        assertTrue(filters.containsKey("endDate"));

        assertEquals("user123", filters.get("userId"));
        assertEquals("USER_LOGIN", filters.get("action"));
    }
}