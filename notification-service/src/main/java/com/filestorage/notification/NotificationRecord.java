package com.filestorage.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class NotificationRecord {
    @Id
    UUID id;
    UUID userId;
    String type;
    String payloadJson;
    Instant createdAt;
    Instant readAt;

    protected NotificationRecord() {
    }

    NotificationRecord(UUID id, UUID userId, String type, String payloadJson) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
    }
}

