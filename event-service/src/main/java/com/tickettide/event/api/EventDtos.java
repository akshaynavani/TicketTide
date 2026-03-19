package com.tickettide.event.api;

import com.tickettide.event.domain.Event;
import com.tickettide.event.domain.EventStatus;
import com.tickettide.event.domain.TicketType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            @NotBlank @Size(max = 200) String venue,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 50) String category,
            @NotNull @Future Instant startsAt,
            @NotNull Instant endsAt) {
    }

    public record TicketTypeRequest(
            @NotBlank @Size(max = 100) String name,
            @Min(0) long priceCents,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @Min(1) @Max(100_000) int capacity) {
    }

    public record TicketTypeResponse(UUID id, String name, long priceCents, String currency,
                                     int capacity, int available) {
        public static TicketTypeResponse from(TicketType t) {
            return new TicketTypeResponse(t.getId(), t.getName(), t.getPriceCents(), t.getCurrency(),
                    t.getCapacity(), t.getAvailable());
        }
    }

    public record EventResponse(UUID id, UUID organizerId, String title, String description, String venue,
                                String city, String category, Instant startsAt, Instant endsAt,
                                EventStatus status, String imageUrl, List<TicketTypeResponse> ticketTypes) {
        public static EventResponse from(Event e, String imageUrl) {
            return new EventResponse(e.getId(), e.getOrganizerId(), e.getTitle(), e.getDescription(),
                    e.getVenue(), e.getCity(), e.getCategory(), e.getStartsAt(), e.getEndsAt(), e.getStatus(),
                    imageUrl, e.getTicketTypes().stream().map(TicketTypeResponse::from).toList());
        }
    }

    public record EventSummary(UUID id, String title, String venue, String city, String category,
                               Instant startsAt, EventStatus status, String imageUrl, Long fromPriceCents) {
        public static EventSummary from(Event e, String imageUrl) {
            Long from = e.getTicketTypes().stream().map(TicketType::getPriceCents).min(Long::compare).orElse(null);
            return new EventSummary(e.getId(), e.getTitle(), e.getVenue(), e.getCity(), e.getCategory(),
                    e.getStartsAt(), e.getStatus(), imageUrl, from);
        }
    }

    public record SalesReport(UUID eventId, String title, List<TicketTypeSales> ticketTypes,
                              int totalSold, long grossRevenueCents) {
    }

    public record TicketTypeSales(UUID id, String name, int capacity, int sold, long revenueCents) {
    }

    // ---- internal contracts used by ticket-service ----

    public record InventoryRequest(@Min(1) @Max(20) int quantity) {
    }

    public record TicketTypeDetails(UUID ticketTypeId, String ticketTypeName, long priceCents, String currency,
                                    UUID eventId, String eventTitle, UUID organizerId, EventStatus eventStatus,
                                    Instant startsAt, boolean onSale) {
        public static TicketTypeDetails from(TicketType t) {
            Event e = t.getEvent();
            return new TicketTypeDetails(t.getId(), t.getName(), t.getPriceCents(), t.getCurrency(),
                    e.getId(), e.getTitle(), e.getOrganizerId(), e.getStatus(), e.getStartsAt(), e.isOnSale());
        }
    }

    public record EventOwnership(UUID eventId, UUID organizerId, String title, EventStatus status) {
    }
}
