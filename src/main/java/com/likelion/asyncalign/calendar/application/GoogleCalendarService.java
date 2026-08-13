package com.likelion.asyncalign.calendar.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.asyncalign.calendar.domain.GoogleCalendarConnection;
import com.likelion.asyncalign.calendar.domain.GoogleCalendarConnectionRepository;
import com.likelion.asyncalign.calendar.dto.CalendarConnectionResponse;
import com.likelion.asyncalign.calendar.dto.GoogleCalendarResponse;
import com.likelion.asyncalign.calendar.dto.HolidayEventResponse;
import com.likelion.asyncalign.global.error.ApiException;
import com.likelion.asyncalign.global.error.ErrorCode;
import com.likelion.asyncalign.user.application.UserService;
import com.likelion.asyncalign.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional
public class GoogleCalendarService {

    private static final String CALENDAR_LIST_SCOPE =
            "https://www.googleapis.com/auth/calendar.calendarlist.readonly";
    private static final String EVENTS_SCOPE =
            "https://www.googleapis.com/auth/calendar.events.readonly";

    private final GoogleCalendarConnectionRepository repository;
    private final UserService userService;
    private final TokenCipher tokenCipher;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final String clientId;
    private final String clientSecret;
    private final String callbackUrl;
    private final String successRedirect;

    public GoogleCalendarService(
            GoogleCalendarConnectionRepository repository,
            UserService userService,
            TokenCipher tokenCipher,
            ObjectMapper objectMapper,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret,
            @Value("${app.public-base-url}") String publicBaseUrl,
            @Value("${app.calendar.success-redirect}") String successRedirect
    ) {
        this.repository = repository;
        this.userService = userService;
        this.tokenCipher = tokenCipher;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUrl = publicBaseUrl.replaceAll("/$", "") + "/api/v1/calendar/oauth/callback";
        this.successRedirect = successRedirect;
    }

