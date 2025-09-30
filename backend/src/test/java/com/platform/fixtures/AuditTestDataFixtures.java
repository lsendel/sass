package com.platform.fixtures;

import com.platform.audit.internal.AuditEvent;
import com.platform.audit.api.dto.AuditLogDetailDTO;
import com.platform.audit.api.dto.AuditLogEntryDTO;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test data fixtures for audit-related tests.
 * Provides reusable test data objects for consistent testing.
 */
public class AuditTestDataFixtures {

    public static AuditEvent createAuditEvent() {
        return createAuditEvent("user123", "USER_LOGIN", "SUCCESS");
    }

    public static AuditEvent createAuditEvent(String userId, String action, String result) {
        UUID actorId = UUID.randomUUID();
        AuditEvent event = AuditEvent.userLogin(actorId, "192.168.1.1", UUID.randomUUID().toString())
                .withDetails(createEventDetails());
        // Manually set ID and timestamp for unit tests since JPA won't generate them
        try {
            java.lang.reflect.Field idField = AuditEvent.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(event, UUID.randomUUID());

            java.lang.reflect.Field createdAtField = AuditEvent.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(event, Instant.now());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set fields for test event", e);
        }
        return event;
    }

    public static List<AuditEvent> createAuditEvents(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createAuditEvent(
                "user" + i,
                "ACTION_" + i,
                i % 2 == 0 ? "SUCCESS" : "FAILURE"
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    public static AuditLogEntryDTO createAuditLogEntryDTO() {
        return new AuditLogEntryDTO(
            UUID.randomUUID().toString(),
            Instant.now(),
            "user123",
            "USER",
            "LOGIN",
            "User login action",
            "SESSION",
            "UserSession",
            "SUCCESS",
            "LOW"
        );
    }

    public static AuditLogDetailDTO createAuditLogDetailDTO() {
        return new AuditLogDetailDTO(
            UUID.randomUUID().toString(),
            Instant.now(),
            "user123",
            "USER_LOGIN",
            "User login action",
            createEventDetails()
        );
    }

    private static Map<String, Object> createEventDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("sessionId", UUID.randomUUID().toString());
        details.put("loginMethod", "password");
        details.put("twoFactorEnabled", true);
        return details;
    }

    public static Map<String, String> createSearchFilters() {
        Map<String, String> filters = new HashMap<>();
        filters.put("userId", "user123");
        filters.put("action", "USER_LOGIN");
        filters.put("startDate", Instant.now().minusSeconds(86400).toString());
        filters.put("endDate", Instant.now().toString());
        return filters;
    }
}