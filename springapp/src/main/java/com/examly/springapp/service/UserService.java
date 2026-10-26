package com.examly.springapp.service;

import com.examly.springapp.model.User;

public interface UserService {
  User createUser(User user);

  User loginUser(User user);

  User loginWithGoogle(String email, String displayName);

  User inviteBuyer(String username, String email, String mobileNumber);

  void completePasswordReset(String token, String password);

  User verifyEmail(String token);

  User verifyAdminLogin(String email, String code);

  void requestPasswordReset(String email);

  void resendRegistrationVerification(String email);
}
