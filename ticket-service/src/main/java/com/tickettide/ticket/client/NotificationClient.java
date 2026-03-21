package com.tickettide.ticket.client;

import com.tickettide.common.web.InternalClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Notifications are best-effort: a failure here must never roll back an order.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    public record NotificationRequest(UUID userId, String email, String type, String subject, String body) {
    }

    private final RestClient client;

    public NotificationClient(RestClient.Builder builder,
                              @Value("${tickettide.services.notification-url}") String baseUrl,
                              @Value("${tickettide.internal.api-key}") String apiKey) {
        this.client = InternalClients.create(builder, baseUrl, apiKey);
    }

    public void send(NotificationRequest request) {
        try {
            client.post().uri("/internal/notifications").body(request).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Failed to send {} notification to user {}: {}", request.type(), request.userId(), e.getMessage());
        }
    }
}
