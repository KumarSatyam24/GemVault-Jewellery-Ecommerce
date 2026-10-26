package com.examly.springapp.service;

import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.repository.JewelleryItemRepo;
import com.examly.springapp.repository.JewelleryOrderRepo;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRefundService {
  private final JewelleryOrderRepo orders;
  private final JewelleryItemRepo items;
  private final RazorpayTestClient razorpay;

  public PaymentRefundService(JewelleryOrderRepo orders, JewelleryItemRepo items,
      RazorpayTestClient razorpay) {
    this.orders = orders;
    this.items = items;
    this.razorpay = razorpay;
  }

  @Transactional
  public JewelleryOrder cancelPaid(Long id, String reason, String finalStatus) {
    JewelleryOrder order = orders.findLockedById(id)
        .orElseThrow(() -> new NoSuchElementException("Order not found"));
    if (!List.of("Pending", "Approved").contains(order.getStatus()))
      throw new BookingException("Only pending or approved orders can be cancelled");
    if (order.getCheckout() == null || order.getCheckout().getRazorpayPaymentId() == null)
      throw new BookingException("This order has no Razorpay payment to refund");
    String paymentId = order.getCheckout().getRazorpayPaymentId();
    String receipt = "gv-item-" + id;
    long amountPaise = BigDecimal.valueOf(order.getTotalPrice())
        .movePointRight(2).longValueExact();
    JsonNode refunds = razorpay.fetchRefunds(paymentId).path("items");
    JsonNode refund = null;
    if (refunds.isArray())
      for (JsonNode candidate : refunds) {
        if (receipt.equals(candidate.path("receipt").asText())) {
          refund = candidate;
          break;
        }
      }
    if (refund == null)
      refund = razorpay.refund(paymentId, amountPaise, receipt);
    if (!paymentId.equals(refund.path("payment_id").asText())
        || amountPaise != refund.path("amount").asLong()
        || !refund.path("id").asText().matches("rfnd_[A-Za-z0-9]+"))
      throw new BookingException("Razorpay did not confirm the refund");
    var item = items.findLockedById(order.getItemId()).orElseThrow();
    item.setStockQuantity(item.getStockQuantity() + order.getQuantity());
    order.setStatus(finalStatus);
    order.setCancellationReason(reason);
    order.setRazorpayRefundId(refund.path("id").asText());
    order.setRefundStatus(refund.path("status").asText("initiated"));
    return orders.save(order);
  }
}
