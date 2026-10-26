package com.examly.springapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MetalRateService {
  private static final Duration CACHE_DURATION = Duration.ofMinutes(10);
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper json;
  private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

  public MetalRateService(ObjectMapper json) {
    this.json = json;
  }

  public record Quote(String material, String purity, double ratePerGram, String source, Instant fetchedAt) {
  }

  private record CacheEntry(double goldInrPerGram, double silverInrPerGram, Instant expiresAt) {
  }

  public Quote quote(String material, String purity) {
    return quote(material, purity, false);
  }

  public Quote quote(String material, String purity, boolean forceRefresh) {
    String normalized = material == null ? "" : material.trim().toUpperCase(Locale.ROOT);
    double pureRate = switch (normalized) {
      case "GOLD" -> indianRates(forceRefresh).goldInrPerGram();
      case "SILVER" -> indianRates(forceRefresh).silverInrPerGram();
      default -> throw new IllegalArgumentException("Live rates are available for Gold and Silver only");
    };
    double multiplier = purityMultiplier(normalized, purity);
    return new Quote(capitalized(normalized), purity, BillingCalculator.round(pureRate * multiplier),
        "OroPocket Indian bullion buyer quote (INR per gram)", Instant.now());
  }

  private CacheEntry indianRates(boolean forceRefresh) {
    CacheEntry current = cache.get("INDIA");
    if (!forceRefresh && current != null && current.expiresAt().isAfter(Instant.now()))
      return current;
    try {
      JsonNode data = read("https://api.oropocket.com/public/prices").path("data");
      double gold = number(data.path("gold").path("buy"));
      double silver = number(data.path("silver").path("buy"));
      if (gold <= 0 || silver <= 0)
        throw new IllegalStateException();
      CacheEntry rates = new CacheEntry(gold, silver, Instant.now().plus(CACHE_DURATION));
      cache.put("INDIA", rates);
      return rates;
    } catch (Exception exception) {
      if (current != null && !forceRefresh)
        return current;
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Live metal rate is unavailable. Enter the rate manually.");
    }
  }

  private JsonNode read(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(8)).header("Accept", "application/json").GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300)
      throw new IllegalStateException();
    return json.readTree(response.body());
  }

  private double purityMultiplier(String material, String purity) {
    String value = purity == null ? "" : purity.toUpperCase(Locale.ROOT);
    if (material.equals("GOLD")) {
      java.util.regex.Matcher karat = java.util.regex.Pattern.compile("(\\d{1,2})K").matcher(value);
      if (karat.find())
        return Integer.parseInt(karat.group(1)) / 24d;
    }
    java.util.regex.Matcher fineness = java.util.regex.Pattern
        .compile("\\b(999|958|950|925|900|875|835|833|800|750|585|417|375)\\b").matcher(value);
    if (fineness.find())
      return Integer.parseInt(fineness.group(1)) / 1000d;
    return 1d;
  }

  private static String capitalized(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase(Locale.ROOT);
  }

  private static double number(JsonNode node) {
    return node == null ? 0d : node.asDouble();
  }
}
