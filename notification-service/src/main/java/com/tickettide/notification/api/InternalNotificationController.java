package com.tickettide.notification.api;

import com.tickettide.notification.api.NotificationDtos.NotificationResponse;
import com.tickettide.notification.api.NotificationDtos.SendRequest;
import com.tickettide.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notifications;

    public InternalNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse send(@Valid @RequestBody SendRequest request) {
        return notifications.send(request);
    }
}
