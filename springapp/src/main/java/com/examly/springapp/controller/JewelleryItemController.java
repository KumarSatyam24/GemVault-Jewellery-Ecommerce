package com.examly.springapp.controller;

import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.service.JewelleryItemService;
import com.examly.springapp.service.ProductCsvImportService;
import com.examly.springapp.repository.FeedbackRepo;
import com.examly.springapp.repository.JewelleryItemRepo;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jewellery-item")
public class JewelleryItemController {
  private final JewelleryItemService service;
  private final ProductCsvImportService csv;
  private final FeedbackRepo feedbacks;
  private final JewelleryItemRepo items;

  public JewelleryItemController(JewelleryItemService service, ProductCsvImportService csv, FeedbackRepo feedbacks,
      JewelleryItemRepo items) {
    this.service = service;
    this.csv = csv;
    this.feedbacks = feedbacks;
    this.items = items;
  }

  @PostMapping
  public ResponseEntity<ApiDto.ItemView> add(@Valid @RequestBody ApiDto.ItemRequest item) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiDto.ItemView.from(service.addItem(item.toEntity())));
  }

  @GetMapping(value = "/import-template", produces = "text/csv")
  public ResponseEntity<String> template() {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gemvault-products-template.csv")
        .body(ProductCsvImportService.HEADER
            + "\n,Example Gold Ring,GV-RING-001,Classic gold ring,https://example.com/ring.jpg,Ring,Gold,22K / 916,,5.2,0,14000,0,1800,0,0,1.5,1.5,0,5,2\n");
  }

  @GetMapping(value = "/export", produces = "text/csv")
  public ResponseEntity<String> exportCsv() {
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=gemvault-products.csv").body(csv.exportAll());
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProductCsvImportService.Result importCsv(
      @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
    return csv.importFile(file);
  }

  @GetMapping
  public List<ApiDto.ItemView> all() {
    return withRatings(service.getAllItems());
  }

  @GetMapping("/low-stock")
  public List<ApiDto.ItemView> lowStock() {
    return withRatings(service.getLowStockItems());
  }

  @GetMapping("/{id}")
  public ApiDto.ItemView one(@PathVariable Long id) {
    return withRating(service.getItemById(id).orElseThrow(() -> new NoSuchElementException("Item not found")));
  }

  private ApiDto.ItemView withRating(JewelleryItem item) {
    Object[] summary = feedbacks.ratingForItem(item.getItemId()).get(0);
    return ApiDto.ItemView.from(item, (Double) summary[0], (Long) summary[1]);
  }

  @GetMapping("/{id}/reviews")
  public List<ApiDto.ProductReviewView> reviews(@PathVariable Long id) {
    service.getItemById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
    return feedbacks.findByItemItemIdOrderByDateDescFeedbackIdDesc(id).stream()
        .map(ApiDto.ProductReviewView::from).toList();
  }

  @GetMapping("/{id}/similar")
  public List<ApiDto.ItemView> similar(@PathVariable Long id) {
    JewelleryItem item = service.getItemById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
    return withRatings(items.findByCategoryIgnoreCaseAndItemIdNotAndActiveTrueOrderByItemIdAsc(
        item.getCategory(), id));
  }

  private List<ApiDto.ItemView> withRatings(List<JewelleryItem> items) {
    if (items.isEmpty())
      return List.of();
    Map<Long, Object[]> summaries = new HashMap<>();
    feedbacks.ratingsForItems(items.stream().map(JewelleryItem::getItemId).toList())
        .forEach(row -> summaries.put((Long) row[0], row));
    return items.stream().map(item -> {
      Object[] row = summaries.get(item.getItemId());
      return row == null ? ApiDto.ItemView.from(item) : ApiDto.ItemView.from(item, (Double) row[1], (Long) row[2]);
    }).toList();
  }

  @PutMapping("/{id}")
  public ApiDto.ItemView update(@PathVariable Long id, @Valid @RequestBody ApiDto.ItemRequest item) {
    return withRating(service.updateItem(id, item.toEntity()));
  }

  @DeleteMapping("/{id}")
  public ApiDto.ItemView delete(@PathVariable Long id) {
    return ApiDto.ItemView.from(service.deleteItem(id));
  }

  @PostMapping("/{id}/archive")
  public ApiDto.ItemView archive(@PathVariable Long id) {
    return withRating(service.archiveItem(id));
  }

  @PostMapping("/{id}/restore")
  public ApiDto.ItemView restore(@PathVariable Long id) {
    return withRating(service.restoreItem(id));
  }

  @PostMapping("/{id}/restock")
  public ApiDto.ItemView restock(@PathVariable Long id, @Valid @RequestBody ApiDto.RestockRequest request) {
    return withRating(service.restockItem(id, request.quantity()));
  }
}
