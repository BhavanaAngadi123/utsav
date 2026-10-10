package com.utsav.payment;

import com.utsav.admin.AuditService;
import com.utsav.booking.Booking;
import com.utsav.booking.BookingRepository;
import com.utsav.booking.BookingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Payments: create intents, confirm, refund, history.
 * Works with or without a live provider (stub mode when Stripe disabled).
 */
@Service
public class PaymentService {

  private final PaymentRepository payments;
  private final BookingRepository bookings;
  private final BookingService bookingService;
  private final Optional<PaymentProvider> provider;
  private final AuditService audit;

  public PaymentService(
      PaymentRepository payments,
      BookingRepository bookings,
      BookingService bookingService,
      Optional<PaymentProvider> provider,
      AuditService audit) {
    this.payments = payments;
    this.bookings = bookings;
    this.bookingService = bookingService;
    this.provider = provider;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public IntentResponse createIntent(UUID customerId, UUID bookingId, Payment.Kind kind) {
    Booking booking =
        bookings
            .findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "booking not found"));
    if (!booking.getCustomer().getId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your booking");
    }
    BigDecimal amount = booking.getTotalAmount();
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "booking has no amount");
    }
    if (kind == Payment.Kind.DEPOSIT) {
      // 25% deposit.
      amount = amount.multiply(new BigDecimal("0.25"));
    }

    Payment payment =
        new Payment(
            booking, booking.getCustomer(), booking.getVendor(), amount, booking.getCurrency(), kind);
    payments.save(payment);

    String clientSecret = null;
    if (provider.isPresent()) {
      var result = provider.get().createIntent(payment.getId(), amount, booking.getCurrency());
      payment.setProvider(provider.get().name());
      payment.setProviderPaymentId(result.providerPaymentId());
      clientSecret = result.clientSecret();
      payments.save(payment);
    } else {
      // Stub mode: no live provider; mark intent created locally.
      payment.setProvider("stub");
      payment.setProviderPaymentId("stub_" + payment.getId());
      payments.save(payment);
    }
    audit.log(
        booking.getCustomer(),
        "PAYMENT_INTENT_CREATED",
        "Payment",
        payment.getId().toString(),
        kind + " " + amount + " " + booking.getCurrency());
    return new IntentResponse(
        payment.getId().toString(), payment.getProviderPaymentId(), clientSecret);
  }

  @Transactional
  public void handleWebhookSucceeded(String providerPaymentId) {
    payments
        .findByProviderPaymentId(providerPaymentId)
        .ifPresent(
            p -> {
              p.setStatus(Payment.Status.SUCCEEDED);
              payments.save(p);
              if (p.getKind() == Payment.Kind.DEPOSIT) {
                bookingService.markDepositPaid(p.getBooking().getId(), p.getAmount());
              } else {
                bookingService.markPaid(p.getBooking().getId());
              }
              audit.log(
                  p.getCustomer(),
                  "PAYMENT_SUCCEEDED",
                  "Payment",
                  p.getId().toString(),
                  providerPaymentId);
            });
  }

  @Transactional
  public void handleWebhookFailed(String providerPaymentId) {
    payments
        .findByProviderPaymentId(providerPaymentId)
        .ifPresent(
            p -> {
              p.setStatus(Payment.Status.FAILED);
              payments.save(p);
              bookingService.markPaymentFailed(p.getBooking().getId());
              audit.log(
                  p.getCustomer(), "PAYMENT_FAILED", "Payment", p.getId().toString(), providerPaymentId);
            });
  }

  @Transactional
  public void handleWebhookRefunded(String providerPaymentId) {
    payments
        .findByProviderPaymentId(providerPaymentId)
        .ifPresent(
            p -> {
              p.setStatus(Payment.Status.REFUNDED);
              payments.save(p);
              audit.log(
                  p.getCustomer(),
                  "PAYMENT_REFUNDED",
                  "Payment",
                  p.getId().toString(),
                  providerPaymentId);
            });
  }

  @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
  @Transactional
  public Payment refund(UUID paymentId) {
    Payment payment =
        payments
            .findById(paymentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found"));
    if (payment.getStatus() != Payment.Status.SUCCEEDED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only succeeded payments can be refunded");
    }
    if (provider.isPresent()) {
      provider.get().refund(payment.getProviderPaymentId(), payment.getAmount());
    }
    payment.setStatus(Payment.Status.REFUNDED);
    payments.save(payment);
    audit.log(null, "PAYMENT_REFUND_INITIATED", "Payment", paymentId.toString(), null);
    return payment;
  }

  public List<Payment> historyForBooking(UUID bookingId) {
    return payments.findByBookingIdOrderByCreatedAtDesc(bookingId);
  }

  public List<Payment> historyForCustomer(UUID customerId) {
    return payments.findByCustomerIdOrderByCreatedAtDesc(customerId);
  }

  public List<Payment> historyForVendor(UUID vendorId) {
    return payments.findByVendorIdOrderByCreatedAtDesc(vendorId);
  }

  public record IntentResponse(String paymentId, String providerPaymentId, String clientSecret) {}
}
