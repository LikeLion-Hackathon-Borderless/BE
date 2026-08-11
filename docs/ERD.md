# 현재 범위 ERD

```mermaid
erDiagram
    USERS ||--o{ CONVERSATION_MEMBERS : participates
    CONVERSATIONS ||--|{ CONVERSATION_MEMBERS : contains
    USERS ||--o{ MESSAGES : sends
    CONVERSATIONS ||--o{ MESSAGES : contains

    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar display_name
        varchar time_zone_id
        varchar preferred_language
        time work_start
        time work_end
        boolean enabled
        timestamptz created_at
        timestamptz updated_at
    }

    CONVERSATIONS {
        uuid id PK
        varchar type
        varchar direct_key UK
        timestamptz last_message_at
        timestamptz created_at
        timestamptz updated_at
    }

    CONVERSATION_MEMBERS {
        uuid id PK
        uuid conversation_id FK
        uuid user_id FK
        timestamptz last_read_at
        timestamptz created_at
        timestamptz updated_at
    }

    MESSAGES {
        uuid id PK
        uuid conversation_id FK
        uuid sender_id FK
        varchar content
        timestamptz created_at
        timestamptz updated_at
    }
```

## 설계 결정

- 대화와 참여자를 분리해 추후 그룹 DM이나 채널로 확장할 수 있게 했습니다.
- 1:1 DM의 `direct_key`는 두 사용자 UUID를 정렬해 결합한 값이며 중복 DM 생성을 막습니다.
- 시간은 DB에 `timestamp with time zone`/UTC 기준으로 저장하고 응답 시 사용자의 IANA 타임존으로 표현합니다.
- `last_read_at` 이후 상대가 보낸 메시지를 계산해 읽지 않은 메시지 수를 제공합니다.
- AI 분석 결과를 메시지 테이블에 미리 넣지 않았습니다. 워크플로우 확정 후 별도 도메인으로 추가해야 메시지 기본 기능과 결합도가 낮습니다.
