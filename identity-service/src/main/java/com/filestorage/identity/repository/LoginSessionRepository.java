package com.filestorage.identity.repository;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {
    public Optional<LoginSession> findByRefreshTokenHash(String refreshTokenHash);

   public  List<LoginSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
