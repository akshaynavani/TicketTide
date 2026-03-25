package com.tickettide.notification;

import com.tickettide.common.security.AuthenticatedUser;
import com.tickettide.common.security.Role;
import com.tickettide.common.web.ApiException;
import com.tickettide.notification.api.NotificationDtos.SendRequest;
import com.tickettide.notification.domain.EmailStatus;
import com.tickettide.notification.domain.Notification;
import com.tickettide.notification.domain.NotificationRepository;
import com.tickettide.notification.email.EmailDispatcher.EmailRequested;
import com.tickettide.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "u@x.io", Role.ATTENDEE);
    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repo, events);
        when(repo.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void sendStoresInboxEntryAndQueuesEmail() {
        var res = service.send(new SendRequest(user.id(), "u@x.io", "ORDER_CONFIRMED", "Tickets", "Body"));

        assertThat(res.read()).isFalse();
        verify(events).publishEvent(new EmailRequested(res.id()));
    }

    @Test
    void sendWithoutEmailSkipsEmailChannel() {
        service.send(new SendRequest(user.id(), null, "ORDER_EXPIRED", "Expired", "Body"));
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void markReadIsScopedToOwner() {
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndUserId(id, user.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(id, user)).isInstanceOf(ApiException.class);
    }

    @Test
    void markReadSetsTimestampOnce() {
        Notification n = new Notification(user.id(), "u@x.io", "T", "S", "B");
        when(repo.findByIdAndUserId(n.getId(), user.id())).thenReturn(Optional.of(n));

        service.markRead(n.getId(), user);
        var firstRead = n.getReadAt();
        service.markRead(n.getId(), user);

        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isEqualTo(firstRead);
        assertThat(n.getEmailStatus()).isEqualTo(EmailStatus.PENDING);
    }
}
