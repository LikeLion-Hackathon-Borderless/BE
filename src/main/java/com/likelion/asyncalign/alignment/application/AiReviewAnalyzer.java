package com.likelion.asyncalign.alignment.application;

import com.likelion.asyncalign.alignment.domain.ConfidenceLevel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiReviewAnalyzer {

    Analysis analyze(AnalysisInput input);

    record AnalysisInput(
            String content,
            String senderLanguage,
            String recipientLanguage,
            UUID recipientId,
            List<String> recentMessages,
            List<String> attachmentContexts
    ) {
    }

    record Analysis(
            String sourceLanguage,
            String translatedContent,
            String task,
            ConfidenceLevel taskConfidence,
            Instant deadline,
            ConfidenceLevel deadlineConfidence,
            String expectedOutcome,
            ConfidenceLevel expectedOutcomeConfidence,
            String provider
    ) {
    }
}
