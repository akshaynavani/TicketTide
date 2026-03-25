package com.tickettide.notification.service;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.web.ApiException;
import com.tickettide.common.web.PageResponse;
import com.tickettide.notification.api.NotificationDtos.NotificationResponse;
import com.tickettide.notification.api.NotificationDtos.SendRequest;
import com.tickettide.notification.api.NotificationDtos.UnreadCount;
import com.tickettide.notification.domain.EmailStatus;
import com.tickettide.notification.domain.Notification;
import com.tickettide.notification.domain.NotificationRepository;
import com.tickettide.notification.email.EmailDispatcher.EmailRequested;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final ApplicationEventPublisher events;

    public NotificationService(NotificationRepository notifications, ApplicationEventPublisher events) {
        this.notifications = notifications;
        this.events = events;
    }

    @Transactional
    public NotificationResponse send(SendRequest req) {
        Notification n = notifications.save(
                new Notification(req.userId(), req.email(), req.type(), req.subject(), req.body()));
        if (n.getEmailStatus() == EmailStatus.PENDING) {
            events.publishEvent(new EmailRequested(n.getId()));
        }
        return NotificationResponse.from(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> inbox(AuthenticatedUser user, boolean unreadOnly, Pageable page) {
        Page<Notification> result = unreadOnly
                ? notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(user.id(), page)
                : notifications.findByUserIdOrderByCreatedAtDesc(user.id(), page);
        return PageResponse.of(result, NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCount unread(AuthenticatedUser user) {
        return new UnreadCount(notifications.countByUserIdAndReadAtIsNull(user.id()));
    }

    @Transactional
    public NotificationResponse markRead(UUID id, AuthenticatedUser user) {
        Notification n = notifications.findByIdAndUserId(id, user.id())
                .orElseThrow(() -> ApiException.notFound("Notification"));
        n.markRead();
        return NotificationResponse.from(n);
    }

    @Transactional
    public UnreadCount markAllRead(AuthenticatedUser user) {
        notifications.markAllRead(user.id(), Instant.now());
        return new UnreadCount(0);
    }
}
