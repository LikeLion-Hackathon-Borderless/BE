package com.likelion.asyncalign.auth.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, UUID> {
    Optional<OAuthLoginCode> findByCode(UUID code);
}
