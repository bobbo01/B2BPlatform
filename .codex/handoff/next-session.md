# Next Session

## Current Baseline

- `spring.jpa.open-in-view=false` 상태에서 전체 `./gradlew test` 통과
- `GET /workspace`는 `WorkspacePageController`로 분리 완료
- workspace/company 관련 POST는 전용 controller로 분리 완료
  - onboarding
  - company join review
  - invite code regenerate/revoke
  - membership leave
  - first company admin request
  - company users admin
  - company profile update
- `COMPANY_ADMIN` 회사 정보 수정 기능 구현 완료
  - 자기 회사 `companyName`, `companyDomain` 수정 가능
  - 자기 회사만 수정 가능
  - 중복 `companyDomain` 차단
- 역할 모델 1차 strict cleanup 완료
  - `User` 구매 역할 helper 정리
  - `COMPANY_ADMIN`은 persisted role이 아니라 `company_admin` 플래그로만 해석
  - `RoleDataInitializer` 기본 role seed에서 `COMPANY_ADMIN` 제거
  - `UserAuthorities`의 legacy `COMPANY_ADMIN` persisted-role fallback 제거
  - `ROLE_...` 이름 fallback lookup 제거
  - 관련 focused test + 전체 `./gradlew test` 통과
- 주문 상태 이력 UI 1차 마감 완료
  - `PurchaseOrderStatusHistoryView`에 `transitionLabel`, `changedByDisplayName`, `changeNoteDisplay` 추가 
  - `PurchaseOrderViewAssembler`에서 상태 이력 표시 문자열과 fallback 조립
  - workspace 주문 상세와 admin 주문 화면에서 템플릿 직접 문자열 조합 제거
  - 상태 이력 블록을 progress UI와 분리된 전용 UI 블록으로 정리
  - 관련 focused test + 전체 `./gradlew test` 통과
- workspace 주문 상세 상태 안내 1차 반영 완료
  - `PurchaseOrderDetailView`에 `statusGuideTitle`, `statusGuideMessage` 추가
  - 주문 상태별 안내 문구를 assembler에서 조립
  - workspace 주문 상세 상단에 상태 안내 블록 추가
  - 관련 fixture 갱신 + 전체 `./gradlew test` 통과

## Completed This Session

- 관리자 주문 화면 / 회사 사용자 주문 화면 마감 완료
  - workspace 주문 상세 액션 영역을 상태 기반으로 정리
  - 액션이 없을 때 전용 안내 문구 노출
  - admin 주문 카드 액션 노출 기준을 filter가 아니라 실제 주문 상태 capability로 정리
  - 종료 상태 / 반려 / 취소 / 배송 진행 상태 안내 문구 정리
  - 권한별로 보여야 할 액션만 남기고 중복 표현 제거
- 상태별 액션/화면 규칙 focused test 보강 완료
  - workspace 주문 상세 액션 노출 검증
  - admin 주문 카드 액션 노출 검증
  - `all` 필터에서 상태와 맞지 않는 버튼이 보이지 않는지 회귀 테스트 추가
  - 실제 템플릿 렌더링 기준 `MockMvc` 페이지 회귀 테스트 추가
- 역할 모델 정합성 focused test 보강 완료
  - platform admin 대상 구매 역할 변경 차단 테스트 추가
  - platform admin 대상 `COMPANY_ADMIN` 부여 차단 테스트 추가
  - 마지막 활성 `COMPANY_ADMIN` 비활성화 차단 테스트 추가
  - commerce workflow 역할 차단 경로 테스트 추가
- 문서 동기화 완료
  - `docs/implementation-checklist.md`를 현재 구현 상태 기준으로 갱신
  - 완료 항목과 실제 남은 작업만 남기도록 정리
- 수동 QA 계획 구체화 완료
  - `docs/implementation-checklist.md`에 workspace 주문 상세 상태별 확인 항목 추가
  - `docs/implementation-checklist.md`에 admin 주문 필터별 확인 항목 추가
  - 수동 QA 실행 순서, 이슈 기록 형식, 종료 기준 정리
- 주문 목록 가독성 개선 완료
  - workspace 주문 전체 조회에서 주문별 카드 간격과 구분선 강화
  - admin 주문 전체 조회에서 주문 카드 헤더, 상태 배지, 카드 간 분리감 추가
- workspace 주문 상세 위치/토글 UX 1차 개선 완료
  - `주문 상세 보기` 선택 시 목록 최하단이 아니라 해당 주문 카드 바로 아래에 상세 표시
  - 펼쳐진 주문에서 버튼을 다시 누르면 `주문 상세 닫기`로 토글되도록 링크 동작 정리

## Remaining Work

1. workspace 주문 상세 보기/닫기 시 화면 이동 없이 토글되도록 구현
   - 현재는 쿼리 파라미터 기반이라 클릭 시 페이지 이동이 발생
   - 다음 작업에서는 같은 화면 위치에서 펼침/닫힘이 되도록 개선 필요
2. 브라우저 기준 수동 QA
   - workspace 주문 상세 버튼/안내 문구 확인
   - admin `all` / `pending` / `delivery` / `rejected` 필터 확인
3. `@MockBean` deprecation 경고 정리 여부 판단
   - 현재 테스트는 통과
   - 추후 테스트 인프라 정리 시 일괄 전환 가능

