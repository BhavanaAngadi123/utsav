package com.utsav.booking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorSlotRepository extends JpaRepository<VendorSlot, UUID> {
  List<VendorSlot> findByVendorIdAndSlotDate(UUID vendorId, LocalDate slotDate);
  Optional<VendorSlot> findByVendorIdAndSlotDateAndSlotLabel(
      UUID vendorId, LocalDate slotDate, String slotLabel);
}
