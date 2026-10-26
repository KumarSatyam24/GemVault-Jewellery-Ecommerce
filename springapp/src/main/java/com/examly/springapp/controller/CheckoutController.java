package com.examly.springapp.controller;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.CheckoutRequest;
import com.examly.springapp.dto.CheckoutResult;
import com.examly.springapp.dto.PaymentCheckout;
import com.examly.springapp.service.CheckoutService;
import com.examly.springapp.service.CheckoutPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
  private final CheckoutService service;
  private final CheckoutPaymentService payments;

  public CheckoutController(CheckoutService service, CheckoutPaymentService payments) {
    this.service = service;
    this.payments = payments;
  }

  @PostMapping
  public ResponseEntity<PaymentCheckout.Start> prepare(@Valid @RequestBody CheckoutRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.prepare(userId(), request));
  }

  @PostMapping("/verify")
  public PaymentCheckout.Status verify(@Valid @RequestBody PaymentCheckout.Verify request) {
    return payments.verify(userId(), request);
  }

  @GetMapping("/{id}/status")
  public PaymentCheckout.Status status(@PathVariable Long id) {
    return payments.status(userId(), id);
  }

  @PostMapping("/razorpay/webhook")
  public ResponseEntity<Void> webhook(@RequestBody String rawBody,
      @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
    payments.webhook(rawBody, signature);
    return ResponseEntity.ok().build();
  }

  private Integer userId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .getUserId();
  }
}
