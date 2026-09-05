package com.filestorage.download;

import java.time.Instant;
import java.util.UUID;

record CreateDownloadUrlRequest(String shareToken) {
}

record DownloadUrlResponse(String url, Instant expiresAt, boolean supportsRange) {
}

record ZipJobResponse(UUID id, String status, Instant resultExpiresAt, String errorMessage) {
}

