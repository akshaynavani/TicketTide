package com.tickettide.payment.client;

import com.tickettide.common.web.InternalClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class TicketClient {

    public record OrderView(UUID id, UUID userId, String userEmail, String eventTitle, String status,
                            long totalCents, String currency, Instant expiresAt, UUID paymentId) {
        public boolean awaitingPayment() {
            return "PENDING_PAYMENT".equals(status) && Instant.now().isBefore(expiresAt);
        }
    }

    private final RestClient client;

    public TicketClient(RestClient.Builder builder,
                        @Value("${tickettide.services.ticket-url}") String baseUrl,
                        @Value("${tickettide.internal.api-key}") String apiKey) {
        this.client = InternalClients.create(builder, baseUrl, apiKey);
    }

    public OrderView order(UUID orderId) {
        return client.get().uri("/internal/orders/{id}", orderId).retrieve().body(OrderView.class);
    }

    public OrderView markPaid(UUID orderId, UUID paymentId) {
        return client.post().uri("/internal/orders/{id}/paid", orderId)
                .body(Map.of("paymentId", paymentId))
                .retrieve()
                .body(OrderView.class);
    }

    public OrderView markRefunded(UUID orderId, UUID paymentId) {
        return client.post().uri("/internal/orders/{id}/refunded", orderId)
                .body(Map.of("paymentId", paymentId))
                .retrieve()
                .body(OrderView.class);
    }
}
