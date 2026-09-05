package com.filestorage.identity;

import com.filestorage.common.Headers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
class IdentityController {
    private final IdentityService service;

    IdentityController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@RequestHeader(value = Headers.CORRELATION_ID, required = false) String correlationId,
                              @Valid @RequestBody RegisterRequest request) {
        return service.register(request, correlationId);
    }

    @PostMapping("/auth/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/auth/refresh")
    TokenResponse refresh() {
        return new TokenResponse("dev-access-token-refreshed", "dev-refresh-token-rotated");
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout() {
    }

    @PostMapping("/auth/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logoutAll() {
    }

    @PostMapping("/auth/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword() {
    }

    @PostMapping("/auth/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword() {
    }

    @GetMapping("/me")
    MeResponse me(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.me(userId);
    }

    @PatchMapping("/me")
    MeResponse updateProfile(@RequestHeader(Headers.USER_ID) UUID userId,
                             @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(userId, request);
    }
}

