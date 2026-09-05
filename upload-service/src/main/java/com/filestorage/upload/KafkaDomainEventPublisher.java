package com.filestorage.upload;

import com.filestorage.common.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaDomainEventPublisher implements DomainEventPublisher {
    private final KafkaTemplate<String, EventEnvelope> kafka;
    private final String topic;

    KafkaDomainEventPublisher(KafkaTemplate<String, EventEnvelope> kafka,
                              @Value("${app.kafka.events-topic:file-storage.events}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void publish(EventEnvelope event) {
        kafka.send(topic, event.aggregateId(), event);
    }
}
