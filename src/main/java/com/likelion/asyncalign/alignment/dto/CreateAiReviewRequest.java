package com.likelion.asyncalign.alignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateAiReviewRequest(
        @NotBlank @Size(max = 4000) String content,
        @Size(max = 10) List<UUID> attachmentIds
) {
    public List<UUID> safeAttachmentIds() {
        return attachmentIds == null ? List.of() : attachmentIds;
    }
}
