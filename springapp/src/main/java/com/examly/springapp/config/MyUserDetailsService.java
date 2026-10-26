package com.examly.springapp.config;

import com.examly.springapp.repository.UserRepo;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {
  private final UserRepo users;

  public MyUserDetailsService(UserRepo users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    return new UserPrinciple(
        users
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found")));
  }
}
