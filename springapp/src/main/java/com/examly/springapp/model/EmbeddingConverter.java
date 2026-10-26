package com.examly.springapp.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmbeddingConverter implements AttributeConverter<float[], String> {
  private static final ObjectMapper JSON = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(float[] value) {
    if (value == null)
      return null;
    try {
      return JSON.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Could not store FAQ embedding", exception);
    }
  }

  @Override
  public float[] convertToEntityAttribute(String value) {
    if (value == null || value.isBlank())
      return null;
    try {
      return JSON.readValue(value, float[].class);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Could not read FAQ embedding", exception);
    }
  }
}
