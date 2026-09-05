package com.filestorage.notification;

import java.time.Instant;
import java.util.UUID;

record NotificationResponse(UUID id, String type, String payloadJson, Instant createdAt, Instant readAt) {
}

