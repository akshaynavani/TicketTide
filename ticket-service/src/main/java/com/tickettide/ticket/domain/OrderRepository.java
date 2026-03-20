package com.tickettide.ticket.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("select o.id from Order o where o.status = :status and o.expiresAt <= :now order by o.expiresAt")
    List<UUID> findIdsByStatusExpiredBefore(@Param("status") OrderStatus status, @Param("now") Instant now,
                                            Pageable limit);
}
