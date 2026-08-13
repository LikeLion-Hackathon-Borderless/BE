package com.likelion.asyncalign.user.dto;

import java.time.LocalTime;
import java.util.UUID;
import java.time.DayOfWeek;
import java.util.Set;

import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.WorkRole;
import com.likelion.asyncalign.user.domain.OnboardingStep;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        WorkRole role,
        String customRole,
        String profileImageUrl,
        String timeZoneId,
        String preferredLanguage,
        LocalTime workStart,
        LocalTime workEnd,
        Set<DayOfWeek> workDays,
        boolean emailVerified,
        OnboardingStep onboardingStep
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCustomRole(),
                user.getProfileImageUrl(),
                user.getTimeZoneId(),
                user.getPreferredLanguage(),
                user.getWorkStart(),
                user.getWorkEnd(),
                user.getWorkDays(),
                user.isEmailVerified(),
                user.getOnboardingStep());
    }
}
