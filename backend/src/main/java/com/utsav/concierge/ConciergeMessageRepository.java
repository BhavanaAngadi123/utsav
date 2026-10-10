package com.utsav.concierge;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConciergeMessageRepository extends JpaRepository<ConciergeMessage, UUID> {
  List<ConciergeMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
