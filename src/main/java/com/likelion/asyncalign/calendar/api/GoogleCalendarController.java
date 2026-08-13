package com.likelion.asyncalign.calendar.api;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

import com.likelion.asyncalign.calendar.application.GoogleCalendarService;
import com.likelion.asyncalign.calendar.dto.CalendarConnectionResponse;
import com.likelion.asyncalign.calendar.dto.GoogleCalendarResponse;
import com.likelion.asyncalign.calendar.dto.SelectCalendarRequest;
import com.likelion.asyncalign.calendar.dto.HolidayEventResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;

    public GoogleCalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PostMapping("/connection")
    CalendarConnectionResponse connect(@AuthenticationPrincipal Jwt jwt) {
        return calendarService.createAuthorizationUrl(userId(jwt));
    }

    @GetMapping("/connection")
    CalendarConnectionResponse status(@AuthenticationPrincipal Jwt jwt) {
        return calendarService.getStatus(userId(jwt));
    }

    @GetMapping("/oauth/callback")
    void callback(
            @RequestParam UUID state,
            @RequestParam String code,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(calendarService.handleCallback(state, code));
    }

    @GetMapping("/calendars")
    List<GoogleCalendarResponse> calendars(@AuthenticationPrincipal Jwt jwt) {
        return calendarService.getCalendars(userId(jwt));
    }

    @PatchMapping("/selected-calendar")
    CalendarConnectionResponse selectCalendar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SelectCalendarRequest request
    ) {
        return calendarService.selectCalendar(userId(jwt), request.calendarId());
    }

    @GetMapping("/holidays")
    List<HolidayEventResponse> holidays(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return calendarService.getHolidays(userId(jwt), from, to);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
