package com.likelion.asyncalign.attachment.dto;

import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.attachment.domain.AttachmentProcessingStatus;
import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID conversationId,
        String originalFileName,
        String contentType,
        long size,
        AttachmentProcessingStatus processingStatus,
        String extractionErrorCode,
        String downloadUrl,
        Instant createdAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getConversation().getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getProcessingStatus(),
                attachment.getExtractionErrorCode(),
                "/api/v1/attachments/" + attachment.getId() + "/content",
                attachment.getCreatedAt());
    }
}
