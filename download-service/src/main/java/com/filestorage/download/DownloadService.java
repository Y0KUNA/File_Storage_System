package com.filestorage.download;

import com.filestorage.common.dto.AccessCheckResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
class DownloadService {
    private final FilesystemClient filesystem;
    private final DownloadObjectStorage objectStorage;
    private final ZipJobRepository zipJobs;

    DownloadService(FilesystemClient filesystem, DownloadObjectStorage objectStorage, ZipJobRepository zipJobs) {
        this.filesystem = filesystem;
        this.objectStorage = objectStorage;
        this.zipJobs = zipJobs;
    }

    DownloadUrlResponse fileUrl(UUID userId, UUID fileId) {
        AccessCheckResponse access = filesystem.accessCheck(fileId, userId);
        if (!access.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DOWNLOAD_NOT_ALLOWED");
        }
        PresignedGet get = objectStorage.presignGet(access.storageKey());
        return new DownloadUrlResponse(get.url(), get.expiresAt(), true);
    }

    @Transactional
    ZipJobResponse requestZip(UUID userId, UUID folderId) {
        ZipJob job = zipJobs.save(new ZipJob(UUID.randomUUID(), folderId, userId));
        return toResponse(job);
    }

    ZipJobResponse getZipJob(UUID jobId) {
        return zipJobs.findById(jobId).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ZIP_JOB_NOT_FOUND"));
    }

    DownloadUrlResponse zipDownloadUrl(UUID jobId) {
        ZipJob job = zipJobs.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ZIP_JOB_NOT_FOUND"));
        if (job.status != ZipJobStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ZIP_NOT_READY");
        }
        PresignedGet get = objectStorage.presignGet(job.resultStorageKey);
        return new DownloadUrlResponse(get.url(), get.expiresAt(), true);
    }

    private ZipJobResponse toResponse(ZipJob job) {
        return new ZipJobResponse(job.id, job.status.name(), job.expiresAt, job.errorMessage);
    }
}

