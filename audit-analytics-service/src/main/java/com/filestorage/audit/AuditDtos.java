package com.filestorage.audit;

import java.time.Instant;

record MetricResponse(String metricType, String period, String dimensionKey, double value, Instant updatedAt) {
}

