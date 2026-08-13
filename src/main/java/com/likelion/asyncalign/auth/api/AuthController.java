package com.likelion.asyncalign.auth.api;

import com.likelion.asyncalign.auth.application.AuthService;
import com.likelion.asyncalign.auth.application.EmailVerificationService;
import com.likelion.asyncalign.auth.application.OAuthLoginCodeService;
import com.likelion.asyncalign.auth.dto.AuthResponse;
import com.likelion.asyncalign.auth.dto.LoginRequest;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import com.likelion.asyncalign.auth.dto.SendVerificationCodeRequest;
import com.likelion.asyncalign.auth.dto.ConfirmVerificationCodeRequest;
import com.likelion.asyncalign.auth.dto.EmailVerificationResponse;
import com.likelion.asyncalign.auth.dto.OAuthCodeExchangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final OAuthLoginCodeService oauthLoginCodeService;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService,
            OAuthLoginCodeService oauthLoginCodeService
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.oauthLoginCodeService = oauthLoginCodeService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        emailVerificationService.sendCode(request.email());
    }

    @PostMapping("/email-verifications/confirm")
    EmailVerificationResponse confirmVerificationCode(@Valid @RequestBody ConfirmVerificationCodeRequest request) {
        return emailVerificationService.confirm(request.email(), request.code());
    }

    @PostMapping("/oauth/exchange")
    AuthResponse exchangeOAuthCode(@Valid @RequestBody OAuthCodeExchangeRequest request) {
        return oauthLoginCodeService.exchange(request.code());
    }
}
