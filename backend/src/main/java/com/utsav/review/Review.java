package com.utsav.review;

import com.utsav.booking.Booking;
import com.utsav.user.User;
import com.utsav.vendor.Vendor;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Customer review of a vendor (one per booking). */
@Entity
@Table(name = "reviews")
public class Review {

  @Id
  private UUID id = UUID.randomUUID();

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "vendor_id", nullable = false)
  private Vendor vendor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @Column(nullable = false)
  private int rating;

  @Column(length = 2000)
  private String comment;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected Review() {}

  public Review(Booking booking, Vendor vendor, User customer, int rating, String comment) {
    this.booking = booking;
    this.vendor = vendor;
    this.customer = customer;
    this.rating = rating;
    this.comment = comment;
  }

  public UUID getId() { return id; }
  public Booking getBooking() { return booking; }
  public Vendor getVendor() { return vendor; }
  public User getCustomer() { return customer; }
  public int getRating() { return rating; }
  public String getComment() { return comment; }
}
