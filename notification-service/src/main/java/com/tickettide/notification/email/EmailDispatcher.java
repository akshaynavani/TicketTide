package com.tickettide.notification.email;

import com.tickettide.notification.domain.EmailStatus;
import com.tickettide.notification.domain.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Sends email off the request thread once the inbox row is committed, so a slow
 * mail provider never slows down order confirmation in ticket-service.
 */
@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    public record EmailRequested(UUID notificationId) {
    }

    private final NotificationRepository notifications;
    private final EmailSender sender;

    public EmailDispatcher(NotificationRepository notifications, EmailSender sender) {
        this.notifications = notifications;
        this.sender = sender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEmailRequested(EmailRequested event) {
        notifications.findById(event.notificationId()).ifPresent(n -> {
            try {
                sender.send(n.getEmail(), n.getSubject(), n.getBody());
                n.emailResult(EmailStatus.SENT);
            } catch (RuntimeException e) {
                log.warn("Email for notification {} failed: {}", n.getId(), e.getMessage());
                n.emailResult(EmailStatus.FAILED);
            }
        });
    }
}
