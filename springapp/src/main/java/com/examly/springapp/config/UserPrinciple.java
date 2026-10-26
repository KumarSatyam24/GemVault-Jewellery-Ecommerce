package com.examly.springapp.config;

import com.examly.springapp.model.User;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrinciple implements UserDetails {
  private final User user;

  public UserPrinciple(User user) {
    this.user = user;
  }

  public Integer getUserId() {
    return user.getUserId();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(
        new org.springframework.security.core.authority.SimpleGrantedAuthority(
            "ROLE_" + user.getUserRole()));
  }

  @Override
  public String getPassword() {
    return user.getPassword();
  }

  @Override
  public String getUsername() {
    return user.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
