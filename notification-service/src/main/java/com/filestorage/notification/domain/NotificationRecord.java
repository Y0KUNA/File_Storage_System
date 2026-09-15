package com.filestorage.notification.domain;


import com.filestorage.notification.config.*;
import com.filestorage.notification.controller.*;
import com.filestorage.notification.domain.*;
import com.filestorage.notification.dto.*;
import com.filestorage.notification.messaging.*;
import com.filestorage.notification.repository.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class NotificationRecord {
    @Id
    public UUID id;
    public UUID userId;
    public String type;
    public String payloadJson;
    public Instant createdAt;
    public Instant readAt;

    protected NotificationRecord() {
    }

    public NotificationRecord(UUID id, UUID userId, String type, String payloadJson) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
    }
}

