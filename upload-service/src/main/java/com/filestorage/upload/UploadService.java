package com.filestorage.upload;

import com.filestorage.common.EventEnvelope;
import com.filestorage.common.UploadMode;
import com.filestorage.common.dto.CreateInternalFileRequest;
import com.filestorage.common.dto.InternalFileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
class UploadService {
    private final long multipartThresholdBytes;
    private final long partSizeBytes;
    private final FilesystemClient filesystem;
    private final ObjectStorageGateway objectStorage;
    private final UploadSessionRepository sessions;
    private final DomainEventPublisher publisher;

    UploadService(@Value("${app.multipart-threshold-bytes:104857600}") long multipartThresholdBytes,
                  @Value("${app.part-size-bytes:10485760}") long partSizeBytes,
                  FilesystemClient filesystem,
                  ObjectStorageGateway objectStorage,
                  UploadSessionRepository sessions,
                  DomainEventPublisher publisher) {
        this.multipartThresholdBytes = multipartThresholdBytes;
        this.partSizeBytes = partSizeBytes;
        this.filesystem = filesystem;
        this.objectStorage = objectStorage;
        this.sessions = sessions;
        this.publisher = publisher;
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
        List<PartUploadUrl> parts = IntStream.rangeClosed(1, session.totalParts)
                .mapToObj(partNumber -> {
                    PresignedPut part = objectStorage.presignPart(storageKey, partNumber);
                    return new PartUploadUrl(partNumber, part.url(), part.expiresAt());
                })
                .toList();
        return new CreateUploadResponse(
                file.fileId(),
                mode,
                null,
                session.id,
                session.partSize,
                session.totalParts,
                parts,
                parts.isEmpty() ? Instant.now() : parts.get(0).expiresAt()
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
            filesystem.releaseQuota(fileId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SIZE_MISMATCH");
        }
        filesystem.updateSize(fileId, actualSize);
        session.status = UploadSessionStatus.COMPLETED;
        session.lastActivityAt = Instant.now();
        publishStored(session, actualSize);
        return new ConfirmUploadResponse(fileId, "PENDING_SCAN", actualSize);
    }

    UploadStatusResponse status(UUID uploadSessionId) {
        UploadSession session = findSession(uploadSessionId);
        List<Integer> uploadedParts = uploadedParts(session);
        List<Integer> missingParts = IntStream.rangeClosed(1, session.totalParts)
                .filter(partNumber -> !uploadedParts.contains(partNumber))
                .boxed()
                .toList();
        return new UploadStatusResponse(session.id, session.fileId, session.status.name(), uploadedParts, missingParts);
    }

    PresignPartsResponse presignParts(UUID uploadSessionId, PresignPartsRequest request) {
        UploadSession session = findMultipartSession(uploadSessionId);
        List<Integer> requestedParts = request.partNumbers() == null ? List.of() : request.partNumbers();
        List<PartUploadUrl> parts = requestedParts.stream()
                .filter(partNumber -> partNumber >= 1 && partNumber <= session.totalParts)
                .map(partNumber -> {
                    PresignedPut put = objectStorage.presignPart(session.storageKey, partNumber);
                    return new PartUploadUrl(partNumber, put.url(), put.expiresAt());
                })
                .toList();
        return new PresignPartsResponse(parts);
    }

    @Transactional
    ConfirmUploadResponse complete(UUID uploadSessionId, CompleteMultipartRequest request) {
        UploadSession session = findMultipartSession(uploadSessionId);
        if (session.status != UploadSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UPLOAD_ALREADY_FINALIZED");
        }
        Set<Integer> providedParts = new HashSet<>(
                request.parts() == null ? List.of() : request.parts().stream().map(CompletedPartRequest::partNumber).toList()
        );
        boolean missing = IntStream.rangeClosed(1, session.totalParts).anyMatch(partNumber -> !providedParts.contains(partNumber));
        if (missing) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PARTS_MISSING");
        }
        long actualSize = objectStorage.completeMultipart(session.storageKey, session.totalParts);
        if (actualSize > session.reservedSize) {
            session.status = UploadSessionStatus.ABORTED;
            objectStorage.abortMultipart(session.storageKey, session.totalParts);
            filesystem.releaseQuota(session.fileId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SIZE_MISMATCH");
        }
        filesystem.updateSize(session.fileId, actualSize);
        session.status = UploadSessionStatus.COMPLETED;
        session.lastActivityAt = Instant.now();
        publishStored(session, actualSize);
        return new ConfirmUploadResponse(session.fileId, "PENDING_SCAN", actualSize);
    }

    @Transactional
    void abort(UUID uploadSessionId) {
        UploadSession session = findMultipartSession(uploadSessionId);
        if (session.status != UploadSessionStatus.IN_PROGRESS) {
            return;
        }
        objectStorage.abortMultipart(session.storageKey, session.totalParts);
        filesystem.releaseQuota(session.fileId);
        session.status = UploadSessionStatus.ABORTED;
        session.lastActivityAt = Instant.now();
    }

    private UploadSession findSession(UUID uploadSessionId) {
        return sessions.findById(uploadSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UPLOAD_SESSION_NOT_FOUND"));
    }

    private UploadSession findMultipartSession(UUID uploadSessionId) {
        UploadSession session = findSession(uploadSessionId);
        if (session.mode != UploadMode.MULTIPART) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NOT_MULTIPART_UPLOAD");
        }
        return session;
    }

    private List<Integer> uploadedParts(UploadSession session) {
        if (session.mode != UploadMode.MULTIPART || session.totalParts <= 0) {
            return List.of();
        }
        return IntStream.rangeClosed(1, session.totalParts)
                .filter(partNumber -> {
                    try {
                        objectStorage.statObjectSize(session.storageKey + ".parts/" + partNumber);
                        return true;
                    } catch (ResponseStatusException ex) {
                        return false;
                    }
                })
                .boxed()
                .toList();
    }

    private void publishStored(UploadSession session, long actualSize) {
        publisher.publish(EventEnvelope.v1(
                "FILE_UPLOAD_STORED",
                "FILE",
                session.fileId.toString(),
                null,
                Map.of(
                        "fileId", session.fileId.toString(),
                        "ownerId", session.ownerId.toString(),
                        "storageKey", session.storageKey,
                        "size", actualSize
                )
        ));
    }
}
