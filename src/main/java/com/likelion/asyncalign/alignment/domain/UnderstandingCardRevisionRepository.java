package com.likelion.asyncalign.alignment.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnderstandingCardRevisionRepository extends JpaRepository<UnderstandingCardRevision, UUID> {

    List<UnderstandingCardRevision> findAllByCardIdOrderByRevisionNumberAsc(UUID cardId);
}
