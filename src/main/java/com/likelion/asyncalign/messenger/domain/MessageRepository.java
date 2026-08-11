package com.likelion.asyncalign.messenger.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select message from Message message
            join fetch message.sender
            where message.conversation.id = :conversationId
              and message.createdAt < :before
            order by message.createdAt desc
            """)
    List<Message> findPageBefore(
            @Param("conversationId") UUID conversationId,
            @Param("before") Instant before,
            Pageable pageable
    );

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndCreatedAtAfterAndSenderIdNot(
            UUID conversationId,
            Instant lastReadAt,
            UUID currentUserId
    );
}
