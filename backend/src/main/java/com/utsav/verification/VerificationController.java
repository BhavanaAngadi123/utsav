package com.utsav.verification;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Government-ID verification (status-only; never stores ID numbers/images). */
@RestController
@RequestMapping("/api/verifications")
@Tag(name = "Verification")
public class VerificationController {

  private final VerificationService verificationService;
  private final JwtService jwtService;

  public VerificationController(VerificationService verificationService, JwtService jwtService) {
    this.verificationService = verificationService;
    this.jwtService = jwtService;
  }

  @GetMapping("/countries")
  @Operation(summary = "13 supported countries and their accepted ID types")
  public Map<String, List<String>> countries() {
    return verificationService.supportedCountries();
  }

  @PostMapping("/vendors/{vendorId}")
  @PreAuthorize("hasRole('VENDOR')")
  @Operation(summary = "Submit ID verification for my vendor profile")
  public ResponseEntity<VerificationView> submit(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID vendorId,
      @RequestBody SubmitRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    IdVerification v =
        verificationService.submit(vendorId, claims.userId(), req.countryCode(), req.idType());
    return ResponseEntity.ok(VerificationView.of(v));
  }

  @GetMapping("/vendors/{vendorId}")
  @Operation(summary = "Verification history for a vendor")
  public List<VerificationView> history(@PathVariable UUID vendorId) {
    return verificationService.history(vendorId).stream().map(VerificationView::of).toList();
  }

  @GetMapping("/queue")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Pending verification queue (ADMIN only)")
  public Page<VerificationView> queue(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return verificationService.reviewQueue(PageRequest.of(page, size)).map(VerificationView::of);
  }

  @PostMapping("/{id}/review")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Approve or reject a verification (ADMIN only)")
  public VerificationView review(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID id,
      @RequestBody ReviewRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return VerificationView.of(
        verificationService.review(id, claims.userId(), req.approve(), req.note()));
  }

  public record SubmitRequest(@NotBlank String countryCode, @NotBlank String idType) {}
  public record ReviewRequest(boolean approve, String note) {}

  public record VerificationView(
      String id, String vendorId, String countryCode, String idType, String status) {
    static VerificationView of(IdVerification v) {
      return new VerificationView(
          v.getId().toString(),
          v.getVendor().getId().toString(),
          v.getCountryCode(),
          v.getIdType(),
          v.getStatus().name());
    }
  }
}
