package com.likelion.asyncalign.messenger.domain;

import java.time.Instant;
import java.util.UUID;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "conversation_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversation_member",
                columnNames = {"conversation_id", "user_id"})
)
public class ConversationMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private Instant lastReadAt;

    protected ConversationMember() {
    }

    public ConversationMember(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
        this.lastReadAt = Instant.now();
    }

    public void markRead(Instant readAt) {
        this.lastReadAt = readAt;
    }

    public UUID getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public User getUser() {
        return user;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }
}
