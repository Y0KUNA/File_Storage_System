package com.filestorage.filesystem;

import com.filestorage.common.ResourceType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class TrashItem {
    @Id
    UUID id;
    UUID ownerId;
    UUID resourceId;

    @Enumerated(EnumType.STRING)
    ResourceType resourceType;

    UUID originalParentId;
    String originalName;
    long size;
    Instant trashedAt;

    protected TrashItem() {
    }

    TrashItem(UUID id, UUID ownerId, UUID resourceId, ResourceType resourceType, UUID originalParentId, String originalName, long size) {
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
