package com.platform.shared.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Health check endpoint for monitoring application status.
 * Provides both basic and detailed health information.
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application health and status monitoring")
public class HealthCheckController {
    
    private final HealthCheckService healthCheckService;
    
    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }
    
    @GetMapping
    @Operation(
        summary = "Basic health check",
        description = "Returns 200 if the application is healthy, 503 otherwise"
    )
    @ApiResponse(responseCode = "200", description = "Application is healthy")
    @ApiResponse(responseCode = "503", description = "Application is unhealthy")
    public ResponseEntity<String> health() {
        var status = healthCheckService.performHealthCheck();
        if (status.healthy()) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.status(503).body("SERVICE_UNAVAILABLE");
        }
    }
    
    @GetMapping("/detailed")
    @Operation(
        summary = "Detailed health check",
        description = "Returns detailed health information about all components"
    )
    @ApiResponse(responseCode = "200", description = "Detailed health information")
    public ResponseEntity<HealthCheckService.HealthStatus> detailedHealth() {
        return ResponseEntity.ok(healthCheckService.performHealthCheck());
    }
}