package com.utsav.user;

import com.utsav.common.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Application user: customer, vendor, or admin. */
@Entity
@Table(name = "users")
public class User {

  @Id
  private UUID id = UUID.randomUUID();

  @Column(nullable = false, unique = true, length = 320)
  private String email;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role = Role.CUSTOMER;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts = 0;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected User() {}

  public User(String email, String passwordHash, String displayName, Role role) {
    this.email = email.toLowerCase().trim();
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.role = role;
  }

  public UUID getId() { return id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email.toLowerCase().trim(); }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public int getFailedAttempts() { return failedAttempts; }
  public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
  public Instant getLockedUntil() { return lockedUntil; }
  public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
  public Instant getCreatedAt() { return createdAt; }

  public boolean isLocked() {
    return lockedUntil != null && lockedUntil.isAfter(Instant.now());
  }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
