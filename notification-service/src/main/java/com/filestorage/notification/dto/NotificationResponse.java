package com.filestorage.notification.dto;


import com.filestorage.notification.config.*;
import com.filestorage.notification.controller.*;
import com.filestorage.notification.domain.*;
import com.filestorage.notification.dto.*;
import com.filestorage.notification.messaging.*;
import com.filestorage.notification.repository.*;
import java.time.Instant;
import java.util.UUID;


public record NotificationResponse(UUID id, String type, String payloadJson, Instant createdAt, Instant readAt) {
}
