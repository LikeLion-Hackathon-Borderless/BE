package com.likelion.asyncalign.alignment.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnderstandingCardRepository extends JpaRepository<UnderstandingCard, UUID> {

    Optional<UnderstandingCard> findByMessageId(UUID messageId);

    @Query("""
            select card from UnderstandingCard card
            join fetch card.message message
            join fetch card.conversation
            join fetch card.sender
            join fetch card.recipient
            left join fetch card.assignee
            left join fetch card.aiReview
            where card.id = :cardId
            """)
    Optional<UnderstandingCard> findWithDetailsById(@Param("cardId") UUID cardId);
}
