package com.likelion.asyncalign.alignment.domain;

import com.likelion.asyncalign.attachment.domain.Attachment;
import com.likelion.asyncalign.global.persistence.BaseEntity;
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
@Table(name = "ai_review_evidence")
public class AiReviewEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_review_id", nullable = false)
    private AiReview review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attachment_id", nullable = false)
    private Attachment attachment;

    @Column(length = 255)
    private String locator;

    @Column(length = 1000)
    private String excerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfidenceLevel confidence;

    @Column(nullable = false)
    private boolean confirmed;

    protected AiReviewEvidence() {
    }

    public AiReviewEvidence(
            AiReview review,
            Attachment attachment,
            String locator,
            String excerpt,
            ConfidenceLevel confidence
    ) {
        this.review = review;
        this.attachment = attachment;
        this.locator = locator;
        this.excerpt = excerpt;
        this.confidence = confidence;
    }

    public void confirm() { this.confirmed = true; }
    public UUID getId() { return id; }
    public AiReview getReview() { return review; }
    public Attachment getAttachment() { return attachment; }
    public String getLocator() { return locator; }
    public String getExcerpt() { return excerpt; }
    public ConfidenceLevel getConfidence() { return confidence; }
    public boolean isConfirmed() { return confirmed; }
}
