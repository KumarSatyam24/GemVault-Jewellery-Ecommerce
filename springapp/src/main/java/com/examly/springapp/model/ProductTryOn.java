package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "product_try_on", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "item_id" }))
public class ProductTryOn {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "item_id", nullable = false)
  private JewelleryItem item;

  @Lob
  @Column(nullable = false, columnDefinition = "LONGBLOB")
  private byte[] image;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Instant generatedAt;

  public Long getId() {
    return id;
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

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}
