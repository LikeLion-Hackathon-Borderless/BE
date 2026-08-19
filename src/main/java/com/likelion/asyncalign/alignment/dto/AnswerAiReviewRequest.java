package com.likelion.asyncalign.alignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerAiReviewRequest(
        @NotBlank @Size(max = 1000) String answer
) {
}
