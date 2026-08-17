package com.likelion.asyncalign.alignment.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateAiReviewRequest(
        @Size(max = 1000) String task,
        UUID assigneeUserId,
        Instant deadline,
        @Size(max = 1000) String expectedOutcome,
        List<UUID> confirmedEvidenceIds,
        boolean confirmed
) {
    public List<UUID> safeConfirmedEvidenceIds() {
        return confirmedEvidenceIds == null ? List.of() : confirmedEvidenceIds;
    }
}
