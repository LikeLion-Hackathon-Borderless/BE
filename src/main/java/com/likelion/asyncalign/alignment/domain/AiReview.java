package com.likelion.asyncalign.alignment.domain;

import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.global.persistence.BaseEntity;
import com.likelion.asyncalign.messenger.domain.Conversation;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_reviews")
public class AiReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiReviewStatus status;

    @Column(nullable = false, length = 4000)
    private String originalContent;

    @Column(length = 10)
    private String sourceLanguage;

    @Column(length = 10)
    private String recipientLanguage;

    @Column(length = 4000)
    private String translatedContent;

    @Column(length = 1000)
    private String aiTask;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfidenceLevel aiTaskConfidence;

    private Instant aiDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfidenceLevel aiDeadlineConfidence;

    @Column(length = 1000)
    private String aiExpectedOutcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfidenceLevel aiExpectedOutcomeConfidence;

    @Column(length = 1000)
    private String finalTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_assignee_user_id")
    private User finalAssignee;

    private Instant finalDeadline;

    @Column(length = 1000)
    private String finalExpectedOutcome;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false)
    private Instant expiresAt;

    @ManyToMany
    @JoinTable(
            name = "ai_review_attachments",
            joinColumns = @JoinColumn(name = "ai_review_id"),
            inverseJoinColumns = @JoinColumn(name = "attachment_id"))
    private List<Attachment> attachments = new ArrayList<>();

    protected AiReview() {
    }

    public AiReview(
            Conversation conversation,
            User creator,
            String originalContent,
            String sourceLanguage,
            String recipientLanguage,
            String translatedContent,
            String aiTask,
            ConfidenceLevel aiTaskConfidence,
            Instant aiDeadline,
            ConfidenceLevel aiDeadlineConfidence,
            String aiExpectedOutcome,
            ConfidenceLevel aiExpectedOutcomeConfidence,
            String provider,
            Instant expiresAt,
            List<Attachment> attachments
    ) {
        this.conversation = conversation;
        this.creator = creator;
        this.status = AiReviewStatus.READY;
        this.originalContent = originalContent;
        this.sourceLanguage = sourceLanguage;
        this.recipientLanguage = recipientLanguage;
        this.translatedContent = translatedContent;
        this.aiTask = aiTask;
        this.aiTaskConfidence = aiTaskConfidence;
        this.aiDeadline = aiDeadline;
        this.aiDeadlineConfidence = aiDeadlineConfidence;
        this.aiExpectedOutcome = aiExpectedOutcome;
        this.aiExpectedOutcomeConfidence = aiExpectedOutcomeConfidence;
        this.provider = provider;
        this.expiresAt = expiresAt;
        this.attachments.addAll(attachments);
    }

    public void updateFinal(
            String task,
            User assignee,
            Instant deadline,
            String expectedOutcome,
            boolean confirmed
    ) {
        this.finalTask = task;
        this.finalAssignee = assignee;
        this.finalDeadline = deadline;
        this.finalExpectedOutcome = expectedOutcome;
        this.status = confirmed ? AiReviewStatus.CONFIRMED : AiReviewStatus.READY;
    }

    public void markSent() {
        this.status = AiReviewStatus.SENT;
    }

    public void expireIfNecessary(Instant now) {
        if (expiresAt.isBefore(now) && status != AiReviewStatus.SENT) {
            this.status = AiReviewStatus.EXPIRED;
        }
    }

    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public User getCreator() { return creator; }
    public AiReviewStatus getStatus() { return status; }
    public String getOriginalContent() { return originalContent; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getRecipientLanguage() { return recipientLanguage; }
    public String getTranslatedContent() { return translatedContent; }
    public String getAiTask() { return aiTask; }
    public ConfidenceLevel getAiTaskConfidence() { return aiTaskConfidence; }
    public Instant getAiDeadline() { return aiDeadline; }
    public ConfidenceLevel getAiDeadlineConfidence() { return aiDeadlineConfidence; }
    public String getAiExpectedOutcome() { return aiExpectedOutcome; }
    public ConfidenceLevel getAiExpectedOutcomeConfidence() { return aiExpectedOutcomeConfidence; }
    public String getFinalTask() { return finalTask; }
    public User getFinalAssignee() { return finalAssignee; }
    public Instant getFinalDeadline() { return finalDeadline; }
    public String getFinalExpectedOutcome() { return finalExpectedOutcome; }
    public String getProvider() { return provider; }
    public Instant getExpiresAt() { return expiresAt; }
    public List<Attachment> getAttachments() { return List.copyOf(attachments); }
}
