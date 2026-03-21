package com.tickettide.ticket;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.security.Role;
import com.tickettide.common.web.ApiException;
import com.tickettide.ticket.api.OrderDtos.CreateOrderRequest;
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
import com.tickettide.ticket.domain.TicketStatus;
import com.tickettide.ticket.service.OrderService;
import com.tickettide.ticket.service.TicketCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final EventClient eventClient = mock(EventClient.class);
    private final NotificationClient notifications = mock(NotificationClient.class);
    private final AuthenticatedUser buyer = new AuthenticatedUser(UUID.randomUUID(), "buyer@x.io", Role.ATTENDEE);
    private final UUID ticketTypeId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, tickets, eventClient, notifications, new TicketCodeGenerator(),
                Duration.ofMinutes(10), 10);
        when(orders.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventClient.reserve(ticketTypeId, 2)).thenReturn(new TicketTypeDetails(ticketTypeId, "GA", 2500, "USD",
                eventId, "Jazz Night", UUID.randomUUID(), "PUBLISHED", Instant.now().plusSeconds(86400), true));
    }

    private Order pendingOrder() {
        return new Order(buyer.id(), buyer.email(), eventId, "Jazz Night", ticketTypeId, "GA", 2, 2500, "USD",
                Duration.ofMinutes(10));
    }

    @Test
    void createSnapshotsPriceAndHoldsSeats() {
        OrderResponse res = service.create(new CreateOrderRequest(ticketTypeId, 2), buyer);

        assertThat(res.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(res.totalCents()).isEqualTo(5000);
        assertThat(res.expiresAt()).isAfter(Instant.now().plus(Duration.ofMinutes(9)));
        verify(eventClient).reserve(ticketTypeId, 2);
    }

    @Test
    void createRejectsOversizedOrdersWithoutTouchingInventory() {
        assertThatThrownBy(() -> service.create(new CreateOrderRequest(ticketTypeId, 11), buyer))
                .isInstanceOf(ApiException.class);
        verify(eventClient, never()).reserve(any(), any(Integer.class));
    }

    @Test
    void createReleasesSeatsWhenPersistFails() {
        when(orders.saveAndFlush(any(Order.class))).thenThrow(new DataIntegrityViolationException("boom"));

        assertThatThrownBy(() -> service.create(new CreateOrderRequest(ticketTypeId, 2), buyer))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(eventClient).release(ticketTypeId, 2);
    }

    @Test
    void markPaidIssuesOneTicketPerSeatAndNotifies() {
        Order order = pendingOrder();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        UUID paymentId = UUID.randomUUID();

        service.markPaid(order.getId(), paymentId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ticket>> issued = ArgumentCaptor.forClass(List.class);
        verify(tickets).saveAll(issued.capture());
        assertThat(issued.getValue()).hasSize(2)
                .allSatisfy(t -> assertThat(t.getCode()).hasSize(12))
                .extracting(Ticket::getCode).doesNotHaveDuplicates();

        ArgumentCaptor<NotificationRequest> note = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notifications).send(note.capture());
        assertThat(note.getValue().type()).isEqualTo("ORDER_CONFIRMED");
        assertThat(note.getValue().body()).contains("$50.00");
    }

    @Test
    void markPaidIsIdempotentForSamePayment() {
        Order order = pendingOrder();
        UUID paymentId = UUID.randomUUID();
        order.confirm(paymentId);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        service.markPaid(order.getId(), paymentId);

        verify(tickets, never()).saveAll(anyList());
    }

    @Test
    void markPaidRejectsExpiredOrder() {
        Order order = pendingOrder();
        order.expire();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markPaid(order.getId(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void refundVoidsTicketsAndReleasesSeats() {
        Order order = pendingOrder();
        UUID paymentId = UUID.randomUUID();
        order.confirm(paymentId);
        Ticket ticket = new Ticket(order, "ABCDEFGHJKMN");
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(tickets.findByOrderId(order.getId())).thenReturn(List.of(ticket));

        service.markRefunded(order.getId(), paymentId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VOID);
        verify(eventClient).release(ticketTypeId, 2);
    }

    @Test
    void cancelByAnotherUserLooksLikeNotFound() {
        Order order = pendingOrder();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        AuthenticatedUser stranger = new AuthenticatedUser(UUID.randomUUID(), "s@x.io", Role.ATTENDEE);

        assertThatThrownBy(() -> service.cancel(order.getId(), stranger))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).status()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(eventClient, never()).release(any(), any(Integer.class));
    }

    @Test
    void expireOnlyActsOnLapsedHolds() {
        Order fresh = pendingOrder();
        when(orders.findById(fresh.getId())).thenReturn(Optional.of(fresh));
        assertThat(service.expire(fresh.getId())).isFalse();

        Order lapsed = new Order(buyer.id(), buyer.email(), eventId, "Jazz Night", ticketTypeId, "GA", 1, 2500,
                "USD", Duration.ZERO);
        when(orders.findById(lapsed.getId())).thenReturn(Optional.of(lapsed));
        assertThat(service.expire(lapsed.getId())).isTrue();
        assertThat(lapsed.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(eventClient).release(ticketTypeId, 1);
    }
}
