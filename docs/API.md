# API 명세

Base URL: `/api/v1`

인증이 필요한 API에는 다음 헤더를 보냅니다.

```http
Authorization: Bearer {accessToken}
```

## 인증

### `POST /auth/signup`

```json
{
  "email": "seoyeon@example.com",
  "password": "password123!",
  "displayName": "이서연",
  "timeZoneId": "Asia/Seoul",
  "preferredLanguage": "ko",
  "workStart": "09:00:00",
  "workEnd": "18:00:00"
}
```

성공: `201 Created`

### `POST /auth/login`

```json
{
  "email": "seoyeon@example.com",
  "password": "password123!"
}
```

성공 응답:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-12T06:00:00Z",
  "user": {
    "id": "UUID",
    "email": "seoyeon@example.com",
    "displayName": "이서연",
    "timeZoneId": "Asia/Seoul",
    "preferredLanguage": "ko",
    "workStart": "09:00:00",
    "workEnd": "18:00:00"
  }
}
```

## 사용자

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/users/me` | 현재 로그인 사용자 조회 |
| `GET` | `/users?query=alex&size=20` | 이름 또는 이메일로 DM 상대 검색 |

사용자 검색 결과에서는 현재 로그인 사용자가 제외됩니다.

## DM 대화

### `POST /conversations/direct`

```json
{
  "otherUserId": "수신자 UUID"
}
```

같은 사용자 조합의 DM이 이미 있으면 기존 대화를 반환합니다. 성공: `201 Created`.

### `GET /conversations`

현재 사용자의 DM 목록을 최근 활동 순으로 반환합니다.

```json
[
  {
    "id": "대화 UUID",
    "type": "DIRECT",
    "otherParticipant": {},
    "latestMessage": {
      "id": "메시지 UUID",
      "senderId": "발신자 UUID",
      "content": "내일까지 검토 부탁드려요.",
      "sentAt": "2026-08-11T06:00:00Z"
    },
    "unreadCount": 1,
    "lastActivityAt": "2026-08-11T06:00:00Z"
  }
]
```

### `PUT /conversations/{conversationId}/read`

현재 시각까지 읽음 처리합니다. 성공: `204 No Content`.

## 메시지

### `GET /conversations/{conversationId}/messages`

쿼리 파라미터:

- `before`: 이전 응답의 `nextBefore` 값. 첫 요청에서는 생략
- `size`: 기본 50, 최대 100

응답의 메시지는 화면 출력에 편한 과거→최신 순입니다.

```json
{
  "messages": [
    {
      "id": "메시지 UUID",
      "conversationId": "대화 UUID",
      "sender": {
        "id": "사용자 UUID",
        "displayName": "이서연",
        "timeZoneId": "Asia/Seoul"
      },
      "content": "내일까지 검토 부탁드려요.",
      "sentAt": "2026-08-11T06:00:00Z",
      "senderLocalSentAt": "2026-08-11T15:00:00+09:00",
      "viewerLocalSentAt": "2026-08-10T23:00:00-07:00"
    }
  ],
  "hasMore": false,
  "nextBefore": null
}
```

### `POST /conversations/{conversationId}/messages`

```json
{
  "content": "내일까지 검토 부탁드려요."
}
```

성공: `201 Created`. 공백만 있는 메시지는 허용하지 않으며 최대 4,000자입니다.

## 오류 형식

```json
{
  "timestamp": "2026-08-11T06:00:00Z",
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": {
    "content": "공백일 수 없습니다"
  }
}
```
