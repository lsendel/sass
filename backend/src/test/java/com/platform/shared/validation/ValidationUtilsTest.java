package com.platform.shared.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtils.
 *
 * <p>These tests verify the improved validation logic and demonstrate
 * the standardized error handling patterns implemented in the clean code improvements.
 */
class ValidationUtilsTest {

    @Test
    @DisplayName("Should successfully validate valid UUID strings")
    void shouldValidateValidUuids() {
        // Given
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";

        // When
        var result = ValidationUtils.validateUuid(validUuid, "testField");

        // Then
        assertTrue(result.isValid());
        assertNotNull(result.getUuid());
        assertEquals(UUID.fromString(validUuid), result.getUuid());
        assertNull(result.getErrorMessage());
        assertNull(result.getErrorCode());
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid UUID formats")
    @ValueSource(strings = {
        "invalid-uuid",
        "12345",
        "550e8400-e29b-41d4-a716",  // Too short
        "550e8400-e29b-41d4-a716-446655440000-extra",  // Too long
        "gggggggg-e29b-41d4-a716-446655440000",  // Invalid characters
        ""
    })
    void shouldRejectInvalidUuidFormats(String invalidUuid) {
        // When
        var result = ValidationUtils.validateUuid(invalidUuid, "testField");

        // Then
        assertFalse(result.isValid());
        assertNull(result.getUuid());
        assertEquals("INVALID_UUID_FORMAT", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("testField"));
    }

    @Test
    @DisplayName("Should handle null and empty UUID strings")
    void shouldHandleNullAndEmptyUuids() {
        // Test null UUID
        var nullResult = ValidationUtils.validateUuid(null, "testField");
        assertFalse(nullResult.isValid());
        assertEquals("MISSING_REQUIRED_FIELD", nullResult.getErrorCode());
        assertTrue(nullResult.getErrorMessage().contains("is required"));

        // Test empty UUID
        var emptyResult = ValidationUtils.validateUuid("", "testField");
        assertFalse(emptyResult.isValid());
        assertEquals("MISSING_REQUIRED_FIELD", emptyResult.getErrorCode());

        // Test whitespace-only UUID
        var whitespaceResult = ValidationUtils.validateUuid("   ", "testField");
        assertFalse(whitespaceResult.isValid());
        assertEquals("MISSING_REQUIRED_FIELD", whitespaceResult.getErrorCode());
    }

    @Test
    @DisplayName("Should trim whitespace from valid UUIDs")
    void shouldTrimWhitespaceFromValidUuids() {
        // Given
        String uuidWithWhitespace = "  550e8400-e29b-41d4-a716-446655440000  ";
        String expectedUuid = "550e8400-e29b-41d4-a716-446655440000";

        // When
        var result = ValidationUtils.validateUuid(uuidWithWhitespace, "testField");

        // Then
        assertTrue(result.isValid());
        assertEquals(UUID.fromString(expectedUuid), result.getUuid());
    }

    @Test
    @DisplayName("Should validate page size within bounds")
    void shouldValidatePageSizeWithinBounds() {
        // Valid page sizes
        assertTrue(ValidationUtils.validatePageSize(1, 100).isValid());
        assertTrue(ValidationUtils.validatePageSize(50, 100).isValid());
        assertTrue(ValidationUtils.validatePageSize(100, 100).isValid());

        // Invalid page sizes
        var tooSmallResult = ValidationUtils.validatePageSize(0, 100);
        assertFalse(tooSmallResult.isValid());
        assertEquals("PAGE_SIZE_TOO_SMALL", tooSmallResult.getErrorCode());

        var tooLargeResult = ValidationUtils.validatePageSize(101, 100);
        assertFalse(tooLargeResult.isValid());
        assertEquals("PAGE_SIZE_TOO_LARGE", tooLargeResult.getErrorCode());
    }

    @Test
    @DisplayName("Should validate page numbers")
    void shouldValidatePageNumbers() {
        // Valid page numbers
        assertTrue(ValidationUtils.validatePageNumber(0).isValid());
        assertTrue(ValidationUtils.validatePageNumber(1).isValid());
        assertTrue(ValidationUtils.validatePageNumber(100).isValid());

        // Invalid page numbers
        var negativeResult = ValidationUtils.validatePageNumber(-1);
        assertFalse(negativeResult.isValid());
        assertEquals("PAGE_NUMBER_NEGATIVE", negativeResult.getErrorCode());
    }

    @Test
    @DisplayName("Should create standardized error responses")
    void shouldCreateStandardizedErrorResponses() {
        // When
        var errorResponse = ValidationUtils.createErrorResponse("TEST_CODE", "Test message");

        // Then
        assertEquals("TEST_CODE", errorResponse.get("code"));
        assertEquals("Test message", errorResponse.get("message"));
        assertNotNull(errorResponse.get("timestamp"));
        assertNotNull(errorResponse.get("correlationId"));

        // Verify correlation ID is a valid UUID
        String correlationId = (String) errorResponse.get("correlationId");
        assertDoesNotThrow(() -> UUID.fromString(correlationId));
    }

    @Test
    @DisplayName("Should create validation result objects correctly")
    void shouldCreateValidationResultObjects() {
        // Success result
        var successResult = ValidationUtils.ValidationResult.success();
        assertTrue(successResult.isValid());
        assertNull(successResult.getErrorCode());
        assertNull(successResult.getErrorMessage());

        // Failure result
        var failureResult = ValidationUtils.ValidationResult.failure("ERROR_CODE", "Error message");
        assertFalse(failureResult.isValid());
        assertEquals("ERROR_CODE", failureResult.getErrorCode());
        assertEquals("Error message", failureResult.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void shouldHandleEdgeCasesGracefully() {
        // Test with very long field names
        String longFieldName = "a".repeat(1000);
        var result = ValidationUtils.validateUuid("invalid", longFieldName);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains(longFieldName));

        // Test with special characters in field names
        var specialCharResult = ValidationUtils.validateUuid("invalid", "field-with-special_chars@123");
        assertFalse(specialCharResult.isValid());
        assertTrue(specialCharResult.getErrorMessage().contains("field-with-special_chars@123"));
    }
}