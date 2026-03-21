package com.tickettide.ticket;

import com.tickettide.ticket.domain.Order;
import com.tickettide.ticket.domain.Ticket;
import com.tickettide.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private final Order order = new Order(UUID.randomUUID(), "a@x.io", UUID.randomUUID(), "Show",
            UUID.randomUUID(), "GA", 1, 1000, "USD", Duration.ofMinutes(10));

    @Test
    void checkInIsSingleUse() {
        Ticket ticket = new Ticket(order, "ABCDEFGHJKMN");
        ticket.checkIn();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CHECKED_IN);
        assertThat(ticket.getCheckedInAt()).isNotNull();
        assertThatThrownBy(ticket::checkIn).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void voidTicketCannotBeCheckedIn() {
        Ticket ticket = new Ticket(order, "ABCDEFGHJKMN");
        ticket.voidTicket();
        assertThatThrownBy(ticket::checkIn).isInstanceOf(IllegalStateException.class);
    }
}
