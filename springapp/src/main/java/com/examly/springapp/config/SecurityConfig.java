package com.examly.springapp.config;

import com.examly.springapp.exceptions.GlobalExceptionHandler;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter filter,
            JwtAuthenticationEntryPoint entryPoint,
            GlobalExceptionHandler errors,
            GoogleOAuthSuccessHandler googleSuccess,
            GoogleOAuthFailureHandler googleFailure,
            CookieOAuthAuthorizationRequestRepository oauthRequests,
            @Value("${gemvault.google.enabled:false}") boolean googleEnabled)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(
                                googleEnabled ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint(entryPoint)
                                .accessDeniedHandler(
                                        (request, response, denied) -> errors.sendUnauthorized(response,
                                                "Access denied")))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()
                                .requestMatchers(
                                        "/api/register",
                                        "/api/login",
                                        "/api/admin-login/verify",
                                        "/api/email/verify",
                                        "/api/email/verify/resend",
                                        "/api/password/forgot",
                                        "/api/password/reset",
                                        "/api/chat",
                                        "/api/chat/**")
                                .permitAll()
                                .requestMatchers("/oauth2/**", "/login/oauth2/**")
                                .permitAll()
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/admin/buyers/invite")
                                .hasRole("Admin")
                                .requestMatchers("/api/admin/dashboard", "/api/admin/dashboard/**")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.GET, "/api/metal-rates")
                                .hasAnyRole("Admin", "User")
                                .requestMatchers("/api/wishlist/**", "/api/recommendations/**")
                                .hasRole("User")
                                .requestMatchers("/api/try-on/**")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.GET, "/api/jewellery-item/low-stock")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.GET, "/api/jewellery-item/export",
                                        "/api/jewellery-item/import-template")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.GET, "/api/jewellery-item", "/api/jewellery-item/*",
                                        "/api/jewellery-item/*/reviews", "/api/jewellery-item/*/similar")
                                .permitAll()
                                .requestMatchers("/api/jewellery-item/**")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.POST, "/api/checkout")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.POST, "/api/checkout/razorpay/webhook")
                                .permitAll()
                                .requestMatchers("/api/checkout/**")
                                .hasRole("User")
                                .requestMatchers("/api/profile", "/api/profile/**")
                                .authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/jewellery-order/*/cancel")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.GET, "/api/jewellery-order/user/*")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.GET, "/api/jewellery-order")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.GET, "/api/jewellery-order/*")
                                .hasAnyRole("Admin", "User")
                                .requestMatchers(HttpMethod.POST, "/api/jewellery-order")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.PUT, "/api/jewellery-order/*")
                                .hasRole("Admin")
                                .requestMatchers(HttpMethod.DELETE, "/api/jewellery-order/*")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.GET, "/api/feedback", "/api/feedback/item/*")
                                .hasAnyRole("Admin", "User")
                                .requestMatchers(HttpMethod.GET, "/api/feedback/user/*")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.GET, "/api/feedback/*")
                                .hasAnyRole("Admin", "User")
                                .requestMatchers(HttpMethod.POST, "/api/feedback")
                                .hasRole("User")
                                .requestMatchers(HttpMethod.DELETE, "/api/feedback/*")
                                .hasRole("User")
                                .requestMatchers("/api/users")
                                .hasRole("Admin")
                                .anyRequest()
                                .authenticated());
        if (googleEnabled) {
            http.oauth2Login(
                    oauth -> oauth.authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestRepository(oauthRequests))
                            .successHandler(googleSuccess).failureHandler(googleFailure));
        }
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:8081",
                        "http://127.0.0.1:8081",
                        "https://*.premiumproject.examly.io"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
