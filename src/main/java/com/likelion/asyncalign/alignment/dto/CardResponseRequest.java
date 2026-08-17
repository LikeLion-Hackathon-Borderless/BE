package com.likelion.asyncalign.alignment.dto;

import com.likelion.asyncalign.alignment.domain.CardResponseType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CardResponseRequest(
        @NotNull CardResponseType type,
        @Size(max = 1000) String comment,
        Instant proposedDeadline
) {
}
