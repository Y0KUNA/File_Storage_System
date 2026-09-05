package com.filestorage.notification;

import com.filestorage.common.Headers;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
class NotificationController {
    private final NotificationRepository notifications;

    NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    List<NotificationResponse> list(@RequestHeader(Headers.USER_ID) UUID userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(item -> new NotificationResponse(item.id, item.type, item.payloadJson, item.createdAt, item.readAt))
                .toList();
    }
}

