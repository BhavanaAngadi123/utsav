package com.utsav.verification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdVerificationRepository extends JpaRepository<IdVerification, UUID> {
  List<IdVerification> findByVendorId(UUID vendorId);
  Optional<IdVerification> findFirstByVendorIdAndStatus(
      UUID vendorId, IdVerification.Status status);
  Page<IdVerification> findByStatus(IdVerification.Status status, Pageable pageable);
}
