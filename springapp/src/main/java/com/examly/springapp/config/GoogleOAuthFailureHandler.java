package com.examly.springapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthFailureHandler implements AuthenticationFailureHandler {
  private static final Logger log = LoggerFactory.getLogger(GoogleOAuthFailureHandler.class);
  private final AppUrlResolver appUrl;

  public GoogleOAuthFailureHandler(AppUrlResolver appUrl) {
    this.appUrl = appUrl;
  }

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {
    log.warn("Google OAuth callback failed: {}", exception.getMessage());
    response.sendRedirect(appUrl.resolve(request) + "/login?oauthError=google_failed");
  }
}
