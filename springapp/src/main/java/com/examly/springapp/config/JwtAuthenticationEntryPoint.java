package com.examly.springapp.config;

import com.examly.springapp.exceptions.GlobalExceptionHandler;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final GlobalExceptionHandler errors;

  public JwtAuthenticationEntryPoint(GlobalExceptionHandler errors) {
    this.errors = errors;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    errors.sendUnauthorized(response, "Authentication required");
  }
}
