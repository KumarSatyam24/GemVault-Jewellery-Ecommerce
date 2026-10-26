package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class JewelleryItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long itemId;

  @Column(unique = true, nullable = false)
  @NotBlank
  private String itemName;

  @Column(unique = true, nullable = false, length = 40)
  @Pattern(regexp = "^$|^[A-Za-z0-9][A-Za-z0-9-]{2,39}$", message = "SKU must be 3 to 40 letters, numbers or hyphens")
  private String sku;

  private Boolean active = true;

  @OneToMany(mappedBy = "item")
  @JsonIgnore
  private List<JewelleryOrder> orders = new ArrayList<>();

  @OneToMany(mappedBy = "item")
  @JsonIgnore
  private List<Feedback> feedbacks = new ArrayList<>();

  @Column(length = 2000)
  @NotBlank(message = "A product description is required")
  @Size(max = 2000)
  private String description;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  @NotBlank(message = "An image URL or uploaded image is required")
  @Size(max = 2800000, message = "Image data must be 2 MB or smaller")
  private String imageUrl;

  @NotBlank
  private String category;

  @NotBlank
  private String material;

  private String purity;

  private String stoneClarity;

  @NotNull
  @DecimalMin(value = "0.0", inclusive = false)
  private Double weightInGrams;

  @NotNull
  @DecimalMin("0.0")
  private Double makingCharges;

  private Double stoneWeightInGrams = 0.0;

  private Double stoneValue = 0.0;

  private Double vaCharges = 0.0;

  private Double discountAmount = 0.0;

  private Double cgstRate = 1.5;

  private Double sgstRate = 1.5;

  private Double roundOff = 0.0;

  @NotNull
  @DecimalMin("0.0")
  private Double pricePerGram;

  private Double totalPrice;

  @NotNull
  @Min(0)
  private Integer stockQuantity;

  @NotNull
  @Min(0)
  private Integer reorderThreshold;

  public JewelleryItem() {
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public List<JewelleryOrder> getOrders() {
    return orders;
  }

  public void setOrders(List<JewelleryOrder> orders) {
    this.orders = orders;
  }

  public List<Feedback> getFeedbacks() {
    return feedbacks;
  }

  public void setFeedbacks(List<Feedback> feedbacks) {
    this.feedbacks = feedbacks;
  }

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getMaterial() {
    return material;
  }

  public void setMaterial(String material) {
    this.material = material;
  }

  public String getPurity() {
    return purity;
  }

  public void setPurity(String purity) {
    this.purity = purity;
  }

  public String getStoneClarity() {
    return stoneClarity;
  }

  public void setStoneClarity(String stoneClarity) {
    this.stoneClarity = stoneClarity;
  }

  public Double getWeightInGrams() {
    return weightInGrams;
  }

  public void setWeightInGrams(Double weightInGrams) {
    this.weightInGrams = weightInGrams;
  }

  public Double getMakingCharges() {
    return makingCharges;
  }

  public void setMakingCharges(Double makingCharges) {
    this.makingCharges = makingCharges;
  }

  public Double getStoneWeightInGrams() {
    return stoneWeightInGrams;
  }

  public void setStoneWeightInGrams(Double stoneWeightInGrams) {
    this.stoneWeightInGrams = stoneWeightInGrams;
  }

  public Double getStoneValue() {
    return stoneValue;
  }

  public void setStoneValue(Double stoneValue) {
    this.stoneValue = stoneValue;
  }

  public Double getVaCharges() {
    return vaCharges;
  }

  public void setVaCharges(Double vaCharges) {
    this.vaCharges = vaCharges;
  }

  public Double getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(Double discountAmount) {
    this.discountAmount = discountAmount;
  }

  public Double getCgstRate() {
    return cgstRate;
  }

  public void setCgstRate(Double cgstRate) {
    this.cgstRate = cgstRate;
  }

  public Double getSgstRate() {
    return sgstRate;
  }

  public void setSgstRate(Double sgstRate) {
    this.sgstRate = sgstRate;
  }

  public Double getRoundOff() {
    return roundOff;
  }

  public void setRoundOff(Double roundOff) {
    this.roundOff = roundOff;
  }

  public Double getPricePerGram() {
    return pricePerGram;
  }

  public void setPricePerGram(Double pricePerGram) {
    this.pricePerGram = pricePerGram;
  }

  public Double getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
  }

  public Integer getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(Integer stockQuantity) {
    this.stockQuantity = stockQuantity;
  }

  public Integer getReorderThreshold() {
    return reorderThreshold;
  }

  public void setReorderThreshold(Integer reorderThreshold) {
    this.reorderThreshold = reorderThreshold;
  }
}
