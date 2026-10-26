package com.examly.springapp.service;

import com.examly.springapp.dto.DashboardDto.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.repository.projection.DashboardOrder;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
  private final JewelleryOrderRepo orders;
  private final JewelleryItemRepo items;
  private final UserRepo users;

  public DashboardService(JewelleryOrderRepo orders, JewelleryItemRepo items, UserRepo users) {
    this.orders = orders;
    this.items = items;
    this.users = users;
  }

  @Transactional(readOnly = true)
  public Overview overview(LocalDate requestedFrom, LocalDate requestedTo) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    LocalDate to = requestedTo == null ? today : requestedTo;
    LocalDate from = requestedFrom == null ? to.minusDays(29) : requestedFrom;
    long days = ChronoUnit.DAYS.between(from, to) + 1;
    if (days < 1 || days > 366 || to.isAfter(today))
      throw new IllegalArgumentException("Choose a date range of 1 to 366 days ending today or earlier");
    LocalDate previousTo = from.minusDays(1), previousFrom = from.minusDays(days);
    List<DashboardOrder> rows = orders.findDashboardOrders(previousFrom, to);
    List<DashboardOrder> current = rows.stream().filter(o -> !o.getOrderDate().isBefore(from)).toList();
    List<DashboardOrder> previous = rows.stream().filter(o -> o.getOrderDate().isBefore(from)).toList();
    List<DashboardOrder> retained = current.stream().filter(this::retainedSale).toList();
    double sales = sumSales(retained);
    var stock = items.findDashboardStock();
    var alerts = stock.stream().filter(i -> i.getStockQuantity() <= i.getReorderThreshold()).toList();
    Summary summary = new Summary(sales, sumSales(previous.stream().filter(this::retainedSale).toList()),
        current.size(), previous.size(), retained.stream().mapToLong(o -> o.getQuantity()).sum(),
        retained.size(), retained.isEmpty() ? 0 : money(sales / retained.size()),
        money(current.stream().filter(o -> o.getRefundStatus() != null
            && !List.of("failed", "null", "").contains(o.getRefundStatus().toLowerCase(Locale.ROOT)))
            .mapToDouble(this::amount).sum()),
        users.countByUserRole("User"), stock.size(), stock.stream().mapToLong(i -> i.getStockQuantity()).sum(),
        alerts.size(), stock.stream().filter(i -> i.getStockQuantity() == 0).count());

    boolean monthly = days > 90;
    Map<LocalDate, double[]> trend = new TreeMap<>();
    LocalDate firstBucket = monthly ? from.withDayOfMonth(1) : from;
    for (LocalDate date = firstBucket; !date.isAfter(to); date = monthly ? date.plusMonths(1) : date.plusDays(1))
      trend.put(date, new double[2]);
    for (DashboardOrder order : current) {
      double[] bucket = trend.get(monthly ? order.getOrderDate().withDayOfMonth(1) : order.getOrderDate());
      bucket[1]++;
      if (retainedSale(order))
        bucket[0] += amount(order);
    }
    Map<String, Long> statuses = new LinkedHashMap<>();
    for (String status : List.of("Pending", "Approved", "Delivered", "AwaitingPayment", "Cancelled", "Rejected"))
      statuses.put(status, 0L);
    for (DashboardOrder order : current)
      statuses.merge(order.getStatus() == null ? "Unknown" : order.getStatus(), 1L, Long::sum);
    Map<String, double[]> categories = new TreeMap<>();
    Map<Long, TopPiece> top = new HashMap<>();
    for (DashboardOrder order : retained) {
      String category = order.getCategory() == null ? "Other" : order.getCategory();
      double[] values = categories.computeIfAbsent(category, ignored -> new double[2]);
      values[0] += amount(order);
      values[1] += order.getQuantity();
      TopPiece existing = top.get(order.getItemId());
      top.put(order.getItemId(), new TopPiece(order.getItemId(), order.getItemName(),
          order.getQuantity() + (existing == null ? 0 : existing.units()),
          money(amount(order) + (existing == null ? 0 : existing.sales()))));
    }
    return new Overview(from, to, previousFrom, previousTo, Instant.now(), monthly ? "month" : "day", summary,
        trend.entrySet().stream().map(e -> new Trend(e.getKey(), money(e.getValue()[0]), (long) e.getValue()[1]))
            .toList(),
        statuses.entrySet().stream().map(e -> new StatusCount(e.getKey(), e.getValue())).toList(),
        categories.entrySet().stream()
            .map(e -> new CategorySales(e.getKey(), money(e.getValue()[0]), (long) e.getValue()[1]))
            .sorted(Comparator.comparingDouble(CategorySales::sales).reversed()).toList(),
        top.values().stream().sorted(Comparator.comparingLong(TopPiece::units).reversed()
            .thenComparing(Comparator.comparingDouble(TopPiece::sales).reversed()).thenComparing(TopPiece::itemId))
            .limit(5).toList(),
        alerts.stream().limit(6).map(i -> new StockAlert(i.getItemId(), i.getItemName(), i.getSku(),
            i.getStockQuantity(), i.getReorderThreshold())).toList(),
        current.stream().sorted(Comparator.comparing(DashboardOrder::getOrderId).reversed()).limit(6)
            .map(o -> new RecentOrder(o.getOrderId(), o.getItemName(), o.getBuyerName(), o.getOrderDate(),
                o.getStatus(), paymentLabel(o), o.getQuantity(), amount(o)))
            .toList());
  }

  private boolean retainedSale(DashboardOrder order) {
    if (List.of("Cancelled", "Rejected", "AwaitingPayment").contains(Objects.toString(order.getStatus(), "")))
      return false;
    // Orders created before payment checkout existed are recognised only after
    // delivery.
    return order.getCheckoutId() == null ? "Delivered".equals(order.getStatus())
        : order.getPaymentStatus() == null || "Paid".equals(order.getPaymentStatus());
  }

  private String paymentLabel(DashboardOrder order) {
    if (order.getCheckoutId() == null)
      return "";
    return order.getPaymentStatus() == null ? "Paid" : order.getPaymentStatus();
  }

  private double amount(DashboardOrder order) {
    return order.getTotalPrice() == null ? 0 : order.getTotalPrice();
  }

  private double sumSales(List<DashboardOrder> rows) {
    return money(rows.stream().mapToDouble(this::amount).sum());
  }

  private double money(double value) {
    return BillingCalculator.round(value);
  }
}
