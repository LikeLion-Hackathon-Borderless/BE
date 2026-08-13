package com.likelion.asyncalign.auth.application;

import java.time.LocalTime;

import com.likelion.asyncalign.auth.dto.AuthResponse;
import com.likelion.asyncalign.auth.dto.LoginRequest;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.user.domain.User;
import com.likelion.asyncalign.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailVerificationService = emailVerificationService;
    }

    public AuthResponse signUp(SignUpRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 사용 중인 이메일입니다.");
        }
        emailVerificationService.consume(email, request.emailVerificationToken());
        User user = User.emailUser(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim());
        return tokenService.issue(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(this::invalidCredentials);
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return tokenService.issue(user);
    }

    public User findOrCreateGoogleUser(String email, String displayName, String pictureUrl) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            User created = User.emailUser(
                    normalizedEmail,
                    passwordEncoder.encode(java.util.UUID.randomUUID().toString()),
                    displayName == null || displayName.isBlank() ? normalizedEmail : displayName.trim());
            if (pictureUrl != null && !pictureUrl.isBlank()) {
                created.updateProfileImageUrl(pictureUrl);
            }
            return userRepository.save(created);
        });
        return user;
    }

    private ApiException invalidCredentials() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
