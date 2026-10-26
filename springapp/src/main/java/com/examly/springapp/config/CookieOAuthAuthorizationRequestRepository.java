package com.examly.springapp.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/** Keeps the OAuth state through portal proxies and backend restarts. */
@Component
public class CookieOAuthAuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
  private static final String COOKIE = "GEMVAULT_OAUTH_REQUEST";
  private static final int MAX_AGE = 300;
  private final byte[] signingKey;

  public CookieOAuthAuthorizationRequestRepository(@Value("${gemvault.jwt.secret}") String secret) {
    this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if (!COOKIE.equals(cookie.getName())) continue;
      try {
        String[] parts = cookie.getValue().split("\\.", 2);
        if (parts.length != 2) return null;
        byte[] expected = sign(parts[0]);
        byte[] supplied = Base64.getUrlDecoder().decode(parts[1]);
        if (!MessageDigest.isEqual(expected, supplied)) return null;
        byte[] bytes = Base64.getUrlDecoder().decode(parts[0]);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
          input.setObjectInputFilter(info -> {
            if (info.depth() > 20 || info.references() > 200 || info.streamBytes() > 8192) {
              return ObjectInputFilter.Status.REJECTED;
            }
            Class<?> type = info.serialClass();
            if (type == null) return ObjectInputFilter.Status.UNDECIDED;
            while (type.isArray()) type = type.getComponentType();
            String name = type.getName();
            return type.isPrimitive() || name.startsWith("java.lang.")
                || name.startsWith("java.util.")
                || name.startsWith("org.springframework.security.oauth2.")
                ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
          });
          Object value = input.readObject();
          if (!(value instanceof OAuth2AuthorizationRequest authorization)) return null;
          String state = request.getParameter("state");
          return state != null && state.equals(authorization.getState()) ? authorization : null;
        }
      } catch (Exception ignored) {
        return null;
      }
    }
    return null;
  }

  @Override
  public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization,
      HttpServletRequest request, HttpServletResponse response) {
    if (authorization == null) {
      clear(response, request);
      return;
    }
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(authorization);
      }
      String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
      String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
      if (payload.length() + signature.length() + COOKIE.length() > 3800) {
        throw new IllegalStateException("Google authorization request exceeds browser cookie limit");
      }
      writeCookie(response, request, payload + "." + signature, MAX_AGE);
    } catch (Exception e) {
      throw new IllegalStateException("Could not save Google authorization request", e);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
      HttpServletResponse response) {
    OAuth2AuthorizationRequest authorization = loadAuthorizationRequest(request);
    clear(response, request);
    return authorization;
  }

  private byte[] sign(String payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
    return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
  }

  private void clear(HttpServletResponse response, HttpServletRequest request) {
    writeCookie(response, request, "", 0);
  }

  private void writeCookie(HttpServletResponse response, HttpServletRequest request,
      String value, int maxAge) {
    StringBuilder header = new StringBuilder(COOKIE).append('=').append(value)
        .append("; Path=/; Max-Age=").append(maxAge).append("; HttpOnly; SameSite=Lax");
    if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
      header.append("; Secure");
    }
    response.addHeader("Set-Cookie", header.toString());
  }
}
