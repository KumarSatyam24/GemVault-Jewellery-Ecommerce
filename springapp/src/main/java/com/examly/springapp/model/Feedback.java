package com.examly.springapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_feedback_user_item", columnNames = { "userId", "itemId" }))
public class Feedback {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long feedbackId;

  @Column(length = 2000)
  @NotBlank
  @Size(max = 2000)
  private String feedbackText;

  @NotNull
  @Min(1)
  @Max(5)
  private Integer rating;

  private LocalDate date;

  @ManyToOne(optional = false)
  @JoinColumn(name = "userId")
  private User user;

  @ManyToOne
  @JoinColumn(name = "itemId")
  private JewelleryItem item;

  public Feedback() {
  }

  public Long getFeedbackId() {
    return feedbackId;
  }

  public void setFeedbackId(Long feedbackId) {
    this.feedbackId = feedbackId;
  }

  public String getFeedbackText() {
    return feedbackText;
  }

  public void setFeedbackText(String feedbackText) {
    this.feedbackText = feedbackText;
  }

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer rating) {
    this.rating = rating;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public JewelleryItem getItem() {
    return item;
  }

  public void setItem(JewelleryItem item) {
    this.item = item;
  }

  @Transient
  public Integer getUserId() {
    return user == null ? null : user.getUserId();
  }

  @Transient
  public Long getItemId() {
    return item == null ? null : item.getItemId();
  }

  public void setUserId(Integer id) {
    if (id != null) {
      user = new User();
      user.setUserId(id);
    }
  }

  public void setItemId(Long id) {
    if (id != null) {
      item = new JewelleryItem();
      item.setItemId(id);
    }
  }
}
