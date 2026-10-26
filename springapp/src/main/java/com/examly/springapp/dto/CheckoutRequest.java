package com.examly.springapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CheckoutRequest {
  @NotNull private Long addressId;
  @NotEmpty private List<@Valid Line> items;
  private String comments;

  public Long getAddressId() {
    return addressId;
  }

  public void setAddressId(Long addressId) {
    this.addressId = addressId;
  }

  public List<Line> getItems() {
    return items;
  }

  public void setItems(List<Line> items) {
    this.items = items;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public static class Line {
    @NotNull private Long itemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    public Long getItemId() {
      return itemId;
    }

    public void setItemId(Long itemId) {
      this.itemId = itemId;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }
}
