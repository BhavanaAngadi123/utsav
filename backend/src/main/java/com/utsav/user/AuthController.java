package com.utsav.user;

import com.utsav.common.Role;
import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Authentication: register, login, refresh, logout. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @PostMapping("/register")
  @Operation(summary = "Register a new customer or vendor (admin registration is blocked)")
  public ResponseEntity<UserView> register(@Valid @RequestBody RegisterRequest req) {
    User user =
        authService.register(req.email(), req.password(), req.displayName(), req.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(UserView.of(user));
  }

  @PostMapping("/login")
  @Operation(summary = "Login with email + password; returns access + refresh tokens")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
    var tokens = authService.login(req.email(), req.password());
    return ResponseEntity.ok(
        new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.accessExpiresInSeconds()));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Rotate refresh token: old is revoked, new pair issued")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
    var tokens = authService.refresh(req.refreshToken());
    return ResponseEntity.ok(
        new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.accessExpiresInSeconds()));
  }

  @PostMapping("/logout")
  @Operation(summary = "Revoke a refresh token")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
    authService.logout(req.refreshToken());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  @Operation(summary = "Current user profile")
  public ResponseEntity<UserView> me(
      @RequestHeader(value = "Authorization", required = false) String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      var claims = jwtService.parseAccessToken(auth.substring(7));
      User user = authService.userByEmail(claims.email());
      return ResponseEntity.ok(UserView.of(user));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  public record RegisterRequest(
      @Email @NotBlank String email,
      @NotBlank String password,
      @NotBlank String displayName,
      Role role) {}

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds) {}

  public record UserView(String id, String email, String displayName, String role) {
    static UserView of(User u) {
      return new UserView(u.getId().toString(), u.getEmail(), u.getDisplayName(), u.getRole().name());
    }
  }
}
