package com.filestorage.filesystem;

import com.filestorage.common.EventEnvelope;
import com.filestorage.common.FileStatus;
import com.filestorage.common.ResourceType;
import com.filestorage.common.dto.AccessCheckResponse;
import com.filestorage.common.dto.ActivateFileRequest;
import com.filestorage.common.dto.ArchiveFileItem;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.FolderArchiveResponse;
import com.filestorage.common.dto.InternalFileResponse;
import com.filestorage.common.dto.RejectFileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
class FilesystemService {
    private static final long DEFAULT_QUOTA = 10L * 1024 * 1024 * 1024;

    private final FolderRepository folders;
    private final FileRepository files;
    private final StorageQuotaRepository quotas;
    private final TrashItemRepository trashItems;
    private final DomainEventPublisher publisher;

    FilesystemService(FolderRepository folders,
                      FileRepository files,
                      StorageQuotaRepository quotas,
                      TrashItemRepository trashItems,
                      DomainEventPublisher publisher) {
        this.folders = folders;
        this.files = files;
        this.quotas = quotas;
        this.trashItems = trashItems;
        this.publisher = publisher;
    }

    @Transactional
    FolderResponse provisionRoot(UUID userId) {
        StorageQuota quota = quotas.findById(userId).orElseGet(() -> new StorageQuota(userId, DEFAULT_QUOTA));
        quotas.save(quota);
        FolderNode root = folders.findByOwnerIdAndParentIdIsNull(userId)
                .orElseGet(() -> folders.save(new FolderNode(UUID.randomUUID(), userId, null, "root")));
        return new FolderResponse(root.id, root.name, root.parentId);
    }

    FolderResponse root(UUID userId) {
        FolderNode root = folders.findByOwnerIdAndParentIdIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ROOT_NOT_PROVISIONED_YET"));
        return new FolderResponse(root.id, root.name, root.parentId);
    }

    ChildrenResponse children(UUID userId, UUID folderId) {
        FolderNode folder = findOwnedFolder(userId, folderId);
        if (isFolderTrashed(folder)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND");
        }
        return new ChildrenResponse(
                folders.findByOwnerIdAndParentId(userId, folderId).stream()
                        .filter(child -> !trashItems.existsByResourceIdAndResourceType(child.id, ResourceType.FOLDER))
                        .map(child -> new FolderResponse(child.id, child.name, child.parentId))
                        .toList(),
                files.findByOwnerIdAndParentFolderId(userId, folderId).stream()
                        .filter(file -> !trashItems.existsByResourceIdAndResourceType(file.id, ResourceType.FILE))
                        .map(file -> new FileResponse(file.id, file.name, file.size, file.mimeType, file.status, file.checksum))
                        .toList()
        );
    }

