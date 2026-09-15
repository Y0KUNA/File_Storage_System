package com.filestorage.identity.controller;


import com.filestorage.identity.config.*;
import com.filestorage.identity.controller.*;
import com.filestorage.identity.domain.*;
import com.filestorage.identity.dto.*;
import com.filestorage.identity.messaging.*;
import com.filestorage.identity.repository.*;
import com.filestorage.identity.service.*;
import com.filestorage.identity.web.*;
import com.filestorage.common.Headers;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class IdentityController {
    private final IdentityService service;

    public IdentityController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestHeader(value = Headers.CORRELATION_ID, required = false) String correlationId,
                              @Valid @RequestBody RegisterRequest request) {
        return service.register(request, correlationId);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@RequestHeader(value = "User-Agent", required = false) String userAgent,
                       @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                       @Valid @RequestBody LoginRequest request) {
                    System.out.println(">>> [DEBUG CONTROLLER] LOGIN CALLED");
        return service.login(request, userAgent, forwardedFor);
    }

    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        service.logout(request);
    }

    @PostMapping("/auth/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@RequestHeader(Headers.USER_ID) UUID userId) {
        service.logoutAll(userId);
    }

    @PostMapping("/auth/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword() {
    }

    @PostMapping("/auth/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword() {
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader(Headers.USER_ID) UUID userId) {
        return service.me(userId);
    }

    @PatchMapping("/me")
    public MeResponse updateProfile(@RequestHeader(Headers.USER_ID) UUID userId,
                             @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(userId, request);
    }
}
