package com.filestorage.common.dto;

import java.util.UUID;

public record ArchiveFileItem(UUID fileId, String name, String storageKey, long size) {
}
