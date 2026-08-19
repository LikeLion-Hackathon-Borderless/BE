# ditto API 명세서

> 버전: v1.0 / 기준일: 2026-08-17<br>
> 대상: Spring Boot 3.5, PostgreSQL, JWT Bearer 인증<br>
> Base URL: `{BACKEND_ORIGIN}/api/v1`

## 문서 사용 방법

| 문서 | 주소 | 포함 범위 |
| --- | --- | --- |
| Swagger UI | `/swagger-ui.html` | 현재 백엔드에 구현되어 실제 호출 가능한 API |
| OpenAPI JSON | `/v3/api-docs` | OpenAPI 3.0 형식의 기계 판독용 명세 |
| OpenAPI YAML | `/v3/api-docs.yaml` | OpenAPI 3.0 YAML 명세 |
| 이 문서 | `docs/API.md` | 현재 API와 앞으로 구현할 MVP 계약 및 업무 흐름 |

Swagger에서 `Authorize`를 누르고 로그인 응답의 `accessToken`만 입력하면 인증 API를 호출할 수 있다. 아래 MVP API 38개는 모두 구현되어 Swagger에서 직접 호출할 수 있다.

### 현재 전달 상태

| 구분 | API 수 | 프론트 사용 방법 |
| --- | ---: | --- |
| 현재 구현 | 38 | 인증부터 합의 기록까지 실제 서버와 Swagger에서 호출 |

API 명세는 구현 완료 후 작성하는 결과물이 아니라 구현 전에 프론트와 합의하는 계약이다. 구현 과정에서 계약이 바뀌면 문서 버전을 올리고 변경 내용을 공유한다.

## 1. 상태 표기와 MVP 범위

| 상태 | 의미 |
| --- | --- |
| 구현 완료 | 현재 코드에서 호출 가능하며 통합 테스트와 Swagger에 포함됨 |

### 1.1 기능 우선순위

| 우선순위 | 기능 |
| --- | --- |
| P0 | 이메일 인증·이메일/비밀번호 로그인·프로필·근무 컨텍스트 |
| P0 | 워크스페이스 생성/선택·멤버 초대 |
| P0 | 1:1 DM·첨부파일·읽음 처리 |
| P0 | AI 검토 → 확정 전송 → 공통 이해 카드 → 수신자 응답 |
| P0 | 합의 기록 조회·“이해 돕기” |
| P1 | 워크스페이스별 근무 컨텍스트 예외 |
| P1 | 예약 전송·초대 재전송/취소·실시간 WebSocket/SSE |

MVP에서는 채널/그룹 채팅, 메시지 수정·삭제, 이모지 반응, 스레드, 전문 검색은 제외한다.

---

## 2. 공통 규칙

### 2.1 인증

인증이 필요한 요청에는 다음 헤더를 보낸다.

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

- 액세스 토큰은 JWT이며 `AuthResponse.expiresAt`까지 유효하다.
- 별도 표기가 없는 `/api/v1/**` API는 인증이 필요하다.
- 공개 API: 이메일 인증 발송/확인, 회원가입, 로그인, 역할 목록, 초대 링크 미리보기.
- 토큰 갱신 API는 현재 범위에 없다. 만료 시 다시 로그인한다.

### 2.2 응답 형식

현재 구현과 호환하기 위해 성공 응답을 별도 `data` envelope로 감싸지 않는다. 생성은 `201 Created`, 정상 조회/수정은 `200 OK`, 응답 본문이 없으면 `204 No Content`를 사용한다.

### 2.3 날짜·시간

- 절대 시각: ISO-8601 UTC `Instant`, 예: `2026-08-14T09:00:00Z`
- 사용자 표시 시각: ISO-8601 offset 포함, 예: `2026-08-14T18:00:00+09:00`
- 날짜: `YYYY-MM-DD`
- 시각: `HH:mm:ss`
- 타임존: IANA Zone ID만 허용, 예: `Asia/Seoul`, `America/Los_Angeles`
- DST 계산은 서버의 IANA 타임존 데이터로 처리한다. `UTC+9` 같은 고정 오프셋은 저장하지 않는다.
- `workDays`: `MONDAY`부터 `SUNDAY`까지의 enum 배열이다.

### 2.4 식별자와 페이지네이션

- 리소스 ID는 UUID 문자열이다.
- 메시지/로그는 cursor pagination을 사용한다.
- `before`를 생략하면 최신 데이터부터 조회한다.
- 기본 `size=50`, 최대 `size=100`이다.
- 목록은 화면 출력에 편하도록 과거→현재 순으로 반환하며, 다음 과거 페이지가 있으면 `nextBefore`를 제공한다.

### 2.5 공통 오류 응답

```json
{
  "timestamp": "2026-08-14T09:00:00Z",
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": {
    "content": "공백일 수 없습니다."
  }
}
```

`fieldErrors`가 없으면 빈 객체 `{}`를 반환한다. 프론트는 HTTP status가 아니라 `code`를 기준으로 사용자 메시지를 분기한다.

JWT가 없거나 유효하지 않은 경우에도 같은 형식으로 `INVALID_CREDENTIALS(401)`를 반환한다. 인증은 되었지만 리소스 권한이 없으면 `ACCESS_DENIED(403)`를 반환한다.

---

## 3. 전체 엔드포인트 목록

### 3.1 인증·사용자·온보딩

| 상태 | Method | Path | 설명 |
| --- | --- | --- | --- |
| 구현 완료 | POST | `/auth/email-verifications` | 6자리 이메일 인증코드 발송 |
| 구현 완료 | POST | `/auth/email-verifications/confirm` | 코드 확인 및 회원가입용 토큰 발급 |
| 구현 완료 | POST | `/auth/signup` | 이메일 회원가입 |
| 구현 완료 | POST | `/auth/login` | 이메일 로그인 |
| 구현 완료 | GET | `/users/me` | 내 계정 및 온보딩 상태 조회 |
| 구현 완료 | GET | `/users/roles` | 역할 목록 조회 |
| 구현 완료 | PATCH | `/users/me/profile` | 이름·역할·사용 언어 저장 |
| 구현 완료 | PUT | `/users/me/profile-image` | 프로필 이미지 업로드 |
| 구현 완료 | PATCH | `/users/me/work-context` | 계정 기본 근무 컨텍스트 저장 |
| 구현 완료 | GET | `/users` | 이름/이메일로 DM 상대 검색 |

### 3.2 인증 범위

이메일 인증과 이메일/비밀번호 로그인만 제공한다. 소셜 로그인과 외부 캘린더 연동은 MVP 범위에 포함하지 않는다.

### 3.3 워크스페이스·초대

