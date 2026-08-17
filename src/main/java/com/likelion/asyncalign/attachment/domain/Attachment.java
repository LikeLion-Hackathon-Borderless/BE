package com.likelion.asyncalign.attachment.domain;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import com.likelion.asyncalign.messenger.domain.Conversation;
import com.likelion.asyncalign.messenger.domain.Message;
import com.likelion.asyncalign.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Column(nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttachmentProcessingStatus processingStatus = AttachmentProcessingStatus.PROCESSING;

    @Column(length = 50)
    private String extractionErrorCode;

    @Column(columnDefinition = "text")
    private String extractedText;

    protected Attachment() {
    }

    public Attachment(
            Conversation conversation,
            User uploader,
            String storageKey,
            String originalFileName,
            String contentType,
            long sizeBytes
    ) {
        this.conversation = conversation;
        this.uploader = uploader;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public void extractionReady(String extractedText) {
        this.extractedText = extractedText;
        this.processingStatus = AttachmentProcessingStatus.READY;
        this.extractionErrorCode = null;
    }

    public void extractionFailed(String errorCode) {
        this.processingStatus = AttachmentProcessingStatus.EXTRACTION_FAILED;
        this.extractionErrorCode = errorCode;
        this.extractedText = null;
    }

    public void attachTo(Message message) {
        this.message = message;
    }

    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public User getUploader() { return uploader; }
    public Message getMessage() { return message; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public AttachmentProcessingStatus getProcessingStatus() { return processingStatus; }
    public String getExtractionErrorCode() { return extractionErrorCode; }
    public String getExtractedText() { return extractedText; }
}
