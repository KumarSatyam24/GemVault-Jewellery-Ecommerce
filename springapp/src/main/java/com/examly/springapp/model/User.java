package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer userId;

  @Column(unique = true, nullable = false)
  private String email;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  private String username;

  private String mobileNumber;

  private String userRole;

  @Column(length = 20)
  private String authProvider = "PASSWORD";

  @Column(nullable = false)
  private boolean emailVerified;

  @Column(nullable = false)
  private boolean passwordResetRequired;

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  private List<SavedAddress> addresses = new ArrayList<>();

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  private List<JewelleryOrder> orders = new ArrayList<>();

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  private List<Feedback> feedbacks = new ArrayList<>();

  @OneToMany(mappedBy = "user")
  @JsonIgnore
  private List<Checkout> checkouts = new ArrayList<>();

  public User() {}

  public List<SavedAddress> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<SavedAddress> addresses) {
    this.addresses = addresses;
  }

  public List<JewelleryOrder> getOrders() {
    return orders;
  }

  public void setOrders(List<JewelleryOrder> orders) {
    this.orders = orders;
  }

  public List<Feedback> getFeedbacks() {
    return feedbacks;
  }

  public void setFeedbacks(List<Feedback> feedbacks) {
    this.feedbacks = feedbacks;
  }

  public List<Checkout> getCheckouts() {
    return checkouts;
  }

  public void setCheckouts(List<Checkout> checkouts) {
    this.checkouts = checkouts;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
  }

  public String getUserRole() {
    return userRole;
  }

  public void setUserRole(String userRole) {
    this.userRole = userRole;
  }

  public String getAuthProvider() { return authProvider; }

  public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

  public boolean isEmailVerified() { return emailVerified; }

  public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

  public boolean isPasswordResetRequired() { return passwordResetRequired; }

  public void setPasswordResetRequired(boolean passwordResetRequired) {
    this.passwordResetRequired = passwordResetRequired;
  }
}
