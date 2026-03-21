package com.tickettide.ticket.api;

import com.tickettide.ticket.domain.Order;
import com.tickettide.ticket.domain.OrderStatus;
import com.tickettide.ticket.domain.Ticket;
import com.tickettide.ticket.domain.TicketStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(@NotNull UUID ticketTypeId, @Min(1) int quantity) {
    }

    public record OrderResponse(UUID id, UUID eventId, String eventTitle, UUID ticketTypeId, String ticketTypeName,
                                int quantity, long unitPriceCents, long totalCents, String currency,
                                OrderStatus status, Instant expiresAt, Instant createdAt,
                                List<TicketResponse> tickets) {
        public static OrderResponse from(Order o, List<Ticket> tickets) {
            return new OrderResponse(o.getId(), o.getEventId(), o.getEventTitle(), o.getTicketTypeId(),
                    o.getTicketTypeName(), o.getQuantity(), o.getUnitPriceCents(), o.getTotalCents(),
                    o.getCurrency(), o.getStatus(), o.getExpiresAt(), o.getCreatedAt(),
                    tickets.stream().map(TicketResponse::from).toList());
        }
    }

    public record TicketResponse(UUID id, UUID orderId, UUID eventId, UUID ticketTypeId, String code,
                                 TicketStatus status, Instant issuedAt, Instant checkedInAt) {
        public static TicketResponse from(Ticket t) {
            return new TicketResponse(t.getId(), t.getOrderId(), t.getEventId(), t.getTicketTypeId(), t.getCode(),
                    t.getStatus(), t.getIssuedAt(), t.getCheckedInAt());
        }
    }

    // ---- internal contracts used by payment-service ----

    public record InternalOrderView(UUID id, UUID userId, String userEmail, String eventTitle, OrderStatus status,
                                    long totalCents, String currency, Instant expiresAt, UUID paymentId) {
        public static InternalOrderView from(Order o) {
            return new InternalOrderView(o.getId(), o.getUserId(), o.getUserEmail(), o.getEventTitle(),
                    o.getStatus(), o.getTotalCents(), o.getCurrency(), o.getExpiresAt(), o.getPaymentId());
        }
    }

    public record PaymentEvent(@NotNull UUID paymentId) {
    }
}
