package com.utsav.budget;

import com.utsav.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Budget Freeze: lock guest count + max budget. */
@RestController
@RequestMapping("/api/budget")
@Tag(name = "Budget")
public class BudgetController {

  private final BudgetService budgetService;
  private final JwtService jwtService;

  public BudgetController(BudgetService budgetService, JwtService jwtService) {
    this.budgetService = budgetService;
    this.jwtService = jwtService;
  }

  @PostMapping("/freeze")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Freeze budget: lock guest count + max budget (CUSTOMER only)")
  public ResponseEntity<FreezeView> freeze(
      @RequestHeader("Authorization") String auth, @RequestBody FreezeRequest req) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    BudgetFreeze freeze =
        budgetService.freeze(claims.userId(), req.guestCount(), req.maxBudget(), req.currency());
    return ResponseEntity.status(HttpStatus.CREATED).body(FreezeView.of(freeze));
  }

  @GetMapping("/freeze/active")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Active budget freeze (CUSTOMER only)")
  public ResponseEntity<FreezeView> active(@RequestHeader("Authorization") String auth) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return budgetService
        .activeFreeze(claims.userId())
        .map(f -> ResponseEntity.ok(FreezeView.of(f)))
        .orElse(ResponseEntity.noContent().build());
  }

  @PostMapping("/freeze/{id}/unfreeze")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Release a budget freeze (CUSTOMER only, own freeze)")
  public ResponseEntity<Void> unfreeze(
      @RequestHeader("Authorization") String auth, @PathVariable UUID id) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    budgetService.unfreeze(claims.userId(), id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/freeze/history")
  @PreAuthorize("hasRole('CUSTOMER')")
  @Operation(summary = "Budget freeze history (CUSTOMER only)")
  public List<FreezeView> history(@RequestHeader("Authorization") String auth) {
    var claims = jwtService.parseAccessToken(auth.substring(7));
    return budgetService.history(claims.userId()).stream().map(FreezeView::of).toList();
  }

  public record FreezeRequest(
      @Min(1) int guestCount, @NotNull BigDecimal maxBudget, String currency) {}

  public record FreezeView(
      String id, int guestCount, BigDecimal maxBudget, String currency, boolean active) {
    static FreezeView of(BudgetFreeze f) {
      return new FreezeView(
          f.getId().toString(), f.getGuestCount(), f.getMaxBudget(), f.getCurrency(), f.isActive());
    }
  }
}
