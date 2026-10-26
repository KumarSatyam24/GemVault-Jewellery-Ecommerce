package com.examly.springapp.repository;

import com.examly.springapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Integer> {
  long countByUserRole(String userRole);
  
  java.util.Optional<User> findByEmailIgnoreCase(String email);

  java.util.List<User> findByUserRole(String userRole);
}
