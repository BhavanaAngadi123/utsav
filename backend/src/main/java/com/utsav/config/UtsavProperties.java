package com.utsav.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Utsav configuration. Secrets come ONLY from environment variables;
 * the app fails fast at startup if required secrets are missing.
 */
@ConfigurationProperties(prefix = "utsav")
public class UtsavProperties {

  private String jwtSecret;
  private long jwtAccessTtlSeconds = 900;
  private long jwtRefreshTtlSeconds = 604800;
  private String corsAllowedOrigins = "http://localhost:8080";
  private final Stripe stripe = new Stripe();
  private final Concierge concierge = new Concierge();
  private final Oauth oauth = new Oauth();

  @PostConstruct
  void validate() {
    if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
      throw new IllegalStateException(
          "UTSAV_JWT_SECRET is required (at least 32 characters). Generate: openssl rand -base64 48");
    }
  }

  public String getJwtSecret() { return jwtSecret; }
  public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
  public Duration accessTtl() { return Duration.ofSeconds(jwtAccessTtlSeconds); }
  public Duration refreshTtl() { return Duration.ofSeconds(jwtRefreshTtlSeconds); }
  public long getJwtAccessTtlSeconds() { return jwtAccessTtlSeconds; }
  public void setJwtAccessTtlSeconds(long v) { this.jwtAccessTtlSeconds = v; }
  public long getJwtRefreshTtlSeconds() { return jwtRefreshTtlSeconds; }
  public void setJwtRefreshTtlSeconds(long v) { this.jwtRefreshTtlSeconds = v; }
  public String getCorsAllowedOrigins() { return corsAllowedOrigins; }
  public void setCorsAllowedOrigins(String v) { this.corsAllowedOrigins = v; }
  public Stripe getStripe() { return stripe; }
  public Concierge getConcierge() { return concierge; }
  public Oauth getOauth() { return oauth; }

  public static class Stripe {
    private boolean enabled = false;
    private String secretKey = "sk_test_PLACEHOLDER";
    private String webhookSecret = "whsec_PLACEHOLDER";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
  }

  public static class Concierge {
    private boolean enabled = true;
    private String ollamaModel = "qwen3:8b";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String ollamaModel) { this.ollamaModel = ollamaModel; }
  }

  public static class Oauth {
    private boolean googleEnabled = false;

    public boolean isGoogleEnabled() { return googleEnabled; }
    public void setGoogleEnabled(boolean googleEnabled) { this.googleEnabled = googleEnabled; }
  }
}
