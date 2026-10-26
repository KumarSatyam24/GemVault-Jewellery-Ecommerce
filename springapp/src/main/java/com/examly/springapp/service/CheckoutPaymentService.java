package com.examly.springapp.service;

import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.dto.CheckoutResult;
import com.examly.springapp.dto.PaymentCheckout;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.model.Checkout;
import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.repository.CheckoutRepo;
import com.examly.springapp.repository.JewelleryItemRepo;
import com.examly.springapp.repository.JewelleryOrderRepo;
import com.examly.springapp.repository.WishlistItemRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CheckoutPaymentService {
  private final CheckoutRepo checkouts;
  private final JewelleryOrderRepo orders;
  private final JewelleryItemRepo items;
  private final WishlistItemRepo wishlist;
  private final RazorpayTestClient razorpay;
  private final ApplicationEventPublisher events;
  private final ObjectMapper json;
  private final TransactionTemplate transactions;

  public CheckoutPaymentService(CheckoutRepo checkouts, JewelleryOrderRepo orders,
      JewelleryItemRepo items, WishlistItemRepo wishlist, RazorpayTestClient razorpay,
      ApplicationEventPublisher events, ObjectMapper json, TransactionTemplate transactions) {
    this.checkouts = checkouts;
    this.orders = orders;
    this.items = items;
    this.wishlist = wishlist;
    this.razorpay = razorpay;
    this.events = events;
    this.json = json;
    this.transactions = transactions;
  }

  public PaymentCheckout.Status verify(Integer userId, PaymentCheckout.Verify input) {
    Checkout checkout = owned(input.checkoutId(), userId);
    if (!input.razorpayOrderId().equals(checkout.getRazorpayOrderId())
        || !razorpay.validCheckoutSignature(checkout.getRazorpayOrderId(),
            input.razorpayPaymentId(), input.razorpaySignature()))
      throw new BookingException("Payment verification failed");
    return recordCaptured(checkout.getCheckoutId(), input.razorpayPaymentId());
  }

  public PaymentCheckout.Status status(Integer userId, Long checkoutId) {
    Checkout checkout = owned(checkoutId, userId);
    if ("Pending".equals(checkout.getPaymentStatus())
        || "Expired".equals(checkout.getPaymentStatus()))
      reconcile(checkoutId);
    Checkout refreshed = owned(checkoutId, userId);
    return view(refreshed);
  }

  public void webhook(String rawBody, String signature) {
    if (!razorpay.validWebhookSignature(rawBody, signature))
      throw new BookingException("Invalid Razorpay webhook signature");
    try {
      JsonNode event = json.readTree(rawBody);
      if (!"payment.captured".equals(event.path("event").asText()))
        return;
      JsonNode payment = event.path("payload").path("payment").path("entity");
      String orderId = payment.path("order_id").asText();
      String paymentId = payment.path("id").asText();
      checkouts.findByRazorpayOrderId(orderId)
          .ifPresent(checkout -> recordCaptured(checkout.getCheckoutId(), paymentId));
    } catch (BookingException exception) {
      throw exception;
    } catch (Exception invalid) {
      throw new BookingException("Invalid Razorpay webhook payload");
    }
  }

  private Checkout owned(Long checkoutId, Integer userId) {
    Checkout checkout = checkouts.findById(checkoutId)
        .orElseThrow(() -> new NoSuchElementException("Checkout not found"));
    if (!checkout.getUser().getUserId().equals(userId))
      throw new NoSuchElementException("Checkout not found");
    return checkout;
  }

  public PaymentCheckout.Status recordCaptured(Long checkoutId, String paymentId) {
    return transactions.execute(ignored -> {
      Checkout checkout = checkouts.findLockedById(checkoutId)
          .orElseThrow(() -> new NoSuchElementException("Checkout not found"));
      if ("Paid".equals(checkout.getPaymentStatus())) {
        if (!paymentId.equals(checkout.getRazorpayPaymentId()))
          throw new BookingException("This checkout was paid with another payment");
        return view(checkout);
      }
      JsonNode payment = razorpay.fetchPayment(paymentId);
      if (!checkout.getRazorpayOrderId().equals(payment.path("order_id").asText())
          || checkout.getAmountPaise() != payment.path("amount").asLong()
          || !"INR".equals(payment.path("currency").asText()))
        throw new BookingException("Payment does not match this checkout");
      if (!"captured".equals(payment.path("status").asText()))
        throw new BookingException("Payment is awaiting capture. Check its status shortly.");
      if ("Refunded".equals(checkout.getPaymentStatus()))
        return view(checkout);
      if ("Expired".equals(checkout.getPaymentStatus())) {
        String receipt = "gv-expired-" + checkoutId;
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
          refund = razorpay.refund(paymentId, checkout.getAmountPaise(), receipt);
        if (checkout.getAmountPaise() != refund.path("amount").asLong()
            || !paymentId.equals(refund.path("payment_id").asText()))
          throw new BookingException("Late payment refund could not be confirmed");
        checkout.setPaymentStatus("Refunded");
        checkout.setRazorpayPaymentId(paymentId);
        checkout.setRazorpayRefundId(refund.path("id").asText());
        return view(checkout);
      }
      if (!"Pending".equals(checkout.getPaymentStatus()))
        throw new BookingException("This checkout is no longer awaiting payment");
      checkout.setPaymentStatus("Paid");
      checkout.setRazorpayPaymentId(paymentId);
      List<JewelleryOrder> lines = orders.findByCheckoutCheckoutId(checkoutId);
      for (JewelleryOrder line : lines) {
        line.setStatus("Pending");
        wishlist.deleteByUserUserIdAndItemItemId(checkout.getUser().getUserId(), line.getItemId());
      }
      checkouts.save(checkout);
      List<LowStockAlertEvent.LowStockAlert> lowStock = lines.stream()
          .map(JewelleryOrder::getItem)
          .filter(item -> item.getStockQuantity() <= item.getReorderThreshold())
          .map(item -> new LowStockAlertEvent.LowStockAlert(item.getItemName(), item.getSku(),
              item.getStockQuantity(), item.getReorderThreshold()))
          .toList();
      if (!lowStock.isEmpty())
        events.publishEvent(new LowStockAlertEvent(lowStock));
      return view(checkout);
    });
  }

  public void reconcile(Long checkoutId) {
    Checkout checkout = checkouts.findById(checkoutId).orElse(null);
    if (checkout == null || !List.of("Pending", "Expired").contains(checkout.getPaymentStatus())
        || checkout.getRazorpayOrderId() == null)
      return;
    JsonNode payments = razorpay.fetchOrderPayments(checkout.getRazorpayOrderId()).path("items");
    if (payments.isArray())
      for (JsonNode payment : payments) {
        if ("captured".equals(payment.path("status").asText())) {
          recordCaptured(checkoutId, payment.path("id").asText());
          return;
        }
      }
  }

  public void expireIfUnpaid(Long checkoutId) {
    transactions.executeWithoutResult(ignored -> {
      Checkout checkout = checkouts.findLockedById(checkoutId).orElse(null);
      if (checkout == null || !"Pending".equals(checkout.getPaymentStatus())
          || checkout.getPaymentExpiresAt().isAfter(LocalDateTime.now()))
        return;
      JsonNode payments = razorpay.fetchOrderPayments(checkout.getRazorpayOrderId()).path("items");
      if (payments.isArray())
        for (JsonNode payment : payments) {
          if ("captured".equals(payment.path("status").asText())) {
            recordCaptured(checkoutId, payment.path("id").asText());
            return;
          }
          if ("authorized".equals(payment.path("status").asText()))
            return;
        }
      for (JewelleryOrder line : orders.findByCheckoutCheckoutId(checkoutId)) {
        var item = items.findLockedById(line.getItemId()).orElseThrow();
        item.setStockQuantity(item.getStockQuantity() + line.getQuantity());
        line.setStatus("Cancelled");
        line.setCancellationReason("Payment was not completed");
      }
      checkout.setPaymentStatus("Expired");
    });
  }

  @Scheduled(fixedDelay = 60000)
  public void expireOldPayments() {
    if (!razorpay.configured())
      return;
    for (Checkout checkout : checkouts.findTop100ByPaymentStatusOrderByCheckoutIdAsc("Pending")) {
      try {
        if (checkout.getPaymentExpiresAt() != null
            && checkout.getPaymentExpiresAt().isBefore(LocalDateTime.now()))
          expireIfUnpaid(checkout.getCheckoutId());
        else
          reconcile(checkout.getCheckoutId());
      } catch (RuntimeException unavailable) {
        // Keep the reservation if Razorpay cannot confirm that it is unpaid.
      }
    }
    for (Checkout checkout : checkouts.findTop100ByPaymentStatusOrderByCheckoutIdAsc("Expired")) {
      try {
        reconcile(checkout.getCheckoutId());
      } catch (RuntimeException unavailable) {
        // A captured late payment is retried until its refund is confirmed.
      }
    }
  }

  private PaymentCheckout.Status view(Checkout checkout) {
    CheckoutResult order = null;
    PaymentCheckout.Start payment = null;
    if ("Paid".equals(checkout.getPaymentStatus()))
      order = new CheckoutResult(checkout.getCheckoutId(), checkout.getTotalPrice(),
          orders.findByCheckoutCheckoutId(checkout.getCheckoutId()).stream()
              .map(ApiDto.OrderView::from).toList());
    if ("Pending".equals(checkout.getPaymentStatus())) {
      var buyer = checkout.getUser();
      payment = new PaymentCheckout.Start(checkout.getCheckoutId(),
          checkout.getRazorpayOrderId(), razorpay.keyId(), checkout.getAmountPaise(),
          "INR", buyer.getUsername(), buyer.getEmail(), buyer.getMobileNumber());
    }
    return new PaymentCheckout.Status(checkout.getCheckoutId(), checkout.getPaymentStatus(), order,
        payment);
  }
}
