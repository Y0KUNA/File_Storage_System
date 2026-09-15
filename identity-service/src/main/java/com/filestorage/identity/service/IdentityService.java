package com.filestorage.identity.service;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import com.filestorage.common.EventEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class IdentityService {
    private final UserRepository users;
    private final LoginSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher publisher;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public IdentityService(UserRepository users,
                    LoginSessionRepository sessions,
                    PasswordEncoder passwordEncoder,
                    DomainEventPublisher publisher,
                    JwtService jwtService) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.publisher = publisher;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, String correlationId) {
        users.findByEmail(request.email()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED");
        });
        UserAccount user = new UserAccount(
                UUID.randomUUID(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                Role.USER
        );
        users.save(user);
        publisher.publish(EventEnvelope.v1(
                "USER_REGISTERED",
                "USER",
                user.id.toString(),
                correlationId,
                Map.of("userId", user.id.toString(), "email", user.email)
        ));
        return new RegisterResponse(user.id, user.role.name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        UserAccount user = users.findByEmail(request.email().toLowerCase())
                .filter(found -> passwordEncoder.matches(request.password(), found.passwordHash))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
        String refreshToken = newRefreshToken();
        LoginSession session = sessions.save(new LoginSession(
                UUID.randomUUID(),
                user.id,
    tokenHash(refreshToken),
                userAgent,
                ipAddress,
                Instant.now().plus(30, ChronoUnit.DAYS)
        ));
        return new AuthResponse(user.id, user.role.name(), jwtService.accessToken(user), refreshToken, session.id);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        LoginSession session = sessions.findByRefreshTokenHash(tokenHash(request.refreshToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));
        Instant now = Instant.now();
    if(!session.activeAt(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REVOKED");
        }
        UserAccount user = users.findById(session.userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
        String rotatedRefreshToken = newRefreshToken();
        session.refreshTokenHash = tokenHash(rotatedRefreshToken);
        session.lastUsedAt = now;
        session.expiresAt = now.plus(30, ChronoUnit.DAYS);
        return new TokenResponse(jwtService.accessToken(user), rotatedRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        sessions.findByRefreshTokenHash(tokenHash(request.refreshToken())).ifPresent(session -> {
            session.revokedAt = Instant.now();
            session.lastUsedAt = session.revokedAt;
        });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        user.tokenVersion++;
        user.updatedAt = Instant.now();
        Instant now = Instant.now();
        sessions.findByUserIdAndRevokedAtIsNull(userId).forEach(session -> {
            session.revokedAt = now;
            session.lastUsedAt = now;
        });
    }

   public  MeResponse me(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        return new MeResponse(user.id, user.email, user.displayName, user.role.name());
    }

    @Transactional
    public MeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        user.displayName = request.displayName();
        return new MeResponse(user.id, user.email, user.displayName, user.role.name());
    }

   private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

   private String tokenHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
    for(byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}
