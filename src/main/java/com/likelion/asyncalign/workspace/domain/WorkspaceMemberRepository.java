package com.likelion.asyncalign.workspace.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("""
            select member from WorkspaceMember member
            join fetch member.workspace workspace
            where member.user.id = :userId and workspace.deletedAt is null
            order by workspace.updatedAt desc
            """)
    List<WorkspaceMember> findActiveMemberships(@Param("userId") UUID userId);

    @Query("""
            select member from WorkspaceMember member
            join fetch member.user user
            where member.workspace.id = :workspaceId
            order by lower(user.displayName), user.id
            """)
    List<WorkspaceMember> findMembers(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            select member from WorkspaceMember member
            join fetch member.user
            where member.workspace.id = :workspaceId and member.user.id = :userId
            """)
    Optional<WorkspaceMember> findMembership(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId
    );

    long countByWorkspaceId(UUID workspaceId);
}
