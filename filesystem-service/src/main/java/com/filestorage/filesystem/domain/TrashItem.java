package com.filestorage.filesystem.domain;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import com.filestorage.common.ResourceType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class TrashItem {
    @Id
    public UUID id;
    public UUID ownerId;
    public UUID resourceId;

    @Enumerated(EnumType.STRING)
    public ResourceType resourceType;

    public UUID originalParentId;
    public String originalName;
    public long size;
    public Instant trashedAt;

    protected TrashItem() {
    }

    public TrashItem(UUID id, UUID ownerId, UUID resourceId, ResourceType resourceType, UUID originalParentId, String originalName, long size) {
        this.id = id;
        this.ownerId = ownerId;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.originalParentId = originalParentId;
        this.originalName = originalName;
        this.size = size;
        this.trashedAt = Instant.now();
    }
}
