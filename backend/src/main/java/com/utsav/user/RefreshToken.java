package com.utsav.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Stored refresh token (SHA-256 hash only, never the raw token). */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected RefreshToken() {}

  public RefreshToken(User user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public UUID getId() { return id; }
  public User getUser() { return user; }
  public String getTokenHash() { return tokenHash; }
  public Instant getExpiresAt() { return expiresAt; }
  public boolean isRevoked() { return revoked; }
  public void setRevoked(boolean revoked) { this.revoked = revoked; }

  public boolean isExpired() {
    return expiresAt.isBefore(Instant.now());
  }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
