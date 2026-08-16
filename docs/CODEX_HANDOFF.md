# Codex 작업 인계 문서

노트북 등 다른 환경에서 아래 문장을 새 작업의 첫 메시지로 사용한다.

```text
이 저장소의 docs/CODEX_HANDOFF.md를 먼저 전부 읽고, git status와 현재 코드를 확인한 뒤 이어서 작업해줘. 기존 변경사항을 임의로 되돌리지 말고 이 문서의 결정사항을 기준으로 해줘.
```

## 프로젝트 개요

- 멋쟁이사자처럼 해커톤 프로젝트
- 글로벌 비동기 협업 메시지의 업무 약속을 구조화하는 서비스
- 백엔드 담당, 개발기간 약 2주
- Slack형 1:1 메신저가 현재 기반 기능
- Java 21, Spring Boot 3.5.16, PostgreSQL, JPA, Flyway, Gradle
- OpenAPI 3.0과 Swagger UI 사용

## 확정된 인증 범위

- Google 로그인은 사용하지 않는다.
- Google Calendar 연동도 사용하지 않는다.
- 이메일 인증 후 이메일/비밀번호로 회원가입한다.
- 로그인 성공 시 JWT access token을 발급한다.
- 인증 API는 `Authorization: Bearer {accessToken}` 헤더를 사용한다.
- 서버 세션을 사용하지 않는 `STATELESS` 정책이다.

## 현재 구현된 기능

### 인증

- 이메일 숫자 6자리 인증코드 실제 발송
- 코드 유효기간 10분
- 재발송 제한 60초
- 최대 5회 오입력 제한
- 이메일 회원가입
- 이메일/비밀번호 로그인
- JWT 발급 및 검증

### 사용자와 온보딩

- 내 정보 조회
- 이름, 역할, 사용자 언어 저장
- 프로필 이미지 업로드
- IANA 타임존, 근무시간, 근무요일 저장
- 사용자 검색
- 온보딩 상태: `PROFILE`, `WORK_CONTEXT`, `WORKSPACE`, `COMPLETED`

### 메신저

- 사용자 간 1:1 대화방 생성
- 동일 사용자 조합의 기존 대화방 재사용
- 내 대화방 목록
- 커서 기반 메시지 조회
- 최대 4,000자 텍스트 메시지 전송
- 읽음 처리와 unread count
- UTC 저장 및 사용자 타임존 현지 시각 응답

### API 문서

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`
- 상세 계약: `docs/API.md`

## 현재 API

```text
POST  /api/v1/auth/email-verifications
POST  /api/v1/auth/email-verifications/confirm
POST  /api/v1/auth/signup
POST  /api/v1/auth/login

GET   /api/v1/users/me
GET   /api/v1/users/roles
GET   /api/v1/users?query=&size=
PATCH /api/v1/users/me/profile
PUT   /api/v1/users/me/profile-image
PATCH /api/v1/users/me/work-context

POST  /api/v1/conversations/direct
GET   /api/v1/conversations
PUT   /api/v1/conversations/{conversationId}/read
GET   /api/v1/conversations/{conversationId}/messages
POST  /api/v1/conversations/{conversationId}/messages
```

## DB 마이그레이션

- V1: 사용자, 대화, 멤버, 메시지
- V2: 사용자 역할
- V3: 온보딩과 근무요일
- V4: 이메일 인증
- V5, V6: 과거 Google 연동 테이블 생성 기록
- V7: Google 로그인·Calendar 제거에 따라 V5, V6 테이블 삭제

이미 적용된 Flyway migration의 checksum을 깨뜨리지 않기 위해 V5와 V6 파일은 삭제하지 않는다. 최종 스키마에는 관련 테이블이 남지 않는다.

## 환경변수

```env
DB_URL=jdbc:postgresql://localhost:5432/async_align
DB_USERNAME=async_align
DB_PASSWORD=async_align
JWT_SECRET=32바이트_이상의_랜덤_비밀값
JWT_ACCESS_TOKEN_TTL=PT24H
PUBLIC_BASE_URL=http://localhost:8080
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=sender@gmail.com
MAIL_PASSWORD=앱_비밀번호
MAIL_FROM=sender@gmail.com
EMAIL_VERIFICATION_REQUIRED=true
```

`MAIL_PASSWORD`는 이메일 인증코드 발송을 위한 SMTP 자격 증명이며 소셜 로그인과 관계없다.

## 다음 구현 후보

1. 워크스페이스 생성·선택·멤버십
2. 이메일 초대와 초대 링크
3. 대화에 workspace 연결
4. 메시지 첨부파일
5. AI 검토와 구조화된 업무 필드
6. 공통 이해 카드와 상태 전이
7. 합의 기록
8. 워크스페이스별 근무 컨텍스트 예외

## 유지할 결정

- 현재 사용자의 변경사항을 임의로 되돌리지 않는다.
- API 성공 응답은 별도 `data` envelope 없이 DTO를 직접 반환한다.
- 오류는 `timestamp`, `status`, `code`, `message`, `fieldErrors` 형식을 사용한다.
- 절대 시각은 UTC로 저장하고 IANA 타임존으로 화면용 현지 시각을 계산한다.
- 일반 전송은 향후 `UNCONFIRMED`, AI 검토 확정 전송은 공통 이해 카드 `REVIEW` 상태로 연결한다.
- 상세 예정 계약과 상태 머신은 `docs/API.md`를 따른다.