## Notes

- URL 구조 변경 금지
- DB 구조 변경 금지
- service/DTO 경계 유지, controller는 얇게 유지
- OSIV 다시 켜지 않기
- 역할 모델은 `구매 역할 1개 + company_admin 플래그` 기준 유지
- `COMPANY_ADMIN`을 persisted role이나 구매 역할처럼 다시 도입하지 않기

## Relevant Docs

- `../../docs/plan.md`
- `../../docs/implementation-checklist.md`
- `../../docs/roles.md`
- `../../docs/schema.md`
- `../../docs/erd.md`

## Background Gradient Analysis

- Scope: plan-only analysis, no code changes applied yet
- Symptom:
  - Main page uses `radial-gradient` + `linear-gradient`
  - On button click or page state change, the radial background appears to shift slightly
  - User wants the background to feel completely fixed
- Primary cause:
  - Global background is applied directly on `body` in `src/main/resources/static/css/base.css`
  - `body::before` is already `position: fixed`, so the grid overlay is viewport-fixed while the main background is document-based
  - This mismatch makes the radial gradient look like it moves during anchor scroll or layout changes
- Supporting causes:
  - `html { scroll-behavior: smooth; }` is enabled in `base.css`
  - Home CTA buttons in `src/main/resources/templates/pages/home.html` use `href="#access-panel"` and `href="/#access-panel"`, so clicking them triggers smooth anchor scrolling
  - The radial gradient uses percent-based positioning such as `circle at 85% 15%`, so viewport width changes can slightly recalculate its visible center
  - Scrollbar appearance/disappearance may amplify the movement perception
- Not considered the primary cause:
  - Button `transform: translateY(-2px)` hover/focus effects in `base.css` and `home.css`
  - Sticky header positioning
- Safest fix candidate:
  - First try a minimal fix in `src/main/resources/static/css/base.css` to make the `body` background viewport-fixed
  - Reviewer recommended `background-attachment: fixed;` as the smallest first step
- Secondary fallback if the first fix is insufficient:
  - Move the global radial/linear background into a separate fixed pseudo-element layer such as `body::after`
  - Keep the existing `body::before` grid overlay as-is
- Additional low-risk mitigation worth considering:
  - `scrollbar-gutter: stable;` on `html` or `body` to reduce width-based background reflow
- Files involved in the analysis:
  - `src/main/resources/static/css/base.css`
  - `src/main/resources/static/css/home.css`
  - `src/main/resources/templates/pages/home.html`
  - `src/main/resources/templates/layout/header.html`
- Suggested next-session order:
  1. Test the smallest CSS-only fix first
  2. Verify background stability on anchor scroll and logged-in/logged-out main page states
  3. Only if needed, escalate to a fixed pseudo-element background layer

## Session Update 2026-06-17

- Workspace template rendering was refactored from one large template into a shell + fragments structure.
- Current workspace shell:
  - `src/main/resources/templates/pages/workspace.html`
- Added workspace fragments:
  - `src/main/resources/templates/pages/workspace/setup-pending.html`
  - `src/main/resources/templates/pages/workspace/overview.html`
  - `src/main/resources/templates/pages/workspace/company-users.html`
  - `src/main/resources/templates/pages/workspace/commerce-shell.html`
  - `src/main/resources/templates/pages/workspace/cart.html`
  - `src/main/resources/templates/pages/workspace/purchase-requests.html`
  - `src/main/resources/templates/pages/workspace/approvals.html`
  - `src/main/resources/templates/pages/workspace/order-drafts.html`
- Order-drafts async inline script was extracted to:
  - `src/main/resources/static/js/workspace-order-drafts.js`
- Existing behavior intentionally preserved:
  - `/workspace` URL structure
  - `section`, `commerceSection`, `orderId` query parameters
  - order detail open/close behavior
  - `pushState` / `replaceState` / `popstate` URL-only updates without full page navigation
- Rendering test was updated for the external JS include:
  - `src/test/java/com/bobbo01/supplyhub/domain/company/controller/WorkspacePageRenderingTest.java`
- Verification:
  - `./gradlew test` passed after the fragment split and JS extraction

## Workspace CSS Stabilization

- Investigated the visual shift when switching workspace fragments.
- No fragment-specific CSS divergence was found; all workspace screens still share:
  - `src/main/resources/static/css/base.css`
  - `src/main/resources/static/css/setup.css`
- Applied low-risk stabilization:
  - added `scrollbar-gutter: stable;` to `html` in `base.css`
  - removed `transform: translateY(-1px);` from `.workspace-nav-group:hover`, `.workspace-nav-group:focus-within`, `.workspace-nav-group.is-open` in `setup.css`
- Verification:
  - `./gradlew test` passed after the CSS change
- Still recommended:
  - browser-check `/workspace` transitions across `overview`, `company-users`, and `commerce/order-drafts`
  - if minor movement remains, next candidate is simplifying `.workspace-subnav` transition behavior

## Additional Next Work

1. Change the admin page rendering structure to match the current workspace rendering approach.
   - Goal: convert `admin.html` into a shell page and split the visible admin sections into Thymeleaf fragments, similar to the current workspace page layout strategy.
   - Keep existing admin URLs and behavior unchanged.
