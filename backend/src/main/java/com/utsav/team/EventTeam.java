package com.utsav.team;

import com.utsav.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An event team: a customer assembles vendors for an event. */
@Entity
@Table(name = "event_teams")
public class EventTeam {

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(name = "occasion_slug", length = 64)
  private String occasionSlug;

  @Column(name = "event_date")
  private LocalDate eventDate;

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventTeamMember> members = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected EventTeam() {}

  public EventTeam(User customer, String name, String occasionSlug, LocalDate eventDate) {
    this.customer = customer;
    this.name = name;
    this.occasionSlug = occasionSlug;
    this.eventDate = eventDate;
  }

  public UUID getId() { return id; }
  public User getCustomer() { return customer; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getOccasionSlug() { return occasionSlug; }
  public LocalDate getEventDate() { return eventDate; }
  public List<EventTeamMember> getMembers() { return members; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
