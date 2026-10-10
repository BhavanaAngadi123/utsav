package com.utsav.booking;

import com.utsav.vendor.Vendor;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A bookable time slot for a vendor. */
@Entity
@Table(name = "vendor_slots")
public class VendorSlot {

  public enum Status {
    AVAILABLE,
    HELD,
    BOOKED,
    BLOCKED
  }

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Vendor vendor;

  @Column(name = "slot_date", nullable = false)
  private LocalDate slotDate;

  @Column(name = "slot_label", nullable = false, length = 64)
  private String slotLabel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.AVAILABLE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected VendorSlot() {}

  public VendorSlot(Vendor vendor, LocalDate slotDate, String slotLabel) {
    this.vendor = vendor;
    this.slotDate = slotDate;
    this.slotLabel = slotLabel;
  }

  public UUID getId() { return id; }
  public Vendor getVendor() { return vendor; }
  public LocalDate getSlotDate() { return slotDate; }
  public String getSlotLabel() { return slotLabel; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
}
