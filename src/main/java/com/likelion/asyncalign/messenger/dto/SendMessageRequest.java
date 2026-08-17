package com.likelion.asyncalign.messenger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.likelion.asyncalign.messenger.domain.DeliveryMode;

public record SendMessageRequest(
        @Schema(description = "메시지 본문", example = "스펙 초안 확인 부탁드려요.", maxLength = 4000)
        @Size(max = 4000) String content,
        @Size(max = 10) List<UUID> attachmentIds,
        @NotNull DeliveryMode deliveryMode,
        Instant scheduledFor
) {
    public List<UUID> safeAttachmentIds() {
        return attachmentIds == null ? List.of() : attachmentIds;
    }
}