    @Transactional
    FolderResponse createFolder(UUID userId, CreateFolderRequest request) {
        FolderNode parent = findOwnedFolder(userId, request.parentId());
        if (isFolderTrashed(parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PARENT_FOLDER_NOT_FOUND");
        }
        String effectiveName = uniqueFolderName(userId, request.parentId(), request.name());
        FolderNode folder = folders.save(new FolderNode(UUID.randomUUID(), userId, request.parentId(), effectiveName));
        return new FolderResponse(folder.id, folder.name, folder.parentId);
    }

    QuotaResponse quota(UUID userId) {
        StorageQuota quota = quotas.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QUOTA_NOT_PROVISIONED_YET"));
        return new QuotaResponse(userId, quota.quotaBytes, quota.currentUsageBytes, quota.quotaBytes - quota.currentUsageBytes);
    }

    @Transactional
    InternalFileResponse createPendingFile(CreateInternalFileRequest request) {
        FolderNode parent = findOwnedFolder(request.ownerId(), request.parentFolderId());
        if (isFolderTrashed(parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PARENT_FOLDER_NOT_FOUND");
        }
        StorageQuota quota = quotas.findById(request.ownerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QUOTA_NOT_PROVISIONED_YET"));
        if (quota.currentUsageBytes + request.size() > quota.quotaBytes) {
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "QUOTA_EXCEEDED");
        }
        quota.currentUsageBytes += request.size();
        quota.updatedAt = Instant.now();
        String effectiveName = uniqueFileName(request.ownerId(), request.parentFolderId(), request.name());
        StoredFile file = files.save(new StoredFile(
                UUID.randomUUID(),
                request.ownerId(),
                effectiveName,
                request.size(),
                request.mimeType(),
                request.storageKey(),
                request.parentFolderId()
        ));
        return internalFile(file);
    }

    @Transactional
    InternalFileResponse updateSize(UUID fileId, long size) {
        StoredFile file = findFile(fileId);
        StorageQuota quota = quotas.findById(file.ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QUOTA_NOT_PROVISIONED_YET"));
        long delta = size - file.size;
        if (delta > 0 && quota.currentUsageBytes + delta > quota.quotaBytes) {
            throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "QUOTA_EXCEEDED");
        }
        quota.currentUsageBytes += delta;
        quota.updatedAt = Instant.now();
        file.size = size;
        file.updatedAt = Instant.now();
        return internalFile(file);
    }

    @Transactional
    InternalFileResponse activate(UUID fileId, ActivateFileRequest request) {
        StoredFile file = findFile(fileId);
        if (file.status != FileStatus.PENDING_SCAN) {
            return internalFile(file);
        }
        file.status = FileStatus.ACTIVE;
        file.checksum = request.checksum();
        file.updatedAt = Instant.now();
        publisher.publish(EventEnvelope.v1(
                "FILE_UPLOAD_COMPLETED",
                "FILE",
                file.id.toString(),
                null,
                Map.of(
                        "fileId", file.id.toString(),
                        "ownerId", file.ownerId.toString(),
                        "size", file.size,
                        "mimeType", file.mimeType,
                        "checksum", file.checksum
                )
        ));
        return internalFile(file);
    }

    @Transactional
    void reject(UUID fileId, RejectFileRequest request) {
        StoredFile file = findFile(fileId);
        StorageQuota quota = quotas.findById(file.ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QUOTA_NOT_PROVISIONED_YET"));
        quota.currentUsageBytes = Math.max(0, quota.currentUsageBytes - file.size);
        quota.updatedAt = Instant.now();
        files.delete(file);
        publisher.publish(EventEnvelope.v1(
                "FILE_INFECTED",
                "FILE",
                file.id.toString(),
                null,
                Map.of("fileId", file.id.toString(), "ownerId", file.ownerId.toString())
        ));
    }

    AccessCheckResponse accessCheck(UUID fileId, UUID userId) {
        StoredFile file = findFile(fileId);
        boolean allowed = file.ownerId.equals(userId)
                && file.status == FileStatus.ACTIVE
                && !trashItems.existsByResourceIdAndResourceType(file.id, ResourceType.FILE)
                && !isFolderTrashed(findOwnedFolder(file.ownerId, file.parentFolderId));
        return new AccessCheckResponse(allowed, file.id, file.ownerId, file.storageKey, file.size);
    }

    FolderArchiveResponse archiveManifest(UUID userId, UUID folderId) {
        FolderNode folder = findOwnedFolder(userId, folderId);
        if (isFolderTrashed(folder)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND");
        }
        List<ArchiveFileItem> manifest = new ArrayList<>();
        collectActiveFiles(userId, folderId, "", manifest);
        return new FolderArchiveResponse(folderId, manifest);
    }

    @Transactional
    void moveToTrash(UUID userId, ResourceType type, UUID resourceId) {
        switch (type) {
            case FILE -> trashFile(userId, resourceId);
            case FOLDER -> trashFolder(userId, resourceId);
        }
    }

    TrashResponse trash(UUID userId) {
        return new TrashResponse(trashItems.findByOwnerIdOrderByTrashedAtDesc(userId).stream()
                .map(item -> new TrashItemResponse(item.id, item.resourceId, item.resourceType, item.originalName,
                        item.originalParentId, item.size, item.trashedAt))
                .toList());
    }

    @Transactional
    RestoreResponse restore(UUID userId, ResourceType type, UUID resourceId) {
        TrashItem item = findTrashItem(userId, type, resourceId);
        switch (type) {
            case FILE -> {
                StoredFile file = findOwnedFile(userId, resourceId);
                FolderNode parent = findOwnedFolder(userId, item.originalParentId);
                if (isFolderTrashed(parent)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "ORIGINAL_PARENT_IN_TRASH");
                }
                file.updatedAt = Instant.now();
                trashItems.delete(item);
                return new RestoreResponse(file.id, ResourceType.FILE, file.name, item.originalParentId);
            }
            case FOLDER -> {
                FolderNode folder = findOwnedFolder(userId, resourceId);
                FolderNode parent = findOwnedFolder(userId, item.originalParentId);
                if (isFolderTrashed(parent)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "ORIGINAL_PARENT_IN_TRASH");
                }
                folder.updatedAt = Instant.now();
                trashItems.delete(item);
                return new RestoreResponse(folder.id, ResourceType.FOLDER, folder.name, item.originalParentId);
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_RESOURCE_TYPE");
    }

    @Transactional
    void purge(UUID userId, ResourceType type, UUID resourceId, boolean permanent) {
        if (!permanent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PERMANENT_FLAG_REQUIRED");
        }
        TrashItem item = findTrashItem(userId, type, resourceId);
        switch (type) {
            case FILE -> {
                StoredFile file = findOwnedFile(userId, resourceId);
                releaseQuota(userId, file.size);
                files.delete(file);
                trashItems.delete(item);
            }
            case FOLDER -> {
                FolderNode folder = findOwnedFolder(userId, resourceId);
                long releasedBytes = purgeFolderTree(userId, folder.id);
                releaseQuota(userId, releasedBytes);
                folders.delete(folder);
                trashItems.delete(item);
            }
        }
    }

    private StoredFile findFile(UUID fileId) {
        return files.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND"));
    }

    private StoredFile findOwnedFile(UUID userId, UUID fileId) {
        StoredFile file = findFile(fileId);
        if (!file.ownerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND");
        }
        return file;
    }

    private FolderNode findOwnedFolder(UUID userId, UUID folderId) {
        FolderNode folder = folders.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND"));
        if (!folder.ownerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FOLDER_NOT_FOUND");
        }
        return folder;
    }

    private TrashItem findTrashItem(UUID userId, ResourceType type, UUID resourceId) {
        return trashItems.findByOwnerIdAndResourceIdAndResourceType(userId, resourceId, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TRASH_ITEM_NOT_FOUND"));
    }

    private void trashFile(UUID userId, UUID fileId) {
        StoredFile file = findOwnedFile(userId, fileId);
        if (trashItems.existsByResourceIdAndResourceType(file.id, ResourceType.FILE)) {
            return;
        }
        if (isFolderTrashed(findOwnedFolder(userId, file.parentFolderId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESOURCE_ALREADY_TRASHED_BY_PARENT");
        }
        trashItems.save(new TrashItem(UUID.randomUUID(), userId, file.id, ResourceType.FILE, file.parentFolderId, file.name, file.size));
        file.updatedAt = Instant.now();
    }

    private void trashFolder(UUID userId, UUID folderId) {
        FolderNode folder = findOwnedFolder(userId, folderId);
        if (folder.parentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANNOT_TRASH_ROOT");
        }
        if (trashItems.existsByResourceIdAndResourceType(folder.id, ResourceType.FOLDER)) {
            return;
        }
        if (isFolderTrashed(findOwnedFolder(userId, folder.parentId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESOURCE_ALREADY_TRASHED_BY_PARENT");
        }
        long size = folderTreeSize(userId, folder.id);
        deleteChildTrashMarkers(userId, folder.id);
        trashItems.save(new TrashItem(UUID.randomUUID(), userId, folder.id, ResourceType.FOLDER, folder.parentId, folder.name, size));
        folder.updatedAt = Instant.now();
    }

    private long folderTreeSize(UUID userId, UUID folderId) {
        long directFileSize = files.findByOwnerIdAndParentFolderId(userId, folderId).stream()
                .mapToLong(file -> file.size)
                .sum();
        long childFolderSize = folders.findByOwnerIdAndParentId(userId, folderId).stream()
                .mapToLong(child -> folderTreeSize(userId, child.id))
                .sum();
        return directFileSize + childFolderSize;
    }

    private void collectActiveFiles(UUID userId, UUID folderId, String prefix, List<ArchiveFileItem> manifest) {
        files.findByOwnerIdAndParentFolderId(userId, folderId).stream()
                .filter(file -> file.status == FileStatus.ACTIVE)
                .filter(file -> !trashItems.existsByResourceIdAndResourceType(file.id, ResourceType.FILE))
                .forEach(file -> manifest.add(new ArchiveFileItem(file.id, prefix + file.name, file.storageKey, file.size)));
        folders.findByOwnerIdAndParentId(userId, folderId).stream()
                .filter(folder -> !trashItems.existsByResourceIdAndResourceType(folder.id, ResourceType.FOLDER))
                .forEach(folder -> collectActiveFiles(userId, folder.id, prefix + folder.name + "/", manifest));
    }

    private void deleteChildTrashMarkers(UUID userId, UUID folderId) {
        for (StoredFile file : files.findByOwnerIdAndParentFolderId(userId, folderId)) {
            trashItems.findByOwnerIdAndResourceIdAndResourceType(userId, file.id, ResourceType.FILE)
                    .ifPresent(trashItems::delete);
        }
        for (FolderNode child : folders.findByOwnerIdAndParentId(userId, folderId)) {
            deleteChildTrashMarkers(userId, child.id);
            trashItems.findByOwnerIdAndResourceIdAndResourceType(userId, child.id, ResourceType.FOLDER)
                    .ifPresent(trashItems::delete);
        }
    }

    private long purgeFolderTree(UUID userId, UUID folderId) {
        long releasedBytes = 0;
        for (FolderNode child : folders.findByOwnerIdAndParentId(userId, folderId)) {
            releasedBytes += purgeFolderTree(userId, child.id);
            trashItems.findByOwnerIdAndResourceIdAndResourceType(userId, child.id, ResourceType.FOLDER)
                    .ifPresent(trashItems::delete);
            folders.delete(child);
        }
        for (StoredFile file : files.findByOwnerIdAndParentFolderId(userId, folderId)) {
            releasedBytes += file.size;
            trashItems.findByOwnerIdAndResourceIdAndResourceType(userId, file.id, ResourceType.FILE)
                    .ifPresent(trashItems::delete);
            files.delete(file);
        }
        return releasedBytes;
    }

    private boolean isFolderTrashed(FolderNode folder) {
        if (trashItems.existsByResourceIdAndResourceType(folder.id, ResourceType.FOLDER)) {
            return true;
        }
        UUID parentId = folder.parentId;
        while (parentId != null) {
            FolderNode parent = folders.findById(parentId).orElse(null);
            if (parent == null) {
                return true;
            }
            if (trashItems.existsByResourceIdAndResourceType(parent.id, ResourceType.FOLDER)) {
                return true;
            }
            parentId = parent.parentId;
        }
        return false;
    }

    private void releaseQuota(UUID userId, long bytes) {
        if (bytes <= 0) {
            return;
        }
        StorageQuota quota = quotas.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QUOTA_NOT_PROVISIONED_YET"));
        quota.currentUsageBytes = Math.max(0, quota.currentUsageBytes - bytes);
        quota.updatedAt = Instant.now();
    }

    private String uniqueFolderName(UUID ownerId, UUID parentId, String requested) {
        String name = requested;
        int suffix = 1;
        while (folders.existsByOwnerIdAndParentIdAndName(ownerId, parentId, name)) {
            name = requested + " (" + suffix++ + ")";
        }
        return name;
    }

    private String uniqueFileName(UUID ownerId, UUID parentId, String requested) {
        String name = requested;
        int suffix = 1;
        while (files.existsByOwnerIdAndParentFolderIdAndName(ownerId, parentId, name)) {
            name = requested + " (" + suffix++ + ")";
        }
        return name;
    }

    private InternalFileResponse internalFile(StoredFile file) {
        return new InternalFileResponse(file.id, file.ownerId, file.name, file.size, file.storageKey, file.status);
    }
}
