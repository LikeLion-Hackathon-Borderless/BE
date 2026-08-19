package com.likelion.asyncalign.auth.application;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.auth.domain.EmailVerification;
import com.likelion.asyncalign.auth.domain.EmailVerificationRepository;
import com.likelion.asyncalign.auth.dto.EmailVerificationResponse;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailVerificationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final EmailVerificationRepository repository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String from;
    private final Duration codeTtl;
    private final Duration resendCooldown;
    private final boolean required;
    private final String testCode;

    public EmailVerificationService(
            EmailVerificationRepository repository,
            UserRepository userRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            @Value("${app.email-verification.from}") String from,
            @Value("${app.email-verification.code-ttl:PT10M}") Duration codeTtl,
            @Value("${app.email-verification.resend-cooldown:PT1M}") Duration resendCooldown,
            @Value("${app.email-verification.required:true}") boolean required,
            @Value("${app.email-verification.test-code:}") String testCode
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.from = from;
        this.codeTtl = codeTtl;
        this.resendCooldown = resendCooldown;
        this.required = required;
        this.testCode = testCode;
    }

    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 가입된 이메일입니다.");
        }

        Instant now = Instant.now();
        repository.findFirstByEmailOrderByCreatedAtDesc(email).ifPresent(latest -> {
            if (latest.getSentAt().plus(resendCooldown).isAfter(now)) {
                throw new ApiException(ErrorCode.VERIFICATION_RESEND_TOO_SOON, "인증 메일은 60초 후 다시 보낼 수 있습니다.");
            }
        });

        String code = testCode.isBlank() ? "%06d".formatted(secureRandom.nextInt(1_000_000)) : testCode;
        repository.save(new EmailVerification(email, passwordEncoder.encode(code), now, now.plus(codeTtl)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[ditto] 이메일 인증번호");
        message.setText("ditto 이메일 인증번호는 " + code + " 입니다. 10분 안에 입력해 주세요.");
        mailSender.send(message);
    }

    public EmailVerificationResponse confirm(String rawEmail, String code) {
        String email = normalize(rawEmail);
        EmailVerification verification = repository.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_VERIFICATION_CODE, "인증 요청을 찾을 수 없습니다."));
        Instant now = Instant.now();
        if (verification.getExpiresAt().isBefore(now)) {
            throw new ApiException(ErrorCode.VERIFICATION_CODE_EXPIRED, "인증번호가 만료되었습니다.");
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS
                || !passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.recordFailure();
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE, "인증번호가 올바르지 않습니다.");
        }
        UUID token = verification.verify(now);
        return new EmailVerificationResponse(token, now);
    }

    public void consume(String rawEmail, UUID token) {
        if (!required) {
            return;
        }
        if (token == null) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_REQUIRED, "이메일 인증이 필요합니다.");
        }
        String email = normalize(rawEmail);
        EmailVerification verification = repository.findByVerificationToken(token)
                .filter(value -> value.getEmail().equals(email))
                .filter(value -> value.getVerifiedAt() != null)
                .filter(value -> value.getConsumedAt() == null)
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_VERIFICATION_REQUIRED, "유효한 이메일 인증이 필요합니다."));
        verification.consume(Instant.now());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
