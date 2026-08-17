package com.likelion.asyncalign.alignment.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiReviewRepository extends JpaRepository<AiReview, UUID> {

    @Query("""
            select distinct review from AiReview review
            join fetch review.conversation conversation
            join fetch review.creator
            left join fetch review.finalAssignee
            left join fetch review.attachments
            where review.id = :reviewId
            """)
    Optional<AiReview> findWithDetailsById(@Param("reviewId") UUID reviewId);
}
