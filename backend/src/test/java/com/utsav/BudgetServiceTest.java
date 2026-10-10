package com.utsav;

import static org.assertj.core.api.Assertions.assertThat;

import com.utsav.budget.BudgetService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Budget Freeze: freeze, active, unfreeze, history. */
class BudgetServiceTest extends IntegrationTestBase {

  @Autowired private BudgetService budgetService;

  @Test
  void freezeCreatesActiveFreeze() {
    var freeze =
        runAs(
            customer,
            () -> budgetService.freeze(customer.getId(), 100, new BigDecimal("5000.00"), "USD"));
    assertThat(freeze.getGuestCount()).isEqualTo(100);
    assertThat(freeze.getMaxBudget()).isEqualByComparingTo("5000.00");
    assertThat(freeze.isActive()).isTrue();
  }

  @Test
  void onlyOneActiveFreezeAtATime() {
    runAs(
        customer,
        () -> {
          budgetService.freeze(customer.getId(), 50, new BigDecimal("2000.00"), "USD");
          budgetService.freeze(customer.getId(), 100, new BigDecimal("5000.00"), "USD");
          return null;
        });
    var active = budgetService.activeFreeze(customer.getId());
    assertThat(active).isPresent();
    assertThat(active.get().getGuestCount()).isEqualTo(100);
    assertThat(budgetService.history(customer.getId())).hasSize(2);
  }

  @Test
  void unfreezeDeactivates() {
    var freeze =
        runAs(
            customer,
            () -> budgetService.freeze(customer.getId(), 100, new BigDecimal("5000.00"), "USD"));
    runAs(
        customer,
        () -> {
          budgetService.unfreeze(customer.getId(), freeze.getId());
          return null;
        });
    assertThat(budgetService.activeFreeze(customer.getId())).isEmpty();
  }
}
