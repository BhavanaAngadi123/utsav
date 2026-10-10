package com.utsav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utsav.booking.Booking;
import com.utsav.booking.BookingService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

/** Bookings: book a slot, double-booking is rejected, cancel frees the slot. */
class BookingServiceTest extends IntegrationTestBase {

  @Autowired private BookingService bookingService;

  @Test
  void customerCanBookAvailableSlot() {
    Booking booking =
        runAs(
            customer,
            () ->
                bookingService.book(
                    customer.getId(),
                    vendor.getId(),
                    LocalDate.now().plusDays(30),
                    "Evening",
                    "mehendi",
                    "Test booking"));
    assertThat(booking.getStatus()).isEqualTo(Booking.Status.PENDING);
    assertThat(booking.getVendor().getId()).isEqualTo(vendor.getId());
  }

  @Test
  void doubleBookingSameSlotIsRejected() {
    LocalDate date = LocalDate.now().plusDays(31);
    runAs(
        customer,
        () -> bookingService.book(customer.getId(), vendor.getId(), date, "Morning", "wedding", null));
    assertThatThrownBy(
            () ->
                runAs(
                    customer,
                    () ->
                        bookingService.book(
                            customer.getId(), vendor.getId(), date, "Morning", "wedding", null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not available");
  }

  @Test
  void customerCanCancelOwnBooking() {
    Booking booking =
        runAs(
            customer,
            () ->
                bookingService.book(
                    customer.getId(), vendor.getId(), LocalDate.now().plusDays(32), "Evening", null, null));
    Booking cancelled = runAs(customer, () -> bookingService.cancel(customer.getId(), booking.getId()));
    assertThat(cancelled.getStatus()).isEqualTo(Booking.Status.CANCELLED);
  }

  @Test
  void vendorCanConfirmBooking() {
    Booking booking =
        runAs(
            customer,
            () ->
                bookingService.book(
                    customer.getId(), vendor.getId(), LocalDate.now().plusDays(33), "Evening", null, null));
    Booking confirmed =
        runAs(vendorOwner, () -> bookingService.confirm(vendor.getId(), booking.getId()));
    assertThat(confirmed.getStatus()).isEqualTo(Booking.Status.CONFIRMED);
  }
}
