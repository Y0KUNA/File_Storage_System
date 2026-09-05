package com.filestorage.common.dto;

import com.filestorage.common.FileStatus;

import java.util.UUID;

public record InternalFileResponse(
        UUID fileId,
        UUID ownerId,
        String name,
        long size,
        String storageKey,
        FileStatus status
) {
}

