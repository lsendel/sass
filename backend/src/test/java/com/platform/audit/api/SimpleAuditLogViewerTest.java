package com.platform.audit.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simple integration test to demonstrate TDD RED phase for audit log viewer.
 * This test verifies that our audit log endpoints return 404 status codes
 * before implementation (RED phase), proving the TDD cycle works.
 */
@SpringBootTest(
        classes = com.platform.AuditApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
@Disabled("TDD RED phase test - disabled in favor of integration tests with real databases")
class SimpleAuditLogViewerTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void verifyTddRedPhaseAuditLogsEndpoint() {
        // This test MUST FAIL initially - endpoint should return 404 until implemented
        String url = "http://localhost:" + port + "/api/audit/logs";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // In TDD RED phase, we expect 404 because the endpoint is not implemented
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Expected 404 for unimplemented audit logs endpoint (TDD RED phase)");
    }

    @Test
    void verifyTddRedPhaseExportEndpoint() {
        // This test MUST FAIL initially - export endpoint should return 404 until implemented
        String url = "http://localhost:" + port + "/api/audit/export";

        ResponseEntity<String> response = restTemplate.postForEntity(url, "{}", String.class);

        // In TDD RED phase, we expect 404 because the export endpoint is not implemented
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Expected 404 for unimplemented audit export endpoint (TDD RED phase)");
    }
}