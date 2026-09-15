package com.filestorage.audit.dto;

import java.time.Instant;

public record MetricResponse(String metricType, String period, String dimensionKey, double value, Instant updatedAt) {
}
