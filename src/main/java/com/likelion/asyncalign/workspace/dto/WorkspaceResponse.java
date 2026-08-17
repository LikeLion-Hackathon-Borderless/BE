package com.likelion.asyncalign.workspace.dto;

import com.likelion.asyncalign.workspace.domain.Workspace;
import com.likelion.asyncalign.workspace.domain.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String organizationDomain,
        WorkspaceRole myMembershipRole,
        long memberCount,
        Instant createdAt
) {
    public static WorkspaceResponse of(
            Workspace workspace,
            WorkspaceRole membershipRole,
            long memberCount
    ) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getOrganizationDomain(),
                membershipRole,
                memberCount,
                workspace.getCreatedAt());
    }
}
