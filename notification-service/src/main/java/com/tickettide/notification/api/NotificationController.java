package com.tickettide.notification.api;

import com.tickettide.common.security.CurrentUser;
import com.tickettide.common.web.PageResponse;
import com.tickettide.notification.api.NotificationDtos.NotificationResponse;
import com.tickettide.notification.api.NotificationDtos.UnreadCount;
import com.tickettide.notification.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public PageResponse<NotificationResponse> inbox(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return notifications.inbox(CurrentUser.get(), unreadOnly,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
    }

    @GetMapping("/unread-count")
    public UnreadCount unread() {
        return notifications.unread(CurrentUser.get());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return notifications.markRead(id, CurrentUser.get());
    }

    @PostMapping("/read-all")
    public UnreadCount markAllRead() {
        return notifications.markAllRead(CurrentUser.get());
    }
}
