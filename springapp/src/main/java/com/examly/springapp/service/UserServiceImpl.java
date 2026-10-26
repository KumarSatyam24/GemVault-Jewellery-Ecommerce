package com.examly.springapp.service;

import com.examly.springapp.dto.ProfileRequest;
import com.examly.springapp.model.SavedAddress;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.SavedAddressRepo;
import com.examly.springapp.repository.UserRepo;
import java.util.*;
import java.security.SecureRandom;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
  private final UserRepo users;
  private final PasswordEncoder encoder;
  private final SavedAddressRepo addresses;
  private final EmailActionService emailActions;
  private final SecureRandom random = new SecureRandom();

  public UserServiceImpl(UserRepo users, PasswordEncoder encoder, SavedAddressRepo addresses,
      EmailActionService emailActions) {
    this.users = users;
    this.encoder = encoder;
    this.addresses = addresses;
    this.emailActions = emailActions;
  }

  public User createUser(User user) {
    if (user.getEmail() == null
        || user.getEmail().isBlank()
        || user.getUsername() == null
        || user.getUsername().isBlank())
      throw new IllegalArgumentException("Email and name are required");
    if (user.getPassword() == null
        || user.getPassword().length() < 8
        || user.getPassword().length() > 72
        || !user.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$"))
      throw new IllegalArgumentException(
          "Password needs 8-72 characters including uppercase, lowercase, number and symbol");
    user.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
    if (users.findByEmailIgnoreCase(user.getEmail()).isPresent())
      throw new IllegalArgumentException("Email already exists");
    if (user.getMobileNumber() != null && !user.getMobileNumber().isBlank()
        && !user.getMobileNumber().matches("^[6-9][0-9]{9}$"))
      throw new IllegalArgumentException("Mobile number must be 10 digits and start with 6, 7, 8, or 9");
    user.setUserId(null);
    if (user.getUserRole() == null || user.getUserRole().isBlank())
      user.setUserRole("User");
    if (!user.getUserRole().equals("User") && !user.getUserRole().equals("Admin"))
      throw new IllegalArgumentException("Role must be Admin or User");
    if (user.getAuthProvider() == null || user.getAuthProvider().isBlank())
      user.setAuthProvider("PASSWORD");
    user.setPassword(encoder.encode(user.getPassword()));
    return users.save(user);
  }

  @Transactional
  public User registerUser(User user) {
    user.setUserRole("User");
    user.setEmailVerified(false);
    user.setPasswordResetRequired(false);
    User saved = createUser(user);
    emailActions.sendVerification(saved);
    return saved;
  }

  public User loginUser(User user) {
    User found = users
        .findByEmailIgnoreCase(user.getEmail() == null ? "" : user.getEmail())
        .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    if (!encoder.matches(user.getPassword() == null ? "" : user.getPassword(), found.getPassword()))
      throw new BadCredentialsException("Invalid email or password");
    if (found.isPasswordResetRequired())
      throw new IllegalArgumentException("Set a password using the invitation email before signing in");
    if (!found.isEmailVerified())
      throw new IllegalArgumentException("Verify your email before signing in");
    return found;
  }

  /**
   * Google has already verified ownership of the email address before this method
   * runs.
   */
  @Transactional
  public User loginWithGoogle(String email, String displayName) {
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    User existing = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
    if (existing != null) {
      if (!"User".equals(existing.getUserRole()))
        throw new IllegalArgumentException("Google sign-in is available for buyer accounts only");
      existing.setEmailVerified(true);
      if (existing.getAuthProvider() == null || "PASSWORD".equals(existing.getAuthProvider()))
        existing.setAuthProvider("GOOGLE");
      return users.save(existing);
    }
    User buyer = new User();
    buyer.setEmail(normalizedEmail);
    buyer.setUsername(
        displayName == null || displayName.isBlank() ? normalizedEmail.substring(0, normalizedEmail.indexOf('@'))
            : displayName.trim());
    buyer.setUserRole("User");
    buyer.setAuthProvider("GOOGLE");
    buyer.setEmailVerified(true);
    buyer.setPasswordResetRequired(false);
    buyer.setPassword(randomPassword());
    return createUser(buyer);
  }

  @Transactional
  public User inviteBuyer(String username, String email, String mobileNumber) {
    String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    User existing = normalized == null ? null : users.findByEmailIgnoreCase(normalized).orElse(null);
    if (existing != null) {
      if (!"User".equals(existing.getUserRole()) || !existing.isPasswordResetRequired())
        throw new IllegalArgumentException("An active account already uses this email");
      existing.setUsername(username);
      existing.setMobileNumber(mobileNumber);
      emailActions.sendInvitation(existing);
      return users.save(existing);
    }
    User user = new User();
    user.setUsername(username);
    user.setEmail(normalized);
    user.setMobileNumber(mobileNumber);
    user.setUserRole("User");
    user.setEmailVerified(false);
    user.setPasswordResetRequired(true);
    user.setPassword(randomPassword());
    User saved = createUser(user);
    emailActions.sendInvitation(saved);
    return saved;
  }

  @Transactional
  public void completePasswordReset(String token, String password) {
    validatePassword(password);
    User user = emailActions.consumePasswordOrInvitation(token);
    user.setPassword(encoder.encode(password));
    user.setEmailVerified(true);
    user.setPasswordResetRequired(false);
    users.save(user);
  }

  @Transactional
  public User verifyEmail(String token) {
    User user = emailActions.consume(token, EmailActionService.VERIFY_EMAIL);
    user.setEmailVerified(true);
    return users.save(user);
  }

  public User verifyAdminLogin(String email, String code) {
    User user = users.findByEmailIgnoreCase(email == null ? "" : email.trim())
        .orElseThrow(() -> new IllegalArgumentException("Invalid admin code"));
    if (!"Admin".equals(user.getUserRole()))
      throw new IllegalArgumentException("Invalid admin code");
    return emailActions.consumeAdminCode(user, code);
  }

  public void requestPasswordReset(String email) {
    users.findByEmailIgnoreCase(email == null ? "" : email.trim())
        .ifPresent(emailActions::sendPasswordReset);
  }

  public void resendRegistrationVerification(String email) {
    User user = users.findByEmailIgnoreCase(email == null ? "" : email.trim())
        .orElseThrow(() -> new IllegalArgumentException("No account exists for this email"));
    if (user.isEmailVerified())
      throw new IllegalArgumentException("This email is already verified");
    emailActions.sendVerification(user);
  }

  public void sendAdminLoginLink(User user) {
    emailActions.sendAdminLogin(user);
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 8 || password.length() > 72
        || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$"))
      throw new IllegalArgumentException(
          "Password needs 8-72 characters including uppercase, lowercase, number and symbol");
  }

  private String randomPassword() {
    byte[] bytes = new byte[18];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "Aa!9";
  }

  public List<User> getAllUsers() {
    return users.findAll();
  }

  public User getProfile(Integer userId) {
    return users.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
  }

  @Transactional
  public User updateProfile(Integer userId, ProfileRequest request) {
    User user = getProfile(userId);
    user.setUsername(request.getUsername().trim());
    user.setMobileNumber(request.getMobileNumber());
    return users.save(user);
  }

  public List<SavedAddress> getAddresses(Integer userId) {
    return addresses.findByUserUserIdOrderByAddressIdAsc(userId);
  }

  public SavedAddress getAddress(Integer userId, Long addressId) {
    return addresses
        .findByAddressIdAndUserUserId(addressId, userId)
        .orElseThrow(() -> new NoSuchElementException("Address not found"));
  }

  @Transactional
  public SavedAddress addAddress(Integer userId, SavedAddress input) {
    input.setAddressId(null);
    input.setUser(getProfile(userId));
    return addresses.save(input);
  }

  @Transactional
  public SavedAddress updateAddress(Integer userId, Long addressId, SavedAddress input) {
    SavedAddress address = getAddress(userId, addressId);
    address.setLabel(input.getLabel());
    address.setLine1(input.getLine1());
    address.setCity(input.getCity());
    address.setState(input.getState());
    address.setPostalCode(input.getPostalCode());
    return addresses.save(address);
  }

  @Transactional
  public void deleteAddress(Integer userId, Long addressId) {
    addresses.delete(getAddress(userId, addressId));
  }
}
