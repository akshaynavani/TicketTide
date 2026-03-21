package com.tickettide.ticket.api;

import com.tickettide.common.security.CurrentUser;
import com.tickettide.ticket.api.OrderDtos.TicketResponse;
import com.tickettide.ticket.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    public List<TicketResponse> wallet() {
        return tickets.wallet(CurrentUser.get());
    }

    @PostMapping("/{code}/check-in")
    public TicketResponse checkIn(@PathVariable String code) {
        return tickets.checkIn(code, CurrentUser.get());
    }
}
