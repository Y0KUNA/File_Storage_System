package com.filestorage.upload;

import com.filestorage.common.EventEnvelope;

interface DomainEventPublisher {
    void publish(EventEnvelope event);
}
