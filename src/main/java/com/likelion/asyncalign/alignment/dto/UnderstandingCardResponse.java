package com.likelion.asyncalign.alignment.dto;

import com.likelion.asyncalign.alignment.domain.CardResponseType;
import com.likelion.asyncalign.alignment.domain.UnderstandingCardState;
import com.likelion.asyncalign.attachment.dto.AttachmentResponse;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record UnderstandingCardResponse(
        UUID id,
        UUID messageId,
        UnderstandingCardState state,
        int revision,
        String task,
        Assignee assignee,
        Deadline deadline,
        String expectedOutcome,
        String originalContent,
        String translatedContent,
        boolean needsClarification,
        List<AttachmentResponse> attachments,
        List<AiReviewResponse.Evidence> evidence,
        LatestResponse latestResponse,
        Instant createdAt,
        Instant updatedAt
) {
    public record Assignee(UUID userId, String displayName) {
    }

    public record Deadline(Instant instant, ZonedDateTime viewerLocal, String viewerTimeZoneId) {
    }

    public record LatestResponse(
            UUID id,
            int revision,
            CardResponseType type,
            String comment,
            Instant proposedDeadline,
            UUID responderId,
            Instant respondedAt
    ) {
    }
}
