package com.examly.springapp.controller;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
  private final WishlistItemRepo wishlist;
  private final JewelleryItemRepo items;
  private final UserRepo users;

  public WishlistController(WishlistItemRepo wishlist, JewelleryItemRepo items, UserRepo users) {
    this.wishlist = wishlist;
    this.items = items;
    this.users = users;
  }

  private Integer userId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
  }

  @GetMapping
  public List<ApiDto.ItemView> all() {
    return wishlist.findByUserUserId(userId()).stream().map(x -> ApiDto.ItemView.from(x.getItem())).toList();
  }

  @PostMapping("/{itemId}")
  @Transactional
  public ResponseEntity<ApiDto.ItemView> add(@PathVariable Long itemId) {
    Integer id = userId();
    JewelleryItem item = items.findById(itemId).orElseThrow(() -> new NoSuchElementException("Item not found"));
    if (!wishlist.existsByUserUserIdAndItemItemId(id, itemId)) {
      WishlistItem entry = new WishlistItem();
      entry.setUser(users.findById(id).orElseThrow());
      entry.setItem(item);
      wishlist.save(entry);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiDto.ItemView.from(item));
  }

  @DeleteMapping("/{itemId}")
  @Transactional
  public ResponseEntity<Void> remove(@PathVariable Long itemId) {
    wishlist.deleteByUserUserIdAndItemItemId(userId(), itemId);
    return ResponseEntity.noContent().build();
  }
}
