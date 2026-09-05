package com.filestorage.filesystem;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class FolderNode {
    @Id
    UUID id;
    UUID ownerId;
    UUID parentId;
    String name;
    Instant createdAt;
    Instant updatedAt;

    protected FolderNode() {
    }

    FolderNode(UUID id, UUID ownerId, UUID parentId, String name) {
        this.id = id;
        this.ownerId = ownerId;
        this.parentId = parentId;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}

