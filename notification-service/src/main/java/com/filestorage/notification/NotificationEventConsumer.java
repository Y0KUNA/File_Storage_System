package com.filestorage.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filestorage.common.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
class NotificationEventConsumer {
    private final NotificationRepository notifications;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration dedupeTtl;

    NotificationEventConsumer(NotificationRepository notifications,
                              StringRedisTemplate redis,
                              ObjectMapper objectMapper,
                              @Value("${app.kafka.dedupe-ttl-hours:72}") long dedupeTtlHours) {
        this.notifications = notifications;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.dedupeTtl = Duration.ofHours(dedupeTtlHours);
    }

    @KafkaListener(topics = "${app.kafka.events-topic:file-storage.events}", groupId = "${spring.kafka.consumer.group-id:notification-service}")
    void onEvent(EventEnvelope event) throws Exception {
        UUID userId = ownerId(event.payload());
        if (userId == null) {
            return;
        }
        Boolean firstSeen = redis.opsForValue().setIfAbsent("notification:processed_event:" + event.eventId(), "1", dedupeTtl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            return;
        }
        notifications.save(new NotificationRecord(UUID.randomUUID(), userId, event.eventType(), objectMapper.writeValueAsString(event.payload())));
    }

    private UUID ownerId(Map<String, Object> payload) {
        Object ownerId = payload.get("ownerId");
        if (ownerId == null) {
            ownerId = payload.get("userId");
        }
        return ownerId == null ? null : UUID.fromString(String.valueOf(ownerId));
    }
}
