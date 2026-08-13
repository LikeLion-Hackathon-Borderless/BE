package com.likelion.asyncalign.auth.domain;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.global.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verifications")
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant sentAt;

    private Instant verifiedAt;

    @Column(unique = true)
    private UUID verificationToken;

    private Instant consumedAt;

    @Column(nullable = false)
    private int failedAttempts;

    protected EmailVerification() {
    }

    public EmailVerification(String email, String codeHash, Instant sentAt, Instant expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    public void recordFailure() {
        failedAttempts++;
    }

    public UUID verify(Instant now) {
        this.verifiedAt = now;
        this.verificationToken = UUID.randomUUID();
        return verificationToken;
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public UUID getVerificationToken() { return verificationToken; }
    public Instant getConsumedAt() { return consumedAt; }
    public int getFailedAttempts() { return failedAttempts; }
}
