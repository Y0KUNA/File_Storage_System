package com.filestorage.notification.repository;


import com.filestorage.notification.config.*;
import com.filestorage.notification.controller.*;
import com.filestorage.notification.domain.*;
import com.filestorage.notification.dto.*;
import com.filestorage.notification.messaging.*;
import com.filestorage.notification.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> {
    public List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

