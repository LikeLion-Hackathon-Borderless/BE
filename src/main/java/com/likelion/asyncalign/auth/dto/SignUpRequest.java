package com.likelion.asyncalign.auth.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 50) String displayName,
        @NotBlank @Size(max = 35) String timeZoneId,
        @NotBlank @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$") String preferredLanguage,
        @NotNull LocalTime workStart,
        @NotNull LocalTime workEnd
) {
}
