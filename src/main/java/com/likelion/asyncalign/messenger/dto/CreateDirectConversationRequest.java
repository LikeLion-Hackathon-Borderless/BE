package com.likelion.asyncalign.messenger.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateDirectConversationRequest(
        @Schema(description = "대화 상대 사용자 ID")
        @NotNull UUID otherUserId
) {
}
