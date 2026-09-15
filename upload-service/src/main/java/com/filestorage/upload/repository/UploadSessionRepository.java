package com.filestorage.upload.repository;


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
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    public Optional<UploadSession> findByFileId(UUID fileId);

   public  List<UploadSession> findByStatusAndLastActivityAtBefore(UploadSessionStatus status, Instant cutoff);
}