    public CalendarConnectionResponse createAuthorizationUrl(UUID userId) {
        User user = userService.getUser(userId);
        UUID state = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(600);
        GoogleCalendarConnection connection = repository.findByUserId(userId)
                .orElseGet(() -> new GoogleCalendarConnection(user, state, expiresAt));
        connection.resetState(state, expiresAt);
        repository.save(connection);

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", CALENDAR_LIST_SCOPE + " " + EVENTS_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("include_granted_scopes", "true")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return new CalendarConnectionResponse(
                connection.getConnectedAt() != null,
                authorizationUrl,
                connection.getSelectedCalendarId(),
                connection.getConnectedAt());
    }

    public String handleCallback(UUID state, String code) {
        GoogleCalendarConnection connection = repository.findByOauthState(state)
                .filter(value -> value.getStateExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(ErrorCode.CALENDAR_OAUTH_FAILED, "캘린더 연결 요청이 만료되었거나 올바르지 않습니다."));
        TokenResponse token = exchangeCode(code);
        connection.connect(
                tokenCipher.encrypt(token.accessToken()),
                tokenCipher.encrypt(token.refreshToken()),
                Instant.now().plusSeconds(token.expiresIn()),
                token.scope());
        return UriComponentsBuilder.fromUriString(successRedirect)
                .queryParam("connected", true)
                .build(true)
                .toUriString();
    }

    @Transactional(readOnly = true)
    public CalendarConnectionResponse getStatus(UUID userId) {
        return repository.findByUserId(userId)
                .map(connection -> new CalendarConnectionResponse(
                        connection.getConnectedAt() != null,
                        null,
                        connection.getSelectedCalendarId(),
                        connection.getConnectedAt()))
                .orElse(new CalendarConnectionResponse(false, null, null, null));
    }

    public List<GoogleCalendarResponse> getCalendars(UUID userId) {
        GoogleCalendarConnection connection = connected(userId);
        String accessToken = validAccessToken(connection);
        String body = restClient.get()
                .uri("https://www.googleapis.com/calendar/v3/users/me/calendarList")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(String.class);
        try {
            JsonNode items = objectMapper.readTree(body).path("items");
            List<GoogleCalendarResponse> calendars = new ArrayList<>();
            for (JsonNode item : items) {
                String id = item.path("id").asText();
                calendars.add(new GoogleCalendarResponse(
                        id,
                        item.path("summary").asText(),
                        item.path("primary").asBoolean(false),
                        id.equals(connection.getSelectedCalendarId())));
            }
            return calendars;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.CALENDAR_OAUTH_FAILED, "Google Calendar 목록을 읽지 못했습니다.");
        }
    }

    public CalendarConnectionResponse selectCalendar(UUID userId, String calendarId) {
        GoogleCalendarConnection connection = connected(userId);
        connection.selectCalendar(calendarId);
        return new CalendarConnectionResponse(true, null, calendarId, connection.getConnectedAt());
    }

    public List<HolidayEventResponse> getHolidays(UUID userId, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || to.isAfter(from.plusYears(1))) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "휴일 조회 기간은 시작일 이후 최대 1년이어야 합니다.");
        }
        GoogleCalendarConnection connection = connected(userId);
        if (connection.getSelectedCalendarId() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "휴일 판정에 사용할 캘린더를 먼저 선택해 주세요.");
        }
        String uri = UriComponentsBuilder.fromUriString("https://www.googleapis.com/calendar/v3")
                .pathSegment("calendars", connection.getSelectedCalendarId(), "events")
                .queryParam("timeMin", from.atStartOfDay().toInstant(ZoneOffset.UTC))
                .queryParam("timeMax", to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .queryParam("singleEvents", true)
                .queryParam("orderBy", "startTime")
                .build()
                .encode()
                .toUriString();
        String body = restClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(validAccessToken(connection)))
                .retrieve()
                .body(String.class);
        try {
            List<HolidayEventResponse> holidays = new ArrayList<>();
            for (JsonNode item : objectMapper.readTree(body).path("items")) {
                String startDate = item.path("start").path("date").asText(null);
                String endDate = item.path("end").path("date").asText(null);
                if (startDate != null && endDate != null) {
                    holidays.add(new HolidayEventResponse(
                            item.path("id").asText(),
                            item.path("summary").asText("휴일"),
                            LocalDate.parse(startDate),
                            LocalDate.parse(endDate)));
                }
            }
            return holidays;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.CALENDAR_OAUTH_FAILED, "선택한 캘린더의 휴일을 읽지 못했습니다.");
        }
    }

    private GoogleCalendarConnection connected(UUID userId) {
        return repository.findByUserId(userId)
                .filter(value -> value.getConnectedAt() != null)
                .orElseThrow(() -> new ApiException(ErrorCode.CALENDAR_NOT_CONNECTED, "Google Calendar가 연결되지 않았습니다."));
    }

    private String validAccessToken(GoogleCalendarConnection connection) {
        if (connection.getAccessTokenExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return tokenCipher.decrypt(connection.getEncryptedAccessToken());
        }
        String refreshToken = tokenCipher.decrypt(connection.getEncryptedRefreshToken());
        if (refreshToken == null) {
            throw new ApiException(ErrorCode.CALENDAR_OAUTH_FAILED, "캘린더 권한을 다시 연결해 주세요.");
        }
        TokenResponse refreshed = refresh(refreshToken);
        connection.connect(
                tokenCipher.encrypt(refreshed.accessToken()),
                null,
                Instant.now().plusSeconds(refreshed.expiresIn()),
                refreshed.scope());
        return refreshed.accessToken();
    }

    private TokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", callbackUrl);
        form.add("grant_type", "authorization_code");
        return tokenRequest(form);
    }

    private TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");
        return tokenRequest(form);
    }

    private TokenResponse tokenRequest(MultiValueMap<String, String> form) {
        try {
            return restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.CALENDAR_OAUTH_FAILED, "Google Calendar 권한 교환에 실패했습니다.");
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_token") String refreshToken,
            String scope,
            @JsonProperty("token_type") String tokenType
    ) {
    }
}
