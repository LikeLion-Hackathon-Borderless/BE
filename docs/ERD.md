# 현재 범위 ERD

```mermaid
erDiagram
    USERS ||--o{ CONVERSATION_MEMBERS : participates
    CONVERSATIONS ||--|{ CONVERSATION_MEMBERS : contains
    USERS ||--o{ MESSAGES : sends
    CONVERSATIONS ||--o{ MESSAGES : contains
    USERS ||--o{ WORKSPACES : creates
    USERS ||--o{ WORKSPACE_MEMBERS : joins
    WORKSPACES ||--|{ WORKSPACE_MEMBERS : contains
    WORKSPACE_MEMBERS ||--o{ WORKSPACE_MEMBER_WORK_DAYS : overrides

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

    WORKSPACES {
        uuid id PK
        varchar name
        varchar organization_domain
        uuid created_by FK
        timestamptz deleted_at
        uuid deleted_by FK
        timestamptz created_at
        timestamptz updated_at
    }

    WORKSPACE_MEMBERS {
        uuid id PK
        uuid workspace_id FK
        uuid user_id FK
        varchar membership_role
        boolean work_context_overridden
        varchar time_zone_id
        time work_start
        time work_end
        timestamptz created_at
        timestamptz updated_at
    }

    WORKSPACE_MEMBER_WORK_DAYS {
        uuid workspace_member_id FK
        varchar day_of_week
    }
```

## 설계 결정

- 대화와 참여자를 분리해 추후 그룹 DM이나 채널로 확장할 수 있게 했습니다.
- 1:1 DM의 `direct_key`는 두 사용자 UUID를 정렬해 결합한 값이며 중복 DM 생성을 막습니다.
- 시간은 DB에 `timestamp with time zone`/UTC 기준으로 저장하고 응답 시 사용자의 IANA 타임존으로 표현합니다.
- `last_read_at` 이후 상대가 보낸 메시지를 계산해 읽지 않은 메시지 수를 제공합니다.
- 워크스페이스 생성자는 `OWNER` 멤버십을 가지며, 일반 멤버는 `MEMBER` 권한을 사용합니다.
- 워크스페이스 삭제는 `deleted_at`, `deleted_by`를 기록하는 소프트 삭제입니다.
- 워크스페이스 멤버의 근무 컨텍스트가 없으면 계정 기본값을 상속하고, 예외가 있으면 해당 워크스페이스에서만 우선합니다.
- AI 분석 결과를 메시지 테이블에 미리 넣지 않았습니다. 워크플로우 확정 후 별도 도메인으로 추가해야 메시지 기본 기능과 결합도가 낮습니다.
