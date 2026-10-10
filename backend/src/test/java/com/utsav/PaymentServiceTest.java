package com.utsav;

import static org.assertj.core.api.Assertions.assertThat;

import com.utsav.booking.Booking;
import com.utsav.booking.BookingService;
import com.utsav.payment.Payment;
import com.utsav.payment.PaymentService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Payments (stub mode): intent, webhook succeeded/failed, refund, history. */
class PaymentServiceTest extends IntegrationTestBase {

  @Autowired private PaymentService paymentService;
  @Autowired private BookingService bookingService;

  private Booking book() {
    return runAs(
        customer,
        () ->
            bookingService.book(
                customer.getId(),
                vendor.getId(),
                LocalDate.now().plusDays(40),
                "Evening",
                "mehendi",
                null));
  }

  @Test
  void createIntentForBooking() {
    Booking booking = book();
    var intent =
        runAs(
            customer,
            () -> paymentService.createIntent(customer.getId(), booking.getId(), Payment.Kind.DEPOSIT));
    assertThat(intent.paymentId()).isNotBlank();
    assertThat(intent.providerPaymentId()).startsWith("stub_");
  }

  @Test
  void webhookSucceededMarksDepositPaid() {
    Booking booking = book();
    var intent =
        runAs(
            customer,
            () -> paymentService.createIntent(customer.getId(), booking.getId(), Payment.Kind.DEPOSIT));
    paymentService.handleWebhookSucceeded(intent.providerPaymentId());
    var payments = paymentService.historyForBooking(booking.getId());
    assertThat(payments).hasSize(1);
    assertThat(payments.get(0).getStatus()).isEqualTo(Payment.Status.SUCCEEDED);
  }

  @Test
  void webhookFailedMarksPaymentFailed() {
    Booking booking = book();
    var intent =
        runAs(
            customer,
            () -> paymentService.createIntent(customer.getId(), booking.getId(), Payment.Kind.FULL));
    paymentService.handleWebhookFailed(intent.providerPaymentId());
    var payments = paymentService.historyForBooking(booking.getId());
    assertThat(payments.get(0).getStatus()).isEqualTo(Payment.Status.FAILED);
  }

  @Test
  void vendorCanRefundSucceededPayment() {
    Booking booking = book();
    var intent =
        runAs(
            customer,
            () -> paymentService.createIntent(customer.getId(), booking.getId(), Payment.Kind.FULL));
    paymentService.handleWebhookSucceeded(intent.providerPaymentId());
    var payments = paymentService.historyForBooking(booking.getId());
    Payment refunded =
        runAs(vendorOwner, () -> paymentService.refund(java.util.UUID.fromString(payments.get(0).getId().toString())));
    assertThat(refunded.getStatus()).isEqualTo(Payment.Status.REFUNDED);
  }

  @Test
  void customerPaymentHistory() {
    Booking booking = book();
    runAs(
        customer,
        () -> paymentService.createIntent(customer.getId(), booking.getId(), Payment.Kind.DEPOSIT));
    var history = paymentService.historyForCustomer(customer.getId());
    assertThat(history).hasSize(1);
  }
}
