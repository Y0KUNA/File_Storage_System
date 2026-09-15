package com.filestorage.audit.domain;



import com.filestorage.audit.dto.*;
import com.filestorage.audit.config.*;
import com.filestorage.audit.controller.*;
import com.filestorage.audit.domain.*;
import com.filestorage.audit.messaging.*;
import com.filestorage.audit.repository.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class AnalyticsMetric {
    @Id
    public String id;
    public String metricType;
    public String period;
    public String dimensionKey;
    public double value;
    public Instant updatedAt;
}
