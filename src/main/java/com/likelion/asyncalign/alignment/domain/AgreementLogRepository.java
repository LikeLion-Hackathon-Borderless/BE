package com.likelion.asyncalign.alignment.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgreementLogRepository extends JpaRepository<AgreementLog, UUID> {

    @Query("""
            select log from AgreementLog log
            join fetch log.card
            left join fetch log.agreedBy
            where log.conversation.id = :conversationId and log.createdAt < :before
            order by log.createdAt desc
            """)
    List<AgreementLog> findPageBefore(
            @Param("conversationId") UUID conversationId,
            @Param("before") Instant before,
            Pageable pageable
    );
}
