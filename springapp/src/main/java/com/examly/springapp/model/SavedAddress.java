package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class SavedAddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long addressId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;

  @NotBlank
  @Size(max = 40)
  private String label;

  @NotBlank
  @Size(max = 250)
  private String line1;

  @NotBlank
  @Size(max = 80)
  private String city;

  @NotBlank
  @Size(max = 80)
  private String state;

  @NotBlank
  @Size(max = 12)
  private String postalCode;

  public Long getAddressId() {
    return addressId;
  }

  public void setAddressId(Long addressId) {
    this.addressId = addressId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getLine1() {
    return line1;
  }

  public void setLine1(String line1) {
    this.line1 = line1;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String formatted() {
    return line1 + ", " + city + ", " + state + " " + postalCode;
  }
}
