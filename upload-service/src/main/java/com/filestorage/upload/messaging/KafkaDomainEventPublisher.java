package com.filestorage.upload.messaging;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
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
