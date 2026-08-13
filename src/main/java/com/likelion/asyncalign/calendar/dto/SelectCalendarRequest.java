package com.likelion.asyncalign.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SelectCalendarRequest(@NotBlank @Size(max = 1024) String calendarId) {
}
