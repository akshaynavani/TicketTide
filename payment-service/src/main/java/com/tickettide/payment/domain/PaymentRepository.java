package com.tickettide.payment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    boolean existsByOrderIdAndStatusIn(UUID orderId, Collection<PaymentStatus> statuses);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
