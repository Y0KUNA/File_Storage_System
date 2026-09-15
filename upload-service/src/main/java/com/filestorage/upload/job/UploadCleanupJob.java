package com.filestorage.upload.job;


import com.filestorage.upload.client.*;
import com.filestorage.upload.controller.*;
import com.filestorage.upload.domain.*;
import com.filestorage.upload.dto.*;
import com.filestorage.upload.job.*;
import com.filestorage.upload.messaging.*;
import com.filestorage.upload.repository.*;
import com.filestorage.upload.service.*;
import com.filestorage.upload.storage.*;
import com.filestorage.upload.web.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class UploadCleanupJob {
    private final UploadSessionRepository sessions;
    private final ObjectStorageGateway objectStorage;
    private final FilesystemClient filesystem;
    private final long abortTtlMinutes;

    public UploadCleanupJob(UploadSessionRepository sessions,
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
    public void abortExpiredUploads() {
        Instant cutoff = Instant.now().minus(abortTtlMinutes, ChronoUnit.MINUTES);
        sessions.findByStatusAndLastActivityAtBefore(UploadSessionStatus.IN_PROGRESS, cutoff).forEach(session -> {
    if(session.mode == com.filestorage.common.UploadMode.MULTIPART) {
                objectStorage.abortMultipart(session.storageKey, session.totalParts);
            }
            filesystem.releaseQuota(session.fileId);
            session.status = UploadSessionStatus.ABORTED;
            session.lastActivityAt = Instant.now();
        });
    }
}
