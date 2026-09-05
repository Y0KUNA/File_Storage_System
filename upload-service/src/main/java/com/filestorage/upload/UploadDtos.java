package com.filestorage.upload;

import com.filestorage.common.UploadMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CreateUploadRequest(@NotNull UUID parentFolderId, @NotBlank String name, @Min(1) long size, @NotBlank String mimeType) {
}

record PartUploadUrl(int partNumber, String url, Instant expiresAt) {
}

record CreateUploadResponse(
        UUID fileId,
        UploadMode mode,
        String uploadUrl,
        UUID uploadSessionId,
        long partSize,
        int totalParts,
        List<PartUploadUrl> parts,
        Instant expiresAt
) {
}

record ConfirmUploadResponse(UUID fileId, String status, long size) {
}

record UploadStatusResponse(UUID uploadSessionId, UUID fileId, String status, List<Integer> uploadedParts, List<Integer> missingParts) {
}

record PresignPartsRequest(List<Integer> partNumbers) {
}

record PresignPartsResponse(List<PartUploadUrl> parts) {
}

record CompleteMultipartRequest(List<CompletedPartRequest> parts) {
}

record CompletedPartRequest(int partNumber, String etag) {
}
