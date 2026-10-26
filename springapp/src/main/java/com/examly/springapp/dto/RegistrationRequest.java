package com.examly.springapp.dto;

import com.examly.springapp.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {
  @NotBlank @Email private String email;

  @NotBlank
  @Size(max = 100)
  private String username;

  @NotBlank
  @Size(min = 8, max = 72)
  @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
      message = "Password needs uppercase, lowercase, number and symbol")
  private String password;

  @NotBlank
  @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Mobile number must be 10 digits and start with 6, 7, 8, or 9")
  private String mobileNumber;

  @Pattern(regexp = "Admin|User", message = "Role must be Admin or User")
  private String userRole = "User";

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void setMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
  }

  public String getUserRole() {
    return userRole;
  }

  public void setUserRole(String userRole) {
    this.userRole = userRole;
  }

  public User toUser() {
    User user = new User();
    user.setEmail(email);
    user.setUsername(username);
    user.setPassword(password);
    user.setMobileNumber(mobileNumber);
    user.setUserRole("User");
    return user;
  }
}
