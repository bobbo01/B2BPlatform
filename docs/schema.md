# SupplyHub 스키마 설명

이 문서는 현재 구현 기준 테이블, 컬럼 의미, 유니크 제약, 상태값을 정리하는 기준 문서다.
권한 해석은 [roles.md](./roles.md), 로그인과 온보딩 흐름은 [auth-flow.md](./auth-flow.md), 관계 구조는 [erd.md](./erd.md)를 따른다.

## 문서 범위

- 현재 코드에 존재하는 주요 영속 모델만 다룬다.
- 이 문서는 테이블, 컬럼, 제약, 상태값 설명에 집중한다.
- 정책 방향 자체는 다른 기준 문서가 소유한다.

## 현재 핵심 테이블

- `companies`
- `users`
- `user_identities`
- `roles`
- `company_registration_requests`
- `company_join_requests`
- `first_company_admin_requests`
- `categories`
- `products`
- `carts`
- `cart_items`
- `purchase_requests`
- `purchase_request_items`
- `approval_requests`
- `purchase_orders`
- `purchase_order_items`

## `companies`

- PK: `company_id`
- 주요 컬럼:
- `company_name`
- `company_domain`
- `invite_code`
- `status`
- `creator_user_id`
- `created_at`
- `updated_at`

메모:

- `company_domain`은 유니크 제약이 있다.
- `invite_code`는 유니크 제약이 있다.
- 현재 주 사용 상태값은 `ACTIVE`, `INACTIVE`다.

## `users`

- PK: `user_id`
- 주요 컬럼:
- `company_id`
- `role_id`
- `company_admin`
- `email`
- `full_name`
- `phone`
- `status`
- `last_login_at`
- `created_at`
- `updated_at`

메모:

- `role_id`는 현재 구매 역할 해석에 사용한다.
- `company_admin`은 회사 운영 역할 여부를 표현한다.
- `email`은 전역 유니크 제약이 있다.
- `company_id`는 `PLATFORM_ADMIN` 또는 회사 미연결 active 사용자 상태에서 `null`일 수 있다.

## `user_identities`

- PK: `user_identity_id`
- 주요 컬럼:
- `user_id`
- `provider`
- `provider_user_id`
- `email`
- `email_verified`
- `last_login_at`
- `created_at`
- `updated_at`

메모:

- `(provider, provider_user_id)` 복합 유니크 제약이 있다.

## `roles`

- PK: `role_id`
- 주요 컬럼:
- `role_name`
- `description`

기본 역할명:

- `PLATFORM_ADMIN`
- `CART_USER`
- `PURCHASER`
- `APPROVER`

## `company_registration_requests`

- PK: `company_registration_request_id`
- 주요 컬럼:
- `provider`
- `provider_user_id`
- `email`
- `email_verified`
- `requester_name`
- `requester_phone`
- `requested_company_name`
- `requested_company_domain`
- `status`
- `reviewed_by_user_id`
- `review_memo`
- `rejection_reason`
- `reviewed_at`
- `created_at`
- `updated_at`

메모:

- 현재 상태값은 `PENDING`, `APPROVED`, `REJECTED`다.
- `(provider, provider_user_id)` 기준 pending 중복 방지를 둔다.
- `requested_company_domain` 기준 pending 중복 방지를 둔다.

## `company_join_requests`

- PK: `company_join_request_id`
- 주요 컬럼:
- `company_id`
- `provider`
- `provider_user_id`
- `requested_email`
- `requested_name`
- `status`
- `reviewed_by_user_id`
- `review_memo`
- `rejection_reason`
- `reviewed_at`
- `created_at`
- `updated_at`

메모:

- 현재 상태값은 `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`다.

## `first_company_admin_requests`

- PK: `first_company_admin_request_id`
- 주요 컬럼:
- `company_id`
- `requester_user_id`
- `reviewed_by_user_id`
- `status`
- `review_note`
- `reviewed_at`
- `created_at`
- `updated_at`

