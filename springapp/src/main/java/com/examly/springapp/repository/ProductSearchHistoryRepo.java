package com.examly.springapp.repository;

import com.examly.springapp.model.ProductSearchHistory;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSearchHistoryRepo extends JpaRepository<ProductSearchHistory, Long> {
  List<ProductSearchHistory> findTop10ByUserUserIdOrderBySearchedAtDesc(Integer userId);
}
