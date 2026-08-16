package com.likelion.asyncalign.user.dto;

import com.likelion.asyncalign.user.domain.WorkRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Schema(example = "이서연")
        @NotBlank @Size(max = 50) String displayName,
        @Schema(description = "GET /users/roles에서 제공하는 역할 코드", example = "PROJECT_MANAGER")
        @NotNull WorkRole role,
        @Schema(description = "role이 OTHER일 때 입력하는 사용자 역할", example = "기술 작가")
        @Size(max = 50) String customRole,
        @Schema(description = "사용 언어 코드", example = "ko", pattern = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$")
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$")
        String preferredLanguage
) {
}
