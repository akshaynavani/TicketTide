package com.tickettide.event;

import com.tickettide.event.domain.Event;
import com.tickettide.event.domain.EventRepository;
import com.tickettide.event.domain.TicketType;
import com.tickettide.event.domain.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:events;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.flyway.enabled=true"
})
class TicketTypeRepositoryTest {

    @Autowired
    private EventRepository events;

    @Autowired
    private TicketTypeRepository ticketTypes;

    private UUID typeId;

    @BeforeEach
    void setUp() {
        Instant start = Instant.now().plus(Duration.ofDays(10));
        Event event = new Event(UUID.randomUUID(), "Conf", null, "Hall A", "Austin", "tech",
                start, start.plus(Duration.ofHours(8)));
        TicketType type = event.addTicketType("Early bird", 4900, "USD", 5);
        events.saveAndFlush(event);
        typeId = type.getId();
    }

    @Test
    void reserveDecrementsUntilSoldOut() {
        assertThat(ticketTypes.reserve(typeId, 3)).isEqualTo(1);
        assertThat(ticketTypes.reserve(typeId, 2)).isEqualTo(1);
        assertThat(ticketTypes.reserve(typeId, 1)).isZero();
        assertThat(ticketTypes.findById(typeId)).get().extracting(TicketType::getAvailable).isEqualTo(0);
    }

    @Test
    void reserveRefusesMoreThanAvailable() {
        assertThat(ticketTypes.reserve(typeId, 6)).isZero();
        assertThat(ticketTypes.findById(typeId)).get().extracting(TicketType::getAvailable).isEqualTo(5);
    }

    @Test
    void releaseNeverExceedsCapacity() {
        ticketTypes.reserve(typeId, 2);
        assertThat(ticketTypes.release(typeId, 2)).isEqualTo(1);
        assertThat(ticketTypes.release(typeId, 1)).isZero();
        assertThat(ticketTypes.findById(typeId)).get().extracting(TicketType::getAvailable).isEqualTo(5);
    }
}
