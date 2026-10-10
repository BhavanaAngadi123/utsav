package com.utsav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utsav.common.Role;
import com.utsav.user.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

/** Auth: registration rules, lockout, refresh rotation, password policy. */
class AuthServiceTest extends IntegrationTestBase {

  @Autowired private AuthService authService;

  @Test
  void weakPasswordIsRejected() {
    assertThatThrownBy(
            () -> authService.register("a@t.local", "weak", "Weak", Role.CUSTOMER))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("password must be at least 10 characters");
  }

  @Test
  void duplicateEmailIsRejected() {
    assertThatThrownBy(
            () ->
                authService.register(
                    customer.getEmail(), "Another!123", "Dup", Role.CUSTOMER))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already registered");
  }

  @Test
  void nobodyCanSelfRegisterAsAdmin() {
    assertThatThrownBy(
            () ->
                authService.register(
                    "evil@t.local", "EvilAdmin!123", "Evil", Role.ADMIN))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("admin registration is not allowed");
  }

  @Test
  void loginIssuesTokenPair() {
    AuthService.AuthTokens tokens = authService.login(customer.getEmail(), "Customer!123");
    assertThat(tokens.accessToken()).isNotBlank();
    assertThat(tokens.refreshToken()).isNotBlank();
    assertThat(tokens.accessExpiresInSeconds()).isEqualTo(900);
  }

  @Test
  void wrongPasswordLocksAccountAfterFiveAttempts() {
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(() -> authService.login(customer.getEmail(), "Wrong!123"))
          .isInstanceOf(ResponseStatusException.class);
    }
    // 6th attempt: account is locked even with the right password
    assertThatThrownBy(() -> authService.login(customer.getEmail(), "Customer!123"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("locked");
  }

  @Test
  void refreshTokenRotates() {
    AuthService.AuthTokens first = authService.login(customer.getEmail(), "Customer!123");
    AuthService.AuthTokens second = authService.refresh(first.refreshToken());
    assertThat(second.accessToken()).isNotEqualTo(first.accessToken());
    // The old refresh token must now be dead (rotation).
    assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
        .isInstanceOf(ResponseStatusException.class);
  }
}