| 상태 | Method | Path | 설명 |
| --- | --- | --- | --- |
| 구현 완료 | POST | `/workspaces` | 워크스페이스 생성 |
| 구현 완료 | GET | `/workspaces` | 내 워크스페이스 목록 |
| 구현 완료 | GET | `/workspaces/{workspaceId}` | 워크스페이스 상세 |
| 구현 완료 | GET | `/workspaces/{workspaceId}/members` | 멤버 목록 |
| 구현 완료 | DELETE | `/workspaces/{workspaceId}` | OWNER의 워크스페이스 소프트 삭제 |
| 구현 완료 | POST | `/workspaces/{workspaceId}/invitations` | 이메일 다중 초대 |
| 구현 완료 | POST | `/workspaces/{workspaceId}/invitation-links` | 공유 초대 링크 생성/재발급 |
| 구현 완료 | GET | `/workspace-invitations/{token}` | 초대 내용 미리보기(공개) |
| 구현 완료 | POST | `/workspace-invitations/{token}/accept` | 로그인 사용자의 초대 수락 |
| 구현 완료 | PUT | `/workspaces/{workspaceId}/members/me/work-context` | 워크스페이스 근무 예외 저장 |
| 구현 완료 | DELETE | `/workspaces/{workspaceId}/members/me/work-context` | 예외 제거 후 계정 기본값 상속 |

### 3.4 대화·메시지·파일

| 상태 | Method | Path | 설명 |
| --- | --- | --- | --- |
| 구현 완료 | POST | `/conversations/direct` | 워크스페이스 1:1 DM 생성/기존 DM 반환 |
| 구현 완료 | GET | `/conversations` | 내 DM 목록 |
| 구현 완료 | PUT | `/conversations/{conversationId}/read` | 현재까지 읽음 처리 |
| 구현 완료 | GET | `/conversations/{conversationId}/messages` | 메시지 조회 |
| 구현 완료 | POST | `/conversations/{conversationId}/messages` | 일반/예약 메시지 전송 |
| 구현 완료 | POST | `/conversations/{conversationId}/attachments` | 첨부파일 업로드 |
| 구현 완료 | GET | `/attachments/{attachmentId}` | 첨부 메타데이터 및 처리 상태 |
| 구현 완료 | GET | `/attachments/{attachmentId}/content` | 권한 확인 후 파일 다운로드 |

### 3.5 AI 검토·공통 이해·합의

| 상태 | Method | Path | 설명 |
| --- | --- | --- | --- |
| 구현 완료 | POST | `/conversations/{conversationId}/ai-reviews` | 초안과 파일의 AI 검토 실행 |
| 구현 완료 | GET | `/ai-reviews/{reviewId}` | AI 검토 결과/상태 조회 |
| 구현 완료 | POST | `/ai-reviews/{reviewId}/answers` | AI 모호성 질문에 답하고 세션 재개 |
| 구현 완료 | PATCH | `/ai-reviews/{reviewId}` | 추출 결과를 사용자가 수정·확정 |
| 구현 완료 | POST | `/ai-reviews/{reviewId}/send` | 확정된 검토 결과와 메시지 전송 |
| 구현 완료 | POST | `/messages/{messageId}/understanding-cards` | 수신자가 “이해 돕기” 생성 |
| 구현 완료 | GET | `/understanding-cards/{cardId}` | 공통 이해 카드 조회 |
| 구현 완료 | POST | `/understanding-cards/{cardId}/responses` | 동의/기한 조정/설명 요청 |
| 구현 완료 | POST | `/understanding-cards/{cardId}/revisions` | 발신자가 카드 수정본 제출 |
| 구현 완료 | GET | `/conversations/{conversationId}/agreement-logs` | 대화의 합의 기록 조회 |

---

## 4. 인증 API

### 4.1 이메일 인증코드 발송

`POST /api/v1/auth/email-verifications`

```json
{
  "email": "seoyeon@example.com"
}
```

- 성공: `204 No Content`
- 코드는 숫자 6자리, 유효기간 10분, 재발송 쿨다운 60초다.
- 동일 이메일의 이전 코드는 새 코드 발송 시 무효화한다.
- 보안상 이메일 존재 여부와 무관하게 성공 메시지는 동일하게 보여주는 것을 권장한다.

주요 오류: `INVALID_REQUEST(400)`, `EMAIL_ALREADY_EXISTS(409)`, `VERIFICATION_RESEND_TOO_SOON(429)`.

### 4.2 이메일 인증코드 확인

`POST /api/v1/auth/email-verifications/confirm`

```json
{
  "email": "seoyeon@example.com",
  "code": "419203"
}
```

`200 OK`

```json
{
  "verificationToken": "f027ed9a-18ee-4d1a-a1ee-6c02dcf85643",
  "verifiedAt": "2026-08-14T09:00:00Z"
}
```

`verificationToken`은 해당 이메일의 회원가입에서 한 번만 사용한다. 5회 오입력 시 해당 코드를 폐기한다.

주요 오류: `INVALID_VERIFICATION_CODE(400)`, `VERIFICATION_CODE_EXPIRED(410)`.

### 4.3 이메일 회원가입

`POST /api/v1/auth/signup`

```json
{
  "email": "seoyeon@example.com",
  "password": "password123!",
  "displayName": "이서연",
  "emailVerificationToken": "f027ed9a-18ee-4d1a-a1ee-6c02dcf85643",
  "termsAccepted": true
}
```

- `email`: 최대 320자
- `password`: 8~72자
- `displayName`: 최대 50자
- `termsAccepted`: 반드시 `true`

성공: `201 Created`, 응답은 `AuthResponse`다.

### 4.4 로그인

`POST /api/v1/auth/login`

```json
{
  "email": "seoyeon@example.com",
  "password": "password123!"
}
```

