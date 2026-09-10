package com.filestorage.common.dto;

import java.util.UUID;

public record AccessCheckResponse(
        boolean allowed,
        UUID fileId,
        UUID ownerId,
        String storageKey,
        String name,
        String mimeType,
        long size
) {
}
