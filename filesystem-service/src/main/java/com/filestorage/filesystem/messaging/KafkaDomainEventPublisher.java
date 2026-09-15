package com.filestorage.filesystem.messaging;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {
    private final KafkaTemplate<String, EventEnvelope> kafka;
    private final String topic;

    public KafkaDomainEventPublisher(KafkaTemplate<String, EventEnvelope> kafka,
                              @Value("${app.kafka.events-topic:file-storage.events}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void publish(EventEnvelope event) {
        kafka.send(topic, event.aggregateId(), event);
    }
}
