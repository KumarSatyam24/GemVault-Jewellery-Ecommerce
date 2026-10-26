package com.examly.springapp.service;

import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.model.ProductTryOn;
import com.examly.springapp.model.User;
import com.examly.springapp.model.TryOnAttempt;
import com.examly.springapp.repository.JewelleryItemRepo;
import com.examly.springapp.repository.ProductTryOnRepo;
import com.examly.springapp.repository.TryOnAttemptRepo;
import com.examly.springapp.repository.UserRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductTryOnService {
  private static final Logger log = LoggerFactory.getLogger(ProductTryOnService.class);
  private static final int MAX_UPLOAD_BYTES = 4 * 1024 * 1024;
  private static final int MAX_OUTPUT_BYTES = 8 * 1024 * 1024;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ObjectMapper json;
  private final JewelleryItemRepo items;
  private final UserRepo users;
  private final ProductTryOnRepo results;
  private final TryOnAttemptRepo attempts;
  private final String key;
  private final String model;
  private final int globalLimit;
  private final int dailyLimit;

  public ProductTryOnService(ObjectMapper json, JewelleryItemRepo items, UserRepo users,
      ProductTryOnRepo results, TryOnAttemptRepo attempts,
      @Value("${openrouter.api.key:}") String key,
      @Value("${openrouter.try-on.model:black-forest-labs/flux.2-klein-4b}") String model,
      @Value("${openrouter.try-on.total-limit:60}") int globalLimit,
      @Value("${openrouter.try-on.daily-user-limit:3}") int dailyLimit) {
    this.json = json;
    this.items = items;
    this.users = users;
    this.results = results;
    this.attempts = attempts;
    this.key = key;
    this.model = model;
    this.globalLimit = globalLimit;
    this.dailyLimit = dailyLimit;
  }

  public ProductTryOn saved(Integer userId, Long itemId) {
    return results.findByUserUserIdAndItemItemId(userId, itemId)
        .orElseThrow(() -> new NoSuchElementException("No saved try-on for this piece"));
  }

  public void delete(Integer userId, Long itemId) {
    results.findByUserUserIdAndItemItemId(userId, itemId).ifPresent(results::delete);
  }

  public ProductTryOn generate(Integer userId, Long itemId, MultipartFile photo) {
    if (key.isBlank())
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Virtual try-on is not configured yet");
    if (photo == null || photo.isEmpty() || photo.getSize() > MAX_UPLOAD_BYTES)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a photo smaller than 4 MB");
    JewelleryItem item = items.findById(itemId)
        .orElseThrow(() -> new NoSuchElementException("Piece not found"));
    if (Boolean.FALSE.equals(item.getActive()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This piece is unavailable");
    String reference = item.getImageUrl();
    if (reference == null || !(reference.startsWith("https://")
        || reference.matches("(?s)^data:image/(jpeg|png|webp);base64,.*")))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "This piece needs a public product photo for virtual try-on");
    byte[] prepared = preparePhoto(photo);
    reserveAttempt(userId, itemId);
    GeneratedImage generated = requestImage(item, reference, prepared);
    User user = users.findById(userId).orElseThrow();
    ProductTryOn result = results.findByUserUserIdAndItemItemId(userId, itemId)
        .orElseGet(ProductTryOn::new);
    result.setUser(user);
    result.setItem(item);
    result.setImage(generated.bytes());
    result.setContentType(generated.contentType());
    result.setGeneratedAt(Instant.now());
    return results.save(result);
  }

  private synchronized void reserveAttempt(Integer userId, Long itemId) {
    if (globalLimit < 1 || attempts.count() >= globalLimit)
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
          "The demo image allowance has been reached");
    Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    if (dailyLimit < 1 || attempts.countByUserIdAndAttemptedAtGreaterThanEqual(userId, today) >= dailyLimit)
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
          "Your daily try-on allowance has been reached. Try again tomorrow.");
    // Count every request before sending it: failed provider calls can still be
    // billed.
    attempts.saveAndFlush(new TryOnAttempt(userId, itemId));
  }

  private byte[] preparePhoto(MultipartFile photo) {
    try {
      byte[] raw = photo.getBytes();
      boolean jpeg = raw.length > 3 && (raw[0] & 0xff) == 0xff
          && (raw[1] & 0xff) == 0xd8;
      boolean png = raw.length > 8 && (raw[0] & 0xff) == 0x89
          && raw[1] == 'P' && raw[2] == 'N' && raw[3] == 'G';
      if (!jpeg && !png)
        throw new IllegalArgumentException("Upload a JPEG or PNG photo");
      BufferedImage source = ImageIO.read(new ByteArrayInputStream(raw));
      if (source == null || source.getWidth() < 160 || source.getHeight() < 160
          || (long) source.getWidth() * source.getHeight() > 25_000_000L)
        throw new IllegalArgumentException("Upload a clear JPEG or PNG photo (at least 160 px)");
      double scale = Math.min(1.0, 1024.0 / Math.max(source.getWidth(), source.getHeight()));
      int width = (int) Math.round(source.getWidth() * scale);
      int height = (int) Math.round(source.getHeight() * scale);
      BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = resized.createGraphics();
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.drawImage(source, 0, 0, width, height, null);
      graphics.dispose();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(resized, "jpeg", output);
      return output.toByteArray();
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not read the photo. Use a JPEG or PNG file.");
    }
  }

  private GeneratedImage requestImage(JewelleryItem item, String reference, byte[] photo) {
    try {
      String category = item.getCategory() == null ? "jewellery" : item.getCategory();
      String prompt = "Create one photorealistic virtual jewellery try-on image. Reference image 1 "
          + "is the customer photo; preserve the person's identity, face, skin tone, pose, clothing, "
          + "hands and background. Reference image 2 is the exact GemVault product: "
          + item.getItemName() + " (" + category + "). Place only that piece naturally on the "
          + placement(category) + ". Match its design, colour and proportions closely. "
          + "Keep the result tasteful and realistic. Do not add text, logos or other jewellery.";
      String customerData = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(photo);
      byte[] body = json.writeValueAsBytes(Map.of(
          "model", model, "prompt", prompt, "n", 1, "aspect_ratio", "auto",
          "output_format", "jpeg",
          "input_references", List.of(
              Map.of("type", "image_url", "image_url", Map.of("url", customerData)),
              Map.of("type", "image_url", "image_url", Map.of("url", reference)))));
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://openrouter.ai/api/v1/images"))
          .timeout(Duration.ofSeconds(100))
          .header("Authorization", "Bearer " + key)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("OpenRouter try-on failed with HTTP {}", response.statusCode());
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
            response.statusCode() == 402 ? "Image credits are exhausted"
                : "Image generation is unavailable. Please try again later.");
      }
      JsonNode image = json.readTree(response.body()).path("data").path(0);
      String base64 = image.path("b64_json").asText();
      if (base64.isBlank())
        throw new IllegalStateException("OpenRouter returned no image");
      byte[] output = Base64.getDecoder().decode(base64);
      if (output.length == 0 || output.length > MAX_OUTPUT_BYTES)
        throw new IllegalStateException("OpenRouter returned an invalid image size");
      String contentType = image.path("media_type").asText("image/jpeg");
      if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")
          && !contentType.equals("image/webp"))
        throw new IllegalStateException("OpenRouter returned an unsupported image");
      return new GeneratedImage(output, contentType);
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image generation was interrupted");
    } catch (Exception exception) {
      log.warn("OpenRouter try-on request failed", exception);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Image generation is unavailable. Please try again later.");
    }
  }

  private String placement(String category) {
    String value = category.toLowerCase();
    if (value.contains("ring"))
      return "finger of the visible hand";
    if (value.contains("bangle") || value.contains("bracelet"))
      return "visible wrist";
    if (value.contains("earring"))
      return "visible ears";
    if (value.contains("necklace") || value.contains("pendant") || value.contains("chain"))
      return "neck and upper chest";
    return "appropriate part of the person";
  }

  private record GeneratedImage(byte[] bytes, String contentType) {
  }
}
