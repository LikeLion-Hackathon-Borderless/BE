package com.likelion.asyncalign.auth.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findFirstByEmailOrderByCreatedAtDesc(String email);
    Optional<EmailVerification> findByVerificationToken(UUID verificationToken);
}
