package com.filestorage.upload;

import com.filestorage.common.UploadMode;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.InternalFileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class UploadService {
    private final long multipartThresholdBytes;
    private final long partSizeBytes;
    private final FilesystemClient filesystem;
    private final ObjectStorageGateway objectStorage;
    private final UploadSessionRepository sessions;

    UploadService(@Value("${app.multipart-threshold-bytes:104857600}") long multipartThresholdBytes,
                  @Value("${app.part-size-bytes:10485760}") long partSizeBytes,
                  FilesystemClient filesystem,
                  ObjectStorageGateway objectStorage,
                  UploadSessionRepository sessions) {
        this.multipartThresholdBytes = multipartThresholdBytes;
        this.partSizeBytes = partSizeBytes;
        this.filesystem = filesystem;
        this.objectStorage = objectStorage;
        this.sessions = sessions;
    }

    @Transactional
    CreateUploadResponse create(UUID ownerId, CreateUploadRequest request) {
        UploadMode mode = request.size() < multipartThresholdBytes ? UploadMode.SINGLE : UploadMode.MULTIPART;
        String storageKey = ownerId + "/" + UUID.randomUUID();
        InternalFileResponse file = filesystem.createPendingFile(new CreateInternalFileRequest(
                ownerId,
                request.parentFolderId(),
                request.name(),
                request.size(),
                request.mimeType(),
                storageKey
        ));
        UploadSession session = sessions.save(new UploadSession(UUID.randomUUID(), file.fileId(), ownerId, storageKey, request.size(), mode));
        if (mode == UploadMode.SINGLE) {
            PresignedPut put = objectStorage.presignPut(storageKey);
            return new CreateUploadResponse(file.fileId(), mode, put.url(), null, 0, 0, List.of(), put.expiresAt());
        }
        session.partSize = partSizeBytes;
        session.totalParts = (int) Math.ceil((double) request.size() / partSizeBytes);
        PresignedPut firstPart = objectStorage.presignPut(storageKey + ".part1");
        return new CreateUploadResponse(
                file.fileId(),
                mode,
                null,
                session.id,
                session.partSize,
                session.totalParts,
                List.of(new PartUploadUrl(1, firstPart.url(), firstPart.expiresAt())),
                firstPart.expiresAt()
        );
    }

    @Transactional
    ConfirmUploadResponse confirm(UUID fileId) {
        UploadSession session = sessions.findByFileId(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UPLOAD_SESSION_NOT_FOUND"));
        if (session.status != UploadSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UPLOAD_ALREADY_FINALIZED");
        }
        long actualSize = objectStorage.statObjectSize(session.storageKey);
        if (actualSize > session.reservedSize) {
            session.status = UploadSessionStatus.ABORTED;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SIZE_MISMATCH");
        }
        filesystem.updateSize(fileId, actualSize);
        session.status = UploadSessionStatus.COMPLETED;
        session.lastActivityAt = Instant.now();
        return new ConfirmUploadResponse(fileId, "PENDING_SCAN", actualSize);
    }
}

