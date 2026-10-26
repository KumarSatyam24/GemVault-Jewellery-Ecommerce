package com.examly.springapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AppUrlResolver {
  private final String localAppUrl;

  public AppUrlResolver(@Value("${gemvault.app.url:http://localhost:8081}") String localAppUrl) {
    this.localAppUrl = localAppUrl.replaceAll("/+$", "");
  }

  public String resolve() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return resolve(attributes.getRequest());
    }
    return localAppUrl;
  }

  public String resolve(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    String portalUrl = portalFrontendUrl(origin);
    if (portalUrl != null) return portalUrl;

    portalUrl = portalFrontendUrl("https://" + request.getServerName());
    if (portalUrl != null) return portalUrl;

    String forwardedHost = request.getHeader("X-Forwarded-Host");
    if (forwardedHost != null && !forwardedHost.contains(",")) {
      portalUrl = portalFrontendUrl("https://" + forwardedHost);
      if (portalUrl != null) return portalUrl;
    }
    if ("localhost".equalsIgnoreCase(request.getServerName())
        || "127.0.0.1".equals(request.getServerName())) {
      return "http://localhost:8081";
    }
    return localAppUrl;
  }

  private String portalFrontendUrl(String value) {
    if (value == null) return null;
    if (value.matches("(?i)^https://8081-[a-z0-9]+\\.premiumproject\\.examly\\.io/?$")) {
      return value.replaceAll("/+$", "");
    }
    if (value.matches("(?i)^https://8080-[a-z0-9]+\\.premiumproject\\.examly\\.io/?$")) {
      return value.replaceFirst("(?i)^https://8080-", "https://8081-").replaceAll("/+$", "");
    }
    return null;
  }
}
