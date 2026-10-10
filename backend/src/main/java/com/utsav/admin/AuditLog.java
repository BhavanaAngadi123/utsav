package com.utsav.admin;

import com.utsav.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Immutable audit record for sensitive actions. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "entity_type", length = 64)
  private String entityType;

  @Column(name = "entity_id", length = 64)
  private String entityId;

  @Column(length = 1000)
  private String detail;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected AuditLog() {}

  public AuditLog(User actor, String action, String entityType, String entityId, String detail) {
    this.actor = actor;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.detail = detail;
  }

  public UUID getId() { return id; }
  public User getActor() { return actor; }
  public String getAction() { return action; }
  public String getEntityType() { return entityType; }
  public String getEntityId() { return entityId; }
  public String getDetail() { return detail; }
  public Instant getCreatedAt() { return createdAt; }
}
