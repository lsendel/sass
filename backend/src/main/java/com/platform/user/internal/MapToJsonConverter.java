package com.platform.user.internal;

import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JPA {@link AttributeConverter} to serialize a map of attributes into a JSON string for database
 * storage and deserialize it back.
 *
 * <p>This converter is used for fields that require a flexible, schema-less map of key-value
 * pairs, such as settings or metadata.
 */
@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger logger = LoggerFactory.getLogger(MapToJsonConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

  /**
   * Converts a map of attributes into a JSON string for database persistence.
   *
   * @param attribute the map of attributes to be converted
   * @return a JSON string representation of the map, or an empty JSON object "{}" if the map is
   *     null, empty, or if a serialization error occurs
   */
  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "{}";
    }

    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      logger.error("Error converting map to JSON", e);
      return "{}";
    }
  }

  /**
   * Converts a JSON string from the database into a map of attributes.
   *
   * @param dbData the JSON string from the database
   * @return a {@code Map<String, Object>} representing the attributes, or an empty map if the JSON
   *     is null, empty, or if a deserialization error occurs
   */
  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return Map.of();
    }

    try {
      return objectMapper.readValue(dbData, typeRef);
    } catch (JsonProcessingException e) {
      logger.error("Error converting JSON to map", e);
      return Map.of();
    }
  }
}
