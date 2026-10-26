package com.examly.springapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {
  private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
  private static final String API = "https://generativelanguage.googleapis.com/v1beta/models/";
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper json;
  private final String apiKey;
  private final String embeddingModel;
  private final String generationModel;
  private volatile long retryAfter;

  public GeminiService(ObjectMapper json,
      @Value("${gemini.api.key:}") String apiKey,
      @Value("${gemini.embedding.model:gemini-embedding-2}") String embeddingModel,
      @Value("${gemini.generation.model:gemini-3.1-flash-lite}") String generationModel) {
    this.json = json;
    this.apiKey = apiKey;
    this.embeddingModel = validModel(embeddingModel);
    this.generationModel = validModel(generationModel);
  }

  public boolean isEnabled() {
    return !apiKey.isBlank() && System.currentTimeMillis() >= retryAfter;
  }

  public float[] embed(String text) {
    if (!isEnabled())
      return null;
    try {
      JsonNode payload = post(embeddingModel, "embedContent", Map.of(
          "model", "models/" + embeddingModel,
          "content", Map.of("parts", List.of(Map.of("text", text))),
          "output_dimensionality", 768));
      return values(payload.path("embedding").path("values"));
    } catch (Exception exception) {
      unavailable(exception);
      return null;
    }
  }

  public List<float[]> embedAll(List<String> texts) {
    if (!isEnabled() || texts.isEmpty())
      return List.of();
    try {
      List<Object> requests = new ArrayList<>();
      for (String text : texts)
        requests.add(Map.of(
            "model", "models/" + embeddingModel,
            "content", Map.of("parts", List.of(Map.of("text", text))),
            "output_dimensionality", 768));
      JsonNode embeddings = post(embeddingModel, "batchEmbedContents", Map.of("requests", requests))
          .path("embeddings");
      if (!embeddings.isArray() || embeddings.size() != texts.size())
        return List.of();
      List<float[]> result = new ArrayList<>();
      for (JsonNode embedding : embeddings)
        result.add(values(embedding.path("values")));
      return result;
    } catch (Exception exception) {
      unavailable(exception);
      return List.of();
    }
  }

  public String generate(String prompt) {
    if (!isEnabled())
      return null;
    try {
      JsonNode parts = post(generationModel, "generateContent", Map.of(
          "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
          "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", 300)))
          .path("candidates").path(0).path("content").path("parts");
      StringBuilder reply = new StringBuilder();
      if (parts.isArray())
        for (JsonNode part : parts)
          reply.append(part.path("text").asText());
      String text = reply.toString().trim();
      return text.isBlank() ? null : text;
    } catch (Exception exception) {
      unavailable(exception);
      return null;
    }
  }

  public JsonNode generateWithTools(Object body) {
    if (!isEnabled())
      return null;
    try {
      return post(generationModel, "generateContent", body);
    } catch (Exception exception) {
      unavailable(exception);
      return null;
    }
  }

  private JsonNode post(String model, String action, Object body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(API + model + ":" + action))
        .timeout(Duration.ofSeconds(30))
        .header("x-goog-api-key", apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300)
      throw new IllegalStateException("Gemini HTTP " + response.statusCode());
    return json.readTree(response.body());
  }

  private float[] values(JsonNode values) {
    if (!values.isArray() || values.isEmpty())
      return null;
    float[] vector = new float[values.size()];
    for (int index = 0; index < values.size(); index++)
      vector[index] = (float) values.get(index).asDouble();
    return vector;
  }

  private void unavailable(Exception exception) {
    retryAfter = System.currentTimeMillis() + Duration.ofMinutes(1).toMillis();
    log.warn("Gemini unavailable; FAQ chat is using lexical answers for one minute: {}", exception.getMessage());
  }

  private String validModel(String model) {
    if (!model.matches("[A-Za-z0-9._-]+"))
      throw new IllegalArgumentException(
          "Gemini model names must contain only letters, numbers, dots, underscores and hyphens");
    return model;
  }
}