메모:

- 현재 상태값은 `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`다.

## `categories`

- PK: `category_id`
- 주요 컬럼:
- `parent_category_id`
- `category_name`
- `category_code`
- `sort_order`
- `is_active`
- `created_at`
- `updated_at`

메모:

- `category_code`는 유니크 제약이 있다.

## `products`

- PK: `product_id`
- 주요 컬럼:
- `category_id`
- `sku`
- `product_name`
- `brand`
- `description`
- `image_url`
- `unit_price`
- `currency_code`
- `min_order_qty`
- `is_active`
- `created_at`
- `updated_at`

메모:

- `sku`는 유니크 제약이 있다.
- `unit_price`는 `BigDecimal` 기반이다.

## `carts`

- PK: `cart_id`
- 주요 컬럼:
- `company_id`
- `owner_user_id`
- `status`
- `checked_out_at`
- `created_at`
- `updated_at`

상태값:

- `OPEN`
- `CHECKED_OUT`
- `ABANDONED`

## `cart_items`

- PK: `cart_item_id`
- 주요 컬럼:
- `cart_id`
- `product_id`
- `quantity`
- `unit_price`
- `currency_code`
- `created_at`
- `updated_at`

메모:

- `quantity`는 1 이상이다.
- `unit_price`, `currency_code`는 상품 값을 복사한 스냅샷이다.

## `purchase_requests`

- PK: `purchase_request_id`
- 주요 컬럼:
- `company_id`
- `requester_user_id`
- `source_cart_id`
- `status`
- `submitted_at`
- `approved_at`
- `rejected_at`
- `cancelled_at`
- `created_at`
- `updated_at`

상태값:

- `DRAFT`
- `SUBMITTED`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

## `purchase_request_items`

- PK: `purchase_request_item_id`
- 주요 컬럼:
- `purchase_request_id`
- `product_id`
- `quantity`
- `unit_price`
- `currency_code`
- `created_at`
- `updated_at`

## `approval_requests`

- PK: `approval_request_id`
- 주요 컬럼:
- `company_id`
- `purchase_request_id`
- `requester_user_id`
- `approver_user_id`
- `status`
- `decision_note`
- `decided_at`
- `created_at`
- `updated_at`

상태값:

- `PENDING`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

## `purchase_orders`

- PK: `purchase_order_id`
- 주요 컬럼:
- `company_id`
- `purchase_request_id`
- `buyer_user_id`
- `status`
- `subtotal_amount`
- `total_amount`
- `currency_code`
- `submitted_for_platform_approval_at`
- `placed_at`
- `paid_at`
- `ready_to_ship_at`
- `shipped_at`
- `delivered_at`
- `cancelled_at`
- `platform_reviewed_by_user_id`
- `platform_reviewed_at`
- `platform_review_memo`
- `platform_rejection_reason`
- `created_at`
- `updated_at`

상태값:

- `DRAFT`
- `PENDING_PLATFORM_APPROVAL`
- `PAYMENT_PENDING`
- `PAID`
- `READY_TO_SHIP`
- `SHIPPED`
- `DELIVERED`
- `REJECTED`
- `CANCELLED`

메모:

