package com.examly.springapp.repository;

import com.examly.springapp.model.EmailActionToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailActionTokenRepo extends JpaRepository<EmailActionToken, Long> {
  Optional<EmailActionToken> findByTokenHashAndPurposeAndUsedAtIsNull(String tokenHash, String purpose);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  Optional<EmailActionToken> findByUserUserIdAndPurposeAndUsedAtIsNull(Integer userId, String purpose);

  @Modifying
  void deleteByUserUserIdAndPurpose(Integer userId, String purpose);
}
