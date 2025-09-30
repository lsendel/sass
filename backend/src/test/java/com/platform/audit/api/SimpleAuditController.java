package com.platform.audit.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal controller for demonstrating TDD RED-GREEN-REFACTOR cycle.
 * This controller intentionally returns 404 to demonstrate the RED phase.
 */
@RestController
@RequestMapping("/api/audit")
public final class SimpleAuditController {

    @GetMapping("/logs")
    public ResponseEntity<?> getAuditLogs() {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportAuditLogs(@RequestBody final Object request) {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/export/{exportId}/status")
    public ResponseEntity<?> getExportStatus(@PathVariable final String exportId) {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/export/{token}/download")
    public ResponseEntity<?> downloadExport(@PathVariable final String token) {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }
}