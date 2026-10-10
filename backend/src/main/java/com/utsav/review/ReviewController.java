package com.utsav.review;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Reviews: customers rate vendors. */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews")
public class ReviewController {

  private final ReviewService reviewService;
  private final JwtService jwtService;

  public ReviewController(ReviewService reviewService, JwtService jwtService) {
    this.reviewService = reviewService;
    this.jwtService = jwtService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Review a completed booking (CUSTOMER only, own booking)")
  public ResponseEntity<ReviewView> add(
      @RequestHeader("Authorization") String auth, @RequestBody AddReviewRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    Review review =
        reviewService.addReview(claims.userId(), req.bookingId(), req.rating(), req.comment());
    return ResponseEntity.status(HttpStatus.CREATED).body(ReviewView.of(review));
  }

  @GetMapping("/vendors/{vendorId}")
  @Operation(summary = "Reviews for a vendor")
  public List<ReviewView> forVendor(@PathVariable UUID vendorId) {
    return reviewService.forVendor(vendorId).stream().map(ReviewView::of).toList();
  }

  public record AddReviewRequest(
      @NotNull UUID bookingId, @Min(1) @Max(5) int rating, String comment) {}

  public record ReviewView(String id, int rating, String comment, String customerName) {
    static ReviewView of(Review r) {
      return new ReviewView(
          r.getId().toString(), r.getRating(), r.getComment(), r.getCustomer().getDisplayName());
    }
  }
}
