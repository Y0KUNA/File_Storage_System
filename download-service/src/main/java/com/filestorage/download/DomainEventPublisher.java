package com.filestorage.download;

import com.filestorage.common.EventEnvelope;

interface DomainEventPublisher {
    void publish(EventEnvelope event);
}
