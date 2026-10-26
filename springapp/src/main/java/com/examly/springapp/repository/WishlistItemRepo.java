package com.examly.springapp.repository;

import com.examly.springapp.model.WishlistItem;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepo extends JpaRepository<WishlistItem, Long> {
  List<WishlistItem> findByUserUserId(Integer userId);

  boolean existsByUserUserIdAndItemItemId(Integer userId, Long itemId);

  void deleteByUserUserIdAndItemItemId(Integer userId, Long itemId);
}
