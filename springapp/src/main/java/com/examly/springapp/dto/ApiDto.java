package com.examly.springapp.dto;

import com.examly.springapp.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import com.examly.springapp.service.BillingCalculator;

/**
 * Values accepted from and returned to the web client. JPA entities stay inside
 * the server.
 */
public final class ApiDto {
  private ApiDto() {
  }

  public record UserView(Integer userId, String email, String username, String mobileNumber, String userRole) {
    public static UserView from(User user) {
      return new UserView(user.getUserId(), user.getEmail(), user.getUsername(), user.getMobileNumber(),
          user.getUserRole());
    }
  }

  public record UserSummary(Integer userId, String username) {
    public static UserSummary from(User user) {
      return user == null ? null : new UserSummary(user.getUserId(), user.getUsername());
    }
  }

  public record ItemSummary(Long itemId, String itemName, String sku, String material, String purity) {
    public static ItemSummary from(JewelleryItem item) {
      return item == null ? null
          : new ItemSummary(item.getItemId(), item.getItemName(), item.getSku(), item.getMaterial(), item.getPurity());
    }
  }

  public record ItemView(Long itemId, String itemName, String sku, Boolean active,
      String description, String imageUrl, String category, String material, String purity, String stoneClarity,
      Double weightInGrams, Double stoneWeightInGrams, Double pricePerGram, Double stoneValue,
      Double makingCharges, Double vaCharges, Double discountAmount, Double cgstRate,
      Double sgstRate, Double roundOff, Double totalPrice, BillingView billing,
      Integer stockQuantity, Integer reorderThreshold, Double ratingAverage, Long ratingCount) {
    public static ItemView from(JewelleryItem item) {
      return from(item, null, 0L);
    }

    public static ItemView from(JewelleryItem item, Double ratingAverage, Long ratingCount) {
      BillingCalculator.Breakdown bill = BillingCalculator.forItem(item);
      return new ItemView(item.getItemId(), item.getItemName(), item.getSku(), item.getActive(),
          item.getDescription(), item.getImageUrl(), item.getCategory(), item.getMaterial(),
          item.getPurity(), item.getStoneClarity(), item.getWeightInGrams(), item.getStoneWeightInGrams(),
          item.getPricePerGram(),
          item.getStoneValue(), item.getMakingCharges(), item.getVaCharges(), item.getDiscountAmount(),
          item.getCgstRate(), item.getSgstRate(), item.getRoundOff(), item.getTotalPrice(),
          BillingView.from(bill), item.getStockQuantity(), item.getReorderThreshold(), ratingAverage, ratingCount);
    }
  }

  public record BillingView(double grossWeight, double stoneWeight, double netMetalWeight,
      double metalRate, double metalValue, double stoneValue, double makingCharges,
      double vaCharges, double discountAmount, double taxableAmount, double cgstRate,
      double cgstAmount, double sgstRate, double sgstAmount, double roundOff, double grandTotal) {
    public static BillingView from(BillingCalculator.Breakdown bill) {
      return new BillingView(bill.grossWeight(), bill.stoneWeight(), bill.netMetalWeight(),
          bill.metalRate(), bill.metalValue(), bill.stoneValue(), bill.makingCharges(),
          bill.vaCharges(), bill.discountAmount(), bill.taxableAmount(), bill.cgstRate(),
          bill.cgstAmount(), bill.sgstRate(), bill.sgstAmount(), bill.roundOff(), bill.grandTotal());
    }
  }

  public record ItemRequest(@NotBlank String itemName,
      @Pattern(regexp = "^$|^[A-Za-z0-9][A-Za-z0-9-]{2,39}$") String sku,
      @NotBlank @Size(max = 2000) String description,
      @NotBlank @Size(max = 2800000) String imageUrl,
      @NotBlank String category, @NotBlank String material,
      String purity, String stoneClarity, @NotNull Double weightInGrams, Double stoneWeightInGrams,
      @NotNull Double makingCharges, @NotNull Double pricePerGram, Double stoneValue,
      Double vaCharges, Double discountAmount, Double cgstRate, Double sgstRate, Double roundOff,
      @NotNull Integer stockQuantity,
      @NotNull Integer reorderThreshold) {
    public JewelleryItem toEntity() {
      JewelleryItem item = new JewelleryItem();
      item.setItemName(itemName);
      item.setSku(sku);
      item.setDescription(description);
      item.setImageUrl(imageUrl);
      item.setCategory(category);
      item.setMaterial(material);
      item.setPurity(purity);
      item.setStoneClarity(stoneClarity);
      item.setWeightInGrams(weightInGrams);
      item.setStoneWeightInGrams(stoneWeightInGrams);
      item.setMakingCharges(makingCharges);
      item.setPricePerGram(pricePerGram);
      item.setStoneValue(stoneValue);
      item.setVaCharges(vaCharges);
      item.setDiscountAmount(discountAmount);
      item.setCgstRate(cgstRate);
      item.setSgstRate(sgstRate);
      item.setRoundOff(roundOff);
      item.setStockQuantity(stockQuantity);
      item.setReorderThreshold(reorderThreshold);
      return item;
    }
  }

