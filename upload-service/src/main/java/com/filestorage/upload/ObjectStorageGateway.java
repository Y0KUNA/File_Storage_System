package com.filestorage.upload;

import java.time.Instant;

interface ObjectStorageGateway {
    PresignedPut presignPut(String storageKey);

    long statObjectSize(String storageKey);
}

record PresignedPut(String url, Instant expiresAt) {
}

