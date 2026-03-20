package com.tickettide.event.api;

import com.tickettide.event.api.EventDtos.EventOwnership;
import com.tickettide.event.api.EventDtos.InventoryRequest;
import com.tickettide.event.api.EventDtos.TicketTypeDetails;
import com.tickettide.event.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalInventoryController {

    private final InventoryService inventory;

    public InternalInventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/ticket-types/{id}")
    public TicketTypeDetails details(@PathVariable UUID id) {
        return inventory.details(id);
    }

    @PostMapping("/ticket-types/{id}/reserve")
    public TicketTypeDetails reserve(@PathVariable UUID id, @Valid @RequestBody InventoryRequest request) {
        return inventory.reserve(id, request.quantity());
    }

    @PostMapping("/ticket-types/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID id, @Valid @RequestBody InventoryRequest request) {
        inventory.release(id, request.quantity());
    }

    @GetMapping("/events/{id}")
    public EventOwnership event(@PathVariable UUID id) {
        return inventory.ownership(id);
    }
}
