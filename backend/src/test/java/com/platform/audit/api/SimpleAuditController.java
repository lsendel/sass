package com.platform.audit.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Minimal controller for demonstrating TDD RED-GREEN-REFACTOR cycle.
 * This controller intentionally returns 404 to demonstrate the RED phase.
 */
@RestController
@RequestMapping("/api/audit")
public class SimpleAuditController {

    @GetMapping("/logs")
    public ResponseEntity<?> getAuditLogs() {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportAuditLogs(@RequestBody Object request) {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/export/{exportId}/status")
    public ResponseEntity<?> getExportStatus(@PathVariable String exportId) {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/export/{token}/download")
    public ResponseEntity<?> downloadExport(@PathVariable String token) {
        // TDD RED phase - intentionally not implemented
        return ResponseEntity.notFound().build();
    }
}