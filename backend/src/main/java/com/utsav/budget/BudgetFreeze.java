package com.utsav.budget;

import com.utsav.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Budget Freeze: a customer locks guest count + max budget.
 * Vendor discovery filters to vendors fitting the budget.
 */
@Entity
@Table(name = "budget_freezes")
public class BudgetFreeze {

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @Column(name = "guest_count", nullable = false)
  private int guestCount;

  @Column(name = "max_budget", nullable = false, precision = 12, scale = 2)
  private BigDecimal maxBudget;

  @Column(nullable = false, length = 3)
  private String currency = "USD";

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected BudgetFreeze() {}

  public BudgetFreeze(User customer, int guestCount, BigDecimal maxBudget, String currency) {
    this.customer = customer;
    this.guestCount = guestCount;
    this.maxBudget = maxBudget;
    if (currency != null) this.currency = currency;
  }

  public UUID getId() { return id; }
  public User getCustomer() { return customer; }
  public int getGuestCount() { return guestCount; }
  public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
  public BigDecimal getMaxBudget() { return maxBudget; }
  public void setMaxBudget(BigDecimal maxBudget) { this.maxBudget = maxBudget; }
  public String getCurrency() { return currency; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
