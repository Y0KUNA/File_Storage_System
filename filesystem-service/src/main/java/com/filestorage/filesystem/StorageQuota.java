package com.filestorage.filesystem;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class StorageQuota {
    @Id
    UUID userId;
    long quotaBytes;
    long currentUsageBytes;
    Instant updatedAt;

    protected StorageQuota() {
    }

    StorageQuota(UUID userId, long quotaBytes) {
        this.userId = userId;
        this.quotaBytes = quotaBytes;
        this.currentUsageBytes = 0L;
        this.updatedAt = Instant.now();
    }
}

