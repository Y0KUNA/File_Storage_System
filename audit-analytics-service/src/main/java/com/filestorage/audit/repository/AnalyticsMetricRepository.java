package com.filestorage.audit.repository;



import com.filestorage.audit.dto.*;
import com.filestorage.audit.config.*;
import com.filestorage.audit.controller.*;
import com.filestorage.audit.domain.*;
import com.filestorage.audit.messaging.*;
import com.filestorage.audit.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalyticsMetricRepository extends JpaRepository<AnalyticsMetric, String> {
    public List<AnalyticsMetric> findByMetricTypeAndPeriod(String metricType, String period);
}

