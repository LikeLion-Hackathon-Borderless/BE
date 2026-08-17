package com.likelion.asyncalign.workspace.domain;

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
@Table(name = "workspaces")
public class Workspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 253)
    private String organizationDomain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    protected Workspace() {
    }

    public Workspace(String name, String organizationDomain, User createdBy) {
        this.name = name;
        this.organizationDomain = organizationDomain;
        this.createdBy = createdBy;
    }

    public void softDelete(User user) {
        this.deletedAt = Instant.now();
        this.deletedBy = user;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOrganizationDomain() {
        return organizationDomain;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
