package com.filestorage.filesystem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FolderRepository extends JpaRepository<FolderNode, UUID> {
    Optional<FolderNode> findByOwnerIdAndParentIdIsNull(UUID ownerId);

    List<FolderNode> findByOwnerIdAndParentId(UUID ownerId, UUID parentId);

    boolean existsByOwnerIdAndParentIdAndName(UUID ownerId, UUID parentId, String name);
}

interface FileRepository extends JpaRepository<StoredFile, UUID> {
    List<StoredFile> findByOwnerIdAndParentFolderId(UUID ownerId, UUID parentFolderId);

    boolean existsByOwnerIdAndParentFolderIdAndName(UUID ownerId, UUID parentFolderId, String name);
}

interface StorageQuotaRepository extends JpaRepository<StorageQuota, UUID> {
}

