package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileRequest {
  @NotBlank
  @Size(max = 100)
  private String username;

  @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "Mobile number must be 10 digits and start with 6, 7, 8, or 9")
  private String mobileNumber = "";

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
  }
}
