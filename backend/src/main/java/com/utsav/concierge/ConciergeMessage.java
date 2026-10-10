package com.utsav.concierge;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A message in a concierge chat session. */
@Entity
@Table(name = "concierge_messages")
public class ConciergeMessage {

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private ConciergeSession session;

  @Column(nullable = false, length = 20)
  private String role;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected ConciergeMessage() {}

  public ConciergeMessage(ConciergeSession session, String role, String content) {
    this.session = session;
    this.role = role;
    this.content = content;
  }

  public UUID getId() { return id; }
  public String getRole() { return role; }
  public String getContent() { return content; }
}
