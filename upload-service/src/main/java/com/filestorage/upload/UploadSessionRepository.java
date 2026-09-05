package com.filestorage.upload;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    Optional<UploadSession> findByFileId(UUID fileId);

    List<UploadSession> findByStatusAndLastActivityAtBefore(UploadSessionStatus status, Instant cutoff);
}
