package com.examly.springapp.repository;

import com.examly.springapp.model.FaqEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepo extends JpaRepository<FaqEntity, Long> {
}
