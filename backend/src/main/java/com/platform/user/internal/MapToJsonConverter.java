package com.platform.user.internal;

import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** JPA converter for Map<String, Object> to JSON string conversion. */
@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger logger = LoggerFactory.getLogger(MapToJsonConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

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
