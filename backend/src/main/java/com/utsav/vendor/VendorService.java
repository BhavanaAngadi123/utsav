package com.utsav.vendor;

import com.utsav.admin.AuditService;
import com.utsav.budget.BudgetService;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Vendor profiles: create, update, search, nearby, collaborators. */
@Service
public class VendorService {

  private final VendorRepository vendors;
  private final UserRepository users;
  private final BudgetService budgetService;
  private final AuditService audit;

  public VendorService(
      VendorRepository vendors, UserRepository users, BudgetService budgetService, AuditService audit) {
    this.vendors = vendors;
    this.users = users;
    this.budgetService = budgetService;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('VENDOR')")
  @Transactional
  public Vendor createProfile(UUID ownerId, Vendor draft) {
    User owner =
        users
            .findById(ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    draft.setOwner(owner);
    Vendor saved = vendors.save(draft);
    audit.log(owner, "VENDOR_CREATE", "Vendor", saved.getId().toString(), draft.getBusinessName());
    return saved;
  }

  @PreAuthorize("hasRole('VENDOR')")
  @Transactional
  public Vendor updateProfile(UUID ownerId, UUID vendorId, VendorDto.Upsert upsert) {
    Vendor vendor =
        vendors
            .findByIdAndOwnerId(vendorId, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendor not found"));
    VendorDto.apply(upsert, vendor);
    audit.log(vendor.getOwner(), "VENDOR_UPDATE", "Vendor", vendorId.toString(), null);
    return vendors.save(vendor);
  }

  public Page<Vendor> search(
      String category, String city, BigDecimal maxPrice, boolean verifiedOnly, Pageable pageable) {
    return vendors.search(category, city, maxPrice, verifiedOnly, pageable);
  }

  /** Search with an optional customer Budget Freeze applied (filters to budget-fit). */
  public Page<Vendor> searchWithBudget(
      UUID customerId, String category, String city, boolean verifiedOnly, Pageable pageable) {
    BigDecimal maxPrice = budgetService.activeMaxBudget(customerId).orElse(null);
    return vendors.search(category, city, maxPrice, verifiedOnly, pageable);
  }

  public List<Vendor> nearby(double lat, double lng, String category, int limit) {
    return vendors.findNearby(lat, lng, category, Pageable.ofSize(limit));
  }

  public Vendor get(UUID id) {
    return vendors
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendor not found"));
  }

  public List<Vendor> myProfiles(UUID ownerId) {
    return vendors.findByOwnerId(ownerId);
  }

  /**
   * Vendor-to-vendor collaborator recommendations: vendors in complementary
   * categories, nearby, highest-rated first.
   */
  public List<Vendor> collaborators(UUID vendorId, int limit) {
    Vendor vendor = get(vendorId);
    String category = complementaryCategory(vendor.getCategorySlug());
    List<Vendor> candidates;
    if (vendor.getLatitude() != null && vendor.getLongitude() != null) {
      candidates =
          vendors.findNearby(
              vendor.getLatitude(), vendor.getLongitude(), category, Pageable.ofSize(limit * 3));
    } else {
      candidates =
          vendors
              .search(category, vendor.getCity(), null, false, Pageable.ofSize(limit * 3))
              .getContent();
    }
    return candidates.stream()
        .filter(v -> !v.getId().equals(vendorId))
        .sorted(
            (a, b) -> {
              int cmp = b.getRatingAvg().compareTo(a.getRatingAvg());
              return cmp != 0 ? cmp : Integer.compare(b.getReviewCount(), a.getReviewCount());
            })
        .limit(limit)
        .toList();
  }

  private String complementaryCategory(String category) {
    return switch (category) {
      case "photographers" -> "decorators";
      case "decorators" -> "photographers";
      case "caterers" -> "decorators";
      case "mehendi-artists" -> "makeup-artists";
      case "makeup-artists" -> "photographers";
      case "djs" -> "photographers";
      case "venues" -> "caterers";
      case "planners" -> "decorators";
      default -> "photographers";
    };
  }
}
