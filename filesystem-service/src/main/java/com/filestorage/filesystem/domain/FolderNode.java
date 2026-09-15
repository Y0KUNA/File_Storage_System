package com.filestorage.filesystem.domain;


import com.filestorage.filesystem.controller.*;
import com.filestorage.filesystem.domain.*;
import com.filestorage.filesystem.dto.*;
import com.filestorage.filesystem.messaging.*;
import com.filestorage.filesystem.repository.*;
import com.filestorage.filesystem.service.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class FolderNode {
    @Id
    public UUID id;
    public UUID ownerId;
    public UUID parentId;
    public String name;
    public Instant createdAt;
    public Instant updatedAt;

    protected FolderNode() {
    }

    public FolderNode(UUID id, UUID ownerId, UUID parentId, String name) {
        this.id = id;
        this.ownerId = ownerId;
        this.parentId = parentId;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}

