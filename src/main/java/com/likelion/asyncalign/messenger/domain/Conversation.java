package com.likelion.asyncalign.messenger.domain;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import com.likelion.asyncalign.workspace.domain.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(unique = true, length = 110)
    private String directKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Column(nullable = false)
    private Instant lastMessageAt;

    protected Conversation() {
    }

    private Conversation(ConversationType type, String directKey, Workspace workspace) {
        this.type = type;
        this.directKey = directKey;
        this.workspace = workspace;
        this.lastMessageAt = Instant.now();
    }

    public static Conversation direct(Workspace workspace, UUID firstUserId, UUID secondUserId) {
        return new Conversation(
                ConversationType.DIRECT,
                directKey(workspace.getId(), firstUserId, secondUserId),
                workspace);
    }

    public static String directKey(UUID workspaceId, UUID firstUserId, UUID secondUserId) {
        String first = firstUserId.toString();
        String second = secondUserId.toString();
        String users = first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first;
        return workspaceId + ":" + users;
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

    public Workspace getWorkspace() {
        return workspace;
    }
}