`200 OK`

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-14T10:00:00Z",
  "user": {
    "id": "19332fd9-6261-4fd4-8c9e-e71602bad19d",
    "email": "seoyeon@example.com",
    "displayName": "이서연",
    "role": "PROJECT_MANAGER",
    "customRole": null,
    "profileImageUrl": "/uploads/profiles/19332fd9.webp",
    "timeZoneId": "Asia/Seoul",
    "preferredLanguage": "ko",
    "workStart": "09:00:00",
    "workEnd": "18:00:00",
    "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
    "emailVerified": true,
    "onboardingStep": "WORKSPACE"
  }
}
```

주요 오류: `INVALID_CREDENTIALS(401)`.

---

## 5. 사용자·한 페이지 온보딩 API

와이어프레임은 한 화면의 큰 모달 안에 모든 필드를 세로로 배치한다. 이것은 프론트 UI 방식이며, 백엔드는 재시도와 부분 저장을 위해 아래 API를 순서대로 호출한다.

```text
회원가입/로그인 → 프로필 저장 → 이미지 업로드(선택)
→ 근무 컨텍스트 저장 → 워크스페이스 생성/선택 → 초대(선택) → 완료
```

페이지를 새로 열면 `GET /users/me`의 `onboardingStep`으로 이어서 진행한다.

### 5.1 내 정보 조회

`GET /api/v1/users/me`

응답은 4.4의 `user` 객체와 동일하다.

`onboardingStep`:

| 값 | 의미 |
| --- | --- |
| `PROFILE` | 이름·역할·언어 필요 |
| `WORK_CONTEXT` | 타임존·근무시간·근무요일 필요 |
| `WORKSPACE` | 워크스페이스 생성 또는 가입 필요 |
| `COMPLETED` | 온보딩 완료 |

### 5.2 역할 목록

`GET /api/v1/users/roles`

`200 OK`

```json
[
  {"code": "DEVELOPER", "label": "개발자", "customInputRequired": false},
  {"code": "PROJECT_MANAGER", "label": "프로젝트 매니저(PM)", "customInputRequired": false},
  {"code": "PRODUCT_MANAGER", "label": "프로덕트 매니저", "customInputRequired": false},
  {"code": "DESIGNER", "label": "디자이너", "customInputRequired": false},
  {"code": "MARKETER", "label": "마케팅", "customInputRequired": false},
  {"code": "DATA_ANALYST", "label": "데이터 분석가", "customInputRequired": false},
  {"code": "QA_ENGINEER", "label": "QA 엔지니어", "customInputRequired": false},
  {"code": "SALES", "label": "영업", "customInputRequired": false},
  {"code": "CUSTOMER_SUCCESS", "label": "고객 성공/고객 지원", "customInputRequired": false},
  {"code": "HR", "label": "인사", "customInputRequired": false},
  {"code": "OPERATIONS", "label": "운영", "customInputRequired": false},
  {"code": "OTHER", "label": "기타", "customInputRequired": true}
]
```

### 5.3 프로필 저장

`PATCH /api/v1/users/me/profile`

```json
{
  "displayName": "이서연",
  "role": "PROJECT_MANAGER",
  "customRole": null,
  "preferredLanguage": "ko"
}
```

- `preferredLanguage`: BCP-47 간소형 언어 코드, 예: `ko`, `en`, `en-US`
- `role=OTHER`이면 `customRole` 필수, 그 외에는 `null`
- 성공: 갱신된 `UserResponse`

### 5.4 프로필 이미지 업로드

`PUT /api/v1/users/me/profile-image`

```http
Content-Type: multipart/form-data

