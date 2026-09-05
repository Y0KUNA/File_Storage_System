package com.filestorage.download;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class ZipJob {
    @Id
    UUID id;
    UUID folderId;
    UUID requestedBy;
    String resultStorageKey;
    String errorMessage;
    Instant expiresAt;
    Instant createdAt;
    Instant updatedAt;

    @Enumerated(EnumType.STRING)
    ZipJobStatus status;

    protected ZipJob() {
    }

    ZipJob(UUID id, UUID folderId, UUID requestedBy) {
        this.id = id;
        this.folderId = folderId;
        this.requestedBy = requestedBy;
        this.status = ZipJobStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}

