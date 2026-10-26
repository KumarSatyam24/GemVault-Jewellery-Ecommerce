package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Checkout {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long checkoutId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;

  @OneToMany(mappedBy = "checkout")
  @JsonIgnore
  private List<JewelleryOrder> orders = new ArrayList<>();

  private LocalDate checkoutDate = LocalDate.now();
  private String deliveryAddress;
  private Double totalPrice;
  private String paymentStatus = "Pending";
  @Column(unique = true)
  private String razorpayOrderId;
  @Column(unique = true)
  private String razorpayPaymentId;
  private String razorpayRefundId;
  private Long amountPaise;
  private LocalDateTime paymentExpiresAt;

  public Long getCheckoutId() {
    return checkoutId;
  }

  public void setCheckoutId(Long checkoutId) {
    this.checkoutId = checkoutId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public List<JewelleryOrder> getOrders() {
    return orders;
  }

  public void setOrders(List<JewelleryOrder> orders) {
    this.orders = orders;
  }

  public LocalDate getCheckoutDate() {
    return checkoutDate;
  }

  public void setCheckoutDate(LocalDate checkoutDate) {
    this.checkoutDate = checkoutDate;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(String deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  public Double getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
  }

  public String getPaymentStatus() {
    return paymentStatus == null ? "Paid" : paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public String getRazorpayOrderId() {
    return razorpayOrderId;
  }

  public void setRazorpayOrderId(String razorpayOrderId) {
    this.razorpayOrderId = razorpayOrderId;
  }

  public String getRazorpayPaymentId() {
    return razorpayPaymentId;
  }

  public void setRazorpayPaymentId(String razorpayPaymentId) {
    this.razorpayPaymentId = razorpayPaymentId;
  }

  public String getRazorpayRefundId() {
    return razorpayRefundId;
  }

  public void setRazorpayRefundId(String razorpayRefundId) {
    this.razorpayRefundId = razorpayRefundId;
  }

  public Long getAmountPaise() {
    return amountPaise;
  }

  public void setAmountPaise(Long amountPaise) {
    this.amountPaise = amountPaise;
  }

  public LocalDateTime getPaymentExpiresAt() {
    return paymentExpiresAt;
  }

  public void setPaymentExpiresAt(LocalDateTime paymentExpiresAt) {
    this.paymentExpiresAt = paymentExpiresAt;
  }
}
