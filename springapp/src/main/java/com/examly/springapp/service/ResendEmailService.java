package com.examly.springapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResendEmailService {
  private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
  private final String apiKey;
  private final String from;
  private final ObjectMapper json;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public ResendEmailService(
      @Value("${resend.api.key:}") String apiKey,
      @Value("${resend.from:}") String from,
      ObjectMapper json) {
    this.apiKey = apiKey;
    this.from = from;
    this.json = json;
  }

  public boolean isConfigured() {
    return !apiKey.isBlank() && !from.isBlank();
  }

  public void send(String recipient, String subject, String html) {
    if (!isConfigured()) {
      log.warn("Email not sent because RESEND_API_KEY or RESEND_FROM is not configured");
      return;
    }
    try {
      String body = json.writeValueAsString(new Mail(from, List.of(recipient), subject, html));
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
          .timeout(Duration.ofSeconds(15))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new IllegalStateException("Resend rejected the email (HTTP " + response.statusCode() + ")");
    } catch (Exception exception) {
      log.error("Could not send transactional email to {}", recipient, exception);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Email could not be sent. Check Resend sender and account settings, then try again.");
    }
  }

  private record Mail(String from, List<String> to, String subject, String html) {
  }
}
