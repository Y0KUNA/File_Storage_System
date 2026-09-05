package com.filestorage.filesystem;

import com.filestorage.common.FileStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class StoredFile {
    @Id
    UUID id;
    UUID ownerId;
    String name;
    long size;
    String mimeType;
    String storageKey;
    UUID parentFolderId;
    String checksum;
    String checksumAlgorithm;

    @Enumerated(EnumType.STRING)
    FileStatus status;

    Instant createdAt;
    Instant updatedAt;

    protected StoredFile() {
    }

    StoredFile(UUID id, UUID ownerId, String name, long size, String mimeType, String storageKey, UUID parentFolderId) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
        this.storageKey = storageKey;
        this.parentFolderId = parentFolderId;
        this.checksumAlgorithm = "SHA-256";
        this.status = FileStatus.PENDING_SCAN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}

