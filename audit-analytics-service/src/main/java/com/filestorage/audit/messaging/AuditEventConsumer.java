package com.filestorage.audit.messaging;



import com.filestorage.audit.dto.*;
import com.filestorage.audit.config.*;
import com.filestorage.audit.controller.*;
import com.filestorage.audit.domain.*;
import com.filestorage.audit.messaging.*;
import com.filestorage.audit.repository.*;
import com.filestorage.common.EventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@Component
public class AuditEventConsumer {
    private final AnalyticsMetricRepository metrics;
    private final StringRedisTemplate redis;
    private final Duration dedupeTtl;

    public AuditEventConsumer(AnalyticsMetricRepository metrics,
                       StringRedisTemplate redis,
                       @Value("${app.kafka.dedupe-ttl-hours:72}") long dedupeTtlHours) {
        this.metrics = metrics;
        this.redis = redis;
        this.dedupeTtl = Duration.ofHours(dedupeTtlHours);
    }

    @KafkaListener(topics = "${app.kafka.events-topic:file-storage.events}", groupId = "${spring.kafka.consumer.group-id:audit-analytics-service}")
    @Transactional
    public void onEvent(EventEnvelope event) {
        Boolean firstSeen = redis.opsForValue().setIfAbsent("audit:processed_event:" + event.eventId(), "1", dedupeTtl);
    if(!Boolean.TRUE.equals(firstSeen)) {
            return;
        }
    increment(event.eventType(), "day:" + LocalDate.now());
    increment("EVENT_TOTAL", "day:" + LocalDate.now());
    }

   private void increment(String metricType, String period) {
        String id = metricType + ":" + period + ":ALL";
        AnalyticsMetric metric = metrics.findById(id).orElseGet(() -> {
            AnalyticsMetric created = new AnalyticsMetric();
            created.id = id;
            created.metricType = metricType;
            created.period = period;
            created.dimensionKey = "ALL";
            created.value = 0;
            return created;
        });
        metric.value += 1;
        metric.updatedAt = Instant.now();
        metrics.save(metric);
    }
}
