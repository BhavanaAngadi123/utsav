package com.utsav.budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetFreezeRepository extends JpaRepository<BudgetFreeze, UUID> {
  List<BudgetFreeze> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
  Optional<BudgetFreeze> findFirstByCustomerIdAndActiveTrueOrderByCreatedAtDesc(UUID customerId);
}
