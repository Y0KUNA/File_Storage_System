package com.filestorage.filesystem;

import com.filestorage.common.FileStatus;
import com.filestorage.common.dto.AccessCheckResponse;
import com.filestorage.common.dto.ActivateFileRequest;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.InternalFileResponse;
import com.filestorage.common.dto.RejectFileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
class FilesystemService {
    private static final long DEFAULT_QUOTA = 10L * 1024 * 1024 * 1024;

    private final FolderRepository folders;
    private final FileRepository files;
    private final StorageQuotaRepository quotas;

    FilesystemService(FolderRepository folders, FileRepository files, StorageQuotaRepository quotas) {
        this.folders = folders;
        this.files = files;
        this.quotas = quotas;
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
        return new ChildrenResponse(
                folders.findByOwnerIdAndParentId(userId, folderId).stream()
                        .map(folder -> new FolderResponse(folder.id, folder.name, folder.parentId))
                        .toList(),
                files.findByOwnerIdAndParentFolderId(userId, folderId).stream()
                        .map(file -> new FileResponse(file.id, file.name, file.size, file.mimeType, file.status, file.checksum))
                        .toList()
        );
    }

    @Transactional
    FolderResponse createFolder(UUID userId, CreateFolderRequest request) {
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
    }

    AccessCheckResponse accessCheck(UUID fileId, UUID userId) {
        StoredFile file = findFile(fileId);
        boolean allowed = file.ownerId.equals(userId) && file.status == FileStatus.ACTIVE;
        return new AccessCheckResponse(allowed, file.id, file.ownerId, file.storageKey, file.size);
    }

    private StoredFile findFile(UUID fileId) {
        return files.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND"));
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

