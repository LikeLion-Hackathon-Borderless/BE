package com.likelion.asyncalign.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.util.UUID;

public record SignUpRequest(
        @Schema(description = "가입 이메일", example = "seoyeon@example.com")
        @NotBlank @Email @Size(max = 320) String email,
        @Schema(description = "8~72자 비밀번호", example = "password123!")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "화면 표시 이름", example = "이서연")
        @NotBlank @Size(max = 50) String displayName,
        @Schema(description = "이메일 인증 완료 후 발급된 일회용 토큰")
        UUID emailVerificationToken,
        @Schema(description = "이용약관 동의 여부", example = "true")
        @NotNull @AssertTrue Boolean termsAccepted
) {
}
