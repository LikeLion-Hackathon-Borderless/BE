package com.likelion.asyncalign.messenger.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("""
            select member from ConversationMember member
            join fetch member.conversation conversation
            where member.user.id = :userId and conversation.workspace.id = :workspaceId
            order by conversation.lastMessageAt desc
            """)
    List<ConversationMember> findAllForUserInWorkspace(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );

    @Query("""
            select member from ConversationMember member
            join fetch member.user
            where member.conversation.id = :conversationId
            """)
    List<ConversationMember> findAllWithUserByConversationId(@Param("conversationId") UUID conversationId);
}
