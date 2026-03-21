package com.tickettide.ticket.service;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.web.ApiException;
import com.tickettide.ticket.api.OrderDtos.TicketResponse;
import com.tickettide.ticket.client.EventClient;
import com.tickettide.ticket.domain.Ticket;
import com.tickettide.ticket.domain.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final EventClient eventClient;

    public TicketService(TicketRepository tickets, EventClient eventClient) {
        this.tickets = tickets;
        this.eventClient = eventClient;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> wallet(AuthenticatedUser user) {
        return tickets.findByUserIdOrderByIssuedAtDesc(user.id()).stream().map(TicketResponse::from).toList();
    }

    /** Door scanning: only the event's organizer (or an admin) may check a ticket in. */
    @Transactional
    public TicketResponse checkIn(String rawCode, AuthenticatedUser staff) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Ticket ticket = tickets.findByCode(code).orElseThrow(() -> ApiException.notFound("Ticket"));
        if (!staff.isAdmin()) {
            var event = eventClient.event(ticket.getEventId());
            if (!event.organizerId().equals(staff.id())) {
                throw ApiException.forbidden("You do not manage this event");
            }
        }
        try {
            ticket.checkIn();
        } catch (IllegalStateException e) {
            throw ApiException.conflict(e.getMessage());
        }
        return TicketResponse.from(ticket);
    }
}
