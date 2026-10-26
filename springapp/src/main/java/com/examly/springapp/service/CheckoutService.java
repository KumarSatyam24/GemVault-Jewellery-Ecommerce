package com.examly.springapp.service;

import com.examly.springapp.dto.CheckoutRequest;
import com.examly.springapp.dto.CheckoutResult;
import com.examly.springapp.dto.PaymentCheckout;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.model.Checkout;
import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.model.SavedAddress;
import com.examly.springapp.repository.CheckoutRepo;
import com.examly.springapp.repository.SavedAddressRepo;
import com.examly.springapp.repository.UserRepo;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {
  private final CheckoutRepo checkouts;
  private final SavedAddressRepo addresses;
  private final UserRepo users;
  private final JewelleryOrderService orders;
  private final RazorpayTestClient razorpay;

  public CheckoutService(
      CheckoutRepo checkouts,
      SavedAddressRepo addresses,
      UserRepo users,
      JewelleryOrderService orders,
      RazorpayTestClient razorpay) {
    this.checkouts = checkouts;
    this.addresses = addresses;
    this.users = users;
    this.orders = orders;
    this.razorpay = razorpay;
  }

  @Transactional
  public PaymentCheckout.Start prepare(Integer userId, CheckoutRequest request) {
    if (!razorpay.configured())
      throw new BookingException("Razorpay test payment is not configured. Add test keys to the backend environment.");
    SavedAddress address = addresses
        .findByAddressIdAndUserUserId(request.getAddressId(), userId)
        .orElseThrow(() -> new BookingException("Select one of your saved addresses"));
    Checkout checkout = new Checkout();
    checkout.setUser(
        users.findById(userId).orElseThrow(() -> new BookingException("Buyer not found")));
    checkout.setDeliveryAddress(address.formatted());
    checkout.setTotalPrice(0.0);
    checkout.setPaymentStatus("Pending");
    checkout = checkouts.save(checkout);

    List<JewelleryOrder> lines = new ArrayList<>();
    Set<Long> itemIds = new HashSet<>();
    double total = 0;
    for (CheckoutRequest.Line line : request.getItems()) {
      if (!itemIds.add(line.getItemId()))
        throw new BookingException("Each item may appear only once");
      JewelleryOrder input = new JewelleryOrder();
      input.setUserId(userId);
      input.setItemId(line.getItemId());
      input.setQuantity(line.getQuantity());
      input.setDeliveryAddress(checkout.getDeliveryAddress());
      input.setComments(request.getComments());
      input.setCheckout(checkout);
      JewelleryOrder saved = orders.addOrder(input);
      saved.setStatus("AwaitingPayment");
      total += saved.getTotalPrice();
      lines.add(saved);
    }
    checkout.setTotalPrice(total);
    long amountPaise = BigDecimal.valueOf(total).movePointRight(2).longValueExact();
    if (amountPaise < 100)
      throw new BookingException("The checkout total must be at least ₹1");
    JsonNode paymentOrder = razorpay.createOrder(amountPaise, "gv-" + checkout.getCheckoutId());
    String orderId = paymentOrder.path("id").asText();
    if (!orderId.matches("order_[A-Za-z0-9]+")
        || paymentOrder.path("amount").asLong() != amountPaise
        || !"INR".equals(paymentOrder.path("currency").asText()))
      throw new BookingException("The payment gateway returned an invalid order");
    checkout.setRazorpayOrderId(orderId);
    checkout.setAmountPaise(amountPaise);
    checkout.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(30));
    checkouts.save(checkout);
    var buyer = checkout.getUser();
    return new PaymentCheckout.Start(checkout.getCheckoutId(), orderId, razorpay.keyId(),
        amountPaise, "INR", buyer.getUsername(), buyer.getEmail(), buyer.getMobileNumber());
  }
}
