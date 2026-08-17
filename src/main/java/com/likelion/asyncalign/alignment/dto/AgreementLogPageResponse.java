package com.likelion.asyncalign.alignment.dto;

import com.likelion.asyncalign.alignment.domain.AgreementStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgreementLogPageResponse(
        List<Log> logs,
        boolean hasMore,
        Instant nextBefore
) {
    public record Log(
            UUID id,
            UUID cardId,
            int revision,
            AgreementStatus status,
            String task,
            Instant deadline,
            String expectedOutcome,
            AgreedBy agreedBy,
            Instant agreedAt,
            List<FileReference> fileReferences,
            Instant recordedAt
    ) {
    }

    public record AgreedBy(UUID userId, String displayName) {
    }

    public record FileReference(UUID attachmentId, String fileName, String locator) {
    }
}
