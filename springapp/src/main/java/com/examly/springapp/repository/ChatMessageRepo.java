package com.examly.springapp.repository;

import com.examly.springapp.model.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(String sessionId);

  List<ChatMessage> findTop6BySessionIdOrderByCreatedAtDescIdDesc(String sessionId);
}
