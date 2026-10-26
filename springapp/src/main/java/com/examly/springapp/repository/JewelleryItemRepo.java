package com.examly.springapp.repository;

import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.repository.projection.DashboardStock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JewelleryItemRepo extends JpaRepository<JewelleryItem, Long> {
  @Query("""
      select i.itemId as itemId, i.itemName as itemName, i.sku as sku,
        i.stockQuantity as stockQuantity, i.reorderThreshold as reorderThreshold
      from JewelleryItem i where i.active = true order by i.stockQuantity asc, i.itemId asc
      """)
  List<DashboardStock> findDashboardStock();

  boolean existsByItemNameIgnoreCase(String itemName);

  boolean existsByItemNameIgnoreCaseAndItemIdNot(String itemName, Long itemId);

  boolean existsBySkuIgnoreCase(String sku);

  boolean existsBySkuIgnoreCaseAndItemIdNot(String sku, Long itemId);

  List<JewelleryItem> findByCategoryIgnoreCaseAndItemIdNotAndActiveTrueOrderByItemIdAsc(
      String category, Long itemId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from JewelleryItem i where i.itemId = :id")
  Optional<JewelleryItem> findLockedById(@Param("id") Long id);

  @Query(
      "select i from JewelleryItem i where i.stockQuantity <= i.reorderThreshold order by"
          + " (i.reorderThreshold - i.stockQuantity) desc, i.itemId asc")
  List<JewelleryItem> findLowStockItems();
}
