package com.filestorage.identity;

import com.filestorage.common.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingDomainEventPublisher implements DomainEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(EventEnvelope event) {
        log.info("domain_event type={} aggregate={} id={}", event.eventType(), event.aggregateType(), event.aggregateId());
    }
}

