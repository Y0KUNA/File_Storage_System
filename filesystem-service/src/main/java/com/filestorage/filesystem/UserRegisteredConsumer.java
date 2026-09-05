package com.filestorage.filesystem;

import com.filestorage.common.EventEnvelope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class UserRegisteredConsumer {
    private final FilesystemService service;

    UserRegisteredConsumer(FilesystemService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${app.kafka.events-topic:file-storage.events}", groupId = "${spring.kafka.consumer.group-id:filesystem-service}")
    void onEvent(EventEnvelope event) {
        if (!"USER_REGISTERED".equals(event.eventType())) {
            return;
        }
        UUID userId = UUID.fromString(String.valueOf(event.payload().get("userId")));
        service.provisionRoot(userId);
    }
}
