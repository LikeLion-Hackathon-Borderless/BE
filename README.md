# Async Align Backend

글로벌 비동기 협업 서비스의 로그인, 워크스페이스 및 Slack형 1:1 메신저 MVP 백엔드

현재 구현 범위

- 이메일/비밀번호 회원가입 및 로그인
- JWT Bearer 인증
- 내 프로필 및 사용자 검색
- 워크스페이스 생성·목록·상세·멤버 조회
- OWNER 워크스페이스 소프트 삭제
- 워크스페이스별 근무 컨텍스트 예외
- 이메일 다중 초대, 공유 초대 링크, 초대 수락
- 사용자 간 1:1 DM 생성과 목록 조회
- 첨부파일 업로드·검증·다운로드
- 일반·예약·AI 확정 메시지 전송
- 읽음 처리와 대화 목록의 읽지 않은 메시지 수
- UTC 저장 및 IANA 타임존 기반 발신자/조회자 현지 시각 표시
- LangGraph 기반 다단계 모호성 확인과 안전한 로컬 폴백
- 공통 이해 카드와 수신자 3지 응답
- 발신자 수정본과 revision 상태 전이
- 대화별 합의 기록과 파일 근거 스냅샷

채널·그룹 채팅, 메시지 수정/삭제, WebSocket 실시간 전송은 MVP 범위에 포함하지 않습니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.16
- Spring Security JWT Bearer 인증
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Gradle 8.14.3 Wrapper
- H2 테스트 DB
- Python 3.13, FastAPI, LangGraph

## 테스트

```powershell
.\gradlew.bat test
```

AI 서비스는 Docker Compose에서 내부 네트워크로만 노출된다. 로컬 mock 전체 실행:

```powershell
docker compose up -d --build
```

실제 OpenAI 호출은 `.env`의 `DITTO_LLM_MODE=live`와 `OPENAI_API_KEY`를 설정한다.
Spring 백엔드는 OpenAI 키를 직접 사용하지 않고 `http://ai:8000`의 내부 AI 서비스만 호출한다.

Windows에서 저장소 전체 경로에 한글이 포함된 경우 Gradle 테스트 워커의 클래스패스 인코딩 문제로 `ClassNotFoundException`이 발생할 수 있습니다. 이 경우 저장소를 영문 경로에 두거나 임시 드라이브에 연결한 경로에서 테스트합니다.

```powershell
subst X: "현재 저장소의 절대 경로"
X:
.\gradlew.bat test
subst X: /D
```

## 문서

- [API 명세](docs/API.md)
- [프론트엔드 API 인계](docs/FRONTEND_API_HANDOFF.md)
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI 3.0 JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI 3.0 YAML: `http://localhost:8080/v3/api-docs.yaml`
- [현재 범위 ERD와 설계 결정](docs/ERD.md)
- [개발 범위 및 다음 단계](docs/ROADMAP.md)
- [온보딩·이메일·배포 설정](docs/ONBOARDING.md)
