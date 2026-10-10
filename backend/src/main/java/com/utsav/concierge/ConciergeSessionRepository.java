package com.utsav.concierge;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConciergeSessionRepository extends JpaRepository<ConciergeSession, UUID> {}
