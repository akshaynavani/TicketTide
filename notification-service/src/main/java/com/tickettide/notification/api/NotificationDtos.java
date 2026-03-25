package com.tickettide.notification.api;

import com.tickettide.notification.domain.Notification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record SendRequest(@NotNull UUID userId,
                              @Email String email,
                              @NotBlank @Size(max = 50) String type,
                              @NotBlank @Size(max = 200) String subject,
                              @NotBlank @Size(max = 5000) String body) {
    }

    public record NotificationResponse(UUID id, String type, String subject, String body, boolean read,
                                       Instant createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getSubject(), n.getBody(), n.isRead(),
                    n.getCreatedAt());
        }
    }

    public record UnreadCount(long unread) {
    }
}