file: (binary)
```

- 허용: JPG/JPEG, PNG, WEBP
- 최대: 5MB
- 성공: 갱신된 `UserResponse`
- 오류: `FILE_UPLOAD_FAILED(400)`
- `profileImageUrl`은 `/uploads/profiles/...` 형태의 상대경로다. 프론트는 API Base URL의 `/api/v1`이 아니라 `{BACKEND_ORIGIN}`을 앞에 붙여 표시한다.

### 5.5 계정 기본 근무 컨텍스트 저장

`PATCH /api/v1/users/me/work-context`

```json
{
  "timeZoneId": "Asia/Seoul",
  "workStart": "09:00:00",
  "workEnd": "18:00:00",
  "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
}
```

- `workStart < workEnd`여야 한다. 야간 교대근무는 MVP에서 제외한다.
- 성공: 갱신된 `UserResponse`
- 설정 변경은 이후 생성되는 AI 검토/카드부터 적용한다. 기존 합의 기록의 시간 표시는 생성 당시 스냅샷을 유지한다.

### 5.6 사용자 검색

현재 계약:

`GET /api/v1/users?workspaceId={workspaceId}&query=alex&size=20`

이름 또는 이메일을 검색하며 현재 사용자는 결과에서 제외한다. `workspaceId`에 소속된 멤버만 반환하고 비멤버가 요청하면 `WORKSPACE_ACCESS_DENIED`를 반환한다.

검색 결과는 다른 사용자에게 공개 가능한 `UserSummaryResponse`만 반환한다.

```json
[
  {
    "id": "e54839db-5a97-433f-bec9-35d85cc0ea12",
    "displayName": "Alex",
    "profileImageUrl": null,
    "role": "DEVELOPER",
    "customRole": null,
    "timeZoneId": "America/Los_Angeles",
    "preferredLanguage": "en"
  }
]
```

이메일, 근무시간, 근무요일, 이메일 인증 여부, 온보딩 상태는 검색 및 대화 상대 응답에 포함하지 않는다. 전체 `UserResponse`는 본인 조회와 본인 설정 변경 API에서만 반환한다.

---

## 6. JWT 인증 사용

로그인 또는 회원가입 응답의 `accessToken`을 인증이 필요한 API에 전달한다.

```http
Authorization: Bearer eyJ...
```

서버는 세션을 생성하지 않으며 JWT가 만료되면 다시 로그인한다. Refresh Token은 현재 MVP 범위에 포함하지 않는다.

---

## 7. 워크스페이스·초대 API

### 7.1 워크스페이스 생성

`POST /api/v1/workspaces`

```json
{
  "name": "Global Async Team",
  "organizationDomain": "company.com"
}
```

- `name`: 1~80자, 필수
- `organizationDomain`: 선택, URL이 아니라 도메인만 입력
- 생성자는 `OWNER` 멤버가 된다.

`201 Created`

```json
{
  "id": "4fa49159-fad4-4937-93ee-a0e5d514933c",
  "name": "Global Async Team",
  "organizationDomain": "company.com",
  "myMembershipRole": "OWNER",
  "memberCount": 1,
  "createdAt": "2026-08-14T09:00:00Z"
}
```

### 7.2 내 워크스페이스 목록

`GET /api/v1/workspaces`

```json
[
  {
    "id": "4fa49159-fad4-4937-93ee-a0e5d514933c",
    "name": "Global Async Team",
    "organizationDomain": "company.com",
    "myMembershipRole": "OWNER",
    "memberCount": 4,
    "createdAt": "2026-08-14T09:00:00Z"
  }
]
```

현재 선택된 워크스페이스는 서버 계정 설정에 저장하지 않는다. 프론트가 URL, 전역 상태 또는 `localStorage`에 `workspaceId`를 저장하고 워크스페이스 범위 API마다 명시적으로 전달한다.

워크스페이스를 전환하면 다음 데이터를 새 `workspaceId`로 다시 조회한다.

- 워크스페이스 상세와 멤버
- 대화방 목록
- 사용자 검색 결과
- 워크스페이스 근무 컨텍스트 예외

### 7.3 워크스페이스 상세와 멤버

`GET /api/v1/workspaces/{workspaceId}`

`200 OK`

```json
{
  "id": "4fa49159-fad4-4937-93ee-a0e5d514933c",
  "name": "Global Async Team",
  "organizationDomain": "company.com",
  "myMembershipRole": "OWNER",
  "memberCount": 4,
  "createdAt": "2026-08-14T09:00:00Z"
}
```

`GET /api/v1/workspaces/{workspaceId}/members`

`200 OK`

```json
[
  {
    "membershipId": "ea16a448-107a-410f-84ac-f4d69040db19",
    "membershipRole": "MEMBER",
    "joinedAt": "2026-08-14T09:30:00Z",
    "user": {
      "id": "e54839db-5a97-433f-bec9-35d85cc0ea12",
      "email": "alex@company.com",
      "displayName": "Alex",
      "profileImageUrl": null,
      "workRole": "DEVELOPER",
      "customRole": null,
      "timeZoneId": "America/Los_Angeles",
      "preferredLanguage": "en"
    },
    "workContext": {
      "overridden": false,
      "timeZoneId": "America/Los_Angeles",
      "workStart": "09:00:00",
      "workEnd": "18:00:00",
      "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
    }
  }
]
```

워크스페이스 멤버 목록은 이름 오름차순으로 반환한다. MVP에서는 별도 페이지네이션 없이 최대 100명까지 반환한다.

권한:

| 기능 | OWNER | MEMBER |
| --- | --- | --- |
| 워크스페이스 조회 | 가능 | 가능 |
| 멤버 조회·DM 생성 | 가능 | 가능 |
| 이메일 초대·초대 링크 생성 | 가능 | 불가 |
| 워크스페이스 이름 변경 | 가능 | 불가 |
| 워크스페이스 삭제 | 가능 | 불가 |
| 개인 근무 컨텍스트 예외 | 가능 | 가능 |

MVP에서는 소유권 이전과 멤버 강제 퇴장을 제공하지 않는다. 워크스페이스 삭제는 OWNER만 가능하다.

### 7.4 워크스페이스 삭제

`DELETE /api/v1/workspaces/{workspaceId}`

- `OWNER`만 호출할 수 있다. `MEMBER`는 `WORKSPACE_OWNER_REQUIRED(403)`을 반환한다.
- 프론트는 삭제 버튼을 누른 뒤 워크스페이스 이름을 다시 입력받아 일치할 때만 요청한다.
- 서버는 행을 물리적으로 지우지 않고 `deletedAt`, `deletedBy`를 기록하는 소프트 삭제를 수행한다.
- 활성 이메일 초대와 공유 초대 링크를 즉시 무효화한다.
- 멤버, 대화, 메시지, 첨부파일, AI 검토, 카드, 합의 기록은 보존하지만 모든 일반 조회와 접근을 차단한다.
- 삭제된 워크스페이스는 목록에서 제외하고 워크스페이스 범위 API에서는 찾을 수 없는 것으로 처리한다.
- 복구 기능은 MVP에서 제공하지 않는다.

성공: `204 No Content`.

주요 오류: `WORKSPACE_NOT_FOUND(404)`, `WORKSPACE_OWNER_REQUIRED(403)`, `WORKSPACE_ALREADY_DELETED(409)`.

권장 DB 필드:

```text
workspaces.deleted_at  timestamptz nullable
workspaces.deleted_by uuid nullable
```

### 7.5 이메일 다중 초대

`POST /api/v1/workspaces/{workspaceId}/invitations`

```json
{
  "emails": ["alex@company.com", "priya@company.com"]
}
```

- 최대 20개, 중복 이메일은 서버에서 제거한다.
- 프론트는 쉼표/Enter 입력 시 이메일 형식을 즉시 검사한다.
- 서버는 일부 주소 발송 실패 때문에 전체 요청을 롤백하지 않고 주소별 결과를 반환한다.

`200 OK`

```json
{
  "results": [
    {"email": "alex@company.com", "status": "SENT", "errorCode": null},
    {"email": "priya@company.com", "status": "ALREADY_INVITED", "errorCode": null}
  ]
}
```

상태: `SENT`, `ALREADY_INVITED`, `ALREADY_MEMBER`, `FAILED`.

### 7.6 공유 초대 링크

`POST /api/v1/workspaces/{workspaceId}/invitation-links`

```json
{
  "expiresInDays": 7,
  "regenerate": false
}
```

`201 Created`

```json
{
  "token": "wsi_2Uuk...",
  "inviteUrl": "https://app.example.com/invitations/wsi_2Uuk...",
  "expiresAt": "2026-08-21T09:00:00Z"
}
```

`regenerate=true`이면 기존 활성 링크를 폐기하고 새 링크를 만든다.

### 7.7 초대 미리보기와 수락

`GET /api/v1/workspace-invitations/{token}` — 인증 불필요

```json
{
  "workspaceId": "4fa49159-fad4-4937-93ee-a0e5d514933c",
  "workspaceName": "Global Async Team",
  "inviterDisplayName": "이서연",
  "invitedEmail": "alex@company.com",
  "expiresAt": "2026-08-21T09:00:00Z",
  "status": "PENDING"
}
```

`POST /api/v1/workspace-invitations/{token}/accept` — 인증 필요

성공: `200 OK`, `WorkspaceResponse`. 초대 이메일이 지정된 경우 로그인 계정 이메일과 일치해야 한다.

주요 오류: `INVITATION_INVALID(400)`, `INVITATION_EXPIRED(410)`, `INVITATION_EMAIL_MISMATCH(403)`.

- 이메일 초대와 공유 링크의 기본 유효기간은 7일이다.
- 초대 링크를 연 사용자가 로그인하지 않았다면 프론트가 token을 보존한 채 로그인/회원가입으로 이동한다.
- 로그인 후 같은 token으로 수락 API를 호출한다.
- 이미 가입한 멤버가 다시 수락하면 현재 워크스페이스 정보를 반환한다. 중복 membership은 생성하지 않는다.

### 7.8 워크스페이스 근무 컨텍스트 예외

`PUT /api/v1/workspaces/{workspaceId}/members/me/work-context`

요청은 5.5와 동일하다. 저장된 예외는 해당 워크스페이스에서만 계정 기본값보다 우선한다.

`200 OK`

```json
{
  "overridden": true,
  "timeZoneId": "America/Los_Angeles",
  "workStart": "08:30:00",
  "workEnd": "17:30:00",
  "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY"]
}
```

`DELETE /api/v1/workspaces/{workspaceId}/members/me/work-context`

성공: `204 No Content`. 이후 계정 기본값을 상속한다.

---

## 8. 대화·메시지 API

### 8.1 1:1 DM 생성/조회

`POST /api/v1/conversations/direct`

요청:

```json
{
  "otherUserId": "e54839db-5a97-433f-bec9-35d85cc0ea12"
}
```

현재 워크스페이스 범위 요청:

```json
{
  "workspaceId": "4fa49159-fad4-4937-93ee-a0e5d514933c",
  "otherUserId": "e54839db-5a97-433f-bec9-35d85cc0ea12"
}
```

같은 워크스페이스·사용자 조합의 DM이 존재하면 새로 만들지 않고 기존 대화를 반환한다. 성공: `201 Created`.

### 8.2 대화 목록

`GET /api/v1/conversations`

워크스페이스 확장 후 `?workspaceId={id}`를 필수로 추가한다.

```json
[
  {
    "id": "74cda6f7-0335-4586-94ae-20beaf3d9941",
    "type": "DIRECT",
    "otherParticipant": {
      "id": "e54839db-5a97-433f-bec9-35d85cc0ea12",
      "displayName": "Alex",
      "profileImageUrl": null,
      "role": "DEVELOPER",
      "customRole": null,
      "timeZoneId": "America/Los_Angeles",
      "preferredLanguage": "en"
    },
    "latestMessage": {
      "id": "66b88929-ae21-4fc6-aa1c-17ccd7a41ec1",
      "senderId": "19332fd9-6261-4fd4-8c9e-e71602bad19d",
      "content": "스펙 초안 확인 부탁드려요.",
      "sentAt": "2026-08-14T09:00:00Z"
    },
    "unreadCount": 1,
    "lastActivityAt": "2026-08-14T09:00:00Z"
  }
]
```

### 8.3 메시지 조회

`GET /api/v1/conversations/{conversationId}/messages?before={instant}&size=50`

기본 응답 필드:

```json
{
  "messages": [
    {
      "id": "66b88929-ae21-4fc6-aa1c-17ccd7a41ec1",
      "conversationId": "74cda6f7-0335-4586-94ae-20beaf3d9941",
      "sender": {
        "id": "19332fd9-6261-4fd4-8c9e-e71602bad19d",
        "displayName": "이서연",
        "timeZoneId": "Asia/Seoul"
      },
      "content": "스펙 초안 확인 부탁드려요.",
      "sentAt": "2026-08-14T09:00:00Z",
      "senderLocalSentAt": "2026-08-14T18:00:00+09:00",
      "viewerLocalSentAt": "2026-08-14T02:00:00-07:00"
    }
  ],
  "hasMore": false,
  "nextBefore": null
}
```

AI/첨부 구현 후 각 메시지에 아래 필드를 추가한다.

```json
{
  "deliveryMode": "AI_REVIEW_CONFIRMED",
  "deliveryStatus": "SENT",
  "confirmationStatus": "REVIEW",
  "attachments": [
    {
      "id": "07af3f4d-d740-4540-8bf1-6f9e8c397045",
      "originalFileName": "스펙_v3.pdf",
      "contentType": "application/pdf",
      "size": 1482031,
      "processingStatus": "READY",
      "downloadUrl": "/api/v1/attachments/07af3f4d-d740-4540-8bf1-6f9e8c397045/content"
    }
  ],
  "understandingCard": {},
  "scheduledFor": null
}
```

### 8.4 일반 메시지 전송

`POST /api/v1/conversations/{conversationId}/messages`

현재 요청 계약:

```json
{
  "content": "스펙 초안 확인 부탁드려요.",
  "attachmentIds": ["07af3f4d-d740-4540-8bf1-6f9e8c397045"],
  "deliveryMode": "AS_IS",
  "scheduledFor": null
}
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `content` | 조건부 | 최대 4,000자. 파일이 없으면 필수 |
| `attachmentIds` | 아니오 | 최대 10개. 모두 이 대화에 업로드된 파일이어야 함 |
| `deliveryMode` | 예 | 일반 전송은 `AS_IS` |
| `scheduledFor` | 아니오 | 미래 UTC 시각. 값이 있으면 `SCHEDULED` |

