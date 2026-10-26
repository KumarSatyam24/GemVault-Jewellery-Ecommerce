package com.examly.springapp.service;

import com.examly.springapp.exceptions.BookingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayTestClient {
  private static final String API = "https://api.razorpay.com/v1";
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final ObjectMapper json;
  private final String keyId;
  private final String keySecret;
  private final String webhookSecret;

  public RazorpayTestClient(ObjectMapper json,
      @Value("${razorpay.key.id:}") String keyId,
      @Value("${razorpay.key.secret:}") String keySecret,
      @Value("${razorpay.webhook.secret:}") String webhookSecret) {
    this.json = json;
    this.keyId = keyId.trim();
    this.keySecret = keySecret.trim();
    this.webhookSecret = webhookSecret.trim();
  }

  public String keyId() {
    return keyId;
  }

  public boolean configured() {
    return keyId.startsWith("rzp_test_") && !keySecret.isBlank();
  }

  public JsonNode createOrder(long amountPaise, String receipt) {
    if (!configured())
      throw new BookingException("Razorpay test payment is not configured");
    return request("POST", "/orders", Map.of("amount", amountPaise, "currency", "INR",
        "receipt", receipt, "payment_capture", 1));
  }

  public JsonNode fetchPayment(String paymentId) {
    if (!paymentId.matches("pay_[A-Za-z0-9]+"))
      throw new BookingException("Invalid payment ID");
    return request("GET", "/payments/" + paymentId, null);
  }

  public JsonNode fetchOrderPayments(String orderId) {
    if (!orderId.matches("order_[A-Za-z0-9]+"))
      throw new BookingException("Invalid payment order ID");
    return request("GET", "/orders/" + orderId + "/payments", null);
  }

  public JsonNode fetchRefunds(String paymentId) {
    if (!paymentId.matches("pay_[A-Za-z0-9]+"))
      throw new BookingException("Invalid payment ID");
    return request("GET", "/payments/" + paymentId + "/refunds", null);
  }

  public JsonNode refund(String paymentId, long amountPaise, String receipt) {
    if (!paymentId.matches("pay_[A-Za-z0-9]+"))
      throw new BookingException("Invalid payment ID");
    return request("POST", "/payments/" + paymentId + "/refund",
        Map.of("amount", amountPaise, "receipt", receipt, "speed", "normal"));
  }

  public boolean validCheckoutSignature(String orderId, String paymentId, String signature) {
    return validSignature(orderId + "|" + paymentId, signature, keySecret);
  }

  public boolean validWebhookSignature(String rawBody, String signature) {
    return !webhookSecret.isBlank() && validSignature(rawBody, signature, webhookSecret);
  }

  private boolean validSignature(String text, String signature, String secret) {
    if (signature == null || !signature.matches("[a-fA-F0-9]{64}"))
      return false;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] expected = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
      return MessageDigest.isEqual(expected, java.util.HexFormat.of().parseHex(signature));
    } catch (Exception invalid) {
      return false;
    }
  }

  private JsonNode request(String method, String path, Object payload) {
    if (!configured())
      throw new BookingException("Razorpay test payment is not configured");
    try {
      String credentials = Base64.getEncoder().encodeToString(
          (keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(API + path))
          .timeout(Duration.ofSeconds(20))
          .header("Authorization", "Basic " + credentials)
          .header("Content-Type", "application/json");
      if ("POST".equals(method))
        builder.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
      else
        builder.GET();
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new BookingException("Razorpay test payment request failed (HTTP "
            + response.statusCode() + ")");
      return json.readTree(response.body());
    } catch (BookingException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BookingException("Razorpay test payment is temporarily unavailable");
    }
  }
}
