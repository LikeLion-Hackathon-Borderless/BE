# 프론트엔드 API 인계

> 계약 버전: v1.0
> 기준일: 2026-08-17
> 인증: 이메일/비밀번호 로그인 + JWT Bearer Token

## 1. 문서 구분

| 자료 | 용도 |
| --- | --- |
| `docs/API.md` | 구현된 MVP API의 상세 계약 |
| Swagger UI `/swagger-ui.html` | 현재 서버에서 실제 호출 가능한 API |
| OpenAPI JSON `/v3/api-docs` | 현재 구현 API의 OpenAPI 3.0 명세 |
| OpenAPI YAML `/v3/api-docs.yaml` | 현재 구현 API의 타입 생성용 명세 |

MVP API 38개가 모두 구현되어 Swagger와 `docs/API.md` 기준으로 실제 서버에 연결할 수 있다.

## 2. 서버 주소

```text
로컬 API: http://localhost:8080/api/v1
로컬 Swagger: http://localhost:8080/swagger-ui.html
배포 API: 백엔드 배포 후 별도 공유
```

운영 주소는 코드에 고정하지 않고 프론트 환경변수로 관리한다.

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_BACKEND_ORIGIN=http://localhost:8080
```

- API 요청은 `VITE_API_BASE_URL`을 사용한다.
- `profileImageUrl=/uploads/...` 같은 상대 파일 경로는 `VITE_BACKEND_ORIGIN`을 앞에 붙인다.

## 3. 구현 상태

| 단계 | 총 API 수 | 상태 |
| --- | ---: | --- |
| 인증부터 합의 기록까지 | 38 | 현재 구현 완료 |

API 개수는 리소스 설계에 따라 소폭 달라질 수 있다. 완료 여부는 회원가입부터 합의까지의 사용자 여정과 테스트를 기준으로 판단한다.

## 4. 현재 호출 가능한 API

### 인증

```text
POST /auth/email-verifications
POST /auth/email-verifications/confirm
POST /auth/signup
POST /auth/login
```

### 사용자·온보딩

```text
GET   /users/me
GET   /users/roles
GET   /users?workspaceId=&query=&size=
PATCH /users/me/profile
PUT   /users/me/profile-image
PATCH /users/me/work-context
```

근무 컨텍스트 저장 후 `WORKSPACE`, 워크스페이스 생성 또는 초대 수락 성공 후 `COMPLETED`가 반환된다.

### 워크스페이스 핵심

```text
POST   /workspaces
GET    /workspaces
GET    /workspaces/{workspaceId}
GET    /workspaces/{workspaceId}/members
DELETE /workspaces/{workspaceId}
PUT    /workspaces/{workspaceId}/members/me/work-context
DELETE /workspaces/{workspaceId}/members/me/work-context
```

### 대화·메시지

```text
POST /conversations/direct
GET  /conversations
PUT  /conversations/{conversationId}/read
GET  /conversations/{conversationId}/messages?before=&size=
POST /conversations/{conversationId}/messages
```

## 5. 추가 구현 완료 API

### 워크스페이스 초대

```text
POST   /workspaces/{workspaceId}/invitations
POST   /workspaces/{workspaceId}/invitation-links
GET    /workspace-invitations/{token}
POST   /workspace-invitations/{token}/accept
```

### 첨부파일

```text
POST /conversations/{conversationId}/attachments
GET  /attachments/{attachmentId}
GET  /attachments/{attachmentId}/content
```

### AI 검토·공통 이해·합의

```text
POST  /conversations/{conversationId}/ai-reviews
GET   /ai-reviews/{reviewId}
PATCH /ai-reviews/{reviewId}
POST  /ai-reviews/{reviewId}/send
POST  /messages/{messageId}/understanding-cards
GET   /understanding-cards/{cardId}
POST  /understanding-cards/{cardId}/responses
POST  /understanding-cards/{cardId}/revisions
GET   /conversations/{conversationId}/agreement-logs?before=&size=
```

### 기존 API 확장

```text
GET  /users?workspaceId={workspaceId}&query=&size=
POST /conversations/direct
GET  /conversations?workspaceId={workspaceId}
GET  /conversations/{conversationId}/messages
POST /conversations/{conversationId}/messages
```

`POST /conversations/direct` 요청에는 `workspaceId`가 추가된다. 메시지 요청에는 `attachmentIds`, `deliveryMode`, `scheduledFor`가 추가되고 응답에는 `attachments`, `confirmationStatus`가 추가된다.

## 6. 확정 정책

### 워크스페이스

- 권한은 `OWNER`, `MEMBER` 두 개만 사용한다.
- 생성자는 자동으로 `OWNER`가 된다.
- 선택한 워크스페이스는 프론트의 URL 또는 로컬 상태로 관리한다.
- 워크스페이스 범위 API마다 `workspaceId`를 명시한다.
- 워크스페이스 안의 멤버만 검색하고 DM을 생성할 수 있다.
- 동일 워크스페이스의 동일 사용자 조합에는 DM을 하나만 만든다.
- 워크스페이스 삭제는 `OWNER`만 가능하며 프론트에서 워크스페이스 이름 재입력을 요구한다.
- 삭제 성공 후 선택한 `workspaceId`를 제거하고 워크스페이스 선택 화면으로 이동한다.
- 삭제는 소프트 삭제이며 초대는 무효화되고 기존 대화·메시지·합의 기록에는 더 이상 접근할 수 없다.
- 소유권 이전, 멤버 강제 퇴장, 삭제 복구는 MVP 범위에 포함하지 않는다.

### 초대

- 이메일 초대는 한 요청에 최대 20명이다.
- 이메일 초대와 공유 링크는 7일 후 만료된다.
- 비로그인 사용자는 초대 token을 유지한 채 로그인 또는 회원가입 후 수락한다.
- 이메일 지정 초대는 로그인 계정 이메일과 일치해야 한다.
- 이미 가입한 멤버가 다시 수락해도 membership을 중복 생성하지 않는다.

### 첨부파일

- 파일당 최대 10MB다.
- 메시지 하나에 최대 10개를 첨부할 수 있다.
- 허용 형식은 PDF, DOCX, TXT, PNG, JPG/JPEG, WEBP다.
- MVP에서는 Docker volume에 저장한다.
- 파일 업로드 후 받은 `attachmentId`를 메시지 전송 요청에 넣는다.

### 실시간 처리

- MVP는 3~5초 polling으로 시작한다.
- WebSocket 또는 SSE는 일정에 여유가 있을 때 추가한다.
- 예약 전송은 구현되어 있으며 서버 dispatcher가 전송 시각이 된 메시지를 노출한다.

## 7. 인증 처리

로그인·회원가입 응답의 `accessToken`을 저장한 뒤 인증이 필요한 요청에 전달한다.

```http
Authorization: Bearer {accessToken}
```

- 현재 Refresh Token은 없다.
- `expiresAt`이 지나거나 `401`이 반환되면 로그인 화면으로 이동한다.
- `403`은 로그인 만료가 아니라 리소스 권한 부족으로 처리한다.
- 토큰 값을 URL, 로그, 오류 메시지에 넣지 않는다.

## 8. 주요 enum

```text
MembershipRole: OWNER | MEMBER
ConversationType: DIRECT
DeliveryMode: AS_IS | AI_REVIEW_CONFIRMED
DeliveryStatus: SCHEDULED | SENT | FAILED
ConfirmationStatus: UNCONFIRMED | REVIEW | PENDING | AGREED
AttachmentStatus: PROCESSING | READY | EXTRACTION_FAILED | UNSUPPORTED
InvitationStatus: PENDING | ACCEPTED | EXPIRED | REVOKED
```

근무요일은 `MONDAY`부터 `SUNDAY`, 역할은 `GET /users/roles` 응답 값을 사용한다.

## 9. 오류 처리

모든 API 오류는 같은 형식을 사용한다.

```json
{
  "timestamp": "2026-08-15T09:00:00Z",
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": {
    "email": "올바른 이메일 형식이어야 합니다."
  }
}
```

- 입력창 오류는 `fieldErrors`를 우선 표시한다.
- 화면 분기는 HTTP status보다 `code`를 기준으로 한다.
- JWT 누락·만료·변조는 `INVALID_CREDENTIALS(401)`, 인증 후 권한 부족은 `ACCESS_DENIED(403)` 형식으로 반환한다.
- 정의되지 않은 오류는 공통 오류 토스트와 재시도 버튼을 표시한다.
- 전체 오류 코드는 `docs/API.md`의 오류 코드 표를 따른다.

## 10. 프론트 개발 순서

1. 이메일 인증·회원가입·로그인과 JWT 처리
2. 한 페이지 온보딩과 프로필 이미지
3. 워크스페이스 허브·생성·상세·멤버·삭제를 실제 API로 연결
4. 워크스페이스 초대 화면을 실제 API로 연결
5. 대화방과 메시지에 `workspaceId` 적용
6. 파일 업로드 후 `attachmentId`로 메시지 전송
7. AI 검토·이해 카드·합의 기록을 실제 API로 연결

## 11. 계약 변경 규칙

- 필드명, enum, HTTP method, URL 변경은 프론트와 합의 후 반영한다.
- 기존 필드를 삭제하거나 의미를 바꾸는 변경은 계약 버전을 올린다.
- 선택 필드 추가는 기존 화면이 깨지지 않도록 nullable 또는 기본값을 제공한다.
- 구현 완료된 API는 Swagger와 `docs/API.md`의 상태를 함께 갱신한다.
