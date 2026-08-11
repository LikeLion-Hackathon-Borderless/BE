package com.likelion.asyncalign.messenger.domain;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    @Column(unique = true, length = 73)
    private String directKey;

    @Column(nullable = false)
    private Instant lastMessageAt;

    protected Conversation() {
    }

    private Conversation(ConversationType type, String directKey) {
        this.type = type;
        this.directKey = directKey;
        this.lastMessageAt = Instant.now();
    }

    public static Conversation direct(UUID firstUserId, UUID secondUserId) {
        return new Conversation(ConversationType.DIRECT, directKey(firstUserId, secondUserId));
    }

    public static String directKey(UUID firstUserId, UUID secondUserId) {
        String first = firstUserId.toString();
        String second = secondUserId.toString();
        return first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first;
    }

    public void touch(Instant sentAt) {
        this.lastMessageAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public ConversationType getType() {
        return type;
    }

    public String getDirectKey() {
        return directKey;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }
}
