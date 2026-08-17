package com.likelion.asyncalign.messenger.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            select message from Message message
            join fetch message.sender
            where message.conversation.id = :conversationId
              and message.createdAt < :before
              and message.deliveryStatus = com.likelion.asyncalign.messenger.domain.DeliveryStatus.SENT
            order by message.createdAt desc
            """)
    List<Message> findPageBefore(
            @Param("conversationId") UUID conversationId,
            @Param("before") Instant before,
            Pageable pageable
    );

    Optional<Message> findFirstByConversationIdAndDeliveryStatusOrderByCreatedAtDesc(
            UUID conversationId,
            DeliveryStatus deliveryStatus
    );

    long countByConversationIdAndCreatedAtAfterAndSenderIdNotAndDeliveryStatus(
            UUID conversationId,
            Instant lastReadAt,
            UUID currentUserId,
            DeliveryStatus deliveryStatus
    );

    List<Message> findAllByDeliveryStatusAndScheduledForLessThanEqual(
            DeliveryStatus deliveryStatus,
            Instant dueAt
    );

    @Query("""
            select message from Message message
            where message.conversation.id = :conversationId
              and message.deliveryStatus = com.likelion.asyncalign.messenger.domain.DeliveryStatus.SENT
            order by message.createdAt desc
            """)
    List<Message> findRecentForContext(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );
}
