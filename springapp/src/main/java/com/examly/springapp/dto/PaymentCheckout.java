package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PaymentCheckout {
    private PaymentCheckout() {
    }

    public record Start(Long checkoutId, String razorpayOrderId, String keyId,
            long amountPaise, String currency, String buyerName, String buyerEmail,
            String buyerMobile) {
    }

    public record Verify(@NotNull Long checkoutId, @NotBlank String razorpayOrderId,
            @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {
    }

    public record Status(Long checkoutId, String paymentStatus, CheckoutResult order,
            Start payment) {
    }
}
