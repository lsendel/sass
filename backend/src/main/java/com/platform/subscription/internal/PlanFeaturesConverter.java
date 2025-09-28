package com.platform.subscription.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JPA {@link AttributeConverter} to serialize a map of plan features into a JSON string for
 * database storage and deserialize it back.
 *
 * <p>This converter is used for the {@code features} field in the {@link Plan} entity, allowing for
 * a flexible, schema-less way to define features for each subscription plan.
 * </p>
 */
@Converter
public class PlanFeaturesConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger logger = LoggerFactory.getLogger(PlanFeaturesConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

  /**
   * Converts a map of plan features into a JSON string for database persistence.
   *
   * @param attribute The map of features to be converted. Can be null or empty.
   * @return A JSON string representation of the map. Returns an empty JSON object "{}" if the
   *     input is null, empty, or if a serialization error occurs.
   */
  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "{}";
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      logger.error("Error converting plan features to JSON", e);
      return "{}";
    }
  }

  /**
   * Converts a JSON string from the database into a map of plan features.
   *
   * @param dbData The JSON string from the database. Can be null or empty.
   * @return A {@code Map<String, Object>} representing the plan features. Returns an empty map if
   *     the input is null, empty, or if a deserialization error occurs.
   */
  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(dbData, typeRef);
    } catch (JsonProcessingException e) {
      logger.error("Error converting JSON to plan features", e);
      return Map.of();
    }
  }
}
