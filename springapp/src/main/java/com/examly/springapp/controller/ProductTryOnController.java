package com.examly.springapp.controller;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.model.ProductTryOn;
import com.examly.springapp.service.ProductTryOnService;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/try-on")
public class ProductTryOnController {
  private final ProductTryOnService service;

  public ProductTryOnController(ProductTryOnService service) {
    this.service = service;
  }

  @GetMapping("/{itemId}")
  public ResponseEntity<byte[]> saved(@PathVariable Long itemId) {
    return image(service.saved(userId(), itemId));
  }

  @PostMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> generate(@PathVariable Long itemId, @RequestParam("photo") MultipartFile photo) {
    return image(service.generate(userId(), itemId, photo));
  }

  @DeleteMapping("/{itemId}")
  public ResponseEntity<Void> delete(@PathVariable Long itemId) {
    service.delete(userId(), itemId);
    return ResponseEntity.noContent().build();
  }

  private ResponseEntity<byte[]> image(ProductTryOn result) {
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.getContentType()))
        .cacheControl(CacheControl.noStore()).body(result.getImage());
  }

  private Integer userId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .getUserId();
  }
}
