package com.utsav.budget;

import com.utsav.admin.AuditService;
import com.utsav.user.User;
import com.utsav.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Budget Freeze: lock guest count + max budget; discovery filters to fit. */
@Service
public class BudgetService {

  private final BudgetFreezeRepository freezes;
  private final UserRepository users;
  private final AuditService audit;

  public BudgetService(
      BudgetFreezeRepository freezes, UserRepository users, AuditService audit) {
    this.freezes = freezes;
    this.users = users;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public BudgetFreeze freeze(UUID customerId, int guestCount, BigDecimal maxBudget, String currency) {
    if (guestCount <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guest count must be positive");
    }
    if (maxBudget == null || maxBudget.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "max budget must be positive");
    }
    User customer =
        users
            .findById(customerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    // Deactivate previous freezes; only one active at a time.
    freezes.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
        .filter(BudgetFreeze::isActive)
        .forEach(f -> f.setActive(false));
    BudgetFreeze freeze = new BudgetFreeze(customer, guestCount, maxBudget, currency);
    freezes.save(freeze);
    audit.log(
        customer,
        "BUDGET_FREEZE",
        "BudgetFreeze",
        freeze.getId().toString(),
        guestCount + " guests / " + maxBudget + " " + freeze.getCurrency());
    return freeze;
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @Transactional
  public void unfreeze(UUID customerId, UUID freezeId) {
    BudgetFreeze freeze =
        freezes
            .findById(freezeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "freeze not found"));
    if (!freeze.getCustomer().getId().equals(customerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your budget freeze");
    }
    freeze.setActive(false);
    freezes.save(freeze);
  }

  public Optional<BudgetFreeze> activeFreeze(UUID customerId) {
    if (customerId == null) return Optional.empty();
    return freezes.findFirstByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId);
  }

  public Optional<BigDecimal> activeMaxBudget(UUID customerId) {
    return activeFreeze(customerId).map(BudgetFreeze::getMaxBudget);
  }

  public List<BudgetFreeze> history(UUID customerId) {
    return freezes.findByCustomerIdOrderByCreatedAtDesc(customerId);
  }
}
