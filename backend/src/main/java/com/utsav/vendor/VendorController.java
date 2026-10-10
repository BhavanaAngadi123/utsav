package com.utsav.vendor;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Vendor profiles: browse, search, nearby, collaborators. */
@RestController
@RequestMapping("/api/vendors")
@Tag(name = "Vendors")
public class VendorController {

  private final VendorService vendorService;
  private final JwtService jwtService;

  public VendorController(VendorService vendorService, JwtService jwtService) {
    this.vendorService = vendorService;
    this.jwtService = jwtService;
  }

  @GetMapping
  @Operation(summary = "Search vendors (category, city, max price, verified only)")
  public Page<VendorDto.View> search(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(defaultValue = "false") boolean verifiedOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return vendorService
        .search(category, city, maxPrice, verifiedOnly, PageRequest.of(page, size, Sort.by("ratingAvg").descending()))
        .map(VendorDto.View::of);
  }

  @GetMapping("/nearby")
  @Operation(summary = "Vendors near a lat/lng, closest first")
  public List<VendorDto.View> nearby(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "20") int limit) {
    return vendorService.nearby(lat, lng, category, limit).stream().map(VendorDto.View::of).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Vendor profile by id")
  public VendorDto.View get(@PathVariable UUID id) {
    return VendorDto.View.of(vendorService.get(id));
  }

  @GetMapping("/{id}/collaborators")
  @Operation(summary = "Vendor-to-vendor collaborator recommendations")
  public List<VendorDto.View> collaborators(
      @PathVariable UUID id, @RequestParam(defaultValue = "5") int limit) {
    return vendorService.collaborators(id, limit).stream().map(VendorDto.View::of).toList();
  }

  @PostMapping
  @PreAuthorize("hasRole('VENDOR')")
  @Operation(summary = "Create my vendor profile (VENDOR only)")
  public ResponseEntity<VendorDto.View> create(
      @RequestHeader("Authorization") String auth, @Valid @RequestBody VendorDto.Upsert req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    Vendor created = vendorService.createProfile(claims.userId(), VendorDto.toEntity(req));
    return ResponseEntity.status(HttpStatus.CREATED).body(VendorDto.View.of(created));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('VENDOR')")
  @Operation(summary = "Update my vendor profile (VENDOR only, own profile)")
  public VendorDto.View update(
      @RequestHeader("Authorization") String auth,
      @PathVariable UUID id,
      @Valid @RequestBody VendorDto.Upsert req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return VendorDto.View.of(vendorService.updateProfile(claims.userId(), id, req));
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('VENDOR')")
  @Operation(summary = "My vendor profiles (VENDOR only)")
  public List<VendorDto.View> myProfiles(@RequestHeader("Authorization") String auth) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return vendorService.myProfiles(claims.userId()).stream().map(VendorDto.View::of).toList();
  }
}
