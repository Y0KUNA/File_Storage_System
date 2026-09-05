package com.filestorage.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {
    Optional<LoginSession> findByRefreshTokenHash(String refreshTokenHash);

    List<LoginSession> findByUserIdAndRevokedAtIsNull(UUID userId);
}
