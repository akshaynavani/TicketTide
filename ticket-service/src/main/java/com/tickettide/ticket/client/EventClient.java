package com.tickettide.ticket.client;

import com.tickettide.common.web.InternalClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class EventClient {

    public record TicketTypeDetails(UUID ticketTypeId, String ticketTypeName, long priceCents, String currency,
                                    UUID eventId, String eventTitle, UUID organizerId, String eventStatus,
                                    Instant startsAt, boolean onSale) {
    }

    public record EventOwnership(UUID eventId, UUID organizerId, String title, String status) {
    }

    private final RestClient client;

    public EventClient(RestClient.Builder builder,
                       @Value("${tickettide.services.event-url}") String baseUrl,
                       @Value("${tickettide.internal.api-key}") String apiKey) {
        this.client = InternalClients.create(builder, baseUrl, apiKey);
    }

    /** Throws RestClientResponseException (409) when seats are unavailable. */
    public TicketTypeDetails reserve(UUID ticketTypeId, int quantity) {
        return client.post()
                .uri("/internal/ticket-types/{id}/reserve", ticketTypeId)
                .body(Map.of("quantity", quantity))
                .retrieve()
                .body(TicketTypeDetails.class);
    }

    public void release(UUID ticketTypeId, int quantity) {
        client.post()
                .uri("/internal/ticket-types/{id}/release", ticketTypeId)
                .body(Map.of("quantity", quantity))
                .retrieve()
                .toBodilessEntity();
    }

    public EventOwnership event(UUID eventId) {
        return client.get()
                .uri("/internal/events/{id}", eventId)
                .retrieve()
                .body(EventOwnership.class);
    }
}
