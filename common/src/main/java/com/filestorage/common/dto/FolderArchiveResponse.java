package com.filestorage.common.dto;

import java.util.List;
import java.util.UUID;

public record FolderArchiveResponse(UUID folderId, List<ArchiveFileItem> files) {
}
