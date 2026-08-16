package com.likelion.asyncalign.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmVerificationCodeRequest(
        @Schema(example = "seoyeon@example.com")
        @NotBlank @Email String email,
        @Schema(description = "숫자 6자리 인증코드", example = "419203", pattern = "^[0-9]{6}$")
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code
) {
}
