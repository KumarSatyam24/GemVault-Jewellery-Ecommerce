package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import com.examly.springapp.service.BillingCalculator;

@Entity
public class JewelleryOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long orderId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "userId")
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "itemId")
  private JewelleryItem item;

  @NotNull
  @Min(1)
  private Integer quantity;

  private LocalDate orderDate;

  private String status;

  @ManyToOne
  @JoinColumn(name = "checkout_id")
  @JsonIgnore
  private Checkout checkout;

  @Column(length = 500)
  private String cancellationReason;

  @NotBlank
  private String deliveryAddress;

  @Column(length = 2000)
  private String comments;

  @JsonIgnore
  @Column(unique = true, length = 80)
  private String seedKey;

  public String getSeedKey() { return seedKey; }

  public void setSeedKey(String seedKey) { this.seedKey = seedKey; }

  private Double totalPrice;
  private String razorpayRefundId;
  private String refundStatus;

  private Double invoiceGrossWeight;
  private Double invoiceStoneWeight;
  private Double invoiceNetMetalWeight;
  private Double invoiceMetalRate;
  private Double invoiceMetalValue;
  private Double invoiceStoneValue;
  private Double invoiceMakingCharges;
  private Double invoiceVaCharges;
  private Double invoiceDiscountAmount;
  private Double invoiceTaxableAmount;
  private Double invoiceCgstRate;
  private Double invoiceCgstAmount;
  private Double invoiceSgstRate;
  private Double invoiceSgstAmount;
  private Double invoiceRoundOff;
  private String invoiceItemName;
  private String invoiceSku;
  private String invoiceMaterial;
  private String invoicePurity;

  public JewelleryOrder() {
  }

  public Checkout getCheckout() {
    return checkout;
  }

  public void setCheckout(Checkout checkout) {
    this.checkout = checkout;
  }

  @Transient
  public Long getCheckoutId() {
    return checkout == null ? null : checkout.getCheckoutId();
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
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

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public LocalDate getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDate orderDate) {
    this.orderDate = orderDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(String deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public Double getTotalPrice() {
    return totalPrice;
  }

  public String getRazorpayRefundId() {
    return razorpayRefundId;
  }

  public void setRazorpayRefundId(String razorpayRefundId) {
    this.razorpayRefundId = razorpayRefundId;
  }

  public String getRefundStatus() {
    return refundStatus;
  }

  public void setRefundStatus(String refundStatus) {
    this.refundStatus = refundStatus;
  }

  public void setTotalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
  }

  public void setBillingSnapshot(BillingCalculator.Breakdown bill, int quantity) {
    invoiceGrossWeight = bill.grossWeight() * quantity;
    invoiceStoneWeight = bill.stoneWeight() * quantity;
    invoiceNetMetalWeight = bill.netMetalWeight() * quantity;
    invoiceMetalRate = bill.metalRate();
    invoiceMetalValue = bill.metalValue() * quantity;
    invoiceStoneValue = bill.stoneValue() * quantity;
    invoiceMakingCharges = bill.makingCharges() * quantity;
    invoiceVaCharges = bill.vaCharges() * quantity;
    invoiceDiscountAmount = bill.discountAmount() * quantity;
    invoiceTaxableAmount = bill.taxableAmount() * quantity;
    invoiceCgstRate = bill.cgstRate();
    invoiceCgstAmount = bill.cgstAmount() * quantity;
    invoiceSgstRate = bill.sgstRate();
    invoiceSgstAmount = bill.sgstAmount() * quantity;
    invoiceRoundOff = bill.roundOff() * quantity;
  }

  public void setInvoiceItemSnapshot(JewelleryItem item) {
    invoiceItemName = item.getItemName();
    invoiceSku = item.getSku();
    invoiceMaterial = item.getMaterial();
    invoicePurity = item.getPurity();
  }

  public String getInvoiceItemName() {
    return invoiceItemName;
  }

  public String getInvoiceSku() {
    return invoiceSku;
  }

  public String getInvoiceMaterial() {
    return invoiceMaterial;
  }

  public String getInvoicePurity() {
    return invoicePurity;
  }

  public BillingCalculator.Breakdown getBillingSnapshot() {
    return new BillingCalculator.Breakdown(value(invoiceGrossWeight), value(invoiceStoneWeight),
        value(invoiceNetMetalWeight), value(invoiceMetalRate), value(invoiceMetalValue),
        value(invoiceStoneValue), value(invoiceMakingCharges), value(invoiceVaCharges),
        value(invoiceDiscountAmount), value(invoiceTaxableAmount), value(invoiceCgstRate),
        value(invoiceCgstAmount), value(invoiceSgstRate), value(invoiceSgstAmount),
        value(invoiceRoundOff), value(totalPrice));
  }

  private double value(Double value) {
    return value == null ? 0d : value;
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
