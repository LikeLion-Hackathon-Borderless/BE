package com.likelion.asyncalign.alignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateCardRevisionRequest(
        @NotBlank @Size(max = 1000) String task,
        @NotNull Instant deadline,
        @NotBlank @Size(max = 1000) String expectedOutcome,
        @NotBlank @Size(max = 1000) String changeNote
) {
}
