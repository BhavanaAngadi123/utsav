package com.utsav.team;

import com.utsav.vendor.Vendor;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A vendor on an event team. */
@Entity
@Table(name = "event_team_members")
public class EventTeamMember {

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private EventTeam team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Vendor vendor;

  @Column(name = "role_label", nullable = false, length = 64)
  private String roleLabel;

  @Column(name = "added_at", nullable = false, updatable = false)
  private Instant addedAt = Instant.now();

  protected EventTeamMember() {}

  public EventTeamMember(EventTeam team, Vendor vendor, String roleLabel) {
    this.team = team;
    this.vendor = vendor;
    this.roleLabel = roleLabel;
  }

  public UUID getId() { return id; }
  public EventTeam getTeam() { return team; }
  public Vendor getVendor() { return vendor; }
  public String getRoleLabel() { return roleLabel; }
}
