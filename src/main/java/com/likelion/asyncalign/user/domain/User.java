package com.likelion.asyncalign.user.domain;

import java.time.LocalTime;
import java.time.Instant;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;
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
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WorkRole role;

    @Column(length = 50)
    private String customRole;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private Instant termsAgreedAt;

    @Column(nullable = false, length = 20)
    private String termsVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OnboardingStep onboardingStep = OnboardingStep.PROFILE;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "user_work_days", joinColumns = @jakarta.persistence.JoinColumn(name = "user_id"))
    @Column(name = "day_of_week", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> workDays = new HashSet<>();

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
        this.emailVerified = true;
        this.termsAgreedAt = Instant.now();
        this.termsVersion = "2026-08-13";
    }

    public static User emailUser(String email, String passwordHash, String displayName) {
        User user = new User(
                email,
                passwordHash,
                displayName,
                "UTC",
                "en",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0));
        user.workDays.addAll(Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY));
        return user;
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

    public WorkRole getRole() {
        return role;
    }

    public String getCustomRole() {
        return customRole;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public OnboardingStep getOnboardingStep() {
        return onboardingStep;
    }

    public Set<DayOfWeek> getWorkDays() {
        return Set.copyOf(workDays);
    }

    public void updateProfile(String displayName, WorkRole role, String customRole, String preferredLanguage) {
        this.displayName = displayName;
        this.role = role;
        this.customRole = role == WorkRole.OTHER ? customRole : null;
        this.preferredLanguage = preferredLanguage;
        this.onboardingStep = OnboardingStep.WORK_CONTEXT;
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
        this.onboardingStep = OnboardingStep.WORKSPACE;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void completeOnboarding() {
        this.onboardingStep = OnboardingStep.COMPLETED;
    }
}
