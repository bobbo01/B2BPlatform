# SupplyHub

SupplyHub는 기업 SSO, 회사 연결, 사용자 권한, 카탈로그 탐색을 중심으로 구현 중인 B2B 플랫폼입니다.

현재 구현 초점:

- OAuth2/OIDC 기반 로그인
- 외부 계정과 내부 사용자 연결
- 회사 생성 및 회사 참여 흐름
- 카테고리, 상품 카탈로그 조회

## 기술 스택

- Java 21
- Spring Boot 3.5.x
- Spring Security OAuth2 Client / OIDC
- Spring Data JPA
- MySQL
- Thymeleaf

## UI Typography

- Primary UI font: `Pretendard`
- New templates and style updates should keep `Pretendard` as the default and heading font unless a separate design decision is documented.

## UI Layout

- Use the shared page shell as the horizontal layout baseline.
- Page body sections should keep the same left and right gutter as the header instead of introducing a narrower outer container.

## 현재 코드 기준 구현 범위

- `Company`, `User`, `UserIdentity`, `Role` 중심 인증/연결 모델
- Google Workspace OIDC 로그인 경로
- 회사 생성 시 회사 초대코드 발급
- 카테고리/상품 카탈로그 조회

중요:

- 문서 정책 기준 역할 체계는 `PLATFORM_ADMIN`, `COMPANY_ADMIN`, `CART_USER`, `PURCHASER`, `APPROVER`
- 현재 코드의 역할 구조는 아직 이 목표 상태와 완전히 일치하지 않을 수 있음
- 최신 정책 기준 문서는 [docs/roles.md](./docs/roles.md)

## 문서

- 인증 흐름: [docs/auth-flow.md](./docs/auth-flow.md)
- 역할 정책: [docs/roles.md](./docs/roles.md)
- 스키마 설명: [docs/schema.md](./docs/schema.md)
- ERD 메모: [docs/erd.md](./docs/erd.md)
- 구현 계획: [docs/plan.md](./docs/plan.md)

## 실행

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```
