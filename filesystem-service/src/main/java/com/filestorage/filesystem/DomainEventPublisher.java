package com.filestorage.filesystem;

import com.filestorage.common.EventEnvelope;

interface DomainEventPublisher {
    void publish(EventEnvelope event);
}
