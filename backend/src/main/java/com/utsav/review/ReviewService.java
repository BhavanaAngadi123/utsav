package com.utsav.review;

import com.utsav.admin.AuditService;
import com.utsav.booking.Booking;
import com.utsav.booking.BookingRepository;
import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reviews: customers rate vendors; vendor rating aggregates update. */
@Service
public class ReviewService {

  private final ReviewRepository reviews;
  private final BookingRepository bookings;
  private final VendorRepository vendors;
  private final AuditService audit;

  public ReviewService(
      ReviewRepository reviews,
      BookingRepository bookings,
      VendorRepository vendors,
      AuditService audit) {
    this.reviews = reviews;
    this.bookings = bookings;
    this.vendors = vendors;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public Review addReview(UUID customerId, UUID bookingId, int rating, String comment) {
    if (rating < 1 || rating > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be 1-5");
    }
    Booking booking =
        bookings
            .findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "booking not found"));
    if (!booking.getCustomer().getId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your booking");
    }
    if (reviews.existsByBookingId(bookingId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "booking already reviewed");
    }
    Review review =
        new Review(booking, booking.getVendor(), booking.getCustomer(), rating, comment);
    reviews.save(review);

    // Update vendor aggregate rating.
    Vendor vendor = booking.getVendor();
    int count = vendor.getReviewCount();
    BigDecimal total = vendor.getRatingAvg().multiply(BigDecimal.valueOf(count));
    BigDecimal newAvg =
        total.add(BigDecimal.valueOf(rating)).divide(BigDecimal.valueOf(count + 1), 2, RoundingMode.HALF_UP);
    vendor.setRatingAvg(newAvg);
    vendor.setReviewCount(count + 1);
    vendors.save(vendor);

    audit.log(
        booking.getCustomer(), "REVIEW_ADDED", "Review", review.getId().toString(), "rating=" + rating);
    return review;
  }

  public List<Review> forVendor(UUID vendorId) {
    return reviews.findByVendorIdOrderByCreatedAtDesc(vendorId);
  }
}
