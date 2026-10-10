package com.utsav;

import com.utsav.common.Role;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for integration tests: full app context, H2, real Flyway migrations.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  @Autowired protected UserRepository users;
  @Autowired protected VendorRepository vendors;
  @Autowired protected PasswordEncoder passwordEncoder;

  protected User customer;
  protected User vendorOwner;
  protected User admin;
  protected Vendor vendor;

  /** Run code with a SecurityContext carrying the user's role (for @PreAuthorize). */
  protected <T> T runAs(User user, java.util.function.Supplier<T> work) {
    var auth =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_" + user.getRole().name())));
    var ctx = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
    ctx.setAuthentication(auth);
    org.springframework.security.core.context.SecurityContextHolder.setContext(ctx);
    try {
      return work.get();
    } finally {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
  }

  @BeforeEach
  void setUpBase() {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    customer =
        users.save(
            new User(
                "cust-" + suffix + "@test.local",
                passwordEncoder.encode("Customer!123"),
                "Test Customer",
                Role.CUSTOMER));
    vendorOwner =
        users.save(
            new User(
                "vend-" + suffix + "@test.local",
                passwordEncoder.encode("Vendor!123"),
                "Test Vendor",
                Role.VENDOR));
    admin =
        users.save(
            new User(
                "admin-" + suffix + "@test.local",
                passwordEncoder.encode("Admin!123"),
                "Test Admin",
                Role.ADMIN));

    Vendor draft =
        com.utsav.vendor.VendorDto.toEntity(
            new com.utsav.vendor.VendorDto.Upsert(
                "Test Decor Co " + suffix,
                "decorators",
                "Test vendor",
                "Boston",
                "US",
                "USD",
                new BigDecimal("1800.00"),
                "event",
                42.3601,
                -71.0589,
                null,
                true));
    draft.setOwner(vendorOwner);
    draft.setRatingAvg(new BigDecimal("4.80"));
    draft.setReviewCount(10);
    vendor = vendors.save(draft);
  }
}
