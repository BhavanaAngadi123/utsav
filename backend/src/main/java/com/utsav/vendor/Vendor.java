package com.utsav.vendor;

import com.utsav.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Vendor profile. */
@Entity
@Table(name = "vendors")
public class Vendor {

  public enum VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED
  }

  @Id
  private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "business_name", nullable = false, length = 160)
  private String businessName;

  @Column(name = "category_slug", nullable = false, length = 64)
  private String categorySlug;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, length = 120)
  private String city;

  @Column(nullable = false, length = 8)
  private String country;

  @Column(nullable = false, length = 3)
  private String currency = "USD";

  @Column(name = "base_price", precision = 12, scale = 2)
  private BigDecimal basePrice;

  @Column(name = "price_unit", nullable = false, length = 32)
  private String priceUnit = "event";

  private Double latitude;
  private Double longitude;

  @Column(name = "portfolio_json", columnDefinition = "TEXT")
  private String portfolioJson;

  @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
  private BigDecimal ratingAvg = BigDecimal.ZERO;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "verified_status", nullable = false, length = 20)
  private VerificationStatus verifiedStatus = VerificationStatus.UNVERIFIED;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Vendor() {}

  // Getters and setters
  public UUID getId() { return id; }
  public User getOwner() { return owner; }
  public void setOwner(User owner) { this.owner = owner; }
  public String getBusinessName() { return businessName; }
  public void setBusinessName(String businessName) { this.businessName = businessName; }
  public String getCategorySlug() { return categorySlug; }
  public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getCountry() { return country; }
  public void setCountry(String country) { this.country = country; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public BigDecimal getBasePrice() { return basePrice; }
  public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
  public String getPriceUnit() { return priceUnit; }
  public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
  public Double getLatitude() { return latitude; }
  public void setLatitude(Double latitude) { this.latitude = latitude; }
  public Double getLongitude() { return longitude; }
  public void setLongitude(Double longitude) { this.longitude = longitude; }
  public String getPortfolioJson() { return portfolioJson; }
  public void setPortfolioJson(String portfolioJson) { this.portfolioJson = portfolioJson; }
  public BigDecimal getRatingAvg() { return ratingAvg; }
  public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
  public int getReviewCount() { return reviewCount; }
  public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
  public VerificationStatus getVerifiedStatus() { return verifiedStatus; }
  public void setVerifiedStatus(VerificationStatus verifiedStatus) { this.verifiedStatus = verifiedStatus; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
