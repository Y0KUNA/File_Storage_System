package com.filestorage.filesystem;

import com.filestorage.common.FileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
