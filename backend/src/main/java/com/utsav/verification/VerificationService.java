package com.utsav.verification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ID verification workflow. Models 13 countries' ID types (status-only).
 * Real document validation is delegated to a verification provider (stubbed).
 */
@org.springframework.stereotype.Service
public class VerificationService {

  /** 13 supported countries → accepted government ID types (labels only). */
  public static final Map<String, List<String>> COUNTRY_ID_TYPES =
      Map.ofEntries(
          Map.entry("US", List.of("Driver's License", "State ID", "Passport")),
          Map.entry("IN", List.of("Aadhaar", "PAN Card", "Passport", "Voter ID")),
          Map.entry("GB", List.of("Driver's Licence", "Passport", "National ID")),
          Map.entry("CA", List.of("Driver's Licence", "Passport", "Provincial ID")),
          Map.entry("AU", List.of("Driver's Licence", "Passport", "Proof of Age Card")),
          Map.entry("DE", List.of("Personalausweis", "Passport", "Driver's Licence")),
          Map.entry("FR", List.of("Carte d'identité", "Passport", "Driver's Licence")),
          Map.entry("AE", List.of("Emirates ID", "Passport", "Driver's Licence")),
          Map.entry("SG", List.of("NRIC", "Passport", "Driver's Licence")),
          Map.entry("JP", List.of("My Number Card", "Driver's Licence", "Passport")),
          Map.entry("BR", List.of("RG", "CNH", "Passport")),
          Map.entry("ZA", List.of("Smart ID Card", "Passport", "Driver's Licence")),
          Map.entry("MX", List.of("INE", "Passport", "Driver's Licence")));

  private final IdVerificationRepository verifications;
  private final com.utsav.vendor.VendorRepository vendors;
  private final com.utsav.user.UserRepository users;
  private final com.utsav.admin.AuditService audit;

  public VerificationService(
      IdVerificationRepository verifications,
      com.utsav.vendor.VendorRepository vendors,
      com.utsav.user.UserRepository users,
      com.utsav.admin.AuditService audit) {
    this.verifications = verifications;
    this.vendors = vendors;
    this.users = users;
    this.audit = audit;
  }

  public Map<String, List<String>> supportedCountries() {
    return COUNTRY_ID_TYPES;
  }

  @org.springframework.transaction.annotation.Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('VENDOR')")
  public IdVerification submit(UUID vendorId, UUID ownerId, String countryCode, String idType) {
    var vendor =
        vendors
            .findByIdAndOwnerId(vendorId, ownerId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "vendor not found"));
    String cc = countryCode.toUpperCase();
    List<String> types = COUNTRY_ID_TYPES.get(cc);
    if (types == null) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "unsupported country: " + cc);
    }
    if (types.stream().noneMatch(t -> t.equalsIgnoreCase(idType))) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "unsupported ID type for " + cc + ": " + idType);
    }
    // One pending at a time per vendor.
    verifications
        .findFirstByVendorIdAndStatus(vendorId, IdVerification.Status.PENDING)
        .ifPresent(
            v -> {
              throw new org.springframework.web.server.ResponseStatusException(
                  org.springframework.http.HttpStatus.CONFLICT, "verification already pending");
            });
    IdVerification verification = new IdVerification(vendor, cc, idType);
    verifications.save(verification);
    vendor.setVerifiedStatus(com.utsav.vendor.Vendor.VerificationStatus.PENDING);
    vendors.save(vendor);
    audit.log(
        vendor.getOwner(),
        "VERIFICATION_SUBMITTED",
        "IdVerification",
        verification.getId().toString(),
        cc + "/" + idType);
    return verification;
  }

  @org.springframework.transaction.annotation.Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  public IdVerification review(
      UUID verificationId, UUID adminId, boolean approve, String note) {
    var verification =
        verifications
            .findById(verificationId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "verification not found"));
    var admin =
        users
            .findById(adminId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "admin not found"));
    verification.setStatus(
        approve ? IdVerification.Status.APPROVED : IdVerification.Status.REJECTED);
    verification.setReviewedBy(admin);
    verification.setReviewedAt(java.time.Instant.now());
    verification.setReviewNote(note);
    verifications.save(verification);
    var vendor = verification.getVendor();
    vendor.setVerifiedStatus(
        approve
            ? com.utsav.vendor.Vendor.VerificationStatus.VERIFIED
            : com.utsav.vendor.Vendor.VerificationStatus.REJECTED);
    vendors.save(vendor);
    audit.log(
        admin,
        approve ? "VERIFICATION_APPROVED" : "VERIFICATION_REJECTED",
        "IdVerification",
        verificationId.toString(),
        note);
    return verification;
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  public Page<IdVerification> reviewQueue(Pageable pageable) {
    return verifications.findByStatus(IdVerification.Status.PENDING, pageable);
  }

  public List<IdVerification> history(UUID vendorId) {
    return verifications.findByVendorId(vendorId);
  }
}
