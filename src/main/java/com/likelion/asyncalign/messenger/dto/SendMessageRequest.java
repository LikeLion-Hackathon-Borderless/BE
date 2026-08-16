package com.likelion.asyncalign.messenger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @Schema(description = "메시지 본문", example = "스펙 초안 확인 부탁드려요.", maxLength = 4000)
        @NotBlank @Size(max = 4000) String content
) {
}
