package com.examly.springapp.controller;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.model.ProductSearchHistory;
import com.examly.springapp.model.WishlistItem;
import com.examly.springapp.repository.JewelleryItemRepo;
import com.examly.springapp.repository.ProductSearchHistoryRepo;
import com.examly.springapp.repository.UserRepo;
import com.examly.springapp.repository.WishlistItemRepo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
  private final JewelleryItemRepo items;
  private final WishlistItemRepo wishlist;
  private final ProductSearchHistoryRepo history;
  private final UserRepo users;

  public RecommendationController(
      JewelleryItemRepo items,
      WishlistItemRepo wishlist,
      ProductSearchHistoryRepo history,
      UserRepo users) {
    this.items = items;
    this.wishlist = wishlist;
    this.history = history;
    this.users = users;
  }

  private Integer userId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .getUserId();
  }

  @GetMapping
  public List<ApiDto.ItemView> recommended() {
    Integer id = userId();
    List<WishlistItem> savedItems = wishlist.findByUserUserId(id);
    Set<Long> savedIds = new LinkedHashSet<>();
    Set<String> categories = new LinkedHashSet<>();
    Set<String> materials = new LinkedHashSet<>();
    Set<String> searchTerms = new LinkedHashSet<>();

    for (WishlistItem saved : savedItems) {
      JewelleryItem item = saved.getItem();
      savedIds.add(item.getItemId());
      categories.add(normalize(item.getCategory()));
      materials.add(normalize(item.getMaterial()));
    }
    for (String term : searchHistory()) {
      for (String token : normalize(term).split("\\s+")) {
        if (token.length() >= 3)
          searchTerms.add(token);
      }
    }

    List<JewelleryItem> candidates = items.findAll().stream()
        .filter(item -> Boolean.TRUE.equals(item.getActive()))
        .filter(item -> item.getStockQuantity() != null && item.getStockQuantity() > 0)
        .filter(item -> !savedIds.contains(item.getItemId()))
        .toList();

    List<JewelleryItem> matching = candidates.stream()
        .filter(item -> score(item, categories, materials, searchTerms) > 0)
        .sorted(
            Comparator.<JewelleryItem>comparingInt(
                item -> score(item, categories, materials, searchTerms))
                .reversed()
                .thenComparing(JewelleryItem::getItemId))
        .limit(3)
        .toList();

    // A new account has no signals yet. Show available pieces until it saves or
    // searches for one.
    List<JewelleryItem> result = matching.isEmpty() && savedItems.isEmpty() && searchTerms.isEmpty()
        ? candidates.stream().limit(3).toList()
        : matching;
    return result.stream().map(ApiDto.ItemView::from).toList();
  }

  @GetMapping("/search-history")
  public List<String> searchHistory() {
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    history.findTop10ByUserUserIdOrderBySearchedAtDesc(userId()).forEach(entry -> unique.add(entry.getSearchTerm()));
    return new ArrayList<>(unique);
  }

  @PostMapping("/search-history")
  public void record(@RequestBody ApiDto.SearchRequest request) {
    String term = request.term().trim().replaceAll("\\s+", " ");
    if (term.length() < 2)
      return;
    boolean alreadyRecent = history.findTop10ByUserUserIdOrderBySearchedAtDesc(userId()).stream()
        .anyMatch(entry -> entry.getSearchTerm().equalsIgnoreCase(term));
    if (alreadyRecent)
      return;

    ProductSearchHistory entry = new ProductSearchHistory();
    entry.setUser(users.findById(userId()).orElseThrow());
    entry.setSearchTerm(term.substring(0, Math.min(120, term.length())));
    history.save(entry);
  }

  private int score(
      JewelleryItem item,
      Set<String> categories,
      Set<String> materials,
      Set<String> searchTerms) {
    String category = normalize(item.getCategory());
    String material = normalize(item.getMaterial());
    String searchable = normalize(item.getItemName() + " " + category + " " + material);
    int score = 0;
    if (categories.contains(category))
      score += 100;
    if (materials.contains(material))
      score += 20;
    for (String term : searchTerms) {
      if (searchable.contains(term))
        score += 40;
    }
    return score;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
