package com.examly.springapp.repository;

import com.examly.springapp.model.Checkout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutRepo extends JpaRepository<Checkout, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Checkout c where c.checkoutId = :id")
  Optional<Checkout> findLockedById(@Param("id") Long id);

  Optional<Checkout> findByRazorpayOrderId(String razorpayOrderId);

  List<Checkout> findByPaymentStatusAndPaymentExpiresAtBefore(
      String paymentStatus, LocalDateTime now);

  List<Checkout> findTop100ByPaymentStatusOrderByCheckoutIdAsc(String paymentStatus);
}
