package com.filestorage.upload;

import java.time.Instant;

interface ObjectStorageGateway {
    PresignedPut presignPut(String storageKey);

    PresignedPut presignPart(String storageKey, int partNumber);

    long statObjectSize(String storageKey);

    long completeMultipart(String storageKey, int totalParts);

    void abortMultipart(String storageKey, int totalParts);
}

record PresignedPut(String url, Instant expiresAt) {
}
