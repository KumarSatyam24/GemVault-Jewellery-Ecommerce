package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class ProductSearchHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long searchHistoryId;
  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;
  @Column(nullable = false, length = 120)
  private String searchTerm;
  @Column(nullable = false)
  private Instant searchedAt = Instant.now();

  public Long getSearchHistoryId() {
    return searchHistoryId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getSearchTerm() {
    return searchTerm;
  }

  public void setSearchTerm(String searchTerm) {
    this.searchTerm = searchTerm;
  }

  public Instant getSearchedAt() {
    return searchedAt;
  }
}
