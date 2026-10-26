package com.examly.springapp.config;

import com.examly.springapp.model.User;
import com.examly.springapp.service.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {
  private final UserServiceImpl users;
  private final JwtUtils jwt;
  private final AppUrlResolver appUrl;

  public GoogleOAuthSuccessHandler(UserServiceImpl users, JwtUtils jwt, AppUrlResolver appUrl) {
    this.users = users;
    this.jwt = jwt;
    this.appUrl = appUrl;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException {
    String target = appUrl.resolve(request);
    if (!(authentication.getPrincipal() instanceof OidcUser google)
        || !Boolean.TRUE.equals(google.getEmailVerified())
        || google.getEmail() == null || google.getEmail().isBlank()) {
      response.sendRedirect(target + "/login?oauthError=missing_verified_email");
      return;
    }
    try {
      User user = users.loginWithGoogle(google.getEmail(), google.getFullName());
      response.sendRedirect(target + "/oauth/callback#token=" + jwt.generateToken(user));
    } catch (IllegalArgumentException e) {
      response.sendRedirect(target + "/login?oauthError=account_not_eligible");
    }
  }
}
