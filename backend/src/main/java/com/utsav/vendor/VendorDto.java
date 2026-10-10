package com.utsav.vendor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Vendor profile payload (create/update). Output adds ids + rating. */
public class VendorDto {

  public record Upsert(
      @NotBlank @Size(max = 160) String businessName,
      @NotBlank @Size(max = 64) String categorySlug,
      @Size(max = 2000) String description,
      @NotBlank @Size(max = 120) String city,
      @NotBlank @Size(max = 8) String country,
      @Size(max = 3) String currency,
      @DecimalMin("0") BigDecimal basePrice,
      @Size(max = 32) String priceUnit,
      Double latitude,
      Double longitude,
      String portfolioJson,
      Boolean active) {}

  public record View(
      String id,
      String businessName,
      String categorySlug,
      String description,
      String city,
      String country,
      String currency,
      BigDecimal basePrice,
      String priceUnit,
      BigDecimal ratingAvg,
      int reviewCount,
      String verifiedStatus,
      Double latitude,
      Double longitude) {

    public static View of(Vendor v) {
      return new View(
          v.getId().toString(),
          v.getBusinessName(),
          v.getCategorySlug(),
          v.getDescription(),
          v.getCity(),
          v.getCountry(),
          v.getCurrency(),
          v.getBasePrice(),
          v.getPriceUnit(),
          v.getRatingAvg(),
          v.getReviewCount(),
          v.getVerifiedStatus().name(),
          v.getLatitude(),
          v.getLongitude());
    }
  }

  /** Build an entity from an upsert payload (owner assigned by service). */
  public static Vendor toEntity(Upsert u) {
    Vendor v = new Vendor();
    apply(u, v);
    return v;
  }

  public static void apply(Upsert u, Vendor v) {
    v.setBusinessName(u.businessName());
    v.setCategorySlug(u.categorySlug());
    v.setDescription(u.description());
    v.setCity(u.city());
    v.setCountry(u.country());
    if (u.currency() != null) v.setCurrency(u.currency());
    v.setBasePrice(u.basePrice());
    if (u.priceUnit() != null) v.setPriceUnit(u.priceUnit());
    v.setLatitude(u.latitude());
    v.setLongitude(u.longitude());
    if (u.portfolioJson() != null) v.setPortfolioJson(u.portfolioJson());
    if (u.active() != null) v.setActive(u.active());
  }
}
