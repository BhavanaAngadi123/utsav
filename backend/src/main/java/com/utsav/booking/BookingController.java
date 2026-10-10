package com.utsav.booking;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Slot bookings: customers book, vendors confirm, both list. */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
public class BookingController {

  private final BookingService bookingService;
  private final JwtService jwtService;

  public BookingController(BookingService bookingService, JwtService jwtService) {
    this.bookingService = bookingService;
    this.jwtService = jwtService;
  }

  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Book a vendor slot (CUSTOMER only)")
  public ResponseEntity<BookingView> book(
      @RequestHeader("Authorization") String auth, @RequestBody BookRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    Booking booking =
        bookingService.book(
            claims.userId(), req.vendorId(), req.eventDate(), req.slotLabel(),
            req.occasionSlug(), req.notes());
    return ResponseEntity.status(HttpStatus.CREATED).body(BookingView.of(booking));
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "My bookings (CUSTOMER only)")
  public List<BookingView> myBookings(@RequestHeader("Authorization") String auth) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return bookingService.myBookings(claims.userId()).stream().map(BookingView::of).toList();
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Cancel my booking (CUSTOMER only, own booking)")
  public BookingView cancel(
      @RequestHeader("Authorization") String auth, @PathVariable UUID id) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return BookingView.of(bookingService.cancel(claims.userId(), id));
  }

  @GetMapping("/vendor")
  @PreAuthorize("hasRole('VENDOR')")
  @Operation(summary = "Bookings for my vendor profile (VENDOR only)")
  public List<BookingView> vendorBookings(
      @RequestHeader("Authorization") String auth, @RequestParam UUID vendorId) {
    jwtService.parseAccessToken(auth.substring(7)); // validates token; service checks ownership
    return bookingService.vendorBookings(vendorId).stream().map(BookingView::of).toList();
  }

  @PostMapping("/{id}/confirm")
  @PreAuthorize("hasRole('VENDOR')")
  @Operation(summary = "Confirm a booking (VENDOR only, own listing)")
  public BookingView confirm(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID id,
      @RequestParam UUID vendorId) {
    jwtService.parseAccessToken(auth.substring(7));
    return BookingView.of(bookingService.confirm(vendorId, id));
  }

  @GetMapping("/vendors/{vendorId}/slots")
  @Operation(summary = "Slots for a vendor on a date")
  public List<SlotView> slots(
      @PathVariable UUID vendorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return bookingService.slotsForDate(vendorId, date).stream()
        .map(s -> new SlotView(s.getId().toString(), s.getSlotLabel(), s.getStatus().name()))
        .toList();
  }

  public record BookRequest(
      @NotNull UUID vendorId,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
      String slotLabel,
      String occasionSlug,
      String notes) {}

  public record BookingView(
      String id,
      String vendorName,
      String occasionSlug,
      LocalDate eventDate,
      String slotLabel,
      String status,
      String paymentStatus,
      String currency) {
    public static BookingView of(Booking b) {
      return new BookingView(
          b.getId().toString(),
          b.getVendor().getBusinessName(),
          b.getOccasionSlug(),
          b.getEventDate(),
          b.getSlotLabel(),
          b.getStatus().name(),
          b.getPaymentStatus().name(),
          b.getCurrency());
    }
  }

  public record SlotView(String id, String label, String status) {}
}
