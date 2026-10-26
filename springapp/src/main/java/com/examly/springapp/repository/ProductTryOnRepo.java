package com.examly.springapp.repository;

import com.examly.springapp.model.ProductTryOn;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTryOnRepo extends JpaRepository<ProductTryOn, Long> {
  Optional<ProductTryOn> findByUserUserIdAndItemItemId(Integer userId, Long itemId);

  void deleteByItemItemId(Long itemId);
}
