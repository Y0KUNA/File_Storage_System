package com.filestorage.identity.messaging;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import com.filestorage.common.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {
    private final KafkaTemplate<String, EventEnvelope> kafka;
    private final String topic;

    public LoggingDomainEventPublisher(KafkaTemplate<String, EventEnvelope> kafka,
                                @Value("${app.kafka.events-topic:file-storage.events}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void publish(EventEnvelope event) {
        kafka.send(topic, event.aggregateId(), event);
    }
}
