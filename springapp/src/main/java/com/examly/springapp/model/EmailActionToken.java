package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_email_action_hash", columnList = "tokenHash", unique = true))
public class EmailActionToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, length = 40)
  private String purpose;

  @Column(nullable = false, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant usedAt;
  private Integer failedAttempts = 0;

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }

  public int getFailedAttempts() {
    return failedAttempts == null ? 0 : failedAttempts;
  }

  public void setFailedAttempts(int failedAttempts) {
    this.failedAttempts = failedAttempts;
  }
}
