package com.filestorage.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AnalyticsMetricRepository extends JpaRepository<AnalyticsMetric, String> {
    List<AnalyticsMetric> findByMetricTypeAndPeriod(String metricType, String period);
}

