package com.filestorage.notification.controller;


import com.filestorage.notification.config.*;
import com.filestorage.notification.controller.*;
import com.filestorage.notification.domain.*;
import com.filestorage.notification.dto.*;
import com.filestorage.notification.messaging.*;
import com.filestorage.notification.repository.*;
import com.filestorage.common.Headers;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader(Headers.USER_ID) UUID userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(item -> new NotificationResponse(item.id, item.type, item.payloadJson, item.createdAt, item.readAt))
                .toList();
    }
}

