package com.filestorage.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password, String displayName) {
}

record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
}

record RefreshTokenRequest(@NotBlank String refreshToken) {
}

record LogoutRequest(@NotBlank String refreshToken) {
}

record AuthResponse(UUID userId, String role, String accessToken, String refreshToken, UUID sessionId) {
}

record RegisterResponse(UUID userId, String role) {
}

record MeResponse(UUID userId, String email, String displayName, String role) {
}

record UpdateProfileRequest(String displayName) {
}

record TokenResponse(String accessToken, String refreshToken) {
}
