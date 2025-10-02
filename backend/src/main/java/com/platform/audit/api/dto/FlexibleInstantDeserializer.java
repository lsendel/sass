package com.platform.audit.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom deserializer that accepts both date-only and full ISO-8601 instant formats.
 * Examples:
 * - "2025-09-01" -> converted to start of day UTC
 * - "2025-09-01T00:00:00Z" -> parsed as Instant
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            // Try parsing as Instant first (full ISO-8601 with time)
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            try {
                // If that fails, try parsing as LocalDate and convert to start of day UTC
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
                return date.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e2) {
                throw new IOException("Unable to parse date/time: " + value, e2);
            }
        }
    }
}
