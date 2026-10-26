package com.examly.springapp.controller;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.CancelReasonRequest;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.service.JewelleryOrderService;
import com.examly.springapp.service.PaymentRefundService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/jewellery-order")
public class JewelleryOrderController {
  private final JewelleryOrderService service;
  private final PaymentRefundService refunds;

  public JewelleryOrderController(JewelleryOrderService service, PaymentRefundService refunds) {
    this.service = service;
    this.refunds = refunds;
  }

  private Integer currentUserId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .getUserId();
  }

  private void requireOwner(Integer userId) {
    if (!currentUserId().equals(userId))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied");
  }

  private boolean isAdmin() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
  }

  @PostMapping
  public ResponseEntity<ApiDto.OrderView> add(@Valid @RequestBody ApiDto.OrderRequest order) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Please use cart checkout and complete payment before placing an order");
  }

  @GetMapping
  public List<ApiDto.OrderView> all() {
    return service.getAllOrders().stream()
        .filter(order -> !"AwaitingPayment".equals(order.getStatus()))
        .map(ApiDto.OrderView::from).toList();
  }

  @GetMapping("/user/{id}")
  public List<ApiDto.OrderView> byUser(@PathVariable Long id) {
    requireOwner(Math.toIntExact(id));
    return service.getOrdersByUserId(id).stream()
        .filter(order -> !"AwaitingPayment".equals(order.getStatus()))
        .map(ApiDto.OrderView::from).toList();
  }

  @GetMapping("/{id}")
  public ApiDto.OrderView one(@PathVariable Long id) {
    JewelleryOrder order = service.getOrderById(id).orElseThrow(() -> new NoSuchElementException("Order not found"));
    if ("AwaitingPayment".equals(order.getStatus()))
      throw new NoSuchElementException("Order not found");
    if (!isAdmin())
      requireOwner(order.getUserId());
    return ApiDto.OrderView.from(order);
  }

  @PutMapping("/{id}")
  public ApiDto.OrderView update(@PathVariable Long id, @RequestBody ApiDto.OrderUpdateRequest order) {
    JewelleryOrder existing = service.getOrderById(id)
        .orElseThrow(() -> new NoSuchElementException("Order not found"));
    if ("Rejected".equals(order.status()) && paidWithRazorpay(existing))
      return ApiDto.OrderView.from(refunds.cancelPaid(id, "Rejected by administrator", "Rejected"));
    return ApiDto.OrderView.from(service.updateOrder(id, order.toEntity()));
  }

  @PostMapping("/{id}/cancel")
  public ApiDto.OrderView cancel(
      @PathVariable Long id, @Valid @RequestBody CancelReasonRequest request) {
    JewelleryOrder existing = service.getOrderById(id)
        .orElseThrow(() -> new NoSuchElementException("Order not found"));
    if (paidWithRazorpay(existing))
      return ApiDto.OrderView.from(refunds.cancelPaid(id, request.getReason().trim(), "Cancelled"));
    return ApiDto.OrderView.from(service.cancelOrder(id, request.getReason()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    JewelleryOrder order = service.getOrderById(id).orElseThrow(() -> new NoSuchElementException("Order not found"));
    requireOwner(order.getUserId());
    if (paidWithRazorpay(order))
      refunds.cancelPaid(id, "Cancelled by buyer", "Cancelled");
    else
      service.deleteOrder(id);
    return ResponseEntity.noContent().build();
  }

  private boolean paidWithRazorpay(JewelleryOrder order) {
    return order.getCheckout() != null && order.getCheckout().getRazorpayPaymentId() != null;
  }
}
