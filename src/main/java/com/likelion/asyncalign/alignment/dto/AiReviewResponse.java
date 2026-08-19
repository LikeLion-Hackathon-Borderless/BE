package com.likelion.asyncalign.alignment.dto;

import com.likelion.asyncalign.alignment.domain.AiReviewStatus;
import com.likelion.asyncalign.alignment.domain.AiAgentSessionStatus;
import com.likelion.asyncalign.alignment.domain.ConfidenceLevel;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record AiReviewResponse(
        UUID id,
        UUID conversationId,
        AiReviewStatus status,
        String originalContent,
        String sourceLanguage,
        String recipientLanguage,
        String translatedContent,
        StructuredFields structuredFields,
        List<Evidence> evidence,
        List<Warning> warnings,
        AgentSession agentSession,
        String provider,
        Instant createdAt,
        Instant expiresAt
) {
    public record StructuredFields(
            TextField task,
            AssigneeField assigneeUserId,
            DeadlineField deadline,
            TextField expectedOutcome
    ) {
    }

    public record TextField(String value, ConfidenceLevel confidence, boolean confirmed) {
    }

    public record AssigneeField(UUID value, ConfidenceLevel confidence, boolean confirmed) {
    }

    public record DeadlineField(
            Instant value,
            ZonedDateTime senderLocal,
            ZonedDateTime recipientLocal,
            ConfidenceLevel confidence,
            boolean confirmed
    ) {
    }

    public record Evidence(
            UUID id,
            UUID attachmentId,
            String fileName,
            String locator,
            String excerpt,
            ConfidenceLevel confidence,
            boolean confirmed
    ) {
    }

    public record Warning(String code, String message, Instant suggestedDeadline) {
    }

    public record AgentSession(
            String threadId,
            AiAgentSessionStatus status,
            Integer step,
            Integer total,
            AmbiguityItem item
    ) {
    }

    public record AmbiguityItem(
            String span,
            String category,
            String reason,
            List<String> candidates,
            String suggestion
    ) {
    }
}
