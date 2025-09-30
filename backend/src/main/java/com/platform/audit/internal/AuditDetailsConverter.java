package com.platform.audit.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JPA {@link AttributeConverter} to serialize a {@code Map<String, Object>} into a JSON string
 * for database storage, and deserialize it back into a Map.
 *
 * <p>This converter is used for the {@code details} field in the {@link AuditEvent} entity,
 * allowing flexible, unstructured data to be stored in a single database column.
 * </p>
 */
@Converter
public class AuditDetailsConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditDetailsConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() { };

  /**
   * Converts a map of audit event details into a JSON string for database persistence.
   *
   * @param attribute The map of details to be converted. Can be null or empty.
   * @return A JSON string representation of the map. Returns an empty JSON object "{}" if the
   *     input is null, empty, or if a serialization error occurs.
   */
    @Override
    public String convertToDatabaseColumn(final Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting audit details to JSON", e);
            return "{}";
        }
    }

  /**
   * Converts a JSON string from the database into a map of audit event details.
   *
   * @param dbData The JSON string from the database. Can be null or empty.
   * @return A {@code Map<String, Object>} representing the audit details. Returns an empty map
   *     if the input is null, empty, or if a deserialization error occurs.
   */
    @Override
    public Map<String, Object> convertToEntityAttribute(final String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Map.of();
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE_REF);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting JSON to audit details", e);
            return Map.of();
        }
    }
}
