# MOIDA Backend Agent Guide

이 문서는 `moida_backend`에서 작업하는 AI 에이전트와 개발자를 위한 하네스 엔지니어링 가이드입니다.
목표는 변경 범위를 작게 유지하고, 프론트엔드와 API 계약을 깨지 않으며, 검증 가능한 상태로 작업을 마치는 것입니다.

## 1. 작업 범위

- 이 디렉터리는 MOIDA 서비스의 Spring Boot 백엔드입니다.
- 실제 백엔드 루트는 `moida_backend`입니다. `moida_frontend/moida_backend` 아래에 보이는 중복 백엔드 복사본은 명시 요청이 없으면 수정하지 않습니다.
- 프론트엔드 변경이 필요하면 `../moida_frontend/agents.md`의 규칙도 함께 확인합니다.

## 2. 기술 스택

- Java 17
- Spring Boot 3.3.4
- Spring Security + JWT
- Spring Data JPA + Hibernate
- QueryDSL 5.1
- MySQL 8.x
- Gradle Wrapper
- Lombok

## 3. 실행 및 검증 하네스

Windows PowerShell 기준 명령입니다.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

- 기본 서버 주소는 `http://localhost:9000`입니다.
- 로컬 DB는 `moida` 데이터베이스를 사용합니다.
- DB가 필요한 통합 검증은 MySQL 실행 여부와 `src/main/resources/application.yml` 설정을 먼저 확인합니다.
- 단순 컴파일/단위 테스트 검증은 `.\gradlew.bat test`를 우선 사용합니다.

## 4. 로컬 환경

기본 로컬 DB 예시:

```sql
CREATE DATABASE moida DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

민감 정보는 코드에 직접 쓰지 않습니다. 필요한 경우 환경 변수나 로컬 전용 설정으로 분리합니다.

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="mysql"
$env:JWT_SECRET="<32 bytes or longer secret>"
```

## 5. API 계약

- 프론트엔드는 `/api/*` 경로로 요청하고, Vite proxy가 백엔드 `localhost:9000`으로 전달합니다.
- JWT 인증 요청은 `Authorization: Bearer <token>` 헤더를 사용합니다.
- 응답 형태를 바꿀 때는 프론트엔드의 `src/api/*`, `src/types/index.ts`, 해당 페이지 컴포넌트를 함께 확인합니다.
- 새 API를 추가할 때는 요청 DTO, 응답 DTO, HTTP 상태, 에러 코드를 한 세트로 맞춥니다.

## 6. 패키지 구조

현재 코드는 도메인별 엔티티/서비스와 공통 컨트롤러 패키지가 섞여 있습니다. 기존 구조를 존중하되, 새 코드는 같은 기능 주변에 가깝게 둡니다.

```text
src/main/java/com/moida
├─ common
│  ├─ entity
│  ├─ exception
│  ├─ request
│  └─ response
├─ config
├─ controller
├─ domain
│  ├─ auction
│  ├─ category
│  ├─ member
│  ├─ product
│  └─ ...
└─ security
```

## 7. 코딩 규칙

- 컨트롤러는 요청 검증, 인증 사용자 추출, 서비스 호출, 응답 포장에 집중합니다.
- 비즈니스 규칙은 서비스 계층에 둡니다.
- JPA 엔티티는 무분별한 setter 추가를 피하고, 의미 있는 메서드로 상태 변경을 표현합니다.
- 에러는 `ErrorCode`, `BusinessException`, `GlobalExceptionHandler` 흐름을 우선 사용합니다.
- 공통 응답이 필요한 API는 `ApiResponse<T>` 사용 여부를 기존 컨트롤러와 맞춥니다.
- 인증 사용자가 필요하면 `@AuthenticationPrincipal CustomUserDetails user` 패턴을 우선 확인합니다.
- QueryDSL 생성물은 `src/main/generated`에 생성됩니다. 사람이 직접 편집하지 않습니다.

## 8. 테스트 기준

- 서비스 로직, 인증/권한 분기, 금액/입찰/상태 전이처럼 위험한 로직은 테스트를 추가합니다.
- 컨트롤러 응답 형태를 바꾸면 성공 케이스와 주요 실패 케이스를 확인합니다.
- DB가 필요한 테스트는 테스트 데이터 격리와 롤백 전략을 명확히 둡니다.
- 테스트가 현재 환경 문제로 실패하면, 실패 원인과 재현 명령을 작업 결과에 남깁니다.

## 9. 변경 전 체크리스트

- 변경하려는 기능의 프론트엔드 호출부가 있는지 확인했습니다.
- 요청/응답 DTO 이름과 필드가 실제 API 사용처와 맞습니다.
- 인증이 필요한 엔드포인트와 공개 엔드포인트를 구분했습니다.
- 새 설정값이나 비밀값을 저장소에 직접 추가하지 않았습니다.
- 검증 명령을 실행했거나, 실행하지 못한 이유를 기록했습니다.
