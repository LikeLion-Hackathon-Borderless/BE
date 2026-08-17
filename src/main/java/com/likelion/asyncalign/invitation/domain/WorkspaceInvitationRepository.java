package com.likelion.asyncalign.invitation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {

    @Query("""
            select invitation from WorkspaceInvitation invitation
            join fetch invitation.workspace workspace
            join fetch invitation.inviter
            where invitation.tokenHash = :tokenHash
            """)
    Optional<WorkspaceInvitation> findByTokenHashWithDetails(@Param("tokenHash") String tokenHash);

    List<WorkspaceInvitation> findAllByWorkspaceIdAndStatus(
            UUID workspaceId,
            InvitationStatus status
    );

    Optional<WorkspaceInvitation> findFirstByWorkspaceIdAndInvitedEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            UUID workspaceId,
            String invitedEmail,
            InvitationStatus status
    );

    Optional<WorkspaceInvitation> findFirstByWorkspaceIdAndTypeAndStatusOrderByCreatedAtDesc(
            UUID workspaceId,
            InvitationType type,
            InvitationStatus status
    );
}
