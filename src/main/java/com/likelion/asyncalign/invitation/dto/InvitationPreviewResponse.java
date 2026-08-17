package com.likelion.asyncalign.invitation.dto;

import com.likelion.asyncalign.invitation.domain.InvitationStatus;
import java.time.Instant;
import java.util.UUID;

public record InvitationPreviewResponse(
        UUID workspaceId,
        String workspaceName,
        String inviterDisplayName,
        String invitedEmail,
        Instant expiresAt,
        InvitationStatus status
) {
}
