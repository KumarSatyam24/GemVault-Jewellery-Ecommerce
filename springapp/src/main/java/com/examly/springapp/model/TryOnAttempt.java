package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "try_on_attempt")
public class TryOnAttempt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private Integer userId;
  @Column(nullable = false)
  private Long itemId;
  @Column(nullable = false)
  private Instant attemptedAt;

  public TryOnAttempt() {
  }

  public TryOnAttempt(Integer userId, Long itemId) {
    this.userId = userId;
    this.itemId = itemId;
    this.attemptedAt = Instant.now();
  }

  public Integer getUserId() {
    return userId;
  }

  public Long getItemId() {
    return itemId;
  }

  public Instant getAttemptedAt() {
    return attemptedAt;
  }
}
