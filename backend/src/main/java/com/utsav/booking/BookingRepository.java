package com.utsav.booking;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
  List<Booking> findByCustomerIdOrderByEventDateDesc(UUID customerId);
  List<Booking> findByVendorIdOrderByEventDateDesc(UUID vendorId);
}
