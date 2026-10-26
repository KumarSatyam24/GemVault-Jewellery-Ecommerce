package com.examly.springapp.repository;

import com.examly.springapp.model.SavedAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedAddressRepo extends JpaRepository<SavedAddress, Long> {
  List<SavedAddress> findByUserUserIdOrderByAddressIdAsc(Integer userId);

  Optional<SavedAddress> findByAddressIdAndUserUserId(Long addressId, Integer userId);
}
