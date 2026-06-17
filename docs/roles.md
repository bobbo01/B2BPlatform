# SupplyHub 역할 정의

이 문서는 역할별 권한, 제약, 운영 원칙을 정의한다.
로그인과 회사 연결 흐름은 [auth-flow.md](./auth-flow.md)를 따른다.

## 사용자 유형

### 플랫폼 운영 계정

- `PLATFORM_ADMIN` 전용 계정이다.
- 회사 사용자 계정이 아니다.
- 구매 역할을 가지지 않는다.
- 플랫폼 전역 운영 기능을 수행한다.

### 회사 사용자 계정

- 연결되면 정확히 1개의 회사에 소속된다.
- 정확히 1개의 구매 역할을 가진다.
- 필요 시 `COMPANY_ADMIN` 운영 역할을 가질 수 있다.

### 회사 미연결 로그인 사용자

- OAuth 로그인에는 성공했지만 아직 회사에 연결되지 않은 상태다.
- 카탈로그 조회와 회사 연결 관련 기능만 사용할 수 있다.

## 역할 축

### 운영 역할

- `PLATFORM_ADMIN`
- `COMPANY_ADMIN`

### 구매 역할

- `CART_USER`
- `PURCHASER`
- `APPROVER`

## 역할 해석 원칙

- 운영 역할과 구매 역할은 서로 다른 축이다.
- 회사 사용자는 구매 역할 1개를 가진다.
- `COMPANY_ADMIN`은 구매 역할을 자동 포함하지 않는다.
- `PLATFORM_ADMIN`과 `COMPANY_ADMIN`은 동시에 보유할 수 없다.
- `PLATFORM_ADMIN`은 구매 역할을 가지지 않는다.

## 기본 부여 규칙

- 회사 사용자는 회사 연결 직후 기본 구매 역할로 `CART_USER`를 가진다.
- 회사 연결 직후 운영 역할은 없다.
- 회사 생성자도 자동으로 `COMPANY_ADMIN`이 되지 않는다.
- `PLATFORM_ADMIN`은 회사 연결 흐름에서 생성되거나 승격되지 않는다.

## 구매 역할 권한

### `CART_USER`

- 상품 조회
- 장바구니 작업

### `PURCHASER`

- `CART_USER` 권한 포함
- 구매 요청 생성
- 주문 초안 작성
- 승인 요청 제출

### `APPROVER`

- `PURCHASER` 권한 포함
- 구매 요청 승인/거절
- 주문 초안 작성
- 주문 초안을 플랫폼 승인 대기로 제출

## 운영 역할 권한

### `COMPANY_ADMIN`

- 자기 회사 사용자 조회
- 자기 회사 사용자 활성/비활성 관리
- 자기 회사 사용자 구매 역할 변경
- 자기 회사 사용자 `COMPANY_ADMIN` 부여/회수
- 자기 회사 참여 요청 승인/거절
- 자기 회사명 수정
- 자기 회사 `company_domain` 수정
- 자기 회사 초대코드 조회
- 자기 회사 초대코드 재발급
- 자기 회사 초대코드 수동 폐기

제약:

- 다른 회사 사용자 조회 또는 수정은 할 수 없다.
- 다른 회사의 회사 정보는 관리할 수 없다.
- 자기 자신에게 `PLATFORM_ADMIN`을 부여할 수 없다.
- `company_domain`은 다른 회사가 이미 사용 중인 값으로 변경할 수 없다.

### `PLATFORM_ADMIN`

- 모든 회사 상태 조회/수정
- 모든 사용자 상태 조회/수정
- 모든 운영 역할 관리
- 모든 구매 역할 관리
- 첫 `COMPANY_ADMIN` 요청 승인/거절

제약:

- 회사 사용자 계정이 아니다.
- 회사 연결 대상이 아니다.
- 구매 역할을 가지지 않는다.

## 첫 `COMPANY_ADMIN` 요청 제약

- 첫 `COMPANY_ADMIN` 요청 자격자는 `회사 생성 완료 트랜잭션을 최종 성공시킨 사용자` 1명뿐이다.
- 그 사용자만 첫 요청을 생성할 수 있다.
- 회사별 승인 대기 첫 요청은 1건만 허용한다.
- 자격 사용자 기준 개인 승인 대기 요청도 1건만 허용한다.
- 첫 `COMPANY_ADMIN` 요청은 `PLATFORM_ADMIN`만 승인/거절할 수 있다.
- 승인 후에도 구매 역할은 유지되며, 운영 역할만 추가된다.

## `COMPANY_ADMIN` 유지 제약

- 회사에는 항상 최소 1명의 `COMPANY_ADMIN`이 남아 있어야 한다.
- 마지막 남은 `COMPANY_ADMIN` 1명은 제거할 수 없다.
- 마지막 남은 `COMPANY_ADMIN` 1명은 `INACTIVE`로 바꿀 수 없다.
- 다른 `COMPANY_ADMIN`이 1명 이상 있을 때만 자기 자신의 `COMPANY_ADMIN` 취소가 가능하다.

## 상태값 관련 권한 규칙

- 회사 상태값은 현재 `ACTIVE`, `INACTIVE`를 사용한다.
- 회사 사용자 상태값도 현재 `ACTIVE`, `INACTIVE`를 사용한다.
- `COMPANY_ADMIN`은 자기 회사 사용자를 `INACTIVE`로 바꿀 수 있다.
- 단, 마지막 `COMPANY_ADMIN` 보호 규칙을 우선 적용한다.

## 현재 모델 해석

- 현재 구현은 `users.role_id`와 `users.company_admin` 조합을 사용한다.
- 문서 기준 역할 해석은 `구매 역할 1개 + COMPANY_ADMIN 운영 역할 여부`다.
- `PLATFORM_ADMIN`은 같은 `users` 테이블을 쓰더라도 개념적으로 회사 사용자와 분리된 운영 계정으로 본다.

## 권한 문자열 규칙

- 역할명
- `PLATFORM_ADMIN`
- `COMPANY_ADMIN`
- `CART_USER`
- `PURCHASER`
- `APPROVER`

- Spring Security 권한명
- `ROLE_PLATFORM_ADMIN`
- `ROLE_COMPANY_ADMIN`
- `ROLE_CART_USER`
- `ROLE_PURCHASER`
- `ROLE_APPROVER`

## 임시 인증 권한

- `ROLE_FIRST_LOGIN_COMPANY_SETUP`은 영속 역할이 아니다.
- 회사 연결 전용 임시 인증 상태에서만 사용한다.
- `roles` 테이블에 저장하지 않는다.
