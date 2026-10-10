package com.utsav.review;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
  List<Review> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
  boolean existsByBookingId(UUID bookingId);
}
