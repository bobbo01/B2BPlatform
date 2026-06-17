# SupplyHub 구현 계획

이 문서는 SupplyHub의 상위 계획, 우선순위, 완료 기준을 관리하는 마스터 문서다.
세부 정책은 각 기준 문서가 소유한다.

- 로그인 및 온보딩 흐름: [auth-flow.md](./auth-flow.md)
- 역할별 권한과 제약: [roles.md](./roles.md)
- 테이블, 컬럼, 제약: [schema.md](./schema.md)
- 엔티티 관계: [erd.md](./erd.md)
- 구현 진행 추적: [implementation-checklist.md](./implementation-checklist.md)

## 문서 역할

- 이 문서는 무엇을 왜 먼저 구현하는지 정리한다.
- 세부 정책 문장을 반복하지 않는다.
- 정책이 바뀌면 이 문서와 해당 기준 문서를 함께 갱신한다.

## 현재 제품 목표

- 기업 사용자가 OAuth2/OIDC로 로그인한다.
- 로그인 후 회사와 연결된다.
- 연결된 사용자가 카탈로그를 탐색하고 기본 구매 흐름에 진입한다.
- 회사 운영 권한과 구매 권한을 분리된 개념으로 유지한다.
- 현재 범위에서는 요청 기반 회사 온보딩과 1단계 구매 승인 흐름을 안정화한다.

## 현재 기준 요약

- 회사는 핵심 테넌시 경계다.
- 회사 미연결 로그인 사용자는 카탈로그 조회만 가능하다.
- 기존 회사 참여는 즉시 연결이 아니라 요청-승인 흐름으로 처리한다.
- 첫 `COMPANY_ADMIN` 부여는 회사 온보딩이 아니라 별도 권한 확장 단계로 다룬다.
- `PLATFORM_ADMIN`은 회사 사용자 흐름과 분리된 플랫폼 운영 계정으로 해석한다.
- `PLATFORM_ADMIN` 승인 시점이 주문확정 시점이다.
- 주문, 승인, 결제, 배송, 정산 영역은 상태 이력 보존과 생성 시점 스냅샷 고정을 강한 원칙으로 유지한다.

## 현재 구현 범위

- OAuth2/OIDC 로그인
- `UserIdentity` 기반 외부 계정 연결
- 신규 회사 등록 요청 및 플랫폼 승인 흐름
- 기존 회사 참여 요청 및 회사 관리자 승인 흐름
- 카테고리/상품 카탈로그 조회
- 장바구니
- 구매 요청 초안 생성 및 제출
- 1단계 승인/거절
- 승인 후 주문 초안 생성
- 플랫폼 관리자 전용 `/admin` 진입 흐름

## 핵심 차이

- 역할 저장 구조와 역할 해석이 아직 완전히 정리되지 않았다.
- 회사 관리자 기능이 일부는 `/workspace`에 남아 있다.
- 커머스 흐름은 동작하지만 후속 상태 모델과 화면이 아직 얕다.

## 우선순위

### 1. 문서 기준 정리

- `plan.md`를 상위 계획의 단일 기준으로 유지한다.
- `.codex/handoff/next-session.md`는 임시 인수인계 메모 전용으로 제한한다.
- `implementation-checklist.md`로 실행 추적을 분리한다.
- 세부 기준 문서는 각 역할에 맞게 계속 유지·정리한다.

### 2. 역할 모델 정합화

- 운영 역할과 구매 역할 해석을 서비스 전반에서 일관되게 맞춘다.
- `PLATFORM_ADMIN`과 회사 사용자 계정의 제약을 코드에서 더 명확히 고정한다.
- 역할 변경 규칙과 권한 판단을 테스트로 보강한다.

### 3. 온보딩 및 권한 확장 안정화

- 신규 회사 등록 요청 흐름을 안정화한다.
- 기존 회사 참여 요청 흐름의 예외 처리와 운영 UX를 보강한다.
- 첫 `COMPANY_ADMIN` 요청의 중복 방지와 운영 처리를 더 단단히 만든다.

