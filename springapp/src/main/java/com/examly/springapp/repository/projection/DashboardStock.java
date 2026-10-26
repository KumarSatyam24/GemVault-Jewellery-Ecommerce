package com.examly.springapp.repository.projection;

public interface DashboardStock {
  Long getItemId();

  String getItemName();

  String getSku();

  Integer getStockQuantity();

  Integer getReorderThreshold();
}
