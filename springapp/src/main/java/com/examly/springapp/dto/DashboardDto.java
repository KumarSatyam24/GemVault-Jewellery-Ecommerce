package com.examly.springapp.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDto {
        private DashboardDto() {
        }

        public record Summary(double netSales, double previousNetSales, long orders, long previousOrders,
                        long unitsSold, long retainedOrders, double averageOrderValue, double refundsInitiated,
                        long buyers, long activePieces, long stockUnits, long lowStockCount, long outOfStockCount) {
        }

        public record Trend(LocalDate date, double sales, long orders) {
        }

        public record StatusCount(String status, long count) {
        }

        public record CategorySales(String category, double sales, long units) {
        }

        public record TopPiece(Long itemId, String itemName, long units, double sales) {
        }

        public record StockAlert(Long itemId, String itemName, String sku, int stock, int threshold) {
        }

        public record RecentOrder(Long orderId, String itemName, String buyerName,
                        LocalDate date, String status, String paymentStatus, int quantity, double total) {
        }

        public record Overview(LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo,
                        Instant generatedAt, String granularity, Summary summary, List<Trend> trend,
                        List<StatusCount> statuses, List<CategorySales> categories, List<TopPiece> topPieces,
                        List<StockAlert> stockAlerts, List<RecentOrder> recentOrders) {
        }
}
