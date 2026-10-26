package com.examly.springapp.service;

import java.util.List;

public record LowStockAlertEvent(List<LowStockAlert> items) {
  public record LowStockAlert(String itemName, String sku, int stockQuantity, int reorderThreshold) {}
}
