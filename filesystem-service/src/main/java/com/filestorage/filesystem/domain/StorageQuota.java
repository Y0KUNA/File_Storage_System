package com.filestorage.filesystem.domain;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class StorageQuota {
    @Id
    public UUID userId;
    public long quotaBytes;
    public long currentUsageBytes;
    public Instant updatedAt;

    protected StorageQuota() {
    }

    public StorageQuota(UUID userId, long quotaBytes) {
        this.userId = userId;
        this.quotaBytes = quotaBytes;
        this.currentUsageBytes = 0L;
        this.updatedAt = Instant.now();
    }
}

