package com.examly.springapp.service;

import com.examly.springapp.model.EmailActionToken;
import com.examly.springapp.config.AppUrlResolver;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.EmailActionTokenRepo;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailActionService {
  public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
  public static final String RESET_PASSWORD = "RESET_PASSWORD";
  public static final String INVITATION = "INVITATION";
  public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
  private final EmailActionTokenRepo tokens;
  private final ResendEmailService email;
  private final AppUrlResolver appUrls;
  private final PasswordEncoder encoder;
  private final SecureRandom random = new SecureRandom();

  public EmailActionService(EmailActionTokenRepo tokens, ResendEmailService email,
      AppUrlResolver appUrls, PasswordEncoder encoder) {
    this.tokens = tokens;
    this.email = email;
    this.appUrls = appUrls;
    this.encoder = encoder;
  }

  @Transactional
  public void sendVerification(User user) {
    send(user, VERIFY_EMAIL, Duration.ofHours(24), "/verify-email", "Verify your GemVault email",
        "Confirm your email before signing in to GemVault.");
  }

  @Transactional
  public void sendPasswordReset(User user) {
    send(user, RESET_PASSWORD, Duration.ofHours(1), "/reset-password", "Reset your GemVault password",
        "Use the secure link below to choose a new password.");
  }

  @Transactional
  public void sendInvitation(User user) {
    if (!email.isConfigured())
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Buyer invitation email is not configured. Set RESEND_API_KEY and RESEND_FROM.");
    send(user, INVITATION, Duration.ofHours(72), "/reset-password", "You are invited to GemVault",
        "Your buyer account is ready. Choose your password to activate it.");
  }

  @Transactional
  public void sendAdminLogin(User user) {
    if (!email.isConfigured())
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Admin sign-in email is not configured. Set RESEND_API_KEY and RESEND_FROM.");
    tokens.deleteByUserUserIdAndPurpose(user.getUserId(), ADMIN_LOGIN);
    String code = String.format("%06d", random.nextInt(1_000_000));
    EmailActionToken token = new EmailActionToken();
    token.setUser(user);
    token.setPurpose(ADMIN_LOGIN);
    token.setTokenHash(encoder.encode(code));
    token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
    tokens.save(token);
    email.send(user.getEmail(), "Your GemVault admin sign-in code",
        "<p>Hello " + safe(user.getUsername()) + ",</p><p>Enter this code on the GemVault sign-in page:</p>"
            + "<p style=\"font-size:28px;letter-spacing:8px;font-weight:bold\">" + code + "</p>"
            + "<p>The code expires in 10 minutes and can be used once. If you did not request it, ignore this email.</p>");
  }

  @Transactional(noRollbackFor = IllegalArgumentException.class)
  public User consumeAdminCode(User user, String code) {
    EmailActionToken token = tokens.findByUserUserIdAndPurposeAndUsedAtIsNull(user.getUserId(), ADMIN_LOGIN)
        .orElseThrow(() -> new IllegalArgumentException("No active admin code. Sign in again to request one."));
    if (token.getExpiresAt().isBefore(Instant.now()) || token.getFailedAttempts() >= 5)
      throw new IllegalArgumentException("This code has expired or is locked. Sign in again for a new code.");
    if (code == null || !code.matches("^[0-9]{6}$") || !encoder.matches(code, token.getTokenHash())) {
      token.setFailedAttempts(token.getFailedAttempts() + 1);
      if (token.getFailedAttempts() >= 5)
        token.setUsedAt(Instant.now());
      tokens.save(token);
      throw new IllegalArgumentException("Incorrect code. Check the email and try again.");
    }
    token.setUsedAt(Instant.now());
    tokens.save(token);
    return user;
  }

  private void send(User user, String purpose, Duration expiry, String path, String subject, String copy) {
    tokens.deleteByUserUserIdAndPurpose(user.getUserId(), purpose);
    String rawToken = randomToken();
    EmailActionToken token = new EmailActionToken();
    token.setUser(user);
    token.setPurpose(purpose);
    token.setTokenHash(hash(rawToken));
    token.setExpiresAt(Instant.now().plus(expiry));
    tokens.save(token);
    String link = appUrls.resolve() + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    String html = "<p>Hello " + safe(user.getUsername()) + ",</p><p>" + copy + "</p>"
        + "<p><a href=\"" + link + "\">Continue to GemVault</a></p>"
        + "<p>This link expires in " + expiry.toHours()
        + " hour(s). If you did not request this, you can ignore this email.</p>";
    email.send(user.getEmail(), subject, html);
  }

  @Transactional
  public User consume(String rawToken, String purpose) {
    EmailActionToken token = tokens.findByTokenHashAndPurposeAndUsedAtIsNull(hash(rawToken), purpose)
        .orElseThrow(() -> new NoSuchElementException("This email link is invalid or has already been used"));
    if (token.getExpiresAt().isBefore(Instant.now()))
      throw new IllegalArgumentException("This email link has expired. Request a new one.");
    token.setUsedAt(Instant.now());
    tokens.save(token);
    return token.getUser();
  }

  @Transactional
  public User consumePasswordOrInvitation(String rawToken) {
    String tokenHash = hash(rawToken);
    EmailActionToken token = tokens.findByTokenHashAndPurposeAndUsedAtIsNull(tokenHash, RESET_PASSWORD)
        .or(() -> tokens.findByTokenHashAndPurposeAndUsedAtIsNull(tokenHash, INVITATION))
        .orElseThrow(() -> new NoSuchElementException("This password link is invalid or has already been used"));
    if (token.getExpiresAt().isBefore(Instant.now()))
      throw new IllegalArgumentException("This password link has expired. Request a new one.");
    token.setUsedAt(Instant.now());
    tokens.save(token);
    return token.getUser();
  }

  private String randomToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Could not secure email token", exception);
    }
  }

  private String safe(String value) {
    return value == null ? "there" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
