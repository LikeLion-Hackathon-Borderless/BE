package com.likelion.asyncalign.alignment.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "understanding_cards")
public class UnderstandingCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_review_id")
    private AiReview aiReview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_state", nullable = false, length = 20)
    private UnderstandingCardState state = UnderstandingCardState.REVIEW;

    @Column(nullable = false)
    private int revisionNumber = 1;

    @Column(length = 1000)
    private String task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private User assignee;

    private Instant deadline;

    @Column(length = 1000)
    private String expectedOutcome;

    @Column(nullable = false, length = 4000)
    private String originalContent;

    @Column(length = 4000)
    private String translatedContent;

    @Column(nullable = false)
    private boolean needsClarification;

    protected UnderstandingCard() {
    }

    public UnderstandingCard(
            Message message,
            AiReview aiReview,
            User sender,
            User recipient,
            String task,
            User assignee,
            Instant deadline,
            String expectedOutcome,
            String translatedContent,
            boolean needsClarification
    ) {
        this.message = message;
        this.conversation = message.getConversation();
        this.aiReview = aiReview;
        this.sender = sender;
        this.recipient = recipient;
        this.task = task;
        this.assignee = assignee;
        this.deadline = deadline;
        this.expectedOutcome = expectedOutcome;
        this.originalContent = message.getContent();
        this.translatedContent = translatedContent;
        this.needsClarification = needsClarification;
    }

    public void agree() { this.state = UnderstandingCardState.AGREED; }
    public void requestChange() { this.state = UnderstandingCardState.PENDING; }

    public void revise(String task, Instant deadline, String expectedOutcome) {
        this.task = task;
        this.deadline = deadline;
        this.expectedOutcome = expectedOutcome;
        this.revisionNumber++;
        this.state = UnderstandingCardState.REVIEW;
        this.needsClarification = task == null || deadline == null || expectedOutcome == null;
    }

    public UUID getId() { return id; }
    public Message getMessage() { return message; }
    public Conversation getConversation() { return conversation; }
    public AiReview getAiReview() { return aiReview; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public UnderstandingCardState getState() { return state; }
    public int getRevisionNumber() { return revisionNumber; }
    public String getTask() { return task; }
    public User getAssignee() { return assignee; }
    public Instant getDeadline() { return deadline; }
    public String getExpectedOutcome() { return expectedOutcome; }
    public String getOriginalContent() { return originalContent; }
    public String getTranslatedContent() { return translatedContent; }
    public boolean isNeedsClarification() { return needsClarification; }
}
