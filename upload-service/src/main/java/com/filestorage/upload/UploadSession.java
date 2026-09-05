package com.filestorage.upload;

import com.filestorage.common.UploadMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class UploadSession {
    @Id
    UUID id;
    UUID fileId;
    UUID ownerId;
    String storageKey;
    String minioUploadId;
    long reservedSize;
    long partSize;
    int totalParts;
    Instant createdAt;
    Instant lastActivityAt;

    @Enumerated(EnumType.STRING)
    UploadMode mode;

    @Enumerated(EnumType.STRING)
    UploadSessionStatus status;

    protected UploadSession() {
    }

    UploadSession(UUID id, UUID fileId, UUID ownerId, String storageKey, long reservedSize, UploadMode mode) {
        this.id = id;
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.storageKey = storageKey;
        this.reservedSize = reservedSize;
        this.mode = mode;
        this.status = UploadSessionStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
        this.lastActivityAt = this.createdAt;
    }
}

