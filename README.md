# MOIDA Backend

**MOIDA(모이다)** — 중고거래 + 실시간 경매 마켓플레이스의 백엔드 API 서버.
Spring Boot 3.3 / JPA / MySQL 기반의 REST API + WebSocket(STOMP) 서버입니다.

> 프론트엔드 저장소: [`moida_front`](../moida_front) (React 19 + Vite SPA)

---

## 기술 스택

| 분류 | 사용 기술 |
|------|-----------|
| 언어 / 런타임 | **Java 17** |
| 프레임워크 | **Spring Boot 3.3.4** (Web, Data JPA, Security, Validation, WebSocket, Mail) |
| DB | **MySQL 8.x** + Hibernate, **QueryDSL 5.1** (동적 쿼리) |
| 인증 | **Spring Security + JWT** (jjwt 0.12.6, Stateless) |
| 실시간 | **WebSocket / STOMP**, **Redis Pub/Sub** (멀티 인스턴스 브로드캐스트 릴레이, prod 전용) |
| 스케줄링 | `@Scheduled` + **ShedLock** (다중 인스턴스 중복 실행 방지) |
| 외부 연동 | **AWS S3** (이미지 presigned URL), **Solapi** (SMS), SweetTracker (배송조회), Passwordless |
| 기타 | Lombok, Jackson(JSR-310) |

---

## 빠른 시작

### 1. MySQL 준비
```sql
CREATE DATABASE moida DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'moida'@'%' IDENTIFIED BY 'moida_pw';
GRANT ALL PRIVILEGES ON moida.* TO 'moida'@'%';
FLUSH PRIVILEGES;
```

### 2. 환경변수 (주요 항목)
```bash
SPRING_PROFILES_ACTIVE=local        # local(기본) | prod | test
DB_USERNAME=moida
DB_PASSWORD=moida_pw
JWT_SECRET=<32바이트 이상 시크릿 키>   # 운영에서는 반드시 환경변수로 주입 (커밋 금지)
# 선택: MAIL_USERNAME, AWS S3, Solapi, Redis, Passwordless 관련 키
```

### 3. 실행
```bash
./gradlew bootRun
```
기본 포트: **`http://localhost:9000`** (프론트 Vite dev 프록시 `/api` → `9000`)

---

## 프로젝트 구조

```
com.moida
├── MoidaBackendApplication
├── controller/          REST 컨트롤러 (43개, 도메인 무관하게 한 패키지에 집약)
├── domain/              도메인별 Entity · Repository · Service · Enum
│   ├── member/  auth/  passwordless/   회원 · 인증 · 패스워드리스
│   ├── product/ auction/ chat/         상품 · 경매/입찰 · 채팅
│   ├── wallet/  settlement/            지갑(포인트) · 정산
│   ├── notification/ review/ report/   알림 · 후기 · 신고
│   ├── inquiry/ faq/ guide/ notice/    문의 · FAQ · 가이드 · 공지
│   ├── category/ banner/ terms/        카테고리 · 배너 · 약관
│   ├── address/ tracking/ sanction/    주소 · 배송조회 · 제재
│   └── audit/                          관리자 감사 로그
├── common/
│   ├── request/         요청 DTO (도메인 공통 집약)
│   ├── response/        응답 DTO + 공통 ApiResponse<T>
│   ├── entity/          BaseTimeEntity (생성/수정 시각 자동 관리)
│   ├── exception/       BusinessException · ErrorCode · GlobalExceptionHandler
│   └── util/
├── config/              Security · CORS · WebSocket · S3 · ShedLock · Redis · DataInitializer
└── security/            JWT (TokenProvider · Filter · EntryPoint · UserDetails)
```

> **배치 컨벤션 참고**: 컨트롤러는 최상위 `controller/`에, 서비스·리포지토리·엔티티는 도메인 패키지에, DTO는 `common/request`·`common/response`에 모읍니다. 새 기능 추가 시 이 규칙을 따르세요.

---

## 주요 도메인 관계

- `Member` 1 ─ N `Product` (판매자)
- `Product` 1 ─ 1 `Auction` (경매 상품) ─ N `Bid`
- `Auction` 1 ─ 1 `Settlement` (낙찰 정산)
- `Product` 1 ─ N `Inquiry` / `ProductLike` / `ProductChatRoom`
- `ProductChatRoom` 1 ─ N `ProductChatMessage`
- `Member` 1 ─ N `Sanction` / `Review` / `Wallet` 거래내역 / `Notification`

---

## 인증 · 권한

JWT 기반 Stateless 인증. 요청 헤더:
```
Authorization: Bearer <accessToken>
```

| 역할 | 설명 |
|------|------|
| `ROLE_USER` | 일반 회원 |
| `ROLE_MANAGER` | 운영자 — `/api/admin/**` 접근 |
| `ROLE_ADMIN` | 관리자 — 운영자 권한 + 회원 역할변경 · 로그인/액션 로그 등 민감 기능 |

- **로그인 방식**: 일반(이메일/비밀번호), **소셜 로그인**(Kakao / Naver / Google OAuth), **Passwordless**
- **공개 엔드포인트(permitAll)**: `/api/auth/**`, `/api/public/health`, 상품·경매·카테고리·공지 등 `GET` 조회
- **인증 필요**: `/api/products/me`, `/bids/me`, `/purchases/me`, `/likes`, `/inquiries/me` 등 "내 것" 조회와 나머지 전체
- 인증/인가는 [`SecurityConfig`](src/main/java/com/moida/config/SecurityConfig.java)에서 일괄 정의하며, `JwtAuthenticationFilter`가 토큰을 검증해 `SecurityContext`에 등록합니다.

비즈니스 예외:
```java
throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
```

---

## 실시간 (WebSocket / STOMP)

- **실시간 입찰**, **상품 채팅**, **알림 푸시**에 STOMP over WebSocket 사용
- WebSocket 핸드셰이크는 단기 **티켓**(`/api/auth/ws-ticket`)으로 인증
- 운영 환경에서 EC2 다중 인스턴스 사용 시, 한 인스턴스의 브로드캐스트를 **Redis Pub/Sub**로 다른 인스턴스에 릴레이 (`config/realtime`, prod 프로파일에서만 활성)

---

## 프로파일

| 프로파일 | 용도 | 비고 |
|----------|------|------|
| `local` (기본) | 로컬 개발 | `application-local.yml` |
| `prod` | 운영 | Redis 릴레이 활성, `ddl-auto=validate/none` 권장 |
| `test` | 테스트 | `application-test.yml` |

---

## 빌드 · 배포

```bash
./gradlew clean bootJar      # 실행 가능한 JAR 생성 (build/libs)
```
- [`Dockerfile`](Dockerfile): Gradle 빌드 → `eclipse-temurin:17` 런타임 멀티스테이지 이미지

---

## 테스트

```bash
./gradlew test
```
현재 보안 경계 · 지갑 · 회원 도메인 위주의 테스트가 있습니다 (`src/test`).

---

## 운영 시 주의

- 운영에서는 `jpa.hibernate.ddl-auto`를 **`validate` 또는 `none`**으로 설정
- `JWT_SECRET` 등 시크릿은 **환경변수로 주입**, 커밋 금지
- 스키마 변경 관리를 위해 **Flyway / Liquibase** 도입 권장
