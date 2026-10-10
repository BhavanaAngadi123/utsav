package com.utsav.config;

import com.utsav.common.Role;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorDto;
import com.utsav.vendor.VendorRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Demo data seeder (demo profile only). Creates sample vendors for the UI.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

  private final UserRepository users;
  private final VendorRepository vendors;
  private final PasswordEncoder encoder;

  public DemoDataSeeder(
      UserRepository users, VendorRepository vendors, PasswordEncoder encoder) {
    this.users = users;
    this.vendors = vendors;
    this.encoder = encoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (vendors.count() > 0) {
      return; // already seeded
    }
    List<SeedVendor> seeds =
        List.of(
            new SeedVendor("Boston Mehendi Art", "mehendi@t.local", "mehendi-artists", "Boston", "US", "USD", "450.00", 42.3601, -71.0589, 4.9, 32),
            new SeedVendor("Cambridge Captures", "photo@t.local", "photographers", "Cambridge", "US", "USD", "1800.00", 42.3736, -71.1097, 4.8, 41),
            new SeedVendor("Sangeet Nights DJ", "dj@t.local", "djs", "Boston", "US", "USD", "900.00", 42.3601, -71.0589, 4.7, 18),
            new SeedVendor("Royal Decor Boston", "decor@t.local", "decorators", "Boston", "US", "USD", "2500.00", 42.3555, -71.0605, 4.9, 27),
            new SeedVendor("Spice Route Catering", "catering@t.local", "caterers", "Waltham", "US", "USD", "3200.00", 42.3765, -71.2356, 4.6, 15));

    for (SeedVendor s : seeds) {
      User owner = new User(s.email(), encoder.encode("Vendor!23456"), s.business(), Role.VENDOR);
      users.save(owner);
      Vendor v =
          VendorDto.toEntity(
              new VendorDto.Upsert(
                  s.business(),
                  s.category(),
                  "Demo vendor profile for " + s.business() + ".",
                  s.city(),
                  s.country(),
                  s.currency(),
                  new BigDecimal(s.price()),
                  "event",
                  s.lat(),
                  s.lng(),
                  null,
                  true));
      v.setOwner(owner);
      v.setRatingAvg(BigDecimal.valueOf(s.rating()));
      v.setReviewCount(s.reviews());
      v.setVerifiedStatus(Vendor.VerificationStatus.VERIFIED);
      vendors.save(v);
    }
  }

  private record SeedVendor(
      String business, String email, String category, String city, String country,
      String currency, String price, double lat, double lng, double rating, int reviews) {}
}
