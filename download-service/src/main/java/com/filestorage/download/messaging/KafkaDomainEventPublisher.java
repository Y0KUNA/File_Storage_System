package com.filestorage.download.messaging;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
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
