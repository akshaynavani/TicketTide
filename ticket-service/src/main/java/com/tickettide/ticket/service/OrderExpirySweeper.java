package com.tickettide.ticket.service;

import com.tickettide.ticket.domain.OrderRepository;
import com.tickettide.ticket.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Releases seats held by unpaid orders. Safe to run on several instances: each
 * order is versioned, so if two sweepers (or a late payment) race, only one wins.
 */
@Component
public class OrderExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirySweeper.class);
    private static final int BATCH = 100;

    private final OrderRepository orders;
    private final OrderService orderService;

    public OrderExpirySweeper(OrderRepository orders, OrderService orderService) {
        this.orders = orders;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${tickettide.orders.expiry-sweep-interval-ms:30000}")
    public void sweep() {
        List<UUID> ids = orders.findIdsByStatusExpiredBefore(OrderStatus.PENDING_PAYMENT, Instant.now(),
                PageRequest.ofSize(BATCH));
        int expired = 0;
        for (UUID id : ids) {
            try {
                if (orderService.expire(id)) {
                    expired++;
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("Order {} changed concurrently; skipping", id);
            } catch (RuntimeException e) {
                log.warn("Failed to expire order {}: {}", id, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} unpaid order(s)", expired);
        }
    }
}
