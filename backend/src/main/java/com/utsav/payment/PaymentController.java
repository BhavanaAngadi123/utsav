package com.utsav.payment;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Payments: intents, refunds, history. */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments")
public class PaymentController {

  private final PaymentService paymentService;
  private final JwtService jwtService;

  public PaymentController(PaymentService paymentService, JwtService jwtService) {
    this.paymentService = paymentService;
    this.jwtService = jwtService;
  }

  @PostMapping("/intents")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Create a payment intent for a booking (CUSTOMER only, own booking)")
  public ResponseEntity<PaymentService.IntentResponse> createIntent(
      @RequestHeader("Authorization") String auth, @RequestBody IntentRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    var result = paymentService.createIntent(claims.userId(), req.bookingId(), req.kind());
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @GetMapping("/bookings/{bookingId}")
  @Operation(summary = "Payment history for a booking")
  public List<PaymentView> forBooking(@PathVariable UUID bookingId) {
    return paymentService.historyForBooking(bookingId).stream().map(PaymentView::of).toList();
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "My payment history (CUSTOMER only)")
  public List<PaymentView> myPayments(@RequestHeader("Authorization") String auth) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return paymentService.historyForCustomer(claims.userId()).stream().map(PaymentView::of).toList();
  }

  @PostMapping("/{id}/refund")
  @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
  @Operation(summary = "Refund a succeeded payment (VENDOR/ADMIN only)")
  public PaymentView refund(@PathVariable UUID id) {
    return PaymentView.of(paymentService.refund(id));
  }

  public record IntentRequest(@NotNull UUID bookingId, @NotNull Payment.Kind kind) {}

  public record PaymentView(
      String id, String kind, String status, String amount, String currency, String provider) {
    static PaymentView of(Payment p) {
      return new PaymentView(
          p.getId().toString(),
          p.getKind().name(),
          p.getStatus().name(),
          p.getAmount().toPlainString(),
          p.getCurrency(),
          p.getProvider());
    }
  }
}
