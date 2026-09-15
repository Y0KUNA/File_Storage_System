package com.filestorage.download.domain;


import com.filestorage.download.client.*;
import com.filestorage.download.controller.*;
import com.filestorage.download.domain.*;
import com.filestorage.download.dto.*;
import com.filestorage.download.messaging.*;
import com.filestorage.download.repository.*;
import com.filestorage.download.service.*;
import com.filestorage.download.storage.*;
import com.filestorage.download.web.*;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class ZipJob {
    @Id
    public UUID id;
    public UUID folderId;
    public UUID requestedBy;
    public String resultStorageKey;
    public String errorMessage;
    public Instant expiresAt;
    public Instant createdAt;
    public Instant updatedAt;

    @Enumerated(EnumType.STRING)
    public ZipJobStatus status;

    protected ZipJob() {
    }

    public ZipJob(UUID id, UUID folderId, UUID requestedBy) {
        this.id = id;
        this.folderId = folderId;
        this.requestedBy = requestedBy;
        this.status = ZipJobStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}

