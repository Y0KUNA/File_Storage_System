package com.filestorage.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
class UploadCleanupJob {
    private final UploadSessionRepository sessions;
    private final ObjectStorageGateway objectStorage;
    private final FilesystemClient filesystem;
    private final long abortTtlMinutes;

    UploadCleanupJob(UploadSessionRepository sessions,
                     ObjectStorageGateway objectStorage,
                     FilesystemClient filesystem,
                     @Value("${app.upload-abort-ttl-minutes:1440}") long abortTtlMinutes) {
        this.sessions = sessions;
        this.objectStorage = objectStorage;
        this.filesystem = filesystem;
        this.abortTtlMinutes = abortTtlMinutes;
    }

    @Scheduled(fixedDelayString = "${app.upload-cleanup-delay-ms:300000}")
    @Transactional
    void abortExpiredUploads() {
        Instant cutoff = Instant.now().minus(abortTtlMinutes, ChronoUnit.MINUTES);
        sessions.findByStatusAndLastActivityAtBefore(UploadSessionStatus.IN_PROGRESS, cutoff).forEach(session -> {
            if (session.mode == com.filestorage.common.UploadMode.MULTIPART) {
                objectStorage.abortMultipart(session.storageKey, session.totalParts);
            }
            filesystem.releaseQuota(session.fileId);
            session.status = UploadSessionStatus.ABORTED;
            session.lastActivityAt = Instant.now();
        });
    }
}
