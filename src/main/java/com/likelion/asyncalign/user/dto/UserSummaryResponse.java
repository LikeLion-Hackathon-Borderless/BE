package com.likelion.asyncalign.user.dto;

import java.util.UUID;

import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.WorkRole;

public record UserSummaryResponse(
        UUID id,
        String displayName,
        String profileImageUrl,
        WorkRole role,
        String customRole,
        String timeZoneId,
        String preferredLanguage
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getDisplayName(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getCustomRole(),
                user.getTimeZoneId(),
                user.getPreferredLanguage());
    }
}
