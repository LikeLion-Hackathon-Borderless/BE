package com.likelion.asyncalign.alignment.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnderstandingCardResponseRepository extends JpaRepository<UnderstandingCardResponse, UUID> {

    Optional<UnderstandingCardResponse> findFirstByCardIdOrderByCreatedAtDesc(UUID cardId);

    boolean existsByCardIdAndRevisionNumber(UUID cardId, int revisionNumber);
}
