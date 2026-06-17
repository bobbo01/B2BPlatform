# SupplyHub 구현 체크리스트

이 문서는 현재 구현 완료 항목, 진행 상태, 다음 작업을 추적한다.  
상위 방향과 우선순위는 [plan.md](./plan.md)를 기준으로 본다.

## 운영 지침

- 방향이나 우선순위가 바뀌면 먼저 `plan.md`와 관련 기준 문서를 수정한다.
- 이 문서에는 구현 상태, 다음 액션, 짧은 메모만 유지한다.
- 임시 handoff 메모는 `.codex/handoff/next-session.md`를 따른다.

## 현재 구현 완료 항목

### 로그인과 회사 연결

- [x] OAuth2/OIDC 로그인
- [x] `UserIdentity` 기반 멀티 계정 연결
- [x] 신규 회사 등록 요청 생성
- [x] 신규 회사 등록 요청에 대한 `PLATFORM_ADMIN` 승인/반려
- [x] 기존 회사 참여 요청 생성
- [x] 기존 회사 참여 요청에 대한 `COMPANY_ADMIN` 승인/반려
- [x] 승인/반려 시 `review_memo`, `rejection_reason` 처리
- [x] 회사 상태와 사용자 상태 기반 접근 제한

### 역할과 운영

- [x] 기본 구매 역할 시드 구성
- [x] `PLATFORM_ADMIN` 전용 `/admin` 진입 흐름
- [x] `company_admin` 기반 회사 운영 역할 분리
- [x] 마지막 `COMPANY_ADMIN` 보호 규칙 구현
- [x] 역할 모델 strict cleanup
  - `COMPANY_ADMIN` persisted role 제거
  - `company_admin` 플래그 기반 해석으로 통일
  - legacy fallback 제거

### 커머스 1차 흐름

- [x] 카탈로그 조회
- [x] 장바구니 담기, 수량 수정, 항목 제거, 비우기
- [x] 최소 주문 수량 검증
- [x] 구매 요청 초안 생성, 조회, 제출, 취소
- [x] 승인함 조회 및 승인/거절 처리
- [x] 승인된 요청에서 주문 초안 자동 생성
- [x] 주문 초안 상세 조회
- [x] 주문 초안 최종 처리 권한을 `APPROVER` 기준으로 제한
- [x] `APPROVER`가 주문 초안을 플랫폼 승인 대기로 제출
- [x] `PLATFORM_ADMIN`의 주문 승인/반려 처리

### 주문 이후 상태 확장

- [x] 플랫폼 승인 이후 주문 확정 기준 고정
- [x] 주문 상태 모델에 `PAYMENT_PENDING`, `PAID`, `READY_TO_SHIP`, `SHIPPED`, `DELIVERED` 추가
- [x] 주문 수정 규칙 고정
  - `PAYMENT_PENDING` 이후 수정 금지
  - `REJECTED` 종료 상태 처리
- [x] 주문 금액 스냅샷 고정
  - `subtotal_amount`
  - `total_amount`
  - `currency_code`
- [x] 결제 상태 기록
  - `PAYMENT_PENDING -> PAID`
  - `paid_at`
- [x] 배송 상태 기록
  - `PAID -> READY_TO_SHIP -> SHIPPED -> DELIVERED`
- [x] 정산 상태 기록
- [x] 주문 상태 이력 보존
  - `purchase_order_status_history`

### 회사 관리자 운영 기능

- [x] `COMPANY_ADMIN`의 자기 회사 범위 사용자 조회/관리 정리
- [x] 구매 역할 변경 기능 보강
- [x] `COMPANY_ADMIN` 보호 제약 보강
- [x] 초대 코드 조회 기능 정리
- [x] 초대 코드 재발급 기능 정리
- [x] 초대 코드 수동 폐기 기능 정리
- [x] 회사명 수정 기능
- [x] `company_domain` 수정 기능

### 주문 화면과 상태 UI

- [x] 주문 상태 이력 UI 노출
  - workspace 주문 상세
  - admin 주문 카드
- [x] workspace 주문 상세 상태 안내 블록 추가
- [x] workspace 주문 상세 액션 영역 상태 기반 정리
- [x] 액션이 없을 때 전용 안내 문구 노출
- [x] 관리자 주문 카드 액션 노출 기준을 실제 주문 상태 capability로 정리
- [x] 종료 상태 / 반려 / 취소 / 배송 진행 상태 안내 문구 정리
- [x] 관리자 주문 화면 마감
- [x] 회사 사용자 주문 화면 마감

