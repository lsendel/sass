package com.platform.audit.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Temporary minimal controller for TDD GREEN phase.
 * This provides a basic working endpoint to get contract tests passing.
 * Will be replaced with full implementation in REFACTOR phase.
 */
@RestController
@RequestMapping("/api/audit")
public class TempAuditController {

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs() {
        // Minimal GREEN phase implementation - return empty list
        Map<String, Object> response = Map.of(
            "content", List.of(),
            "pageNumber", 0,
            "pageSize", 20,
            "totalElements", 0L,
            "totalPages", 0,
            "first", true,
            "last", true
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<Object> getAuditLogDetail() {
        // Return 404 for now
        return ResponseEntity.notFound().build();
    }
}