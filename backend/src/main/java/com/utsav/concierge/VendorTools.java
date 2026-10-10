package com.utsav.concierge;

import com.utsav.vendor.Vendor;
import com.utsav.vendor.VendorDto;
import com.utsav.vendor.VendorService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Agentic tools for the AI concierge: the LLM calls these Java functions
 * to get real vendor data, then curates the answer.
 */
@Component
public class VendorTools {

  private final VendorService vendorService;

  public VendorTools(VendorService vendorService) {
    this.vendorService = vendorService;
  }

  @Tool(description = "Search vendors by event category (e.g. photographers, decorators, caterers, mehendi-artists), city, and optional max price. Returns matching vendors with ratings.")
  public List<VendorDto.View> searchVendors(
      @ToolParam(description = "Event category slug, e.g. 'photographers'") String category,
      @ToolParam(description = "City name, e.g. 'Boston'") String city,
      @ToolParam(description = "Maximum price the customer can pay (optional)") BigDecimal maxPrice) {
    return vendorService
        .search(
            category,
            city,
            maxPrice,
            false,
            PageRequest.of(0, 10, Sort.by("ratingAvg").descending()))
        .map(VendorDto.View::of)
        .getContent();
  }

  @Tool(description = "Find vendors near a latitude/longitude, closest first. Use for 'near me' requests.")
  public List<VendorDto.View> nearbyVendors(
      @ToolParam(description = "Latitude, e.g. 42.36") double latitude,
      @ToolParam(description = "Longitude, e.g. -71.06") double longitude,
      @ToolParam(description = "Event category slug (optional)") String category) {
    return vendorService.nearby(latitude, longitude, category, 10).stream()
        .map(VendorDto.View::of)
        .toList();
  }

  @Tool(description = "Get full details for a specific vendor by id, including rating and price.")
  public VendorDto.View vendorDetails(
      @ToolParam(description = "Vendor id from a previous search") String vendorId) {
    Vendor vendor = vendorService.get(UUID.fromString(vendorId));
    return VendorDto.View.of(vendor);
  }

  @Tool(description = "Score and rank vendors for an event: combines rating, distance, budget fit, and event-type match into a 0-100 score with reasons like 'highest rated near you' or 'fits your budget'.")
  public List<ScoredVendor> recommendVendors(
      @ToolParam(description = "Event type, e.g. 'mehendi', 'wedding', 'sangeet'") String eventType,
      @ToolParam(description = "Number of guests") Integer guests,
      @ToolParam(description = "Maximum budget (optional)") BigDecimal maxBudget,
      @ToolParam(description = "City name (optional)") String city) {
    String category = categoryForEvent(eventType);
    List<Vendor> candidates =
        vendorService
            .search(
                category, city, maxBudget, false, PageRequest.of(0, 30, Sort.by("ratingAvg").descending()))
            .getContent();
    return candidates.stream()
        .map(v -> score(v, maxBudget))
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .limit(5)
        .toList();
  }

  private ScoredVendor score(Vendor v, BigDecimal maxBudget) {
    double score = 0;
    StringBuilder reasons = new StringBuilder();

    // Rating (0-40 points).
    double rating = v.getRatingAvg() != null ? v.getRatingAvg().doubleValue() : 0;
    score += (rating / 5.0) * 40;
    if (rating >= 4.5 && v.getReviewCount() >= 5) {
      reasons.append("highest rated near you; ");
    } else if (rating >= 4.0) {
      reasons.append("highly rated; ");
    }

    // Review count credibility (0-10).
    score += Math.min(v.getReviewCount(), 50) / 50.0 * 10;

    // Budget fit (0-30).
    if (maxBudget != null && v.getBasePrice() != null) {
      if (v.getBasePrice().compareTo(maxBudget) <= 0) {
        score += 30;
        reasons.append("fits your budget; ");
      } else {
        BigDecimal over = v.getBasePrice().subtract(maxBudget);
        if (over.compareTo(maxBudget.multiply(new BigDecimal("0.25"))) <= 0) {
          score += 15;
          reasons.append("slightly over budget but close; ");
        }
      }
    } else {
      score += 15;
    }

    // Verified (0-20).
    if (v.getVerifiedStatus() == Vendor.VerificationStatus.VERIFIED) {
      score += 20;
      reasons.append("ID-verified; ");
    }

    return new ScoredVendor(VendorDto.View.of(v), Math.round(score * 10.0) / 10.0,
        reasons.toString().trim());
  }

  private String categoryForEvent(String eventType) {
    if (eventType == null) return null;
    return switch (eventType.toLowerCase()) {
      case "mehendi", "henna" -> "mehendi-artists";
      case "sangeet", "music", "dj" -> "djs";
      case "decor", "decoration" -> "decorators";
      case "food", "catering" -> "caterers";
      case "makeup", "bridal" -> "makeup-artists";
      // wedding/reception need many categories: don't filter, score all.
      default -> null;
    };
  }

  public record ScoredVendor(VendorDto.View vendor, double score, String reasons) {}
}
