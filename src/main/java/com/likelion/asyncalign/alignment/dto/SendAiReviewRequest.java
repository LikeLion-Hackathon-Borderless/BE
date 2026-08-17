package com.likelion.asyncalign.alignment.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public record SendAiReviewRequest(
        @Size(max = 4000) String content,
        Instant scheduledFor
) {
}
