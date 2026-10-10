package com.utsav.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.utsav.config.UtsavProperties;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stripe payment provider (TEST mode). Swap to live keys via env vars when ready.
 */
@Component
@ConditionalOnProperty(name = "utsav.stripe.enabled", havingValue = "true")
public class StripePaymentProvider implements PaymentProvider {

  private final UtsavProperties props;

  public StripePaymentProvider(UtsavProperties props) {
    this.props = props;
    Stripe.apiKey = props.getStripe().getSecretKey();
  }

  @Override
  public String name() {
    return "stripe";
  }

  @Override
  public PaymentIntentResult createIntent(UUID paymentId, BigDecimal amount, String currency) {
    try {
      long amountMinor = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
      PaymentIntentCreateParams params =
          PaymentIntentCreateParams.builder()
              .setAmount(amountMinor)
              .setCurrency(currency.toLowerCase())
              .putMetadata("utsav_payment_id", paymentId.toString())
              .setAutomaticPaymentMethods(
                  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                      .setEnabled(true)
                      .setAllowRedirects(
                          PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                      .build())
              .build();
      PaymentIntent intent = PaymentIntent.create(params);
      return new PaymentIntentResult(intent.getId(), intent.getClientSecret());
    } catch (StripeException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "stripe error: " + e.getMessage());
    }
  }

  @Override
  public String refund(String providerPaymentId, BigDecimal amount) {
    try {
      long amountMinor = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
      RefundCreateParams params =
          RefundCreateParams.builder()
              .setPaymentIntent(providerPaymentId)
              .setAmount(amountMinor)
              .build();
      Refund refund = Refund.create(params);
      return refund.getId();
    } catch (StripeException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "stripe refund error: " + e.getMessage());
    }
  }

  @Override
  public void verifyWebhookSignature(String payload, String signatureHeader) {
    try {
      Webhook.constructEvent(payload, signatureHeader, props.getStripe().getWebhookSecret());
    } catch (SignatureVerificationException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid webhook signature");
    }
  }
}
