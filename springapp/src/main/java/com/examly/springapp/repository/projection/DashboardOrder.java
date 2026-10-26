package com.examly.springapp.repository.projection;

import java.time.LocalDate;

public interface DashboardOrder {
  Long getOrderId();

  Long getItemId();

  String getItemName();

  String getCategory();

  Integer getQuantity();

  LocalDate getOrderDate();

  String getStatus();

  Double getTotalPrice();

  Long getCheckoutId();

  String getPaymentStatus();

  String getRefundStatus();

  String getBuyerName();
}