`AS_IS` 전송 결과는 `confirmationStatus=UNCONFIRMED`이며 공통 이해 카드와 합의 기록을 만들지 않는다(E02). 성공: `201 Created`, 확장된 `MessageResponse`.

AI 확정 메시지는 이 API에 임의로 `AI_REVIEW_CONFIRMED`를 보내지 않고 반드시 10.4의 전용 전송 API를 사용한다.

### 8.5 읽음 처리

`PUT /api/v1/conversations/{conversationId}/read`

서버 수신 시점까지의 메시지를 읽음 처리한다. 성공: `204 No Content`.

---

## 9. 첨부파일 API

### 9.1 파일 업로드

`POST /api/v1/conversations/{conversationId}/attachments`

```http
Content-Type: multipart/form-data

file: (binary)
```

- 최대 10MB/개, 메시지당 최대 10개
- MVP 허용: PDF, DOCX, TXT, PNG, JPG/JPEG, WEBP
- 확장자만 믿지 않고 서버에서 MIME type과 파일 signature를 함께 확인한다.
- 업로드만으로 메시지에 노출되지 않는다. 전송 요청의 `attachmentIds`로 연결한다.
- 파일명은 표시용으로 보존하되 저장 키는 UUID로 생성한다.
- 다운로드는 원본 파일명과 안전한 `Content-Disposition`을 사용한다.
- MVP 저장소는 Docker volume이며 이후 S3 호환 객체 스토리지로 교체할 수 있게 저장 인터페이스를 분리한다.

`201 Created`

```json
{
  "id": "07af3f4d-d740-4540-8bf1-6f9e8c397045",
  "conversationId": "74cda6f7-0335-4586-94ae-20beaf3d9941",
  "originalFileName": "스펙_v3.pdf",
  "contentType": "application/pdf",
  "size": 1482031,
  "processingStatus": "READY",
  "extractionErrorCode": null,
  "createdAt": "2026-08-14T09:00:00Z"
}
```

`processingStatus`: `PROCESSING`, `READY`, `EXTRACTION_FAILED`, `UNSUPPORTED`.

### 9.2 파일 상태/다운로드

`GET /api/v1/attachments/{attachmentId}`는 9.1 응답을 반환한다.

`GET /api/v1/attachments/{attachmentId}/content`는 대화 멤버에게만 파일을 반환한다. 객체 스토리지를 사용하면 짧은 만료시간의 서명 URL로 `302 Redirect`할 수 있다.

AI 추출 실패(E15/E16)여도 원본 파일 전송은 허용한다. AI 검토 응답에는 실패 상태와 “파일 확인 필요”를 표시한다.

---

## 10. AI 검토 API

### 10.1 AI 검토 생성

`POST /api/v1/conversations/{conversationId}/ai-reviews`

```json
{
  "content": "스펙 초안 확인 부탁드려요. 이 방향은 좋은데 조금 더 고민해 보면 어떨까요?",
  "attachmentIds": ["07af3f4d-d740-4540-8bf1-6f9e8c397045"]
}
```

- 초안은 아직 메시지가 아니며 상대방에게 노출되지 않는다.
- 동일한 `reviewId`를 확정할 때까지 여러 번 수정할 수 있다.
- AI는 모르는 값을 추측하지 않고 `null`과 낮은 confidence로 반환한다(E09).
- 같은 언어면 번역을 건너뛰고 구조화만 한다(E13).

