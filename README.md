# MOIDA Backend

중고거래 + 경매 마켓플레이스 MOIDA의 백엔드 (Spring Boot 3.3 + JPA + MySQL).

## 기술 스택

- **Java 17** / **Spring Boot 3.3.4**
- **Spring Security + JWT** (Stateless 인증)
- **Spring Data JPA** + Hibernate
- **MySQL 8.x**
- **QueryDSL 5.1** (복잡한 동적 쿼리)
- **WebSocket** (실시간 입찰 / 채팅용)
- **Lombok**

## 시작하기

### 1. MySQL 데이터베이스 생성
```sql
CREATE DATABASE moida DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'moida'@'%' IDENTIFIED BY 'moida_pw';
GRANT ALL PRIVILEGES ON moida.* TO 'moida'@'%';
FLUSH PRIVILEGES;
```

### 2. 환경변수 설정 (선택)
```
DB_USERNAME=moida
DB_PASSWORD=moida_pw
JWT_SECRET=<32바이트 이상의 시크릿 키>
```

### 3. 실행
```bash
./gradlew bootRun
```

기본 포트: `http://localhost:8080`

## 패키지 구조

```
com.moida
├── MoidaBackendApplication
├── common
│   ├── entity        BaseTimeEntity (생성/수정 시각 자동 관리)
│   ├── exception     BusinessException, ErrorCode, GlobalExceptionHandler
│   └── response      ApiResponse<T>
├── config            SecurityConfig, CorsConfig
├── security          JWT 관련 (TokenProvider, Filter, EntryPoint, ...)
├── controller        REST 엔드포인트 (팀에서 작업)
├── service           비즈니스 로직 (팀에서 작업)
└── domain
    ├── member        Member, MemberRole, MemberStatus
    ├── category      Category
    ├── product       Product, ProductImage, ProductLike, enums
    ├── auction       Auction, Bid, AuctionStatus
    ├── inquiry       Inquiry
    ├── notice        Notice
    ├── banner        Banner
    ├── report        Report
    ├── sanction      Sanction
    ├── chat          ChatRoom, ChatMessage
    ├── settlement    Settlement
    ├── review        Review
    └── notification  Notification
```

## 주요 도메인 관계

- `Member` 1 ─ N `Product` (판매자)
- `Product` 1 ─ 1 `Auction` (경매 상품인 경우)
- `Auction` 1 ─ N `Bid`
- `Product` 1 ─ N `Inquiry`
- `Product` 1 ─ N `ProductLike` ─ N 1 `Member`
- `Auction` 1 ─ 1 `Settlement`
- `Member` 1 ─ N `Sanction`
- `ChatRoom` 1 ─ N `ChatMessage`

## Spring Security

JWT 기반 인증. 헤더 형식:
```
Authorization: Bearer <token>
```

기본 권한:
- `ROLE_USER`: 일반 회원
- `ROLE_ADMIN`: 관리자 (`/api/admin/**` 접근 가능)

비공개 엔드포인트는 `JwtAuthenticationFilter`가 토큰을 검증하여 `SecurityContext`에 인증 정보를 등록.

## 기본 공개 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/**` | 로그인/회원가입 (구현 예정) |
| GET | `/api/public/health` | 헬스체크 |
| GET | `/api/products/**` | 상품 조회 |
| GET | `/api/auctions/**` | 경매 조회 |
| GET | `/api/categories/**` | 카테고리 조회 |
| GET | `/api/notices/**` | 공지 조회 |
| GET | `/api/banners/**` | 배너 조회 |

## 작업 가이드 (Controller / Service)

도메인별 패키지 안에 `controller`, `service`, `dto` 하위 패키지를 만들어 사용하세요. 예시:
```
com.moida.domain.product
├── Product.java
├── ProductRepository.java
├── controller
│   └── ProductController.java
├── service
│   └── ProductService.java
└── dto
    ├── ProductCreateRequest.java
    └── ProductResponse.java
```

인증된 사용자 정보 가져오기:
```java
@GetMapping("/me")
public ApiResponse<...> me(@AuthenticationPrincipal CustomUserDetails user) {
    Long memberId = user.getMemberId();
    ...
}
```

비즈니스 예외 처리:
```java
throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
```

## 운영 시 주의

- `application.yml`의 `jpa.hibernate.ddl-auto`를 운영에서 반드시 `validate` 또는 `none`으로 변경
- `JWT_SECRET`은 환경변수로 주입 (커밋 금지)
- 운영 환경에서는 Flyway / Liquibase 도입 권장
