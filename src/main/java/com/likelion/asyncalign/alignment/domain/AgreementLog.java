package com.likelion.asyncalign.alignment.domain;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agreement_logs")
public class AgreementLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private UnderstandingCard card;

    @Column(nullable = false)
    private int revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_status", nullable = false, length = 20)
    private AgreementStatus status;

    @Column(length = 1000)
    private String task;

    private Instant deadline;

    @Column(length = 1000)
    private String expectedOutcome;

    @Column(nullable = false, length = 4000)
    private String originalContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreed_by")
    private User agreedBy;

    private Instant agreedAt;

    protected AgreementLog() {
    }

    public AgreementLog(
            UnderstandingCard card,
            AgreementStatus status,
            User agreedBy,
            Instant agreedAt
    ) {
        this.conversation = card.getConversation();
        this.card = card;
        this.revisionNumber = card.getRevisionNumber();
        this.status = status;
        this.task = card.getTask();
        this.deadline = card.getDeadline();
        this.expectedOutcome = card.getExpectedOutcome();
        this.originalContent = card.getOriginalContent();
        this.agreedBy = agreedBy;
        this.agreedAt = agreedAt;
    }

    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public UnderstandingCard getCard() { return card; }
    public int getRevisionNumber() { return revisionNumber; }
    public AgreementStatus getStatus() { return status; }
    public String getTask() { return task; }
    public Instant getDeadline() { return deadline; }
    public String getExpectedOutcome() { return expectedOutcome; }
    public String getOriginalContent() { return originalContent; }
    public User getAgreedBy() { return agreedBy; }
    public Instant getAgreedAt() { return agreedAt; }
}
