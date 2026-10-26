package com.examly.springapp.controller;

import com.examly.springapp.config.JwtUtils;
import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.dto.LoginDTO;
import com.examly.springapp.dto.ProfileRequest;
import com.examly.springapp.dto.RegistrationRequest;
import com.examly.springapp.dto.ApiDto;
import com.examly.springapp.model.SavedAddress;
import com.examly.springapp.model.User;
import com.examly.springapp.service.UserServiceImpl;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
  private final UserServiceImpl users;
  private final JwtUtils jwt;

  public UserController(UserServiceImpl users, JwtUtils jwt) {
    this.users = users;
    this.jwt = jwt;
  }

  @PostMapping("/register")
  public ResponseEntity<ApiDto.UserView> register(@Valid @RequestBody RegistrationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiDto.UserView.from(users.registerUser(request.toUser())));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody ApiDto.LoginRequest credentials) {
    User user = users.loginUser(credentials.toCredentials());
    if ("Admin".equalsIgnoreCase(user.getUserRole())) {
      users.sendAdminLoginLink(user);
      return ResponseEntity.accepted()
          .body(new ApiDto.AdminLoginStartResponse(true,
              "Enter the 6-digit code sent to your email to finish signing in."));
    }
    return ResponseEntity.ok(
        new LoginDTO(jwt.generateToken(user), user.getUsername(), user.getUserRole(), user.getUserId()));
  }

  @PostMapping("/admin-login/verify")
  public LoginDTO verifyAdminLogin(@Valid @RequestBody ApiDto.AdminCodeRequest request) {
    User user = users.verifyAdminLogin(request.email(), request.code());
    return new LoginDTO(jwt.generateToken(user), user.getUsername(), user.getUserRole(), user.getUserId());
  }

  @PostMapping("/email/verify")
  public ApiDto.MessageResponse verifyEmail(@Valid @RequestBody ApiDto.TokenRequest request) {
    users.verifyEmail(request.token());
    return new ApiDto.MessageResponse("Your email is verified. You can now sign in.");
  }

  @PostMapping("/email/verify/resend")
  public ApiDto.MessageResponse resendVerification(@Valid @RequestBody ApiDto.EmailRequest request) {
    users.resendRegistrationVerification(request.email());
    return new ApiDto.MessageResponse("If an unverified account exists, a verification email has been sent.");
  }

  @PostMapping("/password/forgot")
  public ApiDto.MessageResponse forgotPassword(@Valid @RequestBody ApiDto.EmailRequest request) {
    users.requestPasswordReset(request.email());
    return new ApiDto.MessageResponse("If an account exists for that email, a reset link has been sent.");
  }

  @PostMapping("/password/reset")
  public ApiDto.MessageResponse resetPassword(@Valid @RequestBody ApiDto.PasswordResetRequest request) {
    users.completePasswordReset(request.token(), request.password());
    return new ApiDto.MessageResponse("Your password has been set. You can now sign in.");
  }

  @PostMapping("/admin/buyers/invite")
  public ResponseEntity<ApiDto.UserView> inviteBuyer(
      @Valid @RequestBody ApiDto.InviteBuyerRequest request) {
    User buyer = users.inviteBuyer(request.username(), request.email(), request.mobileNumber());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiDto.UserView.from(buyer));
  }

  @GetMapping("/users")
  public List<ApiDto.UserView> all() {
    return users.getAllUsers().stream().map(ApiDto.UserView::from).toList();
  }

  private Integer currentUserId() {
    return ((UserPrinciple) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .getUserId();
  }

  @GetMapping("/profile")
  public ApiDto.UserView profile() {
    return ApiDto.UserView.from(users.getProfile(currentUserId()));
  }

  @PutMapping("/profile")
  public ApiDto.UserView updateProfile(@Valid @RequestBody ProfileRequest request) {
    return ApiDto.UserView.from(users.updateProfile(currentUserId(), request));
  }

  @GetMapping("/profile/addresses")
  public List<ApiDto.AddressView> addresses() {
    return users.getAddresses(currentUserId()).stream().map(ApiDto.AddressView::from).toList();
  }

  @PostMapping("/profile/addresses")
  public ResponseEntity<ApiDto.AddressView> addAddress(@Valid @RequestBody ApiDto.AddressRequest address) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiDto.AddressView.from(users.addAddress(currentUserId(), address.toEntity())));
  }

  @PutMapping("/profile/addresses/{id}")
  public ApiDto.AddressView updateAddress(
      @PathVariable Long id, @Valid @RequestBody ApiDto.AddressRequest address) {
    return ApiDto.AddressView.from(users.updateAddress(currentUserId(), id, address.toEntity()));
  }

  @DeleteMapping("/profile/addresses/{id}")
  public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
    users.deleteAddress(currentUserId(), id);
    return ResponseEntity.noContent().build();
  }
}
