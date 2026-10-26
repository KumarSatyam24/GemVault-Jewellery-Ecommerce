package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "item_id" }))
public class WishlistItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long wishlistItemId;
  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;
  @ManyToOne(optional = false)
  @JoinColumn(name = "item_id")
  @JsonIgnore
  private JewelleryItem item;

  public Long getWishlistItemId() {
    return wishlistItemId;
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
}