### 4. 회사 관리자 기능 정리

- 자기 회사 범위 사용자 관리 기능을 정리한다.
- 초대코드 조회, 재발급, 폐기 기능을 정리한다.
- 필요 시 회사 관리자 전용 운영 화면을 `/workspace`에서 분리한다.

### 5. 구매/승인 흐름 확장

- 역할별 기능 제한을 실제 화면과 서비스에 일관되게 반영한다.
- `APPROVED` 상태를 플랫폼 승인에 따른 주문확정 상태로 유지한다.
- 주문확정 이후 상태는 우선 `PAYMENT_PENDING`, `PAID`, `READY_TO_SHIP`, `SHIPPED`, `DELIVERED` 범위로 확장한다.
- `APPROVER`는 주문 초안을 플랫폼 승인 대기로 제출할 수 있다.
- `PLATFORM_ADMIN`은 제출된 주문에 대해 승인 또는 반려만 수행한다.
- 플랫폼 승인 시 주문은 확정되며 곧바로 `PAYMENT_PENDING`으로 전이한다.
- `PAYMENT_PENDING` 이후에는 주문 품목, 수량, 금액을 수정하지 않는다.
- `REJECTED` 주문은 종료 상태로 보고 같은 주문을 `DRAFT`로 되돌리지 않는다.
- 주문 금액 스냅샷은 주문 초안 생성 시점에 `subtotal_amount`, `total_amount`, `currency_code`로 고정한다.
- 현재 주문 통화는 `KRW` 단일 통화만 허용한다.
- 배송 운영은 `PLATFORM_ADMIN`이 `/admin/orders?orderFilter=delivery`에서 처리한다.
- 주문 초안 후속 처리와 주문 이후 상태 모델을 확장한다.
- 결제, 배송, 정산, 재고는 후속 단계에서 문서와 함께 확장한다.

## 다음 마일스톤

- 다음 마일스톤은 `주문 상태 이력 UI 노출 + 관리자/회사 사용자 주문 화면 마감 + 권한/화면 흐름 회귀 테스트 확대`로 잡는다.
- 이번 마일스톤에서는 이미 저장 중인 `purchase_order_status_history`를 사용자와 운영자가 확인 가능한 형태로 노출하는 데 집중한다.
- 현재 주문 화면은 상태 라벨, 진행 단계, 액션 버튼, 오류 처리까지 포함해 큰 기능 구멍 없이 닫히는 수준을 목표로 한다.
- 권한 검증은 서비스 레벨뿐 아니라 컨트롤러와 화면 액션 연결까지 테스트로 고정한다.
- `정산 상태` 도입은 이번 마일스톤 완료 조건에서 제외하고 후속 단계로 미룬다.

## 현재 범위 밖

- SCIM
- 회사별 다중 IdP 관리
- 이메일 초대
- 다단계 승인 세부 모델
- 외부 ERP 실연동
- 실제 PG 결제 처리
- 실배송 운영
- 운영 감사 도구 고도화

## 완료 기준

- `plan.md`, `auth-flow.md`, `roles.md`, `schema.md`, `erd.md`의 역할 경계가 겹치지 않는다.
- `.codex/handoff/next-session.md`에는 임시 인수인계 메모만 남는다.
- 실행 상태는 `implementation-checklist.md`에서 추적한다.
- 로그인, 회사 연결, 역할 해석, 구매 기본 흐름이 현재 코드와 기준 문서에서 서로 크게 어긋나지 않는다.
- 주문 상태 이력은 `purchase_order_status_history`에 append-only로 저장하고, `purchase_orders`는 최신 상태와 핵심 시각 컬럼을 유지한다.


## Settlement 1st Phase Update

- Settlement is now in the implemented scope.
- Scope: per-order settlement after delivery, tracked on purchase_orders.
- Admin /admin/settlements shows total sales, unsettled sales, and settled sales.
- Current phase supports single-order settlement and bulk settlement without a separate batch entity.

