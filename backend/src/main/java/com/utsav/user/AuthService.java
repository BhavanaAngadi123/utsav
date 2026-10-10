package com.utsav.user;

import com.utsav.admin.AuditService;
import com.utsav.common.Role;
import com.utsav.config.UtsavProperties;
import com.utsav.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registration, login (with lockout), and refresh-token rotation.
 */
@Service
public class AuthService {

  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UtsavProperties props;
  private final AuditService audit;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      UtsavProperties props,
      AuditService audit) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.props = props;
    this.audit = audit;
  }

  @Transactional
  public User register(String email, String rawPassword, String displayName, Role role) {
    if (role == Role.ADMIN) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "admin registration is not allowed");
    }
    String cleanEmail = email.toLowerCase().trim();
    if (users.existsByEmail(cleanEmail)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
    }
    validatePassword(rawPassword);
    User user =
        new User(cleanEmail, passwordEncoder.encode(rawPassword), displayName.trim(), role);
    users.save(user);
    audit.log(user, "REGISTER", "User", user.getId().toString(), "role=" + role);
    return user;
  }

  /**
   * Login with account lockout. The lockout state must commit even though we
   * throw, so failed logins are not rolled back.
   */
  @Transactional(noRollbackFor = ResponseStatusException.class)
  public AuthTokens login(String email, String rawPassword) {
    String cleanEmail = email.toLowerCase().trim();
    User user = users.findByEmail(cleanEmail).orElse(null);

    boolean passwordOk =
        user != null
            && user.getPasswordHash() != null
            && passwordEncoder.matches(rawPassword, user.getPasswordHash());

    if (user == null) {
      // Constant-time dummy check to avoid user-enumeration timing leaks.
      passwordEncoder.matches(rawPassword, passwordEncoder.encode("dummy"));
      audit.log("LOGIN_FAILED", "User", cleanEmail, "unknown email");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
    }
    if (!user.isEnabled()) {
      audit.log(user, "LOGIN_FAILED", "User", user.getId().toString(), "disabled account");
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "account disabled");
    }
    if (user.isLocked()) {
      audit.log(user, "LOGIN_FAILED", "User", user.getId().toString(), "account locked");
      throw new ResponseStatusException(
          HttpStatus.LOCKED, "account locked after repeated failures — try again later");
    }
    if (!passwordOk) {
      int attempts = user.getFailedAttempts() + 1;
      user.setFailedAttempts(attempts);
      if (attempts >= MAX_FAILED_ATTEMPTS) {
        user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
        user.setFailedAttempts(0);
        audit.log(user, "ACCOUNT_LOCKED", "User", user.getId().toString(), "too many failed logins");
      } else {
        audit.log(user, "LOGIN_FAILED", "User", user.getId().toString(), "bad password");
      }
      users.save(user);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
    }
    user.setFailedAttempts(0);
    user.setLockedUntil(null);
    users.save(user);
    audit.log(user, "LOGIN", "User", user.getId().toString(), "password login");
    return issueTokens(user);
  }

  /** Rotate: the presented refresh token is revoked, a fresh pair is issued. */
  @Transactional
  public AuthTokens refresh(String rawRefreshToken) {
    UUID userId;
    try {
      userId = jwtService.parseRefreshTokenSubject(rawRefreshToken);
    } catch (JwtException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
    }
    RefreshToken stored =
        refreshTokens
            .findByTokenHash(sha256(rawRefreshToken))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token"));
    if (stored.isRevoked() || stored.isExpired() || !stored.getUser().getId().equals(userId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
    }
    stored.setRevoked(true);
    refreshTokens.save(stored);
    User user = stored.getUser();
    audit.log(user, "TOKEN_REFRESHED", "User", user.getId().toString(), null);
    return issueTokens(user);
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    try {
      UUID userId = jwtService.parseRefreshTokenSubject(rawRefreshToken);
      refreshTokens
          .findByTokenHash(sha256(rawRefreshToken))
          .ifPresent(
              t -> {
                if (t.getUser().getId().equals(userId)) {
                  t.setRevoked(true);
                  refreshTokens.save(t);
                }
              });
    } catch (JwtException e) {
      // Already invalid: nothing to do.
    }
  }

  private AuthTokens issueTokens(User user) {
    String access = jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
    String refresh = jwtService.issueRefreshToken(user.getId());
    refreshTokens.save(
        new RefreshToken(user, sha256(refresh), Instant.now().plus(props.refreshTtl())));
    return new AuthTokens(access, refresh, props.accessTtl().toSeconds());
  }

  public User userByEmail(String email) {
    return users
        .findByEmail(email.toLowerCase().trim())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
  }

  private void validatePassword(String raw) {
    if (raw == null
        || raw.length() < 10
        || !raw.matches(".*[A-Z].*")
        || !raw.matches(".*[a-z].*")
        || !raw.matches(".*\\d.*")
        || !raw.matches(".*[^A-Za-z0-9].*")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "password must be at least 10 characters with upper, lower, digit and symbol");
    }
  }

  static String sha256(String raw) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public record AuthTokens(String accessToken, String refreshToken, long accessExpiresInSeconds) {}
}
