package com.examly.springapp.repository;

import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.repository.projection.DashboardOrder;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JewelleryOrderRepo extends JpaRepository<JewelleryOrder, Long> {
  boolean existsBySeedKey(String seedKey);

  @Query("""
      select o.orderId as orderId, i.itemId as itemId,
        coalesce(o.invoiceItemName, i.itemName) as itemName, i.category as category,
        o.quantity as quantity, o.orderDate as orderDate, o.status as status,
        o.totalPrice as totalPrice, c.checkoutId as checkoutId,
        c.paymentStatus as paymentStatus, o.refundStatus as refundStatus, u.username as buyerName
      from JewelleryOrder o join o.item i join o.user u left join o.checkout c
      where o.orderDate between :from and :to order by o.orderId desc
      """)
  List<DashboardOrder> findDashboardOrders(
      @Param("from") LocalDate from, @Param("to") LocalDate to);

  List<JewelleryOrder> findByUserUserId(Integer userId);

  List<JewelleryOrder> findByCheckoutCheckoutId(Long checkoutId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from JewelleryOrder o where o.orderId = :id")
  Optional<JewelleryOrder> findLockedById(@Param("id") Long id);

  boolean existsByItemItemId(Long itemId);

  boolean existsByUserUserIdAndItemItemIdAndStatusIn(
      Integer userId, Long itemId, Collection<String> statuses);
}
