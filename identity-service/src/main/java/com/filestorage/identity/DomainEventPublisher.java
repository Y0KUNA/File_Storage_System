package com.filestorage.identity;

import com.filestorage.common.EventEnvelope;

interface DomainEventPublisher {
    void publish(EventEnvelope event);
}

