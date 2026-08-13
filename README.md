# Async Align Backend

글로벌 비동기 협업 서비스의 로그인 및 Slack형 1:1 메신저 MVP 백엔드

현재 구현 범위는 와이어프레임과 AI 워크플로우 확정 전 단계에 맞춰 다음으로 제한

- 이메일/비밀번호 회원가입 및 로그인
- Google OAuth 로그인 후 동일한 JWT 발급
- JWT Bearer 인증
- 내 프로필 및 사용자 검색
- 사용자 간 1:1 DM 생성과 목록 조회
- 메시지 커서 페이지 조회 및 전송
- 읽음 처리와 대화 목록의 읽지 않은 메시지 수
- UTC 저장 및 IANA 타임존 기반 발신자/조회자 현지 시각 표시

AI 검토, 공동 이해 카드, 합의 로그, WebSocket 실시간 전송은 아직 포함 x

## 기술 스택

- Java 21
- Spring Boot 3.5.16
- Spring Security + OAuth2 Resource Server JWT
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Gradle 8.14.3 Wrapper
- H2 테스트 DB


Google Cloud Console에서 OAuth 2.0 Web Client를 만든 뒤 승인된 리디렉션 URI에
`http://localhost:8080/login/oauth2/code/google`을 등록합니다. 프론트의 Google 버튼은
`GET http://localhost:8080/oauth2/authorization/google`로 이동시키면 됩니다. 로그인 성공 후
`OAUTH2_SUCCESS_REDIRECT`로 60초짜리 일회용 `code`가 전달됩니다. 프론트는 이를
`POST /api/v1/auth/oauth/exchange`로 한 번 교환해 JWT를 받습니다.

## 테스트

```powershell
.\gradlew.bat test
```

Windows에서 저장소 전체 경로에 한글이 포함된 경우 Gradle 테스트 워커의 클래스패스 인코딩 문제로 `ClassNotFoundException`이 발생할 수 있습니다. 이 경우 저장소를 영문 경로에 두거나 임시 드라이브에 연결한 경로에서 테스트합니다.

```powershell
subst X: "현재 저장소의 절대 경로"
X:
.\gradlew.bat test
subst X: /D
```

## 문서

- [API 명세](docs/API.md)
- [현재 범위 ERD와 설계 결정](docs/ERD.md)
- [개발 범위 및 다음 단계](docs/ROADMAP.md)
- [단계별 온보딩·이메일·Calendar·배포 설정](docs/ONBOARDING.md)
