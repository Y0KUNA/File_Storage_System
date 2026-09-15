package com.filestorage.upload.domain;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
import com.filestorage.common.UploadMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class UploadSession {
    @Id
    public UUID id;
    public UUID fileId;
    public UUID ownerId;
    public String storageKey;
    public String minioUploadId;
    public long reservedSize;
    public long partSize;
    public int totalParts;
    public Instant createdAt;
    public Instant lastActivityAt;

    @Enumerated(EnumType.STRING)
    public UploadMode mode;

    @Enumerated(EnumType.STRING)
    public UploadSessionStatus status;

    protected UploadSession() {
    }

    public UploadSession(UUID id, UUID fileId, UUID ownerId, String storageKey, long reservedSize, UploadMode mode) {
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

