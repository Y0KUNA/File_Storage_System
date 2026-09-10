package com.filestorage.download;

import com.filestorage.common.EventEnvelope;
import com.filestorage.common.dto.FolderArchiveResponse;
import com.filestorage.common.dto.AccessCheckResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

@Service
class DownloadService {
    private final FilesystemClient filesystem;
    private final DownloadObjectStorage objectStorage;
    private final ZipJobRepository zipJobs;
    private final DomainEventPublisher publisher;

    DownloadService(FilesystemClient filesystem, DownloadObjectStorage objectStorage, ZipJobRepository zipJobs,
            DomainEventPublisher publisher) {
        this.filesystem = filesystem;
        this.objectStorage = objectStorage;
        this.zipJobs = zipJobs;
        this.publisher = publisher;
    }

    DownloadUrlResponse fileUrl(UUID userId, UUID fileId) {
        System.out.println(">>> DOWNLOAD SERVICE HIT");
        System.out.println(">>> userId = " + userId);
        System.out.println(">>> fileId = " + fileId);
        AccessCheckResponse access = filesystem.accessCheck(fileId, userId);
        System.out.println(">>> ACCESS ALLOWED = " + access.allowed());
        System.out.println(">>> STORAGE KEY = " + access.storageKey());
        if (!access.allowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DOWNLOAD_NOT_ALLOWED");
        }
        publisher.publish(EventEnvelope.v1(
                "FILE_DOWNLOAD_REQUESTED",
                "FILE",
                fileId.toString(),
                null,
                Map.of("fileId", fileId.toString(), "ownerId", userId.toString(), "viaShare", false)));
        PresignedGet get = objectStorage.presignGet(access.storageKey(), access.name(), access.mimeType());
        return new DownloadUrlResponse(get.url(), get.expiresAt(), true);
    }

    @Transactional
    ZipJobResponse requestZip(UUID userId, UUID folderId) {
        ZipJob job = zipJobs.save(new ZipJob(UUID.randomUUID(), folderId, userId));
        publisher.publish(EventEnvelope.v1(
                "ZIP_REQUESTED",
                "FOLDER",
                folderId.toString(),
                null,
                Map.of("jobId", job.id.toString(), "folderId", folderId.toString(), "ownerId", userId.toString())));
        processZip(job);
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

    private void processZip(ZipJob job) {
        job.status = ZipJobStatus.PROCESSING;
        job.updatedAt = Instant.now();
        try {
            FolderArchiveResponse manifest = filesystem.archiveManifest(job.folderId, job.requestedBy);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
                for (var file : manifest.files()) {
                    objectStorage.writeZipEntry(zip, file);
                }
            }
            String resultKey = "zip-jobs/" + job.id + ".zip";
            byte[] zipBytes = buffer.toByteArray();
            objectStorage.putObject(resultKey, new ByteArrayInputStream(zipBytes), zipBytes.length, "application/zip");
            job.resultStorageKey = resultKey;
            job.expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
            job.status = ZipJobStatus.READY;
            job.updatedAt = Instant.now();
            publisher.publish(EventEnvelope.v1(
                    "ZIP_READY",
                    "ZIP_JOB",
                    job.id.toString(),
                    null,
                    Map.of("jobId", job.id.toString(), "folderId", job.folderId.toString(), "ownerId",
                            job.requestedBy.toString())));
        } catch (Exception ex) {
            job.status = ZipJobStatus.FAILED;
            job.errorMessage = ex.getMessage();
            job.updatedAt = Instant.now();
            publisher.publish(EventEnvelope.v1(
                    "ZIP_FAILED",
                    "ZIP_JOB",
                    job.id.toString(),
                    null,
                    Map.of("jobId", job.id.toString(), "folderId", job.folderId.toString(), "ownerId",
                            job.requestedBy.toString())));
        }
    }
}
