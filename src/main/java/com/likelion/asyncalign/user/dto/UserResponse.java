package com.likelion.asyncalign.user.dto;

import java.time.LocalTime;
import java.util.UUID;

import com.likelion.asyncalign.user.domain.User;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String timeZoneId,
        String preferredLanguage,
        LocalTime workStart,
        LocalTime workEnd
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimeZoneId(),
                user.getPreferredLanguage(),
                user.getWorkStart(),
                user.getWorkEnd());
    }
}
