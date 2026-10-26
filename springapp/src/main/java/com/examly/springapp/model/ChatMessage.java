package com.examly.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_session", columnList = "session_id"))
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "session_id", nullable = false, length = 36)
  private String sessionId;
  private Integer ownerUserId;
  @Column(nullable = false, length = 1000)
  private String userMessage;
  @Lob
  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String botReply;
  private Long matchedFaqId;
  private Double similarityScore;
  @Column(nullable = false)
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public Integer getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Integer ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  public String getBotReply() {
    return botReply;
  }

  public void setBotReply(String botReply) {
    this.botReply = botReply;
  }

  public Long getMatchedFaqId() {
    return matchedFaqId;
  }

  public void setMatchedFaqId(Long matchedFaqId) {
    this.matchedFaqId = matchedFaqId;
  }

  public Double getSimilarityScore() {
    return similarityScore;
  }

  public void setSimilarityScore(Double similarityScore) {
    this.similarityScore = similarityScore;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
