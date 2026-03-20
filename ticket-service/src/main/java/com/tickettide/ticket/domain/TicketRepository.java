package com.tickettide.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByUserIdOrderByIssuedAtDesc(UUID userId);

    List<Ticket> findByOrderId(UUID orderId);

    Optional<Ticket> findByCode(String code);

    boolean existsByCode(String code);
}
