package com.likelion.asyncalign.calendar.dto;

import java.time.LocalDate;

public record HolidayEventResponse(String eventId, String title, LocalDate startDate, LocalDate endDateExclusive) {
}
