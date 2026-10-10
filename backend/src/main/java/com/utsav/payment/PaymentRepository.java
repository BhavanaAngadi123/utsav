package com.utsav.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);
  List<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
  List<Payment> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
  Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}
