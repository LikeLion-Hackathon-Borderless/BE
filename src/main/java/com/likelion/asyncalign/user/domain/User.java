package com.likelion.asyncalign.user.domain;

import java.time.LocalTime;
import java.util.UUID;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String displayName;

    @Column(nullable = false, length = 35)
    private String timeZoneId;

    @Column(nullable = false, length = 10)
    private String preferredLanguage;

    @Column(nullable = false)
    private LocalTime workStart;

    @Column(nullable = false)
    private LocalTime workEnd;

    @Column(nullable = false)
    private boolean enabled = true;

    protected User() {
    }

    public User(
            String email,
            String passwordHash,
            String displayName,
            String timeZoneId,
            String preferredLanguage,
            LocalTime workStart,
            LocalTime workEnd
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.timeZoneId = timeZoneId;
        this.preferredLanguage = preferredLanguage;
        this.workStart = workStart;
        this.workEnd = workEnd;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
