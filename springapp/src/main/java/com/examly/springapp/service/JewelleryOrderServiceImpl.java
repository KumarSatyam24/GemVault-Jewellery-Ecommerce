package com.examly.springapp.service;

import com.examly.springapp.exceptions.*;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JewelleryOrderServiceImpl implements JewelleryOrderService {
  private final JewelleryOrderRepo orders;
  private final JewelleryItemRepo items;
  private final UserRepo users;

  public JewelleryOrderServiceImpl(
      JewelleryOrderRepo orders, JewelleryItemRepo items, UserRepo users) {
    this.orders = orders;
    this.items = items;
    this.users = users;
  }

  public List<JewelleryOrder> getAllOrders() {
    return orders.findAll();
  }

  public List<JewelleryOrder> getOrdersByUserId(Long id) {
    return orders.findByUserUserId(Math.toIntExact(id));
  }

  public Optional<JewelleryOrder> getOrderById(Long id) {
    return orders.findById(id);
  }

  private JewelleryItem item(Long id) {
    if (id == null)
      throw new BookingException("Select an item");
    return items.findLockedById(id).orElseThrow(() -> new BookingException("Item not found"));
  }

  private void reserve(JewelleryItem item, int qty) {
    if (item.getStockQuantity() < qty)
      throw new InsufficientStockException("Only " + item.getStockQuantity() + " in stock");
    item.setStockQuantity(item.getStockQuantity() - qty);
  }

  @Transactional
  public JewelleryOrder addOrder(JewelleryOrder order) {
    if (order.getQuantity() == null || order.getQuantity() < 1)
      throw new BookingException("Quantity must be at least 1");
    JewelleryItem item = item(order.getItemId());
    if (Boolean.FALSE.equals(item.getActive()))
      throw new BookingException("Item is no longer available");
    if (order.getUserId() == null)
      throw new BookingException("Select a buyer");
    User user = users
        .findById(order.getUserId())
        .orElseThrow(() -> new BookingException("Buyer not found"));
    reserve(item, order.getQuantity());
    order.setOrderId(null);
    order.setUser(user);
    order.setItem(item);
    order.setStatus("Pending");
    if (order.getOrderDate() == null)
      order.setOrderDate(LocalDate.now());
    BillingCalculator.Breakdown bill = BillingCalculator.forItem(item);
    order.setBillingSnapshot(bill, order.getQuantity());
    order.setInvoiceItemSnapshot(item);
    order.setTotalPrice(BillingCalculator.round(bill.grandTotal() * order.getQuantity()));
    items.save(item);
    return orders.save(order);
  }

  @Transactional
  public JewelleryOrder updateOrder(Long id, JewelleryOrder input) {
    JewelleryOrder order = orders.findById(id).orElseThrow(() -> new NoSuchElementException("Order not found"));
    String status = input.getStatus();
    if ("AwaitingPayment".equals(order.getStatus()))
      throw new BookingException("This order is awaiting payment");
    if ("Rejected".equals(status) && order.getCheckout() != null
        && order.getCheckout().getRazorpayPaymentId() != null)
      throw new BookingException("Use the refund flow to reject a paid order");
    if ("Cancelled".equals(order.getStatus()) || "Delivered".equals(order.getStatus()))
      throw new BookingException("Cancelled or delivered orders cannot be changed");
    if (status == null || !List.of("Pending", "Approved", "Rejected", "Delivered").contains(status))
      throw new BookingException("Invalid order status");
    boolean wasRejected = "Rejected".equals(order.getStatus()),
        nowRejected = "Rejected".equals(status);
    JewelleryItem item = item(order.getItemId());
    if (!wasRejected && nowRejected)
      item.setStockQuantity(item.getStockQuantity() + order.getQuantity());
    if (wasRejected && !nowRejected)
      reserve(item, order.getQuantity());
    order.setStatus(status);
    if (input.getDeliveryAddress() != null)
      order.setDeliveryAddress(input.getDeliveryAddress());
    if (input.getComments() != null)
      order.setComments(input.getComments());
    items.save(item);
    return orders.save(order);
  }

  @Transactional
  public boolean deleteOrder(Long id) {
    Optional<JewelleryOrder> found = orders.findById(id);
    if (found.isEmpty())
      return false;
    JewelleryOrder order = found.get();
    if (order.getCheckout() != null && order.getCheckout().getRazorpayPaymentId() != null)
      throw new BookingException("Use the refund flow to cancel a paid order");
    if (!"Pending".equals(order.getStatus()) && !"Approved".equals(order.getStatus()))
      throw new BookingException("Only pending or approved orders can be cancelled");
    JewelleryItem item = item(order.getItemId());
    item.setStockQuantity(item.getStockQuantity() + order.getQuantity());
    items.save(item);
    order.setStatus("Cancelled");
    order.setCancellationReason("Cancelled by buyer");
    orders.save(order);
    return true;
  }

  @Transactional
  public JewelleryOrder cancelOrder(Long id, String reason) {
    JewelleryOrder order = orders.findById(id).orElseThrow(() -> new NoSuchElementException("Order not found"));
    if (order.getCheckout() != null && order.getCheckout().getRazorpayPaymentId() != null)
      throw new BookingException("Use the refund flow to cancel a paid order");
    if (!"Pending".equals(order.getStatus()) && !"Approved".equals(order.getStatus()))
      throw new BookingException("Only pending or approved orders can be cancelled");
    JewelleryItem item = item(order.getItemId());
    item.setStockQuantity(item.getStockQuantity() + order.getQuantity());
    order.setStatus("Cancelled");
    order.setCancellationReason(reason.trim());
    items.save(item);
    return orders.save(order);
  }
}
