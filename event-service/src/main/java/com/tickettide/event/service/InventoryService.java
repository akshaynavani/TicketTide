package com.tickettide.event.service;

import com.tickettide.common.web.ApiException;
import com.tickettide.event.api.EventDtos.EventOwnership;
import com.tickettide.event.api.EventDtos.TicketTypeDetails;
import com.tickettide.event.domain.EventRepository;
import com.tickettide.event.domain.TicketType;
import com.tickettide.event.domain.TicketTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Seat inventory operations invoked by ticket-service over /internal. */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final TicketTypeRepository ticketTypes;
    private final EventRepository events;

    public InventoryService(TicketTypeRepository ticketTypes, EventRepository events) {
        this.ticketTypes = ticketTypes;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public TicketTypeDetails details(UUID ticketTypeId) {
        return TicketTypeDetails.from(load(ticketTypeId));
    }

    @Transactional(readOnly = true)
    public EventOwnership ownership(UUID eventId) {
        return events.findById(eventId)
                .map(e -> new EventOwnership(e.getId(), e.getOrganizerId(), e.getTitle(), e.getStatus()))
                .orElseThrow(() -> ApiException.notFound("Event"));
    }

    @Transactional
    public TicketTypeDetails reserve(UUID ticketTypeId, int quantity) {
        TicketType type = load(ticketTypeId);
        if (!type.getEvent().isOnSale()) {
            throw ApiException.conflict("Tickets for this event are not on sale");
        }
        if (ticketTypes.reserve(ticketTypeId, quantity) == 0) {
            throw ApiException.conflict("Not enough tickets remaining");
        }
        log.info("Reserved {} x {}", quantity, ticketTypeId);
        return TicketTypeDetails.from(load(ticketTypeId));
    }

    @Transactional
    public void release(UUID ticketTypeId, int quantity) {
        load(ticketTypeId);
        if (ticketTypes.release(ticketTypeId, quantity) == 0) {
            // Should not happen unless a release is replayed; log loudly but don't fail the caller.
            log.error("Release of {} x {} would exceed capacity; ignoring", quantity, ticketTypeId);
            return;
        }
        log.info("Released {} x {}", quantity, ticketTypeId);
    }

    private TicketType load(UUID id) {
        return ticketTypes.findById(id).orElseThrow(() -> ApiException.notFound("Ticket type"));
    }
}
