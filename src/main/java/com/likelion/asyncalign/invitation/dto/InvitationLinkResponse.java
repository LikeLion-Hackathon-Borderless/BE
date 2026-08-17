package com.likelion.asyncalign.invitation.dto;

import java.time.Instant;

public record InvitationLinkResponse(
        String token,
        String inviteUrl,
        Instant expiresAt
) {
}
