# 온보딩·이메일·배포 설정

## 인증 방식

인증은 이메일/비밀번호 로그인과 JWT Bearer 토큰만 사용한다.

```http
Authorization: Bearer {accessToken}
```

소셜 로그인과 외부 캘린더 연동은 MVP 범위에서 제외한다.

## 온보딩 API 순서

### 1. 이메일 인증

1. `POST /api/v1/auth/email-verifications`로 이메일 인증코드를 요청한다.
2. 사용자가 받은 숫자 6자리를 `POST /api/v1/auth/email-verifications/confirm`으로 확인한다.
3. 응답의 `verificationToken`을 회원가입 요청에 포함한다.

인증코드는 10분 동안 유효하고 60초 후 재발송할 수 있다. 5회 이상 틀리면 새 코드를 받아야 한다.

### 2. 계정 생성

`POST /api/v1/auth/signup`

```json
{
  "email": "seoyeon@example.com",
  "password": "password123!",
  "displayName": "이서연",
  "emailVerificationToken": "UUID",
  "termsAccepted": true
}
```

응답의 `accessToken`을 이후 온보딩 API에 Bearer 토큰으로 사용한다.

### 3. 프로필 저장

`PATCH /api/v1/users/me/profile`

```json
{
  "displayName": "이서연",
  "role": "PROJECT_MANAGER",
  "customRole": null,
  "preferredLanguage": "ko"
}
```

프로필 사진은 `PUT /api/v1/users/me/profile-image`에 `multipart/form-data`의 `file` 필드로 전송한다. JPG, PNG, WEBP 형식만 허용하며 최대 5MB다.

### 4. 근무 컨텍스트 저장

`PATCH /api/v1/users/me/work-context`

```json
{
  "timeZoneId": "Asia/Seoul",
  "workStart": "09:00:00",
  "workEnd": "18:00:00",
  "workDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
}
```

`onboardingStep`은 `PROFILE → WORK_CONTEXT → WORKSPACE → COMPLETED` 순서로 진행한다.

## 이메일 발송 설정

Gmail SMTP를 사용할 경우 발송 계정에서 2단계 인증을 켜고 앱 비밀번호를 발급한다.

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=sender@gmail.com
MAIL_PASSWORD=16자리_앱_비밀번호
MAIL_FROM=sender@gmail.com
```

운영 발송량이 커지면 AWS SES, SendGrid, Mailgun 같은 트랜잭션 메일 서비스로 교체한다.

## Docker 배포 주소

컨테이너 내부의 `8080` 포트는 Docker 호스트의 포트 또는 HTTPS 리버스 프록시를 통해 공개한다.

```env
PUBLIC_BASE_URL=https://api.example.com
FRONTEND_BASE_URL=https://app.example.com
CORS_ALLOWED_ORIGINS=https://app.example.com
OPENAI_API_KEY=프로젝트_API_키
DITTO_LLM_MODE=live
DITTO_OPENAI_MODEL=o3-mini
DITTO_INTERNAL_API_KEY=백엔드와_AI서비스가_공유할_랜덤값
```

프론트는 배포된 `PUBLIC_BASE_URL`의 로그인 API에서 JWT를 받고 이후 요청의 Authorization 헤더에 넣는다.

## 프로필 파일 보존

현재 Docker Compose는 `async-align-uploads` 볼륨에 프로필 이미지와 대화 첨부파일을 보존한다. 단일 서버 MVP에는 충분하지만 여러 인스턴스를 운영하면 S3 호환 객체 스토리지로 교체해야 한다.

## AI 검토 설정

- Docker Compose는 Spring과 Python/LangGraph AI 서비스를 별도 컨테이너로 실행한다.
- `DITTO_LLM_MODE=mock`은 키 없이 테스트하고, `live`는 `OPENAI_API_KEY`로 실제 호출한다.
- OpenAI 키는 AI 컨테이너에만 전달하고 Spring 컨테이너에는 전달하지 않는다.
- AI 서비스 호출에 실패하면 모호한 값을 추측하지 않는 로컬 분석으로 자동 폴백한다.
- 로컬 폴백에서도 발신자가 업무·담당자·정확한 기한·기대 결과를 직접 확정해야 전송할 수 있다.
