package com.utsav.team;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTeamRepository extends JpaRepository<EventTeam, UUID> {
  List<EventTeam> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
