package com.tickettide.event.api;

import com.tickettide.common.security.CurrentUser;
import com.tickettide.common.web.PageResponse;
import com.tickettide.event.api.EventDtos.EventRequest;
import com.tickettide.event.api.EventDtos.EventResponse;
import com.tickettide.event.api.EventDtos.EventSummary;
import com.tickettide.event.api.EventDtos.SalesReport;
import com.tickettide.event.api.EventDtos.TicketTypeRequest;
import com.tickettide.event.api.EventDtos.TicketTypeResponse;
import com.tickettide.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final int MAX_PAGE_SIZE = 50;

    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    @GetMapping
    public PageResponse<EventSummary> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String city,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) Instant before,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("startsAt").ascending());
        return events.search(q, city, category, before, pageable);
    }

    @GetMapping("/mine")
    public PageResponse<EventSummary> mine(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("startsAt").descending());
        return events.mine(CurrentUser.get(), pageable);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable UUID id) {
        return events.get(id, CurrentUser.optional().orElse(null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return events.create(request, CurrentUser.get());
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return events.update(id, request, CurrentUser.get());
    }

    @PostMapping("/{id}/ticket-types")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketTypeResponse addTicketType(@PathVariable UUID id, @Valid @RequestBody TicketTypeRequest request) {
        return events.addTicketType(id, request, CurrentUser.get());
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable UUID id) {
        return events.publish(id, CurrentUser.get());
    }

    @PostMapping("/{id}/cancel")
    public EventResponse cancel(@PathVariable UUID id) {
        return events.cancel(id, CurrentUser.get());
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EventResponse uploadImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return events.uploadImage(id, file, CurrentUser.get());
    }

    @GetMapping("/{id}/sales")
    public SalesReport sales(@PathVariable UUID id) {
        return events.sales(id, CurrentUser.get());
    }
}
