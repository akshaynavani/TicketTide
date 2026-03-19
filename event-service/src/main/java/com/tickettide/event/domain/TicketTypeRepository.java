package com.tickettide.event.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    /**
     * Atomically decrements inventory. The WHERE clause makes the check-and-set a
     * single row-level operation, so concurrent buyers can never oversell.
     *
     * @return 1 if seats were reserved, 0 if not enough remain
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TicketType t
               set t.available = t.available - :qty, t.version = t.version + 1
             where t.id = :id and t.available >= :qty
            """)
    int reserve(@Param("id") UUID id, @Param("qty") int quantity);

    /** Returns seats to the pool, never exceeding capacity. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TicketType t
               set t.available = t.available + :qty, t.version = t.version + 1
             where t.id = :id and t.available + :qty <= t.capacity
            """)
    int release(@Param("id") UUID id, @Param("qty") int quantity);
}
