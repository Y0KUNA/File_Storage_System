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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class UserAccount {
    @Id
    public UUID id;
    public String email;
    public String passwordHash;
    public String displayName;
    public int tokenVersion;
    public Instant createdAt;
    public Instant updatedAt;

    @Enumerated(EnumType.STRING)
    public Role role;

    protected UserAccount() {
    }

    public UserAccount(UUID id, String email, String passwordHash, String displayName, Role role) {
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

