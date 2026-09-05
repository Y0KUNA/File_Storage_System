package com.filestorage.identity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
class UserAccount {
    @Id
    UUID id;
    String email;
    String passwordHash;
    String displayName;
    int tokenVersion;
    Instant createdAt;
    Instant updatedAt;

    @Enumerated(EnumType.STRING)
    Role role;

    protected UserAccount() {
    }

    UserAccount(UUID id, String email, String passwordHash, String displayName, Role role) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.tokenVersion = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}