`201 Created`

```json
{
  "id": "e329653a-4aaf-473d-91c1-352ac83a8cbc",
  "conversationId": "74cda6f7-0335-4586-94ae-20beaf3d9941",
  "status": "READY",
  "originalContent": "스펙 초안 확인 부탁드려요. 이 방향은 좋은데 조금 더 고민해 보면 어떨까요?",
  "sourceLanguage": "ko",
  "recipientLanguage": "en",
  "translatedContent": "Please review the draft specification...",
  "structuredFields": {
    "task": {
      "value": "문서 3면 섹션 검토",
      "confidence": "HIGH",
      "confirmed": false
    },
    "assigneeUserId": {
      "value": "e54839db-5a97-433f-bec9-35d85cc0ea12",
      "confidence": "HIGH",
      "confirmed": false
    },
    "deadline": {
      "value": "2026-08-15T09:00:00Z",
      "senderLocal": "2026-08-15T18:00:00+09:00[Asia/Seoul]",
      "recipientLocal": "2026-08-15T02:00:00-07:00[America/Los_Angeles]",
      "confidence": "MEDIUM",
      "confirmed": false
    },
    "expectedOutcome": {
      "value": "방향 유지 및 세부 수정 제안",
      "confidence": "MEDIUM",
      "confirmed": false
    }
  },
  "evidence": [
    {
      "attachmentId": "07af3f4d-d740-4540-8bf1-6f9e8c397045",
      "fileName": "스펙_v3.pdf",
      "locator": "p.4, §3",
      "excerpt": "3면 데이터 모델",
      "confidence": "HIGH"
    }
  ],
  "warnings": [
    {
      "code": "OUTSIDE_RECIPIENT_WORK_HOURS",
      "message": "수신자의 근무시간 밖입니다.",
      "suggestedDeadline": "2026-08-15T16:00:00Z"
    }
  ],
  "agentSession": {
    "threadId": "56cc2b3f-844b-4ad8-9d4d-ab6fbdf9e230",
    "status": "INTERRUPT",
    "step": 1,
    "total": 2,
    "item": {
      "span": "조금 더 고민해 보면 어떨까요?",
      "category": "REQUEST_INTENT",
      "reason": "여러 의도로 해석될 수 있습니다.",
      "candidates": ["현재 방향 유지 + 세부 보완 요청", "완곡한 반대", "추가 논의 요청"],
      "suggestion": "실제 의도를 선택해 주세요."
    }
  },
  "provider": "DITTO_AGENT",
  "createdAt": "2026-08-14T09:00:00Z",
  "expiresAt": "2026-08-15T09:00:00Z"
}
```

`status`: `PROCESSING`, `READY`, `FAILED`, `CONFIRMED`, `SENT`, `EXPIRED`.

`confidence`: `HIGH`, `MEDIUM`, `LOW`, `UNKNOWN`.

`agentSession.status`: `INTERRUPT`, `DONE`, `FAILED`. AI 서비스가 비활성화된 로컬
fallback에서는 `agentSession`이 `null`이다.

### 10.2 AI 검토 상태 조회

`GET /api/v1/ai-reviews/{reviewId}`

10.1 응답과 같다. 초기 구현을 비동기로 바꿀 경우 생성 API는 `202 Accepted + status=PROCESSING`을 반환하고 이 API를 1초 간격으로 조회한다. MVP 첫 구현은 동기 `201`로 시작해도 계약의 결과 구조는 유지한다.

### 10.3 AI 모호성 확인 답변

`POST /api/v1/ai-reviews/{reviewId}/answers`

```json
{
  "answer": "현재 방향 유지 + 세부 보완 요청"
}
```

- `agentSession.status=INTERRUPT`인 경우에만 AI 검토 생성자(발신자)가 호출한다.
- 응답은 10.1과 같은 `AiReviewResponse`다.
- 다음 질문이 있으면 `INTERRUPT`와 새 `item`이 반환되므로 같은 API를 반복 호출한다.
- 모든 모호성을 확인하면 `DONE`이 되고 구조화 필드에 agent 결과가 반영된다.
- `DONE` 이후에는 10.4의 수정·확정 API로 최종값을 검토한다.

### 10.4 사용자의 추출 결과 수정·확정

`PATCH /api/v1/ai-reviews/{reviewId}`

```json
{
  "task": "문서 3면 섹션 검토",
  "assigneeUserId": "e54839db-5a97-433f-bec9-35d85cc0ea12",
  "deadline": "2026-08-15T16:00:00Z",
  "expectedOutcome": "방향 유지 및 세부 수정 제안",
  "confirmedEvidenceIds": ["7e83e3aa-3c47-4c96-86c4-6c08ed46d730"],
  "confirmed": true
}
```

- `confirmed=true`일 때 `task`, `assigneeUserId`, `deadline`, `expectedOutcome`을 최종 값으로 고정한다.
- 계정 생성 시 기본 타임존 `UTC`가 저장되며, 프로필 또는 워크스페이스 근무 설정에서 IANA 타임존으로 변경할 수 있다.
- 수정한 값과 AI 원안을 둘 다 저장해 감사 로그에 남긴다.

### 10.5 AI 검토 확정 후 전송

`POST /api/v1/ai-reviews/{reviewId}/send`

```json
{
  "content": "스펙 초안 확인 부탁드려요.",
  "scheduledFor": null
}
```

- 검토가 `CONFIRMED` 상태일 때만 가능하다.
- 서버는 메시지, 공통 이해 카드, 최초 revision을 하나의 트랜잭션으로 생성한다.
- 카드 초기 상태는 `REVIEW`, 메시지 `confirmationStatus`도 `REVIEW`다.
- 성공: `201 Created`, 확장 `MessageResponse`.

---

## 11. 공통 이해 카드 API

### 11.1 카드 데이터 모델

```json
{
  "id": "729b92b3-0ff4-4c84-996e-1b5de3cb98d2",
  "messageId": "66b88929-ae21-4fc6-aa1c-17ccd7a41ec1",
  "state": "REVIEW",
  "revision": 1,
  "task": "문서 3면 섹션 검토",
  "assignee": {
    "userId": "e54839db-5a97-433f-bec9-35d85cc0ea12",
    "displayName": "Alex"
  },
  "deadline": {
    "instant": "2026-08-15T16:00:00Z",
    "viewerLocal": "2026-08-15T09:00:00-07:00",
    "viewerTimeZoneId": "America/Los_Angeles"
  },
  "expectedOutcome": "방향 유지 및 세부 수정 제안",
  "originalContent": "스펙 초안 확인 부탁드려요.",
  "translatedContent": "Please review the draft specification.",
  "attachments": [],
  "evidence": [],
  "latestResponse": null,
  "createdAt": "2026-08-14T09:00:00Z",
  "updatedAt": "2026-08-14T09:00:00Z"
}
```

