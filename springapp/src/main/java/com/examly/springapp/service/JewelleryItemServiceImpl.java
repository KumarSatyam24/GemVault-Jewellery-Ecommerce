package com.examly.springapp.service;

import com.examly.springapp.exceptions.*;
import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.repository.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JewelleryItemServiceImpl implements JewelleryItemService {
  private static final Pattern IMAGE_REFERENCE = Pattern.compile(
      "^(https?://\\S+|data:image/(?:png|jpeg|webp);base64,[A-Za-z0-9+/=\\r\\n]+)$",
      Pattern.CASE_INSENSITIVE);
  private final JewelleryItemRepo items;
  private final JewelleryOrderRepo orders;
  private final FeedbackRepo feedbacks;
  private final ProductTryOnRepo tryOns;

  public JewelleryItemServiceImpl(
      JewelleryItemRepo items, JewelleryOrderRepo orders, FeedbackRepo feedbacks,
      ProductTryOnRepo tryOns) {
    this.items = items;
    this.orders = orders;
    this.feedbacks = feedbacks;
    this.tryOns = tryOns;
  }

  private void validate(JewelleryItem item) {
    if (item.getItemName() == null || item.getItemName().isBlank())
      throw new IllegalArgumentException("Item name is required");
    if (item.getDescription() == null || item.getDescription().isBlank()
        || item.getDescription().length() > 2000)
      throw new IllegalArgumentException("Add a description of up to 2000 characters");
    if (item.getWeightInGrams() == null
        || item.getWeightInGrams() <= 0
        || item.getPricePerGram() == null
        || item.getPricePerGram() < 0
        || item.getMakingCharges() == null
        || item.getMakingCharges() < 0)
      throw new IllegalArgumentException("Weight and prices must be valid");
    if (item.getStockQuantity() == null
        || item.getStockQuantity() < 0
        || item.getReorderThreshold() == null
        || item.getReorderThreshold() < 0)
      throw new IllegalArgumentException("Stock and reorder threshold must be zero or greater");
    if (item.getCategory() == null
        || item.getCategory().isBlank()
        || item.getMaterial() == null
        || item.getMaterial().isBlank())
      throw new IllegalArgumentException("Category and material are required");
    if (item.getImageUrl() == null
        || !IMAGE_REFERENCE.matcher(item.getImageUrl().trim()).matches())
      throw new IllegalArgumentException(
          "Provide a valid image URL or a PNG, JPEG, or WebP image upload");
    item.setStoneWeightInGrams(zero(item.getStoneWeightInGrams()));
    item.setStoneValue(zero(item.getStoneValue()));
    item.setVaCharges(zero(item.getVaCharges()));
    item.setDiscountAmount(zero(item.getDiscountAmount()));
    item.setCgstRate(zero(item.getCgstRate()));
    item.setSgstRate(zero(item.getSgstRate()));
    item.setRoundOff(zero(item.getRoundOff()));
    if (item.getStoneWeightInGrams() > item.getWeightInGrams())
      throw new IllegalArgumentException("Stone weight cannot exceed gross weight");
    if (item.getStoneValue() < 0 || item.getVaCharges() < 0 || item.getDiscountAmount() < 0
        || item.getCgstRate() < 0 || item.getSgstRate() < 0)
      throw new IllegalArgumentException("Billing values must be zero or greater");
    item.setImageUrl(item.getImageUrl().trim());
    item.setTotalPrice(BillingCalculator.forItem(item).grandTotal());
  }

  private double zero(Double value) {
    return value == null ? 0d : value;
  }

  private String skuOrNew(String requested) {
    if (requested != null && !requested.isBlank())
      return requested.trim().toUpperCase(Locale.ROOT);
    String generated;
    do {
      generated = "GV-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
    } while (items.existsBySkuIgnoreCase(generated));
    return generated;
  }

  @Transactional
  public JewelleryItem addItem(JewelleryItem item) {
    validate(item);
    if (items.existsByItemNameIgnoreCase(item.getItemName()))
      throw new DuplicateItemException("Item name already exists");
    item.setItemId(null);
    item.setSku(skuOrNew(item.getSku()));
    if (items.existsBySkuIgnoreCase(item.getSku()))
      throw new DuplicateItemException("SKU already exists");
    item.setActive(true);
    return items.save(item);
  }

  public Optional<JewelleryItem> getItemById(Long id) {
    return items.findById(id);
  }

  public List<JewelleryItem> getAllItems() {
    return items.findAll();
  }

  @Transactional
  public JewelleryItem updateItem(Long id, JewelleryItem input) {
    JewelleryItem item = items.findById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
    validate(input);
    if (items.existsByItemNameIgnoreCaseAndItemIdNot(input.getItemName(), id))
      throw new DuplicateItemException("Item name already exists");
    String sku = input.getSku() == null || input.getSku().isBlank()
        ? item.getSku()
        : skuOrNew(input.getSku());
    if (items.existsBySkuIgnoreCaseAndItemIdNot(sku, id))
      throw new DuplicateItemException("SKU already exists");
    item.setSku(sku);
    item.setItemName(input.getItemName());
    item.setDescription(input.getDescription());
    item.setImageUrl(input.getImageUrl());
    item.setCategory(input.getCategory());
    item.setMaterial(input.getMaterial());
    item.setPurity(input.getPurity());
    item.setStoneClarity(input.getStoneClarity());
    item.setWeightInGrams(input.getWeightInGrams());
    item.setMakingCharges(input.getMakingCharges());
    item.setStoneWeightInGrams(input.getStoneWeightInGrams());
    item.setStoneValue(input.getStoneValue());
    item.setVaCharges(input.getVaCharges());
    item.setDiscountAmount(input.getDiscountAmount());
    item.setCgstRate(input.getCgstRate());
    item.setSgstRate(input.getSgstRate());
    item.setRoundOff(input.getRoundOff());
    item.setPricePerGram(input.getPricePerGram());
    item.setTotalPrice(input.getTotalPrice());
    item.setStockQuantity(input.getStockQuantity());
    item.setReorderThreshold(input.getReorderThreshold());
    return items.save(item);
  }

  @Transactional
  public JewelleryItem deleteItem(Long id) {
    JewelleryItem item = items.findById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
    if (orders.existsByItemItemId(id))
      throw new ItemDeletionException("Item has orders and cannot be deleted");
    feedbacks.findByItemItemId(id).forEach(feedback -> feedback.setItem(null));
    tryOns.deleteByItemItemId(id);
    items.delete(item);
    return item;
  }

  @Transactional
  public JewelleryItem archiveItem(Long id) {
    JewelleryItem item = items.findById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
    item.setActive(false);
    return items.save(item);
  }

  @Transactional
  public JewelleryItem restoreItem(Long id) {
    JewelleryItem item = items.findById(id).orElseThrow(() -> new NoSuchElementException("Item not found"));
    item.setActive(true);
    return items.save(item);
  }

  public List<JewelleryItem> getLowStockItems() {
    return items.findLowStockItems();
  }

  @Transactional
  public JewelleryItem restockItem(Long id, int quantity) {
    if (quantity < 1)
      throw new IllegalArgumentException("Restock quantity must be at least 1");
    JewelleryItem item = items.findLockedById(id)
        .orElseThrow(() -> new NoSuchElementException("Item not found"));
    if (item.getStockQuantity() > Integer.MAX_VALUE - quantity)
      throw new IllegalArgumentException("Restock quantity is too large");
    item.setStockQuantity(item.getStockQuantity() + quantity);
    return items.save(item);
  }
}
