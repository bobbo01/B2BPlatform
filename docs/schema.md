# B2B 사무용품 플랫폼 Schema

이 문서는 [erd.md](./erd.md)의 상세 컬럼 설명 문서입니다.

## 컬럼 설명

### `COMPANY`

- `company_id`: 고객사 식별자
- `company_name`: 고객사명
- `business_registration_no`: 사업자등록번호
- `tax_id`: 세금 식별 번호
- `industry_type`: 업종 유형
- `status`: 고객사 상태
- `created_at`: 생성 일시
- `updated_at`: 수정 일시

### `USER`

- `user_id`: 사용자 식별자
- `company_id`: 소속 고객사 식별자
- `role_id`: 사용자 권한 식별자
- `email`: 로그인 이메일
- `password_hash`: 암호화된 비밀번호
- `full_name`: 사용자 이름
- `phone`: 연락처
- `status`: 사용자 상태
- `last_login_at`: 마지막 로그인 일시
- `created_at`: 생성 일시
- `updated_at`: 수정 일시

### `ROLE`

- `role_id`: 권한 식별자
- `role_name`: 권한명
- `description`: 권한 설명

### `ADDRESS`

- `address_id`: 주소 식별자
- `company_id`: 소속 고객사 식별자
- `address_type`: 주소 유형
- `recipient_name`: 수령인 이름
- `phone`: 수령지 연락처
- `postal_code`: 우편번호
- `address_line1`: 기본 주소
- `address_line2`: 상세 주소
- `city`: 도시
- `state`: 주 또는 지역
- `country`: 국가
- `is_default`: 기본 주소 여부

### `CATEGORY`

- `category_id`: 카테고리 식별자
- `parent_category_id`: 상위 카테고리 식별자
- `category_name`: 카테고리명
- `category_code`: 카테고리 코드
- `sort_order`: 정렬 순서
- `is_active`: 사용 여부

### `SUPPLIER`

- `supplier_id`: 공급사 식별자
- `supplier_name`: 공급사명
- `contact_name`: 담당자명
- `contact_email`: 담당자 이메일
- `contact_phone`: 담당자 연락처
- `status`: 공급사 상태

### `PRODUCT`

- `product_id`: 상품 식별자
- `category_id`: 상품 카테고리 식별자
- `supplier_id`: 공급사 식별자
- `sku`: 상품 관리 코드
- `product_name`: 상품명
- `brand`: 브랜드명
- `description`: 상품 설명
- `unit_price`: 판매 단가
- `currency_code`: 통화 코드
- `min_order_qty`: 최소 주문 수량
- `is_active`: 판매 여부
- `created_at`: 생성 일시
- `updated_at`: 수정 일시

### `CART`

- `cart_id`: 장바구니 식별자
- `company_id`: 장바구니 소유 고객사 식별자
- `user_id`: 장바구니 생성 사용자 식별자
- `status`: 장바구니 상태
- `created_at`: 생성 일시
- `updated_at`: 수정 일시

### `CART_ITEM`

- `cart_item_id`: 장바구니 항목 식별자
- `cart_id`: 소속 장바구니 식별자
- `product_id`: 상품 식별자
- `quantity`: 담은 수량
- `unit_price`: 담은 시점의 단가
- `created_at`: 생성 일시

### `PURCHASE_ORDER`

- `order_id`: 주문 식별자
- `company_id`: 주문 고객사 식별자
- `user_id`: 주문 생성 사용자 식별자
- `billing_address_id`: 청구지 식별자
- `shipping_address_id`: 배송지 식별자
- `order_number`: 주문번호
- `order_status`: 주문 상태
- `subtotal_amount`: 상품 합계 금액
- `tax_amount`: 세금 금액
- `shipping_amount`: 배송비
- `total_amount`: 최종 결제 금액
- `currency_code`: 통화 코드
- `ordered_at`: 주문 확정 일시
- `created_by`: 생성 사용자 식별자
- `updated_by`: 수정 사용자 식별자
- `created_at`: 생성 일시
- `updated_at`: 수정 일시

### `ORDER_ITEM`

- `order_item_id`: 주문 항목 식별자
- `order_id`: 소속 주문 식별자
- `product_id`: 주문 상품 식별자
- `quantity`: 주문 수량
- `unit_price`: 주문 단가
- `discount_amount`: 할인 금액
- `line_total_amount`: 항목 합계 금액
- `created_by`: 생성 사용자 식별자
- `created_at`: 생성 일시
- `updated_by`: 수정 사용자 식별자
- `updated_at`: 수정 일시

### `APPROVAL`

- `approval_id`: 승인 식별자
- `order_id`: 승인 대상 주문 식별자
- `approver_user_id`: 승인 사용자 식별자
- `approval_status`: 승인 상태
- `approval_level`: 승인 단계
- `comment`: 승인 의견
- `created_by`: 생성 사용자 식별자
- `created_at`: 생성 일시
- `updated_by`: 수정 사용자 식별자
- `updated_at`: 수정 일시
- `decided_at`: 승인 또는 반려 처리 일시

### `SHIPMENT`

- `shipment_id`: 배송 식별자
- `order_id`: 원주문 식별자
- `shipment_number`: 배송번호
- `carrier_name`: 배송사명
- `tracking_number`: 운송장번호
- `shipment_status`: 배송 상태
- `shipped_at`: 출고 일시
- `delivered_at`: 배송 완료 일시

### `SHIPMENT_ITEM`

- `shipment_item_id`: 배송 항목 식별자
- `shipment_id`: 소속 배송 식별자
- `product_id`: 출고 상품 식별자
- `shipped_quantity`: 출고 수량

### `INVOICE`

- `invoice_id`: 청구서 식별자
- `order_id`: 대상 주문 식별자
- `company_id`: 청구 대상 고객사 식별자
- `invoice_number`: 청구서 번호
- `invoice_status`: 청구서 상태
- `invoice_amount`: 청구 금액
- `created_by`: 생성 사용자 식별자
- `created_at`: 생성 일시
- `updated_by`: 수정 사용자 식별자
- `updated_at`: 수정 일시
- `issued_at`: 발행 일시
- `due_at`: 납기 일시
- `paid_at`: 수납 완료 일시

### `PAYMENT`

- `payment_id`: 결제 식별자
- `invoice_id`: 대상 청구서 식별자
- `payment_method`: 결제 수단
- `paid_amount`: 결제 금액
- `payment_status`: 결제 상태
- `transaction_reference`: 결제 거래 참조값
- `created_by`: 생성 사용자 식별자
- `created_at`: 생성 일시
- `updated_by`: 수정 사용자 식별자
- `updated_at`: 수정 일시
- `paid_at`: 결제 일시

### `WAREHOUSE`

- `warehouse_id`: 창고 식별자
- `warehouse_name`: 창고명
- `warehouse_code`: 창고 코드
- `phone`: 창고 연락처
- `status`: 창고 상태

### `INVENTORY`

- `inventory_id`: 재고 식별자
- `warehouse_id`: 창고 식별자
- `product_id`: 상품 식별자
- `on_hand_qty`: 현재 보유 수량
- `reserved_qty`: 예약 수량
- `available_qty`: 사용 가능 수량
- `updated_at`: 수정 일시

### `INVENTORY_TRANSACTION`

- `inventory_tx_id`: 재고 이력 식별자
- `inventory_id`: 대상 재고 식별자
- `tx_type`: 재고 변동 유형
- `quantity_delta`: 수량 변화값
- `reference_type`: 참조 대상 유형
- `reference_id`: 참조 대상 식별자
- `created_at`: 생성 일시
