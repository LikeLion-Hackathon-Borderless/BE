package com.likelion.asyncalign.workspace.domain;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import com.likelion.asyncalign.user.domain.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workspace_members")
public class WorkspaceMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_role", nullable = false, length = 20)
    private WorkspaceRole role;

    @Column(nullable = false)
    private boolean workContextOverridden;

    @Column(length = 35)
    private String timeZoneId;

    private LocalTime workStart;

    private LocalTime workEnd;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "workspace_member_work_days",
            joinColumns = @JoinColumn(name = "workspace_member_id"))
    @Column(name = "day_of_week", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> workDays = new HashSet<>();

    protected WorkspaceMember() {
    }

    public WorkspaceMember(Workspace workspace, User user, WorkspaceRole role) {
        this.workspace = workspace;
        this.user = user;
        this.role = role;
    }

    public void updateWorkContext(
            String timeZoneId,
            LocalTime workStart,
            LocalTime workEnd,
            Set<DayOfWeek> workDays
    ) {
        this.timeZoneId = timeZoneId;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.workDays.clear();
        this.workDays.addAll(workDays);
        this.workContextOverridden = true;
    }

    public void clearWorkContext() {
        this.timeZoneId = null;
        this.workStart = null;
        this.workEnd = null;
        this.workDays.clear();
        this.workContextOverridden = false;
    }

    public UUID getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getUser() {
        return user;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public boolean isWorkContextOverridden() {
        return workContextOverridden;
    }

    public String getEffectiveTimeZoneId() {
        return workContextOverridden ? timeZoneId : user.getTimeZoneId();
    }

    public LocalTime getEffectiveWorkStart() {
        return workContextOverridden ? workStart : user.getWorkStart();
    }

    public LocalTime getEffectiveWorkEnd() {
        return workContextOverridden ? workEnd : user.getWorkEnd();
    }

    public Set<DayOfWeek> getEffectiveWorkDays() {
        return workContextOverridden ? Set.copyOf(workDays) : user.getWorkDays();
    }
}
