package com.filestorage.filesystem;

import com.filestorage.common.FileStatus;
import com.filestorage.common.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record FolderResponse(UUID id, String name, UUID parentId) {
}

record FileResponse(UUID id, String name, long size, String mimeType, FileStatus status, String checksum) {
}

record ChildrenResponse(List<FolderResponse> folders, List<FileResponse> files) {
}

record CreateFolderRequest(@NotNull UUID parentId, @NotBlank String name) {
}

record QuotaResponse(UUID userId, long quotaBytes, long currentUsageBytes, long remainingBytes) {
}

record TrashItemResponse(UUID id, UUID resourceId, ResourceType resourceType, String name, UUID originalParentId, long size, Instant trashedAt) {
}

record TrashResponse(List<TrashItemResponse> items) {
}

record RestoreResponse(UUID resourceId, ResourceType resourceType, String effectiveName, UUID parentId) {
}
