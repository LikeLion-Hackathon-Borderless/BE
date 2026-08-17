package com.likelion.asyncalign.workspace.dto;

import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.WorkRole;
import java.util.UUID;

public record WorkspaceMemberUserResponse(
        UUID id,
        String email,
        String displayName,
        String profileImageUrl,
        WorkRole workRole,
        String customRole,
        String timeZoneId,
        String preferredLanguage
) {
    public static WorkspaceMemberUserResponse from(User user) {
        return new WorkspaceMemberUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getCustomRole(),
                user.getTimeZoneId(),
                user.getPreferredLanguage());
    }
}
