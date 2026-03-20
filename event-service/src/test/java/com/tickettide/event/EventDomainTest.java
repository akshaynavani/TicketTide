package com.tickettide.event;

import com.tickettide.event.domain.Event;
import com.tickettide.event.domain.EventStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventDomainTest {

    private static Event draft(Instant startsAt) {
        return new Event(UUID.randomUUID(), "Jazz Night", "desc", "Blue Note", "New York", "music",
                startsAt, startsAt.plus(Duration.ofHours(3)));
    }

    @Test
    void cannotPublishWithoutTicketTypes() {
        Event event = draft(Instant.now().plus(Duration.ofDays(7)));
        assertThatThrownBy(event::publish).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishMakesEventOnSale() {
        Event event = draft(Instant.now().plus(Duration.ofDays(7)));
        event.addTicketType("General", 2500, "usd", 100);
        event.publish();

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.isOnSale()).isTrue();
        assertThat(event.getTicketTypes().get(0).getCurrency()).isEqualTo("USD");
        assertThat(event.getTicketTypes().get(0).getAvailable()).isEqualTo(100);
    }

    @Test
    void cannotPublishPastEvent() {
        Event event = draft(Instant.now().minus(Duration.ofDays(1)));
        event.addTicketType("General", 2500, "USD", 100);
        assertThatThrownBy(event::publish).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsEndBeforeStart() {
        Instant start = Instant.now().plus(Duration.ofDays(2));
        assertThatThrownBy(() -> new Event(UUID.randomUUID(), "t", null, "v", "c", null, start, start.minusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelledEventIsNotOnSaleAndRejectsNewTicketTypes() {
        Event event = draft(Instant.now().plus(Duration.ofDays(7)));
        event.addTicketType("General", 2500, "USD", 100);
        event.publish();
        event.cancel();

        assertThat(event.isOnSale()).isFalse();
        assertThatThrownBy(() -> event.addTicketType("VIP", 9000, "USD", 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidTicketTypeValues() {
        Event event = draft(Instant.now().plus(Duration.ofDays(7)));
        assertThatThrownBy(() -> event.addTicketType("Free", -1, "USD", 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> event.addTicketType("Zero", 0, "USD", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
