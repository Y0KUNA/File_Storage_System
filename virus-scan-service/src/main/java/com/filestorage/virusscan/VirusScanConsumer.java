package com.filestorage.virusscan;

import com.filestorage.common.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
class VirusScanConsumer {
    private final VirusScanService service;
    private final StringRedisTemplate redis;
    private final Duration dedupeTtl;

    VirusScanConsumer(VirusScanService service,
                      StringRedisTemplate redis,
                      @Value("${app.kafka.dedupe-ttl-hours:72}") long dedupeTtlHours) {
        this.service = service;
        this.redis = redis;
        this.dedupeTtl = Duration.ofHours(dedupeTtlHours);
    }

    @KafkaListener(topics = "${app.kafka.events-topic:file-storage.events}", groupId = "${spring.kafka.consumer.group-id:virus-scan-service}")
    void onEvent(EventEnvelope event) {
        if (!"FILE_UPLOAD_STORED".equals(event.eventType())) {
            return;
        }
        Boolean firstSeen = redis.opsForValue().setIfAbsent("processed_event:" + event.eventId(), "1", dedupeTtl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            return;
        }
        UUID fileId = UUID.fromString(String.valueOf(event.payload().get("fileId")));
        String storageKey = String.valueOf(event.payload().get("storageKey"));
        service.scan(new ScanRequest(fileId, storageKey));
    }
}
