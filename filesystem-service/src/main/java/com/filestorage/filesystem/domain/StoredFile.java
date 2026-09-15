package com.filestorage.filesystem.domain;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.FileStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class StoredFile {
    @Id
    public UUID id;
    public UUID ownerId;
    public String name;
    public long size;
    public String mimeType;
    public String storageKey;
    public UUID parentFolderId;
    public String checksum;
    public String checksumAlgorithm;

    @Enumerated(EnumType.STRING)
    public FileStatus status;

    public Instant createdAt;
    public Instant updatedAt;

    protected StoredFile() {
    }

    public StoredFile(UUID id, UUID ownerId, String name, long size, String mimeType, String storageKey, UUID parentFolderId) {
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

