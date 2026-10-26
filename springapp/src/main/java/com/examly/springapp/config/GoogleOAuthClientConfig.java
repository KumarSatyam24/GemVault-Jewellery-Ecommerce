package com.examly.springapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@ConditionalOnProperty(name = "gemvault.google.enabled", havingValue = "true")
public class GoogleOAuthClientConfig {
  @Bean
  public ClientRegistrationRepository googleClientRegistrationRepository(
      @Value("${gemvault.google.client-id}") String clientId,
      @Value("${gemvault.google.client-secret}") String clientSecret,
      @Value("${gemvault.google.redirect-uri}") String redirectUri) {
    if (clientId.isBlank() || clientSecret.isBlank()) {
      throw new IllegalStateException("Google OAuth is enabled but its client credentials are missing");
    }
    return new InMemoryClientRegistrationRepository(
        CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .scope("openid", "profile", "email")
            .redirectUri(redirectUri)
            .build());
  }
}
