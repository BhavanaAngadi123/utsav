package com.utsav.payment;

import com.utsav.booking.Booking;
import com.utsav.user.User;
import com.utsav.vendor.Vendor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payment record: amount, currency, status, linked booking/vendor/customer. */
@Entity
@Table(name = "payments")
public class Payment {

  public enum Kind {
    DEPOSIT,
    FULL,
    REFUND
  }

  public enum Status {
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    CANCELLED
  }

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Vendor vendor;

  @Column(nullable = false, length = 32)
  private String provider = "stripe";

  @Column(name = "provider_payment_id", length = 255)
  private String providerPaymentId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency = "USD";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Kind kind = Kind.DEPOSIT;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Payment() {}

  public Payment(
      Booking booking, User customer, Vendor vendor, BigDecimal amount, String currency, Kind kind) {
    this.booking = booking;
    this.customer = customer;
    this.vendor = vendor;
    this.amount = amount;
    this.currency = currency;
    this.kind = kind;
  }

  public UUID getId() { return id; }
  public Booking getBooking() { return booking; }
  public User getCustomer() { return customer; }
  public Vendor getVendor() { return vendor; }
  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }
  public String getProviderPaymentId() { return providerPaymentId; }
  public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
  public BigDecimal getAmount() { return amount; }
  public String getCurrency() { return currency; }
  public Kind getKind() { return kind; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
