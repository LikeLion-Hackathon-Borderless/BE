package com.likelion.asyncalign.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkContextRequest(
        @Schema(description = "IANA 타임존 ID", example = "Asia/Seoul")
        @NotBlank @Size(max = 35) String timeZoneId,
        @Schema(example = "09:00:00")
        @NotNull LocalTime workStart,
        @Schema(example = "18:00:00")
        @NotNull LocalTime workEnd,
        @Schema(description = "근무요일", example = "[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"]")
        @NotEmpty Set<DayOfWeek> workDays
) {
}
