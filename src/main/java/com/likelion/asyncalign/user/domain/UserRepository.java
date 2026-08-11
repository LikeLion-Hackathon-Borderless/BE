package com.likelion.asyncalign.user.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String displayName,
            String email,
            Pageable pageable
    );
}