- `PLATFORM_ADMIN` 승인 시 주문은 확정되며 곧바로 `PAYMENT_PENDING` 상태가 된다.
- 현재 기준에서 주문은 `PAYMENT_PENDING` 이전까지 초안 또는 플랫폼 승인 대기 상태로 본다.
- `APPROVER`는 `DRAFT` 주문만 플랫폼 승인 대기로 제출할 수 있다.
- 주문 취소는 `DRAFT`, `PENDING_PLATFORM_APPROVAL` 상태에서만 허용한다.
- `PAYMENT_PENDING` 이후에는 주문 품목, 수량, 금액을 수정하거나 취소하지 않는다.
- `paid_at`은 사용자가 결제하기를 눌러 `PAID`로 전이된 시각이다.
- `ready_to_ship_at`은 `PLATFORM_ADMIN`이 배송 준비 처리한 시각이다.
- `shipped_at`은 `PLATFORM_ADMIN`이 발송 처리한 시각이다.
- `delivered_at`은 `PLATFORM_ADMIN`이 배송 완료 처리한 시각이다.
- `REJECTED`는 종료 상태이며 같은 주문을 다시 `DRAFT`로 되돌리지 않는다.
- `subtotal_amount`, `total_amount`, `currency_code`는 주문 초안 생성 시점의 금액 스냅샷이다.
- 현재 주문 통화는 `KRW`만 허용한다.
- `PAYMENT_PENDING`, `PAID`, `READY_TO_SHIP`, `SHIPPED`, `DELIVERED`는 주문확정 이후 상태 모델이다.
- 현재 결제 단계는 실제 PG 연동 없이 `PAYMENT_PENDING -> PAID` 상태 전이와 결제 시각 기록만 구현한다.
- 현재 배송 단계는 실제 택배 연동 없이 `PAID -> READY_TO_SHIP -> SHIPPED -> DELIVERED` 상태 전이와 시각 기록만 구현한다.
- 정산 상태는 아직 현재 구현 범위 밖이다.

## `purchase_order_items`

- PK: `purchase_order_item_id`
- 주요 컬럼:
- `purchase_order_id`
- `product_id`
- `quantity`
- `unit_price`
- `currency_code`
- `created_at`
- `updated_at`

## 금액 스냅샷 규칙

- `products.unit_price`는 카탈로그 기준 가격이다.
- `cart_items.unit_price`
- `purchase_request_items.unit_price`
- `purchase_order_items.unit_price`

위 값들은 각 단계에서 복사된 스냅샷이다.

- `purchase_orders.subtotal_amount`
- `purchase_orders.total_amount`
- `purchase_orders.currency_code`

위 값들은 주문 초안 생성 시점에 계산되어 저장되는 주문 단위 스냅샷이다.

## 현재 핵심 유니크 제약

- `companies.company_domain`
- `companies.invite_code`
- `users.email`
- `roles.role_name`
- `categories.category_code`
- `products.sku`
- `user_identities(provider, provider_user_id)`

## 현재 범위 밖

- 결제 테이블
- 배송 테이블
- 재고 테이블
- 정산 테이블
- 다단계 승인 전용 저장 모델
## `purchase_order_status_history`

- PK: `purchase_order_status_history_id`
- 주요 컬럼:
- `purchase_order_id`
- `from_status`
- `to_status`
- `changed_by_user_id`
- `change_note`
- `changed_at`

## Settlement 1st Phase

- Settlement is separate from customer payment.
- Settlement is stored per purchase order, not as a separate batch entity.
- Only `DELIVERED` orders are settlement targets in the current scope.
- `purchase_orders.settlement_status` uses `UNSETTLED` and `SETTLED`.
- `purchase_orders.settled_at` stores when settlement was completed.
- `purchase_orders.settled_by_user_id` references the `USER` who completed settlement.
- Existing rows may have a null settlement column value during local schema update; application logic treats that as `UNSETTLED`.
- Settlement does not write to `purchase_order_status_history` in the current scope because it is not part of the order-status lifecycle.

메모:

- `purchase_orders`는 최신 상태와 핵심 시각 컬럼을 유지한다.
- `purchase_order_status_history`는 상태 변경 이력을 append-only로 저장한다.
- 주문 초안 자동 생성은 현재 이력 테이블에 기록하지 않는다.
- `changed_by_user_id`는 구매자 제출/결제와 플랫폼 관리자 승인/반려/배송 처리 주체를 저장한다.
- `change_note`는 플랫폼 승인 메모, 반려 사유, 결제 액션 같은 상태 전이 메모를 저장할 수 있다.
