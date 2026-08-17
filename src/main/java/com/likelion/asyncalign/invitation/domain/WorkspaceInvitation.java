package com.likelion.asyncalign.invitation.domain;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.workspace.domain.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_invitations")
public class WorkspaceInvitation extends BaseEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @Column(length = 320)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_type", nullable = false, length = 20)
    private InvitationType type;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private User acceptedBy;

    private Instant acceptedAt;

    protected WorkspaceInvitation() {
    }

    public WorkspaceInvitation(
            Workspace workspace,
            User inviter,
            String invitedEmail,
            InvitationType type,
            String tokenHash,
            Instant expiresAt
    ) {
        this.workspace = workspace;
        this.inviter = inviter;
        this.invitedEmail = invitedEmail;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsable(Instant now) {
        return status == InvitationStatus.PENDING && !isExpired(now);
    }

    public void accept(User user, Instant now) {
        if (type == InvitationType.EMAIL) {
            this.status = InvitationStatus.ACCEPTED;
            this.acceptedBy = user;
            this.acceptedAt = now;
        }
    }

    public void revoke() {
        if (status == InvitationStatus.PENDING) {
            this.status = InvitationStatus.REVOKED;
        }
    }

    public void initializeTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public User getInviter() { return inviter; }
    public String getInvitedEmail() { return invitedEmail; }
    public InvitationType getType() { return type; }
    public InvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public User getAcceptedBy() { return acceptedBy; }
}
