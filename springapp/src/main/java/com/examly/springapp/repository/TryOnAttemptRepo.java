package com.examly.springapp.repository;

import com.examly.springapp.model.TryOnAttempt;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TryOnAttemptRepo extends JpaRepository<TryOnAttempt, Long> {
  long countByUserIdAndAttemptedAtGreaterThanEqual(Integer userId, Instant since);
}
