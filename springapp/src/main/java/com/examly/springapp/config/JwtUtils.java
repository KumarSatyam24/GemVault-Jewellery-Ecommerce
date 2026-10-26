package com.examly.springapp.config;

import com.examly.springapp.model.User;
import io.jsonwebtoken.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
  private final byte[] key;

  public JwtUtils(@Value("${gemvault.jwt.secret}") String secret) {
    this.key = secret.getBytes(StandardCharsets.UTF_8);
  }

  public String generateToken(User user) {
    Date now = new Date();
    return Jwts.builder()
        .setSubject(user.getEmail())
        .claim("role", user.getUserRole())
        .claim("userId", user.getUserId())
        .claim("username", user.getUsername())
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + 24L * 60 * 60 * 1000))
        .signWith(SignatureAlgorithm.HS256, key)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
  }
}