`viewerLocal`은 요청한 사용자의 타임존에 맞춰 계산한다. 합의의 기준 값은 항상 `instant`다.

### 11.2 카드 조회

`GET /api/v1/understanding-cards/{cardId}`

대화 멤버만 조회할 수 있다. 응답은 11.1과 같다.

### 11.3 수신자 “이해 돕기” 생성

`POST /api/v1/messages/{messageId}/understanding-cards`

일반 메시지를 받은 사용자가 누른다. 서버는 메시지 본문, 대화 맥락, 첨부파일을 근거로 구조화한다.

```json
{
  "includeConversationContext": true
}
```

- 이미 카드가 있으면 기존 카드를 `200 OK`로 반환한다.
- 새로 만들면 `201 Created`다.
- 확신이 낮은 필드는 빈 값으로 두고 `needsClarification=true`를 표시한다.
- 이 카드 자체는 발신자·수신자 합의가 아니며, 수신자가 `AGREE`하기 전까지 `REVIEW`다.

### 11.4 수신자 응답

`POST /api/v1/understanding-cards/{cardId}/responses`

동의:

```json
{
  "type": "AGREE",
  "comment": null
}
```

기한 조정 요청:

```json
{
  "type": "REQUEST_DEADLINE_CHANGE",
  "comment": "기한 조정이 필요합니다.",
  "proposedDeadline": "2026-08-17T16:00:00Z"
}
```

요건 설명 요청:

```json
{
  "type": "REQUEST_CLARIFICATION",
  "comment": "완료 기준을 조금 더 구체적으로 알려주세요."
}
```

| `type` | 카드 전이 | 의미 |
| --- | --- | --- |
| `AGREE` | `REVIEW → AGREED` | 현재 revision에 동의, 잠금 |
| `REQUEST_DEADLINE_CHANGE` | `REVIEW → PENDING` | 발신자 수정 대기 |
| `REQUEST_CLARIFICATION` | `REVIEW → PENDING` | 발신자 설명/수정 대기 |

`AGREED` 상태에서는 같은 revision에 다시 응답할 수 없다.

### 11.5 발신자 수정본 제출

`POST /api/v1/understanding-cards/{cardId}/revisions`

```json
{
  "task": "문서 3면 섹션 검토",
  "deadline": "2026-08-17T16:00:00Z",
  "expectedOutcome": "필수 수정사항 목록과 승인 여부",
  "changeNote": "요청한 기한과 완료 기준을 반영했습니다."
}
```

`PENDING → REVIEW`, `revision`이 1 증가한다. 이전 revision과 응답은 변경하지 않고 보존하며 수신자는 새 revision에 다시 응답한다.

### 11.6 상태 머신

```text
AI 확정 전송 또는 이해 돕기 생성
              │
              ▼
           REVIEW
          /      \
      AGREE       수정/설명 요청
        │               │
        ▼               ▼
     AGREED          PENDING
                        │ 발신자 revision
                        └────────→ REVIEW
```

`UNCONFIRMED`는 카드 상태가 아니라 일반 전송 메시지의 상태다. 카드와 합의 로그가 존재하지 않는다.

---

## 12. 합의 기록 API

### 12.1 대화별 합의 기록 조회

`GET /api/v1/conversations/{conversationId}/agreement-logs?before={instant}&size=50`

```json
{
  "logs": [
    {
      "id": "48a1502c-cb5e-47f2-8990-335a4f4ab82f",
      "cardId": "729b92b3-0ff4-4c84-996e-1b5de3cb98d2",
      "revision": 2,
      "status": "AGREED",
      "task": "문서 3면 섹션 검토",
      "deadline": "2026-08-17T16:00:00Z",
      "agreedBy": {
        "userId": "e54839db-5a97-433f-bec9-35d85cc0ea12",
        "displayName": "Alex"
      },
      "agreedAt": "2026-08-15T09:15:00Z",
      "fileReferences": [
        {
          "attachmentId": "07af3f4d-d740-4540-8bf1-6f9e8c397045",
          "fileName": "스펙_v3.pdf",
          "locator": "p.4, §3"
        }
      ]
    }
  ],
  "hasMore": false,
  "nextBefore": null
}
```

`status`: `AGREED`, `PENDING`. 일반 전송 `UNCONFIRMED` 메시지는 목록에 넣지 않되, UI가 전체 메시지 이벤트를 함께 보여줄 경우 “미확정”으로 표시할 수 있다.

합의 당시 본문·구조화 필드·파일 메타데이터·근무 컨텍스트 버전을 스냅샷으로 보존한다. 원본 파일 삭제/보존 기간은 출시 전 정책 결정이 필요하다.

---

## 13. 예외·빈 상태 계약

### 13.1 도메인 예외 매핑

| 와이어프레임 | API code | HTTP | 처리 |
| --- | --- | --- | --- |
| E01 모호성 없음 | 오류 아님 | - | AI 검토 생략 가능 안내 |
| E02 발신자 확인 스킵 | 오류 아님 | - | `AS_IS`, `UNCONFIRMED`, 합의 로그 없음 |
| E04 수신자 비근무시간 | `OUTSIDE_RECIPIENT_WORK_HOURS` | 200 warning | 대안 시각 제시; 수정/그대로/예약 선택 |
| E05 타임존 미등록 | 오류 아님 | - | 가입 기본값 `UTC` 적용, 설정 화면에서 변경 가능 |
| E09 AI 신뢰도 낮음 | 오류 아님 | - | 값을 `null`, confidence `LOW/UNKNOWN`으로 반환 |
| E10 AI 추출/번역 실패 | `AI_REVIEW_FAILED` | 200 warning | 로컬 분석으로 폴백하고 원문 전송 허용 |
| E13 같은 언어 | 오류 아님 | - | 번역 생략, 구조화만 수행 |
| E14 DST/지역 변경 | 오류 아님 | - | IANA timezone으로 자동 반영 |
| E15 파일 추출 오류 | `ATTACHMENT_EXTRACTION_FAILED` | 200 warning | 낮은 확신 근거 후보, 발신자 확인 필요 |
| E16 AI가 파일을 못 읽음 | `ATTACHMENT_EXTRACTION_FAILED` | 200 warning | 원본 파일 전송 가능, 실패 표시 |
| O-a 가입 검증 실패 | `INVALID_REQUEST` | 400 | 필드별 오류 표시 |
| O-b 인증코드 만료 | `VERIFICATION_CODE_EXPIRED` | 410 | 재발송 버튼 활성화 |
| O-c 초대코드 무효 | `INVITATION_INVALID` | 400 | 초대코드를 확인할 수 없음 표시 |

경고는 성공 응답의 `warnings[]`에 포함하며 요청 전체를 실패시키지 않는다.

### 13.2 전체 오류 코드

현재 구현 HTTP 오류 코드:

