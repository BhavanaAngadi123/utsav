package com.utsav.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utsav.config.UtsavProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe webhook endpoint. Verifies the Stripe signature; in stub mode
 * (Stripe disabled) accepts a test envelope without a secret.
 */
@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks")
public class StripeWebhookController {

  private final PaymentService paymentService;
  private final Optional<PaymentProvider> provider;
  private final UtsavProperties props;
  private final ObjectMapper objectMapper;

  public StripeWebhookController(
      PaymentService paymentService,
      Optional<PaymentProvider> provider,
      UtsavProperties props,
      ObjectMapper objectMapper) {
    this.paymentService = paymentService;
    this.provider = provider;
    this.props = props;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/stripe")
  @Operation(summary = "Stripe webhook: payment events (signature-verified)")
  public ResponseEntity<Void> stripe(
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
    if (provider.isPresent()) {
      if (signature == null) {
        return ResponseEntity.badRequest().build();
      }
      provider.get().verifyWebhookSignature(payload, signature);
    }
    // Parse the event (works for real Stripe events and test envelopes).
    try {
      JsonNode root = objectMapper.readTree(payload);
      String type = root.path("type").asText("");
      String paymentIntentId =
          root.path("data").path("object").path("id").asText(null);
      if (paymentIntentId == null) {
        // Test envelope: { "paymentIntentId": "pi_...", "type": "..." }
        paymentIntentId = root.path("paymentIntentId").asText(null);
      }
      if (paymentIntentId == null) {
        return ResponseEntity.badRequest().build();
      }
      switch (type) {
        case "payment_intent.succeeded" -> paymentService.handleWebhookSucceeded(paymentIntentId);
        case "payment_intent.payment_failed" -> paymentService.handleWebhookFailed(paymentIntentId);
        case "charge.refunded" -> paymentService.handleWebhookRefunded(paymentIntentId);
        default -> {
          // Ignore unhandled event types.
        }
      }
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
