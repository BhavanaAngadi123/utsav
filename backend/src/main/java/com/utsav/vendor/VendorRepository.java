package com.utsav.vendor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

  Optional<Vendor> findByIdAndOwnerId(UUID id, UUID ownerId);

  List<Vendor> findByOwnerId(UUID ownerId);

  @Query(
      """
      SELECT v FROM Vendor v WHERE v.active = true
        AND (:category IS NULL OR v.categorySlug = :category)
        AND (:city IS NULL OR LOWER(v.city) LIKE LOWER(CONCAT('%', :city, '%')))
        AND (:maxPrice IS NULL OR v.basePrice IS NULL OR v.basePrice <= :maxPrice)
        AND (:verifiedOnly = false OR v.verifiedStatus = com.utsav.vendor.Vendor$VerificationStatus.VERIFIED)
      """)
  Page<Vendor> search(
      @Param("category") String category,
      @Param("city") String city,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("verifiedOnly") boolean verifiedOnly,
      Pageable pageable);

  @Query(
      """
      SELECT v FROM Vendor v WHERE v.active = true
        AND v.latitude IS NOT NULL AND v.longitude IS NOT NULL
        AND (:category IS NULL OR v.categorySlug = :category)
      ORDER BY ((v.latitude - :lat) * (v.latitude - :lat) + (v.longitude - :lng) * (v.longitude - :lng))
      """)
  List<Vendor> findNearby(
      @Param("lat") double lat, @Param("lng") double lng, @Param("category") String category, Pageable pageable);
}
