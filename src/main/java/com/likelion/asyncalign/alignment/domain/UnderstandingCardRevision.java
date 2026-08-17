package com.likelion.asyncalign.alignment.domain;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import com.likelion.asyncalign.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "understanding_card_revisions")
public class UnderstandingCardRevision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private UnderstandingCard card;

    @Column(nullable = false)
    private int revisionNumber;

    @Column(length = 1000)
    private String task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private User assignee;

    private Instant deadline;

    @Column(length = 1000)
    private String expectedOutcome;

    @Column(length = 1000)
    private String changeNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    protected UnderstandingCardRevision() {
    }

    public UnderstandingCardRevision(
            UnderstandingCard card,
            int revisionNumber,
            String task,
            User assignee,
            Instant deadline,
            String expectedOutcome,
            String changeNote,
            User createdBy
    ) {
        this.card = card;
        this.revisionNumber = revisionNumber;
        this.task = task;
        this.assignee = assignee;
        this.deadline = deadline;
        this.expectedOutcome = expectedOutcome;
        this.changeNote = changeNote;
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public int getRevisionNumber() { return revisionNumber; }
    public String getTask() { return task; }
    public User getAssignee() { return assignee; }
    public Instant getDeadline() { return deadline; }
    public String getExpectedOutcome() { return expectedOutcome; }
    public String getChangeNote() { return changeNote; }
}
