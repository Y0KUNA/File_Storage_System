package com.filestorage.identity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class LoginSession {
    @Id
    UUID id;
    UUID userId;
    String refreshTokenHash;
    String deviceInfo;
    String ipAddress;
    Instant createdAt;
    Instant lastUsedAt;
    Instant expiresAt;
    Instant revokedAt;

    protected LoginSession() {
    }

    LoginSession(UUID id, UUID userId, String refreshTokenHash, String deviceInfo, String ipAddress, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.lastUsedAt = this.createdAt;
    }

    boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