### 테스트와 회귀 방지

- [x] 상태별 액션/화면 규칙 focused test 보강
- [x] `all` 필터에서 상태와 맞지 않는 버튼이 보이지 않는지 회귀 테스트 추가
- [x] workspace / admin 페이지 렌더링 `MockMvc` 회귀 테스트 추가
- [x] 역할 변경 규칙 focused test 보강
- [x] 권한 차단 경로 focused test 보강

## 남아 있는 작업

### 수동 QA

- [ ] 브라우저 기준 workspace 주문 상세 버튼/안내 문구 확인
  - [ ] `DRAFT` 주문에서 초안 상태 안내와 허용 액션 노출 확인
  - [ ] `PENDING_PLATFORM_APPROVAL` 주문에서 승인 대기 안내와 허용 액션 노출 확인
  - [ ] `PAYMENT_PENDING` 주문에서 결제 대기 안내와 비허용 액션 미노출 확인
  - [ ] `PAID` 주문에서 결제 완료 안내와 배송 전 상태 안내 확인
  - [ ] `READY_TO_SHIP` 주문에서 배송 준비 안내와 액션 영역 비노출 확인
  - [ ] `SHIPPED` 주문에서 배송 진행 안내와 액션 영역 비노출 확인
  - [ ] `DELIVERED` 주문에서 배송 완료 안내와 액션 영역 비노출 확인
  - [ ] `REJECTED` 주문에서 반려 사유/종료 상태 안내 확인
  - [ ] `CANCELLED` 주문에서 취소 상태 안내와 액션 영역 비노출 확인
  - [ ] 액션이 없는 상태에서 전용 안내 문구가 자연스럽게 표시되는지 확인
  - [ ] 상태 이력 블록의 전이 라벨, 변경 주체, 메모 표시 확인
- [ ] 브라우저 기준 admin `pending` / `delivery` / `rejected` / `all` 필터 확인
  - [ ] `pending` 필터에서 플랫폼 승인 대기 주문만 노출되는지 확인
  - [ ] `pending` 필터에서 승인/반려 가능 주문에만 액션 버튼이 노출되는지 확인
  - [ ] `delivery` 필터에서 `PAID`, `READY_TO_SHIP`, `SHIPPED` 배송 진행 주문만 노출되는지 확인
  - [ ] `delivery` 필터에서 상태별 허용 액션만 노출되는지 확인
  - [ ] `rejected` 필터에서 반려 주문만 노출되고 종료 상태 안내가 자연스러운지 확인
  - [ ] `all` 필터에서 상태가 다른 주문이 함께 보여도 각 주문 카드 액션이 capability 기준으로 맞는지 확인
  - [ ] 각 필터에서 상태와 맞지 않는 버튼이 노출되지 않는지 확인
  - [ ] 각 필터에서 빈 결과일 때 빈 상태 메시지가 어색하지 않은지 확인
- [ ] 수동 QA 실행 순서, 이슈 기록 형식, 종료 기준 정리
  - [ ] 실행 순서를 `workspace 주문 상세 -> admin 필터 -> admin all 재검토`로 고정
  - [ ] 이슈 기록 형식을 `주문 ID / 현재 상태 / 사용자 유형 / 기대 결과 / 실제 결과 / 재현 단계`로 통일
  - [ ] 필요 시 스크린샷 또는 화면 캡처를 함께 남기도록 정리
  - [ ] 종료 기준을 `상태/권한별 버튼과 안내 문구가 문서 의도와 일치`로 명시

### 테스트 인프라 정리

- [ ] `@MockBean` deprecation 경고 정리 방식 결정
- [ ] 필요 시 페이지 렌더링 테스트의 mock 구성 최신 방식으로 전환

## 보류 항목

- [ ] SCIM
- [ ] 회사별 다중 IdP 관리
- [ ] 이메일 초대
- [ ] 다단계 승인
- [ ] 외부 ERP 연동
- [ ] 실제 PG 결제 처리

## 메모

- 회사 관리자 화면은 현재 `/workspace` 안에 분리되어 있다.
- 주문 상태 이력은 append-only로 유지하고 `purchase_orders`는 최신 상태와 주요 시각 컬럼만 가진다.
- Settlement 1st Phase는 `purchase_orders` 기준 per-order 정산으로 구현되어 있다.
