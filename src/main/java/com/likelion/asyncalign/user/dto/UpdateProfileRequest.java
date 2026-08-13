package com.likelion.asyncalign.user.dto;

import com.likelion.asyncalign.user.domain.WorkRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 50) String displayName,
        @NotNull WorkRole role,
        @Size(max = 50) String customRole,
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$")
        String preferredLanguage
) {
}
