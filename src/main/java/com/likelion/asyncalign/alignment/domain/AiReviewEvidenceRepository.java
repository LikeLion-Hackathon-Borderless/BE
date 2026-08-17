package com.likelion.asyncalign.alignment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReviewEvidenceRepository extends JpaRepository<AiReviewEvidence, UUID> {

    List<AiReviewEvidence> findAllByReviewIdOrderByCreatedAtAsc(UUID reviewId);
}
