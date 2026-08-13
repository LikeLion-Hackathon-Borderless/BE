package com.likelion.asyncalign.calendar.dto;

import java.time.Instant;

public record CalendarConnectionResponse(
        boolean connected,
        String authorizationUrl,
        String selectedCalendarId,
        Instant connectedAt
) {
}
