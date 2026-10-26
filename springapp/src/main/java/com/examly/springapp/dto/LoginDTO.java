package com.examly.springapp.dto;

public class LoginDTO {
  private final String token;
  private final String username;
  private final String userRole;
  private final Integer userId;

  public LoginDTO(String token, String username, String userRole, Integer userId) {
    this.token = token;
    this.username = username;
    this.userRole = userRole;
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public String getUsername() {
    return username;
  }

  public String getUserRole() {
    return userRole;
  }

  public Integer getUserId() {
    return userId;
  }
}
