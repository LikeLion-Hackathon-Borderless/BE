package com.likelion.asyncalign.auth.api;

import com.likelion.asyncalign.auth.application.AuthService;
import com.likelion.asyncalign.auth.application.EmailVerificationService;
import com.likelion.asyncalign.auth.dto.AuthResponse;
import com.likelion.asyncalign.auth.dto.LoginRequest;
import com.likelion.asyncalign.auth.dto.SignUpRequest;
import com.likelion.asyncalign.auth.dto.SendVerificationCodeRequest;
import com.likelion.asyncalign.auth.dto.ConfirmVerificationCodeRequest;
import com.likelion.asyncalign.auth.dto.EmailVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "인증", description = "이메일 인증, 회원가입, 로그인, JWT 발급")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "이메일 회원가입", description = "이메일 인증 완료 토큰을 사용해 계정을 생성하고 JWT를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 또는 이메일 인증 토큰 오류"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
    })
    AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호를 확인하고 JWT를 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "이메일 인증코드 발송", description = "10분 동안 유효한 숫자 6자리 코드를 발송합니다. 재발송 대기시간은 60초입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "발송 성공"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일"),
            @ApiResponse(responseCode = "429", description = "재발송 대기시간 미경과")
    })
    void sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        emailVerificationService.sendCode(request.email());
    }

    @PostMapping("/email-verifications/confirm")
    @Operation(summary = "이메일 인증코드 확인", description = "6자리 코드를 확인하고 회원가입에서 한 번 사용할 인증 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증코드"),
            @ApiResponse(responseCode = "410", description = "인증코드 만료")
    })
    EmailVerificationResponse confirmVerificationCode(@Valid @RequestBody ConfirmVerificationCodeRequest request) {
        return emailVerificationService.confirm(request.email(), request.code());
    }

}
