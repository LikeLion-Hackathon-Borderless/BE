package com.likelion.asyncalign.attachment.application;

import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.attachment.domain.AttachmentRepository;
import com.likelion.asyncalign.attachment.dto.AttachmentResponse;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.messenger.application.ConversationService;
import com.likelion.asyncalign.messenger.domain.Conversation;
import com.likelion.asyncalign.messenger.domain.ConversationMember;
import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.storage.FileStorageService;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ConversationService conversationService;
    private final AttachmentFileValidator fileValidator;
    private final AttachmentContentExtractor contentExtractor;
    private final FileStorageService fileStorageService;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            ConversationService conversationService,
            AttachmentFileValidator fileValidator,
            AttachmentContentExtractor contentExtractor,
            FileStorageService fileStorageService
    ) {
        this.attachmentRepository = attachmentRepository;
        this.conversationService = conversationService;
        this.fileValidator = fileValidator;
        this.contentExtractor = contentExtractor;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AttachmentResponse upload(UUID conversationId, UUID userId, MultipartFile file) {
        ConversationMember membership = conversationService.getMembership(conversationId, userId);
        AttachmentFileValidator.ValidatedFile validated = fileValidator.validate(file);
        String storageKey = fileStorageService.storeAttachment(
                conversationId,
                UUID.randomUUID(),
                file,
                validated.extension());
        Attachment attachment = new Attachment(
                membership.getConversation(),
                membership.getUser(),
                storageKey,
                validated.originalName(),
                validated.contentType(),
                file.getSize());
        try {
            attachment.extractionReady(contentExtractor.extract(file, validated.contentType()));
        } catch (Exception exception) {
            attachment.extractionFailed("ATTACHMENT_EXTRACTION_FAILED");
        }
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    public AttachmentResponse get(UUID attachmentId, UUID userId) {
        Attachment attachment = getAuthorized(attachmentId, userId);
        return AttachmentResponse.from(attachment);
    }

    public DownloadFile download(UUID attachmentId, UUID userId) {
        Attachment attachment = getAuthorized(attachmentId, userId);
        return new DownloadFile(
                fileStorageService.loadAttachment(attachment.getStorageKey()),
                attachment.getOriginalFileName(),
                attachment.getContentType());
    }

    @Transactional
    public List<Attachment> attachToMessage(
            Conversation conversation,
            UUID userId,
            List<UUID> requestedIds,
            Message message
    ) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>(requestedIds);
        if (ids.size() != requestedIds.size() || ids.size() > 10) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "첨부파일 ID는 중복 없이 최대 10개까지 지정할 수 있습니다.");
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Attachment> attachments = attachmentRepository.findAllByIdIn(List.copyOf(ids));
        if (attachments.size() != ids.size()) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND, "첨부파일을 찾을 수 없습니다.");
        }
        for (Attachment attachment : attachments) {
            if (!attachment.getConversation().getId().equals(conversation.getId())
                    || !attachment.getUploader().getId().equals(userId)
                    || attachment.getMessage() != null) {
                throw new ApiException(
                        ErrorCode.ATTACHMENT_ACCESS_DENIED,
                        "이 대화에 업로드한 미사용 첨부파일만 전송할 수 있습니다.");
            }
            attachment.attachTo(message);
        }
        return attachments;
    }

    public List<AttachmentResponse> getMessageAttachments(UUID messageId) {
        return attachmentRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    public List<Attachment> getMessageAttachmentEntities(UUID messageId) {
        return attachmentRepository.findAllByMessageIdOrderByCreatedAtAsc(messageId);
    }

    public List<Attachment> getAttachmentsForReview(
            Conversation conversation,
            UUID userId,
            List<UUID> requestedIds
    ) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>(requestedIds);
        if (ids.size() != requestedIds.size() || ids.size() > 10) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "첨부파일 ID는 중복 없이 최대 10개까지 지정할 수 있습니다.");
        }
        List<Attachment> attachments = attachmentRepository.findAllByIdIn(List.copyOf(ids));
        if (attachments.size() != ids.size()) {
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND, "첨부파일을 찾을 수 없습니다.");
        }
        for (Attachment attachment : attachments) {
            if (!attachment.getConversation().getId().equals(conversation.getId())
                    || !attachment.getUploader().getId().equals(userId)
                    || attachment.getMessage() != null) {
                throw new ApiException(ErrorCode.ATTACHMENT_ACCESS_DENIED, "검토에 사용할 수 없는 첨부파일입니다.");
            }
        }
        return attachments;
    }

    private Attachment getAuthorized(UUID attachmentId, UUID userId) {
        Attachment attachment = attachmentRepository.findWithDetailsById(attachmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND, "첨부파일을 찾을 수 없습니다."));
        conversationService.getMembership(attachment.getConversation().getId(), userId);
        return attachment;
    }

    public record DownloadFile(Path path, String originalFileName, String contentType) {
    }
}
