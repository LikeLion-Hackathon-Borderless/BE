# Async Align Backend

글로벌 비동기 협업 서비스의 로그인 및 Slack형 1:1 메신저 MVP 백엔드입니다.

현재 구현 범위는 와이어프레임과 AI 워크플로우 확정 전 단계에 맞춰 다음으로 제한합니다.

- 이메일/비밀번호 회원가입 및 로그인
- JWT Bearer 인증
- 내 프로필 및 사용자 검색
- 사용자 간 1:1 DM 생성과 목록 조회
- 메시지 커서 페이지 조회 및 전송
- 읽음 처리와 대화 목록의 읽지 않은 메시지 수
- UTC 저장 및 IANA 타임존 기반 발신자/조회자 현지 시각 표시

AI 검토, 공동 이해 카드, 합의 로그, WebSocket 실시간 전송은 아직 포함하지 않습니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.16
- Spring Security + OAuth2 Resource Server JWT
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Gradle 8.14.3 Wrapper
- H2 테스트 DB

## 실행

PostgreSQL을 먼저 실행합니다.

```powershell
docker compose up -d
```

애플리케이션을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

기본 서버 주소는 `http://localhost:8080`입니다. 환경 변수 예시는 [.env.example](.env.example)을 참고하세요.

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
