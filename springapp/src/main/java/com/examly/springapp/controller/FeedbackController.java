package com.examly.springapp.controller;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.model.Feedback;
import com.examly.springapp.service.FeedbackService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
  private final FeedbackService service;

  public FeedbackController(FeedbackService service) {
    this.service = service;
  }

  private Integer currentUserId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .getUserId();
  }

  private void requireOwner(Integer userId) {
    if (!currentUserId().equals(userId))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied");
  }

  private boolean isAdmin() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_Admin"));
  }

  @PostMapping
  public ResponseEntity<ApiDto.FeedbackView> add(@Valid @RequestBody ApiDto.FeedbackRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiDto.FeedbackView.from(service.createFeedback(request.toEntity(currentUserId()))));
  }

  @GetMapping
  public List<ApiDto.FeedbackView> all() {
    var feedbacks = isAdmin() ? service.getAllFeedbacks() : service.getFeedbacksByUserId(currentUserId());
    return feedbacks.stream().map(ApiDto.FeedbackView::from).toList();
  }

  @GetMapping("/user/{id}")
  public List<ApiDto.FeedbackView> byUser(@PathVariable int id) {
    if (!isAdmin())
      requireOwner(id);
    return service.getFeedbacksByUserId(id).stream().map(ApiDto.FeedbackView::from).toList();
  }

  @GetMapping("/item/{id}")
  public List<ApiDto.FeedbackView> byItem(@PathVariable Long id) {
    var feedbacks = isAdmin() ? service.getFeedbacksByItemId(id) : service.getFeedbacksByUserId(currentUserId());
    return feedbacks.stream()
        .filter(feedback -> feedback.getItemId().equals(id))
        .map(ApiDto.FeedbackView::from)
        .toList();
  }

  @GetMapping("/{id}")
  public ApiDto.FeedbackView one(@PathVariable Long id) {
    Feedback feedback = service
        .getFeedbackById(id)
        .orElseThrow(() -> new NoSuchElementException("Feedback not found"));
    if (!isAdmin())
      requireOwner(feedback.getUserId());
    return ApiDto.FeedbackView.from(feedback);
  }

  @DeleteMapping("/{id}")
  public ApiDto.FeedbackView delete(@PathVariable Long id) {
    Feedback feedback = service
        .getFeedbackById(id)
        .orElseThrow(() -> new NoSuchElementException("Feedback not found"));
    requireOwner(feedback.getUserId());
    return ApiDto.FeedbackView.from(service.deleteFeedback(id));
  }
}
