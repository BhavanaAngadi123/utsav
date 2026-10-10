package com.utsav.verification;

import com.utsav.user.User;
import com.utsav.vendor.Vendor;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Government-ID verification record.
 *
 * <p>PRIVACY: stores type + country + status ONLY. Never stores real
 * government ID numbers or document images.
 */
@Entity
@Table(name = "id_verifications")
public class IdVerification {

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
  }

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Vendor vendor;

  @Column(name = "country_code", nullable = false, length = 8)
  private String countryCode;

  @Column(name = "id_type", nullable = false, length = 64)
  private String idType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private User reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "review_note", length = 500)
  private String reviewNote;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected IdVerification() {}

  public IdVerification(Vendor vendor, String countryCode, String idType) {
    this.vendor = vendor;
    this.countryCode = countryCode.toUpperCase();
    this.idType = idType;
  }

  public UUID getId() { return id; }
  public Vendor getVendor() { return vendor; }
  public String getCountryCode() { return countryCode; }
  public String getIdType() { return idType; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public User getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
  public Instant getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
  public String getReviewNote() { return reviewNote; }
  public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
