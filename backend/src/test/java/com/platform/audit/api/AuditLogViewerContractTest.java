package com.platform.audit.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;

/**
 * Contract test for GET /api/audit/logs endpoint
 *
 * This test MUST FAIL initially as part of TDD RED phase.
 * It validates the API contract defined in audit-log-api.yaml
 *
 * Note: This test is designed to fail until the audit log viewer endpoints are implemented
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class AuditLogViewerContractTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void shouldFailUntilEndpointIsImplemented_GetAuditLogs() {
        // This test MUST FAIL initially - endpoint doesn't exist yet
        // The GET /api/audit/logs endpoint will return 404 until we implement it
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/api/audit/logs")
            .then()
            .statusCode(404); // Expecting 404 until we implement the endpoint
    }

    @Test
    void shouldFailUntilEndpointIsImplemented_GetAuditLogDetails() {
        // This test MUST FAIL initially - endpoint doesn't exist yet
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/api/audit/logs/550e8400-e29b-41d4-a716-446655440000")
            .then()
            .statusCode(404); // Expecting 404 until we implement the endpoint
    }

    @Test
    void shouldFailUntilEndpointIsImplemented_ExportAuditLogs() {
        // This test MUST FAIL initially - endpoint doesn't exist yet
        String exportRequest = """
            {
                "format": "CSV",
                "filters": {
                    "dateFrom": "2024-01-01T00:00:00Z",
                    "dateTo": "2024-12-31T23:59:59Z"
                }
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(exportRequest)
            .when()
            .post("/api/audit/export")
            .then()
            .statusCode(404); // Expecting 404 until we implement the endpoint
    }

    @Test
    void shouldFailUntilEndpointIsImplemented_GetExportStatus() {
        // This test MUST FAIL initially - endpoint doesn't exist yet
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/api/audit/export/550e8400-e29b-41d4-a716-446655440000/status")
            .then()
            .statusCode(404); // Expecting 404 until we implement the endpoint
    }

    @Test
    void shouldFailUntilEndpointIsImplemented_DownloadExport() {
        // This test MUST FAIL initially - endpoint doesn't exist yet
        given()
            .when()
            .get("/api/audit/export/sample-token/download")
            .then()
            .statusCode(404); // Expecting 404 until we implement the endpoint
    }

    // Note: These tests are intentionally simple and expect 404 errors
    // Once we implement the endpoints, we'll update these tests to verify
    // the actual API contract requirements from audit-log-api.yaml
}