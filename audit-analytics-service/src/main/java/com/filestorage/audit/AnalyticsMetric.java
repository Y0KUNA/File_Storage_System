package com.filestorage.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
class AnalyticsMetric {
    @Id
    String id;
    String metricType;
    String period;
    String dimensionKey;
    double value;
    Instant updatedAt;
}

