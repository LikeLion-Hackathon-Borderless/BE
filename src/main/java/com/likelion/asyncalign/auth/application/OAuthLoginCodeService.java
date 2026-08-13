package com.likelion.asyncalign.auth.application;

import java.time.Instant;
import java.util.UUID;

import com.likelion.asyncalign.auth.domain.OAuthLoginCode;
import com.likelion.asyncalign.auth.domain.OAuthLoginCodeRepository;
import com.likelion.asyncalign.auth.dto.AuthResponse;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OAuthLoginCodeService {

    private final OAuthLoginCodeRepository repository;
    private final TokenService tokenService;

    public OAuthLoginCodeService(OAuthLoginCodeRepository repository, TokenService tokenService) {
        this.repository = repository;
        this.tokenService = tokenService;
    }

    public UUID create(User user) {
        return repository.save(new OAuthLoginCode(user)).getCode();
    }

    public AuthResponse exchange(UUID code) {
        OAuthLoginCode loginCode = repository.findByCode(code)
                .filter(value -> value.getConsumedAt() == null)
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "로그인 코드가 만료되었거나 올바르지 않습니다."));
        loginCode.consume();
        return tokenService.issue(loginCode.getUser());
    }
}
