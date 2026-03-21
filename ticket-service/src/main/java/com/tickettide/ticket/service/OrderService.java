package com.tickettide.ticket.service;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.web.ApiException;
import com.tickettide.common.web.PageResponse;
import com.tickettide.ticket.api.OrderDtos.CreateOrderRequest;
import com.tickettide.ticket.api.OrderDtos.InternalOrderView;
import com.tickettide.ticket.api.OrderDtos.OrderResponse;
import com.tickettide.ticket.client.EventClient;
import com.tickettide.ticket.client.EventClient.TicketTypeDetails;
import com.tickettide.ticket.client.NotificationClient;
import com.tickettide.ticket.client.NotificationClient.NotificationRequest;
import com.tickettide.ticket.domain.Order;
import com.tickettide.ticket.domain.OrderRepository;
import com.tickettide.ticket.domain.OrderStatus;
import com.tickettide.ticket.domain.Ticket;
import com.tickettide.ticket.domain.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final TicketRepository tickets;
    private final EventClient eventClient;
    private final NotificationClient notifications;
    private final TicketCodeGenerator codes;
    private final Duration holdDuration;
    private final int maxQuantity;

    public OrderService(OrderRepository orders, TicketRepository tickets, EventClient eventClient,
                        NotificationClient notifications, TicketCodeGenerator codes,
                        @Value("${tickettide.orders.hold-duration:PT10M}") Duration holdDuration,
                        @Value("${tickettide.orders.max-quantity:10}") int maxQuantity) {
        this.orders = orders;
        this.tickets = tickets;
        this.eventClient = eventClient;
        this.notifications = notifications;
        this.codes = codes;
        this.holdDuration = holdDuration;
        this.maxQuantity = maxQuantity;
    }

    /**
     * Reserves seats in event-service first, then records the order. If persisting
     * fails, the reservation is compensated so seats are not leaked.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest req, AuthenticatedUser user) {
        if (req.quantity() > maxQuantity) {
            throw ApiException.badRequest("You can buy at most " + maxQuantity + " tickets per order");
        }
        TicketTypeDetails details = eventClient.reserve(req.ticketTypeId(), req.quantity());
        try {
            Order order = orders.saveAndFlush(new Order(user.id(), user.email(), details.eventId(),
                    details.eventTitle(), details.ticketTypeId(), details.ticketTypeName(), req.quantity(),
                    details.priceCents(), details.currency(), holdDuration));
            log.info("Order {} created for user {} ({} x {})", order.getId(), user.id(), req.quantity(),
                    details.ticketTypeId());
            return OrderResponse.from(order, List.of());
        } catch (RuntimeException e) {
            eventClient.release(req.ticketTypeId(), req.quantity());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> mine(AuthenticatedUser user, Pageable page) {
        return PageResponse.of(orders.findByUserIdOrderByCreatedAtDesc(user.id(), page),
                o -> OrderResponse.from(o, tickets.findByOrderId(o.getId())));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id, AuthenticatedUser user) {
        Order order = loadOwned(id, user);
        return OrderResponse.from(order, tickets.findByOrderId(order.getId()));
    }

    @Transactional
    public OrderResponse cancel(UUID id, AuthenticatedUser user) {
        Order order = loadOwned(id, user);
        transition(order::cancel);
        eventClient.release(order.getTicketTypeId(), order.getQuantity());
        return OrderResponse.from(order, List.of());
    }

    // ---- internal operations driven by payment-service ----

    @Transactional(readOnly = true)
    public InternalOrderView internalView(UUID id) {
        return InternalOrderView.from(load(id));
    }

    /**
     * Idempotent: replaying the same payment returns the confirmed order. A payment
     * for an order whose hold already lapsed is rejected so payment-service refunds it.
     */
    @Transactional
    public InternalOrderView markPaid(UUID id, UUID paymentId) {
        Order order = load(id);
        if (order.getStatus() == OrderStatus.CONFIRMED && paymentId.equals(order.getPaymentId())) {
            return InternalOrderView.from(order);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("Order is " + order.getStatus() + " and can no longer be paid");
        }
        transition(() -> order.confirm(paymentId));

        List<Ticket> issued = new ArrayList<>(order.getQuantity());
        for (int i = 0; i < order.getQuantity(); i++) {
            issued.add(new Ticket(order, uniqueCode()));
        }
        tickets.saveAll(issued);

        String codesList = String.join(", ", issued.stream().map(Ticket::getCode).toList());
        AfterCommit.run(() -> notifications.send(new NotificationRequest(order.getUserId(), order.getUserEmail(),
                "ORDER_CONFIRMED",
                "Your tickets for " + order.getEventTitle(),
                "Payment of %s received. %d x %s. Ticket codes: %s".formatted(
                        money(order.getTotalCents(), order.getCurrency()), order.getQuantity(),
                        order.getTicketTypeName(), codesList))));
        log.info("Order {} confirmed with payment {}", order.getId(), paymentId);
        return InternalOrderView.from(order);
    }

    @Transactional
    public InternalOrderView markRefunded(UUID id, UUID paymentId) {
        Order order = load(id);
        if (order.getStatus() == OrderStatus.REFUNDED) {
            return InternalOrderView.from(order);
        }
        if (!paymentId.equals(order.getPaymentId())) {
            throw ApiException.conflict("Payment does not belong to this order");
        }
        transition(order::refund);
        tickets.findByOrderId(order.getId()).forEach(Ticket::voidTicket);
        eventClient.release(order.getTicketTypeId(), order.getQuantity());

        AfterCommit.run(() -> notifications.send(new NotificationRequest(order.getUserId(), order.getUserEmail(),
                "ORDER_REFUNDED",
                "Refund issued for " + order.getEventTitle(),
                "We refunded %s. Your %d ticket(s) have been voided.".formatted(
                        money(order.getTotalCents(), order.getCurrency()), order.getQuantity()))));
        log.info("Order {} refunded", order.getId());
        return InternalOrderView.from(order);
    }

    /** Called by the expiry sweeper, one order per transaction. */
    @Transactional
    public boolean expire(UUID id) {
        Order order = load(id);
        if (!order.isHoldExpired(java.time.Instant.now())) {
            return false;
        }
        order.expire();
        eventClient.release(order.getTicketTypeId(), order.getQuantity());
        AfterCommit.run(() -> notifications.send(new NotificationRequest(order.getUserId(), order.getUserEmail(),
                "ORDER_EXPIRED",
                "Your reservation for " + order.getEventTitle() + " expired",
                "We released your %d ticket(s) because payment was not completed in time."
                        .formatted(order.getQuantity()))));
        return true;
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = codes.next();
            if (!tickets.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique ticket code");
    }

    private Order loadOwned(UUID id, AuthenticatedUser user) {
        Order order = load(id);
        if (!user.isAdmin() && !order.getUserId().equals(user.id())) {
            // Don't reveal that someone else's order exists.
            throw ApiException.notFound("Order");
        }
        return order;
    }

    private Order load(UUID id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
    }

    private static void transition(Runnable change) {
        try {
            change.run();
        } catch (IllegalStateException e) {
            throw ApiException.conflict(e.getMessage());
        }
    }

    static String money(long cents, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setCurrency(Currency.getInstance(currency));
        return format.format(cents / 100.0);
    }
}
