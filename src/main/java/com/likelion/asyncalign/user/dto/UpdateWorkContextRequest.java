package com.likelion.asyncalign.user.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkContextRequest(
        @NotBlank @Size(max = 35) String timeZoneId,
        @NotNull LocalTime workStart,
        @NotNull LocalTime workEnd,
        @NotEmpty Set<DayOfWeek> workDays
) {
}
