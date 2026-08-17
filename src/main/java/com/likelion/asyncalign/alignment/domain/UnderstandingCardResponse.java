package com.likelion.asyncalign.alignment.domain;

import com.likelion.asyncalign.global.persistence.BaseEntity;
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
@Table(name = "understanding_card_responses")
public class UnderstandingCardResponse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private UnderstandingCard card;

    @Column(nullable = false)
    private int revisionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responder_id", nullable = false)
    private User responder;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_type", nullable = false, length = 40)
    private CardResponseType type;

    @Column(length = 1000)
    private String comment;

    private Instant proposedDeadline;

    protected UnderstandingCardResponse() {
    }

    public UnderstandingCardResponse(
            UnderstandingCard card,
            int revisionNumber,
            User responder,
            CardResponseType type,
            String comment,
            Instant proposedDeadline
    ) {
        this.card = card;
        this.revisionNumber = revisionNumber;
        this.responder = responder;
        this.type = type;
        this.comment = comment;
        this.proposedDeadline = proposedDeadline;
    }

    public UUID getId() { return id; }
    public int getRevisionNumber() { return revisionNumber; }
    public User getResponder() { return responder; }
    public CardResponseType getType() { return type; }
    public String getComment() { return comment; }
    public Instant getProposedDeadline() { return proposedDeadline; }
}
