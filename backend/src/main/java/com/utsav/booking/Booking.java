package com.utsav.booking;

import com.utsav.user.User;
import com.utsav.vendor.Vendor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A customer's booking of a vendor. */
@Entity
@Table(name = "bookings")
public class Booking {

  public enum Status {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
  }

  public enum PaymentStatus {
    UNPAID,
    DEPOSIT_PAID,
    PAID,
    REFUNDED,
    FAILED
  }

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Vendor vendor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "slot_id")
  private VendorSlot slot;

  @Column(name = "occasion_slug", length = 64)
  private String occasionSlug;

  @Column(name = "event_date")
  private LocalDate eventDate;

  @Column(name = "slot_label", length = 64)
  private String slotLabel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

  @Column(name = "total_amount", precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 3)
  private String currency = "USD";

  @Column(length = 1000)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Booking() {}

  // Getters and setters
  public UUID getId() { return id; }
  public User getCustomer() { return customer; }
  public void setCustomer(User customer) { this.customer = customer; }
  public Vendor getVendor() { return vendor; }
  public void setVendor(Vendor vendor) { this.vendor = vendor; }
  public VendorSlot getSlot() { return slot; }
  public void setSlot(VendorSlot slot) { this.slot = slot; }
  public String getOccasionSlug() { return occasionSlug; }
  public void setOccasionSlug(String occasionSlug) { this.occasionSlug = occasionSlug; }
  public LocalDate getEventDate() { return eventDate; }
  public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
  public String getSlotLabel() { return slotLabel; }
  public void setSlotLabel(String slotLabel) { this.slotLabel = slotLabel; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public PaymentStatus getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
