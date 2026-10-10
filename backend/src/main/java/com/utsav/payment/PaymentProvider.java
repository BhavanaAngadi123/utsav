package com.utsav.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provider-agnostic payment interface. Stripe is the first implementation;
 * Razorpay (India/UPI) can be added later behind this same interface.
 */
public interface PaymentProvider {

  /** Provider name: "stripe", "razorpay", ... */
  String name();

  /** Create a payment intent; returns the provider's payment id + client secret. */
  PaymentIntentResult createIntent(UUID paymentId, BigDecimal amount, String currency);

  /** Refund a succeeded payment; returns the provider's refund id. */
  String refund(String providerPaymentId, BigDecimal amount);

  /** Verify a webhook signature; throws on invalid signature. */
  void verifyWebhookSignature(String payload, String signatureHeader);

  record PaymentIntentResult(String providerPaymentId, String clientSecret) {}
}
