package com.filestorage.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> {
    List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

