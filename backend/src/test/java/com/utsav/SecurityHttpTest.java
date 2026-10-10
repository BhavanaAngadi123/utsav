package com.utsav;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utsav.user.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP security: public endpoints open, protected endpoints require JWT,
 * RBAC enforced, malformed JWT rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHttpTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthService authService;

  private String customerToken;
  private String adminToken;

  @BeforeEach
  void setUpTokens() {
    customerToken = authService.login(customer.getEmail(), "Customer!123").accessToken();
    adminToken = authService.login(admin.getEmail(), "Admin!123").accessToken();
  }

  @Test
  void publicEndpointsAreOpen() throws Exception {
    mockMvc.perform(get("/api/vendors")).andExpect(status().isOk());
    mockMvc.perform(get("/api/concierge/status")).andExpect(status().isOk());
    mockMvc.perform(get("/api/catalog/categories")).andExpect(status().isOk());
  }

  @Test
  void protectedEndpointsRequireJwt() throws Exception {
    mockMvc.perform(get("/api/bookings/me")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/budget/freeze/active")).andExpect(status().isUnauthorized());
  }

  @Test
  void malformedJwtIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/bookings/me").header("Authorization", "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void customerCanReachOwnEndpoints() throws Exception {
    mockMvc
        .perform(
            get("/api/bookings/me").header("Authorization", "Bearer " + customerToken))
        .andExpect(status().isOk());
  }

  @Test
  void customerCannotReachAdminApi() throws Exception {
    mockMvc
        .perform(get("/api/admin/audit").header("Authorization", "Bearer " + customerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void webhookAcceptsTestEnvelopeWithoutSecret() throws Exception {
    // Stripe disabled in tests: webhook accepts test envelope (no signature).
    String envelope =
        objectMapper.writeValueAsString(
            java.util.Map.of("type", "payment_intent.succeeded", "paymentIntentId", "stub_test"));
    mockMvc
        .perform(post("/api/webhooks/stripe").contentType(MediaType.APPLICATION_JSON).content(envelope))
        .andExpect(status().isOk());
  }
}
