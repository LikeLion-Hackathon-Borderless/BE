# 단계별 온보딩 및 외부 연동

## 화면과 API 순서

### 1. 이메일 인증

1. `POST /api/v1/auth/email-verifications`로 이메일을 보냅니다.
2. 사용자가 받은 6자리 코드를 `POST /api/v1/auth/email-verifications/confirm`으로 확인합니다.
3. 응답의 `verificationToken`을 계정 생성 요청에 포함합니다.

인증번호는 10분 동안 유효하고 60초 후 재발송할 수 있으며, 5회 이상 틀리면 새 코드를 받아야 합니다.

### 2. 계정 생성

`POST /api/v1/auth/signup`

```json
{
  "email": "seoyeon@example.com",
  "password": "password123!",
  "displayName": "이서연",
  "emailVerificationToken": "UUID",
  "termsAccepted": true
}
```

응답 JWT를 이후 온보딩 API의 Bearer 토큰으로 사용합니다. Google 가입 사용자는 이메일 인증과 계정 생성 단계를 건너뛰고 같은 프로필 단계로 이동합니다.

### 3. 프로필

`PATCH /api/v1/users/me/profile`

```json
{
  "displayName": "이서연",
  "role": "PROJECT_MANAGER",
  "customRole": null,
  "preferredLanguage": "ko"
}
```

프로필 사진은 `PUT /api/v1/users/me/profile-image`에 `multipart/form-data`의 `file` 필드로 전송합니다. JPG, PNG, WEBP 형식만 허용하며 최대 5MB입니다.

### 4. 근무 컨텍스트

`PATCH /api/v1/users/me/work-context`

```json
{
  "timeZoneId": "Asia/Seoul",
  "workStart": "09:00:00",
  "workEnd": "18:00:00",
  "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
}
```

응답의 `onboardingStep`이 다음 화면을 결정합니다: `PROFILE → WORK_CONTEXT → WORKSPACE → COMPLETED`.

## 실제 이메일 발송 설정

Google OAuth Client와 메일 발송 권한은 별개입니다. Gmail SMTP를 사용할 경우 발송 계정에서 2단계 인증을 켜고 앱 비밀번호를 발급한 뒤 다음 환경변수를 설정합니다.

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=sender@gmail.com
MAIL_PASSWORD=16자리_앱_비밀번호
MAIL_FROM=sender@gmail.com
```

운영 발송량이 커지면 Gmail 대신 AWS SES, SendGrid, Mailgun 같은 트랜잭션 메일 서비스로 교체하는 편이 안정적입니다.

## Google Calendar 연결

Google Cloud Console에서 Google Calendar API를 활성화하고 OAuth 동의 화면에 다음 범위를 추가합니다.

- `https://www.googleapis.com/auth/calendar.calendarlist.readonly`
- `https://www.googleapis.com/auth/calendar.events.readonly`

OAuth Web Client의 승인된 리디렉션 URI에는 두 주소가 필요합니다.

```text
https://api.example.com/login/oauth2/code/google
https://api.example.com/api/v1/calendar/oauth/callback
```

연결 흐름:

1. 로그인한 사용자가 `POST /api/v1/calendar/connection`을 호출합니다.
2. 응답의 `authorizationUrl`로 브라우저를 이동시킵니다.
3. 사용자가 캘린더 읽기 권한에 동의하면 Google이 백엔드 콜백으로 이동합니다.
4. 백엔드는 암호화한 access/refresh token을 저장하고 프론트 설정 화면으로 이동합니다.
5. `GET /api/v1/calendar/calendars`로 목록을 조회합니다.
6. `PATCH /api/v1/calendar/selected-calendar`로 휴일 판정에 사용할 캘린더를 선택합니다.
7. `GET /api/v1/calendar/holidays?from=2026-08-01&to=2026-08-31`로 선택한 캘린더의 종일 일정을 휴일로 조회합니다.

Google OAuth 앱이 `Testing` 상태이면 등록된 테스트 사용자만 연결할 수 있고, Calendar 범위가 포함된 refresh token은 테스트 상태에서 제한된 수명을 가질 수 있습니다. 공개 사용자에게 배포하려면 OAuth 앱 게시와 필요한 검증 절차를 확인해야 합니다.

## Docker 배포 주소

컨테이너 내부의 `8080`은 외부 접근 가능 여부와 무관합니다. 호스트 포트 또는 HTTPS 리버스 프록시로 공개하고 아래 값을 실제 주소로 설정해야 합니다.

```env
PUBLIC_BASE_URL=https://api.example.com
CORS_ALLOWED_ORIGINS=https://app.example.com
OAUTH2_SUCCESS_REDIRECT=https://app.example.com/oauth/callback
CALENDAR_SUCCESS_REDIRECT=https://app.example.com/settings/calendar
```

Google 로그인 성공 시 JWT를 URL에 직접 싣지 않습니다. 프론트는 콜백의 60초짜리 `code`를 `POST /api/v1/auth/oauth/exchange`에 보내 JWT로 한 번만 교환합니다.

## 프로필 파일 보존

현재 Docker Compose는 `async-align-uploads` 볼륨에 프로필 이미지를 보존합니다. 단일 서버 MVP에는 충분하지만, 여러 인스턴스를 운영하거나 클라우드에서 무중단 배포하려면 저장 구현을 S3 호환 객체 스토리지로 교체해야 합니다.
