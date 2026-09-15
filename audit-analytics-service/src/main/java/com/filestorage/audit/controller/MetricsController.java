package com.filestorage.audit.controller;



import com.filestorage.audit.dto.*;
import com.filestorage.audit.config.*;
import com.filestorage.audit.controller.*;
import com.filestorage.audit.domain.*;
import com.filestorage.audit.messaging.*;
import com.filestorage.audit.repository.*;
import com.filestorage.common.Headers;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class MetricsController {
    private final AnalyticsMetricRepository metrics;

    public MetricsController(AnalyticsMetricRepository metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/metrics")
    public List<MetricResponse> metrics(@RequestHeader(Headers.USER_ROLE) String role,
                                 @RequestParam String metricType,
                                 @RequestParam(defaultValue = "day") String period) {
    if(!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED");
        }
        return metrics.findByMetricTypeAndPeriod(metricType, period).stream()
                .map(metric -> new MetricResponse(metric.metricType, metric.period, metric.dimensionKey, metric.value, metric.updatedAt))
                .toList();
    }
}

