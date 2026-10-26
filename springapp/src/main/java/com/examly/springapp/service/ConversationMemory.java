package com.examly.springapp.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ConversationMemory {
  private final ConcurrentHashMap<String, Deque<Turn>> sessions = new ConcurrentHashMap<>();

  public List<Turn> recent(String sessionId) {
    Deque<Turn> turns = sessions.get(sessionId);
    if (turns == null)
      return List.of();
    synchronized (turns) {
      return new ArrayList<>(turns);
    }
  }

  public void add(String sessionId, Turn turn) {
    if (sessions.size() > 500)
      sessions.clear();
    Deque<Turn> turns = sessions.computeIfAbsent(sessionId, ignored -> new ArrayDeque<>());
    synchronized (turns) {
      turns.addLast(turn);
      while (turns.size() > 6)
        turns.removeFirst();
    }
  }

  public void clear(String sessionId) {
    sessions.remove(sessionId);
  }

  public record Turn(String question, String reply, String matchedQuestion) {
  }
}
