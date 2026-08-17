package com.likelion.asyncalign.invitation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateInvitationLinkRequest(
        @Min(1) @Max(30) Integer expiresInDays,
        boolean regenerate
) {
    public int effectiveExpiresInDays() {
        return expiresInDays == null ? 7 : expiresInDays;
    }
}
