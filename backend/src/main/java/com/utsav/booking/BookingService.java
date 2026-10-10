package com.utsav.booking;

import com.utsav.admin.AuditService;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Direct slot bookings: customers book vendors, vendors manage their bookings. */
@Service
public class BookingService {

  private final BookingRepository bookings;
  private final VendorSlotRepository slots;
  private final VendorRepository vendors;
  private final UserRepository users;
  private final AuditService audit;

  public BookingService(
      BookingRepository bookings,
      VendorSlotRepository slots,
      VendorRepository vendors,
      UserRepository users,
      AuditService audit) {
    this.bookings = bookings;
    this.slots = slots;
    this.vendors = vendors;
    this.users = users;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public Booking book(
      UUID customerId,
      UUID vendorId,
      LocalDate eventDate,
      String slotLabel,
      String occasionSlug,
      String notes) {
    User customer =
        users
            .findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    Vendor vendor =
        vendors
            .findById(vendorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendor not found"));
    if (!vendor.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "vendor is not active");
    }

    VendorSlot slot = null;
    if (eventDate != null && slotLabel != null) {
      slot =
          slots
              .findByVendorIdAndSlotDateAndSlotLabel(vendorId, eventDate, slotLabel)
              .orElseGet(() -> slots.save(new VendorSlot(vendor, eventDate, slotLabel)));
      if (slot.getStatus() != VendorSlot.Status.AVAILABLE) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "slot is not available");
      }
      slot.setStatus(VendorSlot.Status.BOOKED);
      slots.save(slot);
    }

    Booking booking = new Booking();
    booking.setCustomer(customer);
    booking.setVendor(vendor);
    booking.setSlot(slot);
    booking.setOccasionSlug(occasionSlug);
    booking.setEventDate(eventDate);
    booking.setSlotLabel(slotLabel);
    booking.setNotes(notes);
    booking.setTotalAmount(vendor.getBasePrice());
    booking.setCurrency(vendor.getCurrency());
    bookings.save(booking);
    audit.log(
        customer, "BOOKING_CREATED", "Booking", booking.getId().toString(), vendor.getBusinessName());
    return booking;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public Booking cancel(UUID customerId, UUID bookingId) {
    Booking booking =
        bookings
            .findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "booking not found"));
    if (!booking.getCustomer().getId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your booking");
    }
    booking.setStatus(Booking.Status.CANCELLED);
    if (booking.getSlot() != null) {
      booking.getSlot().setStatus(VendorSlot.Status.AVAILABLE);
      slots.save(booking.getSlot());
    }
    audit.log(
        booking.getCustomer(), "BOOKING_CANCELLED", "Booking", bookingId.toString(), null);
    return bookings.save(booking);
  }

  @PreAuthorize("hasRole('VENDOR')")
  @Transactional
  public Booking confirm(UUID vendorId, UUID bookingId) {
    Booking booking = getVendorBooking(vendorId, bookingId);
    booking.setStatus(Booking.Status.CONFIRMED);
    audit.log(null, "BOOKING_CONFIRMED", "Booking", bookingId.toString(), null);
    return bookings.save(booking);
  }

  public List<Booking> myBookings(UUID customerId) {
    return bookings.findByCustomerIdOrderByEventDateDesc(customerId);
  }

  public List<Booking> vendorBookings(UUID vendorId) {
    return bookings.findByVendorIdOrderByEventDateDesc(vendorId);
  }

  public Booking getForCustomer(UUID customerId, UUID bookingId) {
    Booking booking =
        bookings
            .findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "booking not found"));
    if (!booking.getCustomer().getId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your booking");
    }
    return booking;
  }

  private Booking getVendorBooking(UUID vendorId, UUID bookingId) {
    Booking booking =
        bookings
            .findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "booking not found"));
    if (!booking.getVendor().getId().equals(vendorId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your booking");
    }
    return booking;
  }

  @PreAuthorize("hasRole('VENDOR')")
  @Transactional
  public VendorSlot blockSlot(UUID vendorId, LocalDate date, String label) {
    Vendor vendor =
        vendors
            .findById(vendorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendor not found"));
    VendorSlot slot =
        slots
            .findByVendorIdAndSlotDateAndSlotLabel(vendorId, date, label)
            .orElseGet(() -> new VendorSlot(vendor, date, label));
    slot.setStatus(VendorSlot.Status.BLOCKED);
    return slots.save(slot);
  }

  public List<VendorSlot> slotsForDate(UUID vendorId, LocalDate date) {
    return slots.findByVendorIdAndSlotDate(vendorId, date);
  }

  /** Called by payments when a deposit/full payment succeeds. */
  @Transactional
  public void markDepositPaid(UUID bookingId, BigDecimal amount) {
    bookings
        .findById(bookingId)
        .ifPresent(
            b -> {
              if (b.getPaymentStatus() == Booking.PaymentStatus.UNPAID) {
                b.setPaymentStatus(Booking.PaymentStatus.DEPOSIT_PAID);
              }
              bookings.save(b);
            });
  }

  @Transactional
  public void markPaid(UUID bookingId) {
    bookings
        .findById(bookingId)
        .ifPresent(
            b -> {
              b.setPaymentStatus(Booking.PaymentStatus.PAID);
              if (b.getStatus() == Booking.Status.PENDING) {
                b.setStatus(Booking.Status.CONFIRMED);
              }
              bookings.save(b);
            });
  }

  @Transactional
  public void markPaymentFailed(UUID bookingId) {
    bookings
        .findById(bookingId)
        .ifPresent(
            b -> {
              b.setPaymentStatus(Booking.PaymentStatus.FAILED);
              bookings.save(b);
            });
  }
}