| code | HTTP |
| --- | --- |
| `INVALID_REQUEST` | 400 |
| `INVALID_CREDENTIALS` | 401 |
| `ACCESS_DENIED` | 403 |
| `USER_NOT_FOUND` | 404 |
| `CONVERSATION_NOT_FOUND` | 404 |
| `MESSAGE_NOT_FOUND` | 404 |
| `WORKSPACE_NOT_FOUND` | 404 |
| `WORKSPACE_ACCESS_DENIED` | 403 |
| `WORKSPACE_OWNER_REQUIRED` | 403 |
| `WORKSPACE_ALREADY_DELETED` | 409 |
| `EMAIL_ALREADY_EXISTS` | 409 |
| `EMAIL_VERIFICATION_REQUIRED` | 400 |
| `INVALID_VERIFICATION_CODE` | 400 |
| `VERIFICATION_CODE_EXPIRED` | 410 |
| `VERIFICATION_RESEND_TOO_SOON` | 429 |
| `FILE_UPLOAD_FAILED` | 400 |
| `INVITATION_INVALID` | 400 |
| `INVITATION_EXPIRED` | 410 |
| `INVITATION_EMAIL_MISMATCH` | 403 |
| `INVITATION_LIMIT_EXCEEDED` | 400 |
| `ATTACHMENT_NOT_FOUND` | 404 |
| `ATTACHMENT_ACCESS_DENIED` | 403 |
| `FILE_SIZE_EXCEEDED` | 413 |
| `UNSUPPORTED_FILE_TYPE` | 415 |
| `AI_REVIEW_NOT_FOUND` | 404 |
| `AI_REVIEW_FAILED` | 502 |
| `AI_REVIEW_NOT_CONFIRMED` | 409 |
| `AI_REVIEW_EXPIRED` | 410 |
| `UNDERSTANDING_CARD_NOT_FOUND` | 404 |
| `CARD_INVALID_STATE` | 409 |
| `CARD_RESPONSE_NOT_ALLOWED` | 403 |
| `REVISION_LIMIT_EXCEEDED` | 409 |
| `DIRECT_CONVERSATION_WITH_SELF` | 400 |
| `INTERNAL_SERVER_ERROR` | 500 |

성공 응답의 `warnings[]`에 포함되는 코드:

| code | 의미 |
| --- | --- |
| `AMBIGUOUS_DEADLINE` | 정확한 날짜·시각·타임존 확인 필요 |
| `AMBIGUOUS_EXPECTED_OUTCOME` | 기대 결과 또는 완료 기준 확인 필요 |
| `OUTSIDE_RECIPIENT_WORK_HOURS` | 수신자 근무시간 밖이며 대안 시각 제공 |
| `AI_REVIEW_FAILED` | OpenAI 실패 후 로컬 분석으로 폴백 |
| `ATTACHMENT_EXTRACTION_FAILED` | 첨부 텍스트 추출 실패, 원본 확인 필요 |

### 13.3 빈 상태

| 화면 | 판단 기준 |
| --- | --- |
| DM 없음 | `GET /conversations`가 `[]` |
| 메시지 없음 | `messages=[]`, `hasMore=false` |
| 워크스페이스 없음 | `GET /workspaces`가 `[]`, `onboardingStep=WORKSPACE` |
| 합의 기록 없음 | `logs=[]`, `hasMore=false` |
| AI 실패 | 응답 `warnings[].code=AI_REVIEW_FAILED`, 결과는 로컬 분석 폴백 |
| 파일 추출 실패 | `Attachment.processingStatus=EXTRACTION_FAILED` |

---

## 14. 프론트엔드 호출 시나리오

### 14.1 이메일 가입 + 한 페이지 온보딩

```text
POST /auth/email-verifications
→ POST /auth/email-verifications/confirm
→ POST /auth/signup
→ PATCH /users/me/profile
→ PUT /users/me/profile-image (선택)
→ PATCH /users/me/work-context
→ POST /workspaces 또는 POST /workspace-invitations/{token}/accept
→ POST /workspaces/{id}/invitations (선택)
```

현재 백엔드는 근무 컨텍스트 저장 후 `onboardingStep=WORKSPACE`로 진행하고, 워크스페이스 생성 또는 초대 수락 성공 트랜잭션에서 `COMPLETED`로 변경한다.

### 14.2 일반 전송

```text
첨부가 있으면 POST /conversations/{id}/attachments
→ POST /conversations/{id}/messages (deliveryMode=AS_IS)
→ confirmationStatus=UNCONFIRMED, 합의 기록 없음
```

### 14.3 AI 검토 후 합의

```text
POST /conversations/{id}/ai-reviews
→ PATCH /ai-reviews/{id} (사용자 확인)
→ POST /ai-reviews/{id}/send
→ 카드 REVIEW
→ POST /understanding-cards/{id}/responses
   ├─ AGREE → AGREED
   └─ 수정/설명 요청 → PENDING
       → POST /understanding-cards/{id}/revisions
       → REVIEW → 재응답
```

### 14.4 수신자의 “이해 돕기”

```text
수신 메시지에서 POST /messages/{messageId}/understanding-cards
→ 낮은 확신 필드는 빈 값 표시
→ 필요하면 POST /understanding-cards/{id}/responses (REQUEST_CLARIFICATION)
```

---

## 15. 구현 결정과 운영 전 확인 항목

아래 항목은 API 계약에서 기본값을 정했지만 팀 합의 후 변경할 수 있다.

1. 파일 저장소: 로컬/Docker volume은 데모용, 배포는 S3 호환 객체 스토리지 권장.
2. 초대 링크 만료: 현재 명세 기본 7일, 이메일 초대도 동일하게 적용.
3. AI 처리는 동기 응답이며 `OPENAI_API_KEY`가 없거나 호출에 실패하면 안전한 로컬 분석으로 폴백한다.
4. 예약 전송은 DB 저장 후 5초 주기의 dispatcher가 만료된 예약 메시지를 `SENT`로 전환한다.
5. 합의·파일 보존 기간: 해커톤 데모는 무기한, 실제 서비스 전 개인정보 정책 필요.
6. 워크스페이스 권한: MVP는 `OWNER`, `MEMBER` 두 단계만 사용.
7. 실시간성: MVP는 3~5초 polling으로 시작하고 시간 여유가 있으면 WebSocket/SSE 추가.

## 16. 구현 완료 순서

1. `workspace`, `workspace_member`, `workspace_invitation` 및 워크스페이스 소프트 삭제
2. 대화에 `workspace_id` 연결
3. `attachment` 업로드/다운로드
4. 메시지 확장 필드와 일반 전송
5. `ai_review` 및 AI 결과 JSON 저장
6. `understanding_card`, `card_revision`, `card_response`
7. 합의 로그 조회
8. 근무시간·타임존 경고 반영
