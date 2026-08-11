package com.likelion.asyncalign.auth.application;

import java.time.ZoneId;

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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse signUp(SignUpRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 사용 중인 이메일입니다.");
        }
        validateTimeZone(request.timeZoneId());
        if (!request.workStart().isBefore(request.workEnd())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "근무 시작 시각은 종료 시각보다 빨라야 합니다.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.timeZoneId(),
                request.preferredLanguage().toLowerCase(),
                request.workStart(),
                request.workEnd());
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

    private void validateTimeZone(String timeZoneId) {
        try {
            ZoneId.of(timeZoneId);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효한 IANA 타임존을 입력해 주세요.");
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
