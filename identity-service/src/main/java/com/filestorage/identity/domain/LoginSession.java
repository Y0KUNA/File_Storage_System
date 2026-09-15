package com.filestorage.identity.domain;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class LoginSession {
    @Id
    public UUID id;
    public UUID userId;
    public String refreshTokenHash;
    public String deviceInfo;
    public String ipAddress;
    public Instant createdAt;
    public Instant lastUsedAt;
    public Instant expiresAt;
    public Instant revokedAt;

    protected LoginSession() {
    }

    public LoginSession(UUID id, UUID userId, String refreshTokenHash, String deviceInfo, String ipAddress, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.lastUsedAt = this.createdAt;
    }

   public  boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
