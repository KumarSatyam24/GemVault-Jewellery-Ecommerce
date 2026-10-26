package com.examly.springapp.controller;

import com.examly.springapp.dto.ChatDto.ChatRequest;
import com.examly.springapp.dto.ChatDto.ChatResponse;
import com.examly.springapp.dto.ChatDto.FaqResponse;
import com.examly.springapp.model.ChatMessage;
import com.examly.springapp.repository.FaqRepo;
import com.examly.springapp.service.ChatService;
import com.examly.springapp.config.UserPrinciple;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api")
public class ChatController {
  private final ChatService chat;
  private final FaqRepo faqs;

  public ChatController(ChatService chat, FaqRepo faqs) {
    this.chat = chat;
    this.faqs = faqs;
  }

  @PostMapping("/chat")
  public ChatResponse message(@Valid @RequestBody ChatRequest request) {
    return chat.reply(request, currentUser());
  }

  @GetMapping("/chat/history/{sessionId}")
  public List<ChatMessage> history(@PathVariable String sessionId) {
    return chat.history(sessionId, currentUser());
  }

  @DeleteMapping("/chat/memory/{sessionId}")
  public ResponseEntity<Void> clear(@PathVariable String sessionId) {
    chat.clearMemory(sessionId, currentUser());
    return ResponseEntity.noContent().build();
  }

  private UserPrinciple currentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    return principal instanceof UserPrinciple user ? user : null;
  }

  @GetMapping("/faqs")
  public List<FaqResponse> faqs() {
    return faqs.findAll().stream()
        .map(faq -> new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer())).toList();
  }
}
