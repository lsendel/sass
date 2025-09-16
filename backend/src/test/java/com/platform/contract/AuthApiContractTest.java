package com.platform.contract;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for Authentication API endpoints.
 * These tests validate API contracts defined in specs/005-spring-boot-modulith/contracts/auth-api.yml
 *
 * CRITICAL: These tests MUST FAIL initially as no implementation exists yet.
 * This follows the TDD RED-GREEN-Refactor cycle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Auth API Contract Tests")
class AuthApiContractTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("GET /auth/providers - should return list of OAuth2 providers")
    void shouldReturnOAuth2Providers() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/auth/providers")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("providers", isA(java.util.List.class))
            .body("providers", hasSize(greaterThan(0)))
            .body("providers[0].name", notNullValue())
            .body("providers[0].authorizationUrl", notNullValue());
    }

    @Test
    @DisplayName("GET /auth/authorize - should initiate OAuth2 authorization")
    void shouldInitiateOAuth2Authorization() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "google")
            .queryParam("redirect_uri", "http://localhost:3000/callback")
        .when()
            .get("/api/v1/auth/authorize")
        .then()
            .statusCode(302) // Redirect to OAuth provider
            .header("Location", containsString("accounts.google.com"));
    }

    @Test
    @DisplayName("GET /auth/authorize - should reject invalid provider")
    void shouldRejectInvalidProvider() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "invalid-provider")
            .queryParam("redirect_uri", "http://localhost:3000/callback")
        .when()
            .get("/api/v1/auth/authorize")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", equalTo("INVALID_PROVIDER"))
            .body("message", containsString("provider"));
    }

    @Test
    @DisplayName("POST /auth/callback - should handle OAuth2 callback")
    void shouldHandleOAuth2Callback() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("code", "test-auth-code")
            .queryParam("state", "test-state")
        .when()
            .post("/api/v1/auth/callback")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("accessToken", notNullValue())
            .body("user.id", notNullValue())
            .body("user.email", notNullValue())
            .body("user.name", notNullValue());
    }

    @Test
    @DisplayName("GET /auth/session - should return current user session")
    void shouldReturnCurrentSession() {
        // This test requires authentication - should fail without valid token
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer invalid-token")
        .when()
            .get("/api/v1/auth/session")
        .then()
            .statusCode(401)
            .contentType(ContentType.JSON)
            .body("error", equalTo("UNAUTHORIZED"))
            .body("message", containsString("token"));
    }

    @Test
    @DisplayName("POST /auth/logout - should invalidate session")
    void shouldInvalidateSession() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
        .when()
            .post("/api/v1/auth/logout")
        .then()
            .statusCode(204); // No content on successful logout
    }

    @Test
    @DisplayName("GET /auth/session - should return proper error structure")
    void shouldReturnProperErrorStructure() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/auth/session") // No auth header
        .then()
            .statusCode(401)
            .contentType(ContentType.JSON)
            .body("error", notNullValue())
            .body("message", notNullValue())
            .body("timestamp", notNullValue())
            .body("path", equalTo("/api/v1/auth/session"));
    }

    @Test
    @DisplayName("All auth endpoints should include security headers")
    void shouldIncludeSecurityHeaders() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/auth/providers")
        .then()
            .header("X-Content-Type-Options", "nosniff")
            .header("X-Frame-Options", "DENY")
            .header("X-XSS-Protection", "1; mode=block");
    }

    @Test
    @DisplayName("Should handle CORS for frontend requests")
    void shouldHandleCORS() {
        given()
            .header("Origin", "http://localhost:3000")
        .when()
            .options("/api/v1/auth/providers")
        .then()
            .statusCode(200)
            .header("Access-Control-Allow-Origin", "http://localhost:3000")
            .header("Access-Control-Allow-Methods", containsString("GET"))
            .header("Access-Control-Allow-Headers", containsString("Authorization"));
    }
}