  public record OrderView(Long orderId, Long checkoutId, String cancellationReason,
      Integer userId, Long itemId, Integer quantity, LocalDate orderDate, String status,
      String deliveryAddress, String comments, Double totalPrice, ItemSummary item,
      UserSummary user, BillingView billing, InvoiceItemView invoiceItem,
      String paymentStatus, String refundStatus) {
    public static OrderView from(JewelleryOrder order) {
      return new OrderView(order.getOrderId(), order.getCheckoutId(), order.getCancellationReason(),
          order.getUserId(), order.getItemId(), order.getQuantity(), order.getOrderDate(),
          order.getStatus(), order.getDeliveryAddress(), order.getComments(), order.getTotalPrice(),
          ItemSummary.from(order.getItem()), UserSummary.from(order.getUser()),
          BillingView.from(order.getBillingSnapshot()), InvoiceItemView.from(order),
          order.getCheckout() == null ? null : order.getCheckout().getPaymentStatus(),
          order.getRefundStatus());
    }
  }

  public record InvoiceItemView(String itemName, String sku, String material, String purity) {
    public static InvoiceItemView from(JewelleryOrder order) {
      JewelleryItem item = order.getItem();
      return new InvoiceItemView(
          order.getInvoiceItemName() == null && item != null ? item.getItemName() : order.getInvoiceItemName(),
          order.getInvoiceSku() == null && item != null ? item.getSku() : order.getInvoiceSku(),
          order.getInvoiceMaterial() == null && item != null ? item.getMaterial() : order.getInvoiceMaterial(),
          order.getInvoicePurity() == null && item != null ? item.getPurity() : order.getInvoicePurity());
    }
  }

  public record OrderRequest(@NotNull Long itemId, @NotNull @Min(1) Integer quantity,
      @NotBlank String deliveryAddress, String comments) {
    public JewelleryOrder toEntity(Integer userId) {
      JewelleryOrder order = new JewelleryOrder();
      order.setUserId(userId);
      order.setItemId(itemId);
      order.setQuantity(quantity);
      order.setDeliveryAddress(deliveryAddress);
      order.setComments(comments);
      return order;
    }
  }

  public record OrderUpdateRequest(String status, String deliveryAddress, String comments) {
    public JewelleryOrder toEntity() {
      JewelleryOrder order = new JewelleryOrder();
      order.setStatus(status);
      order.setDeliveryAddress(deliveryAddress);
      order.setComments(comments);
      return order;
    }
  }

  public record RestockRequest(@NotNull @Min(1) Integer quantity) {
  }

  public record FeedbackView(Long feedbackId, Integer userId, Long itemId,
      String feedbackText, Integer rating, LocalDate date, UserSummary user, ItemSummary item) {
    public static FeedbackView from(Feedback feedback) {
      return new FeedbackView(feedback.getFeedbackId(), feedback.getUserId(), feedback.getItemId(),
          feedback.getFeedbackText(), feedback.getRating(), feedback.getDate(), UserSummary.from(feedback.getUser()),
          ItemSummary.from(feedback.getItem()));
    }
  }

  public record ProductReviewView(String reviewerName, String feedbackText, Integer rating, LocalDate date) {
    public static ProductReviewView from(Feedback feedback) {
      return new ProductReviewView(feedback.getUser() == null ? "GemVault customer" : feedback.getUser().getUsername(),
          feedback.getFeedbackText(), feedback.getRating(), feedback.getDate());
    }
  }

  public record FeedbackRequest(@NotNull Long itemId, @NotBlank @Size(max = 2000) String feedbackText,
      @NotNull @Min(1) @Max(5) Integer rating) {
    public Feedback toEntity(Integer userId) {
      Feedback feedback = new Feedback();
      feedback.setUserId(userId);
      feedback.setItemId(itemId);
      feedback.setFeedbackText(feedbackText);
      feedback.setRating(rating);
      return feedback;
    }
  }

  public record AddressView(Long addressId, String label, String line1, String city,
      String state, String postalCode) {
    public static AddressView from(SavedAddress address) {
      return new AddressView(address.getAddressId(), address.getLabel(), address.getLine1(),
          address.getCity(), address.getState(), address.getPostalCode());
    }
  }

  public record AddressRequest(@NotBlank @Size(max = 40) String label,
      @NotBlank @Size(max = 250) String line1, @NotBlank @Size(max = 80) String city,
      @NotBlank @Size(max = 80) String state, @NotBlank @Size(max = 12) String postalCode) {
    public SavedAddress toEntity() {
      SavedAddress address = new SavedAddress();
      address.setLabel(label);
      address.setLine1(line1);
      address.setCity(city);
      address.setState(state);
      address.setPostalCode(postalCode);
      return address;
    }
  }

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    public User toCredentials() {
      User user = new User();
      user.setEmail(email);
      user.setPassword(password);
      return user;
    }
  }

  public record MessageResponse(String message) {
  }

  public record SearchRequest(@NotBlank @Size(max = 120) String term) {
  }

  public record AdminLoginStartResponse(boolean requiresAdminVerification, String message) {
  }

  public record AdminCodeRequest(@NotBlank @Email String email,
      @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "Enter the 6-digit code") String code) {
  }

  public record TokenRequest(@NotBlank String token) {
  }

  public record EmailRequest(@NotBlank @Email String email) {
  }

  public record PasswordResetRequest(@NotBlank String token, @NotBlank String password) {
  }

  public record InviteBuyerRequest(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Email String email,
      @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile number must contain 10 digits") String mobileNumber) {
  }
}
