package com.utsav.concierge;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A concierge chat session. */
@Entity
@Table(name = "concierge_sessions")
public class ConciergeSession {

  @Id
  private UUID id = UUID.randomUUID();

  @Column(name = "customer_id")
  private UUID customerId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected ConciergeSession() {}

  public ConciergeSession(UUID customerId) {
    this.customerId = customerId;
  }

  public UUID getId() { return id; }
  public UUID getCustomerId() { return customerId; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
