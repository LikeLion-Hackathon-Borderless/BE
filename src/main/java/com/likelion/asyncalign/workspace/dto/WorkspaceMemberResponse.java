package com.likelion.asyncalign.workspace.dto;

import com.likelion.asyncalign.workspace.domain.WorkspaceMember;
import com.likelion.asyncalign.workspace.domain.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID membershipId,
        WorkspaceRole membershipRole,
        Instant joinedAt,
        WorkspaceMemberUserResponse user,
        WorkspaceWorkContextResponse workContext
) {
    public static WorkspaceMemberResponse from(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getRole(),
                member.getCreatedAt(),
                WorkspaceMemberUserResponse.from(member.getUser()),
                WorkspaceWorkContextResponse.from(member));
    }
}
