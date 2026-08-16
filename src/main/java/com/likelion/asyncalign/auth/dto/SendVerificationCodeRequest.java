package com.likelion.asyncalign.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendVerificationCodeRequest(
        @Schema(description = "인증코드를 받을 이메일", example = "seoyeon@example.com")
        @NotBlank @Email @Size(max = 320) String email
) {
}
