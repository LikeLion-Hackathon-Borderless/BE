package com.likelion.asyncalign.calendar.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "google_calendar_connections")
public class GoogleCalendarConnection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private UUID oauthState;

    @Column(nullable = false)
    private Instant stateExpiresAt;

    @Column(columnDefinition = "text")
    private String encryptedAccessToken;

    @Column(columnDefinition = "text")
    private String encryptedRefreshToken;

    private Instant accessTokenExpiresAt;

    @Column(length = 500)
    private String grantedScopes;

    @Column(length = 1024)
    private String selectedCalendarId;

    private Instant connectedAt;

    protected GoogleCalendarConnection() {
    }

    public GoogleCalendarConnection(User user, UUID oauthState, Instant stateExpiresAt) {
        this.user = user;
        this.oauthState = oauthState;
        this.stateExpiresAt = stateExpiresAt;
    }

    public void resetState(UUID state, Instant expiresAt) {
        this.oauthState = state;
        this.stateExpiresAt = expiresAt;
    }

    public void connect(String accessToken, String refreshToken, Instant expiresAt, String scopes) {
        this.encryptedAccessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.encryptedRefreshToken = refreshToken;
        }
        this.accessTokenExpiresAt = expiresAt;
        this.grantedScopes = scopes;
        this.connectedAt = Instant.now();
    }

    public void selectCalendar(String calendarId) {
        this.selectedCalendarId = calendarId;
    }

    public User getUser() { return user; }
    public UUID getOauthState() { return oauthState; }
    public Instant getStateExpiresAt() { return stateExpiresAt; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public Instant getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public String getGrantedScopes() { return grantedScopes; }
    public String getSelectedCalendarId() { return selectedCalendarId; }
    public Instant getConnectedAt() { return connectedAt; }
}
