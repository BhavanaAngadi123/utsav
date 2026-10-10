package com.utsav;

import static org.assertj.core.api.Assertions.assertThat;

import com.utsav.concierge.VendorTools;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Concierge tools: search, nearby, recommend (pure-Java scoring). */
class ConciergeToolsTest extends IntegrationTestBase {

  @Autowired private VendorTools vendorTools;

  @Test
  void searchVendorsFindsTestVendor() {
    var results = vendorTools.searchVendors("decorators", "Boston", null);
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).businessName()).contains("Test Decor Co");
  }

  @Test
  void nearbyVendorsFindsTestVendor() {
    var results = vendorTools.nearbyVendors(42.3601, -71.0589, null);
    assertThat(results).isNotEmpty();
  }

  @Test
  void recommendVendorsScoresAndExplains() {
    var recs = vendorTools.recommendVendors("wedding", 100, new BigDecimal("5000.00"), "Boston");
    assertThat(recs).isNotEmpty();
    var top = recs.get(0);
    assertThat(top.score()).isGreaterThan(0);
    assertThat(top.reasons()).isNotBlank();
  }

  @Test
  void budgetFitIsRewarded() {
    // Vendor base price is $1800; budget of $5000 should fit.
    var recs = vendorTools.recommendVendors("wedding", 100, new BigDecimal("5000.00"), null);
    assertThat(recs).anyMatch(r -> r.reasons().contains("fits your budget"));
  }
}
