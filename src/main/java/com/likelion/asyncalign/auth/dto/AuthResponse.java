package com.likelion.asyncalign.auth.dto;

import java.time.Instant;

import com.likelion.asyncalign.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserResponse user
) {
}
