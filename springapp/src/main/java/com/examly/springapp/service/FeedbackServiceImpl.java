package com.examly.springapp.service;

import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackServiceImpl implements FeedbackService {
  private final FeedbackRepo feedbacks;
  private final UserRepo users;
  private final JewelleryItemRepo items;
  private final JewelleryOrderRepo orders;

  public FeedbackServiceImpl(FeedbackRepo feedbacks, UserRepo users, JewelleryItemRepo items,
      JewelleryOrderRepo orders) {
    this.feedbacks = feedbacks;
    this.users = users;
    this.items = items;
    this.orders = orders;
  }

  public Feedback createFeedback(Feedback feedback) {
    if (feedback.getFeedbackText() == null || feedback.getFeedbackText().isBlank())
      throw new IllegalArgumentException("Feedback is required");
    if (feedback.getRating() == null || feedback.getRating() < 1 || feedback.getRating() > 5)
      throw new IllegalArgumentException("Choose a rating from 1 to 5 stars");
    if (feedback.getUserId() == null)
      throw new IllegalArgumentException("Select a buyer");
    feedback.setUser(
        users
            .findById(feedback.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("Buyer not found")));
    if (feedback.getItemId() == null)
      throw new IllegalArgumentException("Select a purchased piece");
    if (!orders.existsByUserUserIdAndItemItemIdAndStatusIn(
        feedback.getUserId(), feedback.getItemId(), List.of("Delivered")))
      throw new IllegalArgumentException("You can review this piece after delivery");
    if (feedbacks.existsByUserUserIdAndItemItemId(feedback.getUserId(), feedback.getItemId()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this piece");
    feedback.setItem(items.findById(feedback.getItemId())
        .orElseThrow(() -> new IllegalArgumentException("Item not found")));
    feedback.setFeedbackId(null);
    if (feedback.getDate() == null)
      feedback.setDate(LocalDate.now());
    return feedbacks.save(feedback);
  }

  public Optional<Feedback> getFeedbackById(Long id) {
    return feedbacks.findById(id);
  }

  public List<Feedback> getAllFeedbacks() {
    return feedbacks.findAll();
  }

  public Feedback deleteFeedback(Long id) {
    Feedback f = feedbacks.findById(id).orElseThrow(() -> new NoSuchElementException("Feedback not found"));
    feedbacks.delete(f);
    return f;
  }

  public List<Feedback> getFeedbacksByUserId(int id) {
    return feedbacks.findByUserUserId(id);
  }

  public List<Feedback> getFeedbacksByItemId(Long id) {
    return feedbacks.findByItemItemId(id);
  }
}
