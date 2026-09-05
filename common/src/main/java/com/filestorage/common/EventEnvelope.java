package com.filestorage.common;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String correlationId,
        int schemaVersion,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static EventEnvelope v1(String eventType, String aggregateType, String aggregateId,
                                   String correlationId, Map<String, Object> payload) {
        return new EventEnvelope(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                correlationId,
                1,
                Instant.now(),
                payload
        );
    }
}

