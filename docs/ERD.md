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
    WORKSPACES ||--o{ WORKSPACE_INVITATIONS : invites
    WORKSPACES ||--o{ CONVERSATIONS : scopes
    CONVERSATIONS ||--o{ ATTACHMENTS : owns
    MESSAGES ||--o{ ATTACHMENTS : includes
    CONVERSATIONS ||--o{ AI_REVIEWS : reviews
    AI_REVIEWS ||--o{ AI_REVIEW_EVIDENCE : cites
    MESSAGES ||--o| UNDERSTANDING_CARDS : structures
    UNDERSTANDING_CARDS ||--o{ UNDERSTANDING_CARD_REVISIONS : revises
    UNDERSTANDING_CARDS ||--o{ UNDERSTANDING_CARD_RESPONSES : receives
    UNDERSTANDING_CARDS ||--o{ AGREEMENT_LOGS : snapshots
    AGREEMENT_LOGS ||--o{ AGREEMENT_LOG_FILE_REFERENCES : cites

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
        uuid workspace_id FK
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
        varchar delivery_mode
        varchar delivery_status
        varchar confirmation_status
        timestamptz scheduled_for
        uuid ai_review_id FK
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

    WORKSPACE_INVITATIONS {
        uuid id PK
        uuid workspace_id FK
        uuid inviter_id FK
        varchar invited_email
        varchar invitation_type
        varchar token_hash UK
        varchar status
        timestamptz expires_at
        uuid accepted_by FK
    }

    ATTACHMENTS {
        uuid id PK
        uuid conversation_id FK
        uuid uploader_id FK
        uuid message_id FK
        varchar storage_key UK
        varchar original_file_name
        varchar content_type
        bigint size_bytes
        varchar processing_status
        text extracted_text
    }

    AI_REVIEWS {
        uuid id PK
        uuid conversation_id FK
        uuid creator_id FK
        varchar status
        varchar original_content
        varchar final_task
        uuid final_assignee_user_id FK
        timestamptz final_deadline
        varchar final_expected_outcome
        varchar provider
    }

    AI_REVIEW_EVIDENCE {
        uuid id PK
        uuid ai_review_id FK
        uuid attachment_id FK
        varchar locator
        varchar excerpt
        varchar confidence
        boolean confirmed
    }

    UNDERSTANDING_CARDS {
        uuid id PK
        uuid message_id FK
        uuid conversation_id FK
        uuid ai_review_id FK
        uuid sender_id FK
        uuid recipient_id FK
        varchar card_state
        integer revision_number
        varchar task
        timestamptz deadline
        varchar expected_outcome
    }

    UNDERSTANDING_CARD_REVISIONS {
        uuid id PK
        uuid card_id FK
        integer revision_number
        varchar change_note
        uuid created_by FK
    }

    UNDERSTANDING_CARD_RESPONSES {
        uuid id PK
        uuid card_id FK
        integer revision_number
        uuid responder_id FK
        varchar response_type
        varchar comment
        timestamptz proposed_deadline
    }

    AGREEMENT_LOGS {
        uuid id PK
        uuid conversation_id FK
        uuid card_id FK
        integer revision_number
        varchar agreement_status
        uuid agreed_by FK
        timestamptz agreed_at
    }

    AGREEMENT_LOG_FILE_REFERENCES {
        uuid id PK
        uuid agreement_log_id FK
        uuid attachment_id
        varchar file_name
        varchar locator
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
- AI 분석 결과는 별도 `ai_reviews` 도메인에 저장하고, 확정 전송된 메시지만 `ai_review_id`로 연결합니다.
- 공통 이해 카드는 메시지당 하나이며 수정 이력·수신자 응답·합의 로그를 분리해 상태 전이와 스냅샷을 보존합니다.
- 합의 로그의 파일 참조는 파일 이름과 위치를 복사해 두어 이후 원본 메타데이터가 바뀌어도 당시 근거를 확인할 수 있습니다.
