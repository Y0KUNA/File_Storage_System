package com.filestorage.identity;

import com.filestorage.common.EventEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
class IdentityService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventPublisher publisher;

    IdentityService(UserRepository users, PasswordEncoder passwordEncoder, DomainEventPublisher publisher) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.publisher = publisher;
    }

    @Transactional
    RegisterResponse register(RegisterRequest request, String correlationId) {
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

    AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmail(request.email().toLowerCase())
                .filter(found -> passwordEncoder.matches(request.password(), found.passwordHash))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
        String token = "dev-access-token-" + user.id + "-v" + user.tokenVersion;
        String refresh = "dev-refresh-token-" + UUID.randomUUID();
        return new AuthResponse(user.id, user.role.name(), token, refresh);
    }

    MeResponse me(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        return new MeResponse(user.id, user.email, user.displayName, user.role.name());
    }

    @Transactional
    MeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        user.displayName = request.displayName();
        return new MeResponse(user.id, user.email, user.displayName, user.role.name());
    }
}

