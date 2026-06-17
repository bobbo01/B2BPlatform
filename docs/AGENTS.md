# Module Context

This directory owns product-facing design documentation for the current implementation scope.
It is the source of truth for ERD, schema descriptions, status definitions, and authentication flow notes that explain how the code is intended to behave.

# Tech Stack And Constraints

- Write plain Markdown only.
- Use UTF-8 encoding for documentation files.
- Keep terminology aligned with code package names and entity names.
- Prefer concise sections and lists over narrative prose.
- Preserve portability of Mermaid ERD blocks.
- UI typography guidance in docs should treat `Pretendard` as the default product font unless explicitly re-decided.

# Implementation Patterns

- Treat docs in this directory as the source of truth for product behavior and terminology.
- Keep `plan.md` as the master document for direction, priorities, and completion criteria.
- Keep `implementation-checklist.md` as the execution-tracking document for current status and next tasks.
- Keep handoff-only notes under `.codex/handoff/`; do not store long-lived policy there.
- Update `erd.md` when entity relationships, keys, or table scope change.
- Update `schema.md` when columns, constraints, field meanings, status values, or monetary rules change.
- Update `auth-flow.md` when login, account linking, provider handling, or domain-match policy changes.
- Keep `roles.md` focused on role-based permissions and constraints.
- Keep `auth-flow.md` focused on login and onboarding flow, not role policy detail.
- Keep `schema.md` focused on tables, columns, constraints, and status values.
- Keep `erd.md` focused on entity relationships.
- Keep the implemented scope explicit; planned future domains must be labeled as future scope, not current behavior.
- If a documentation change implies runtime behavior changes, update the code guidance in the same task.

# Domain Consistency Rules

- Distinguish clearly between current implemented behavior and future planned expansion.
- Do not describe `cart`, `purchase_order`, `order_item`, `approval`, `inventory`, tax, or monetary lifecycle rules as implemented unless matching code exists.
- If future commerce scope is documented, label it explicitly as future scope and keep it separate from current behavior.
- Reflect VAT and monetary rules in `schema.md` only when those rules exist in code or are clearly marked as planned.

# ERD Rules

- Show primary keys and foreign keys explicitly where the diagram format allows.
- Represent relationship cardinality clearly.
- Remove orphan or removed entities from diagrams immediately.
- Keep entity names aligned with Java entity names and database naming conventions.

# Status And Monetary Rules

- All status fields must document allowed values and their meanings.
- All monetary fields must document currency assumptions and calculation rules.
- `subtotal_amount`, `tax_amount`, `shipping_amount`, and `total_amount` must be explained consistently.
- Do not describe historical order amounts as recalculated values.

# Testing Strategy

- After doc changes tied to code changes, verify entity names, field names, statuses, and monetary rules against the Java source.
- Re-read changed Mermaid blocks for broken relations or orphan entities.
- Re-check auth-flow steps when login or account-linking behavior changes.

# Local Golden Rules

- Do describe current behavior separately from planned future expansion.
- Do remove stale entities from docs when they are removed from code.
- Do keep Korean business explanations consistent across files.
- Do keep status names and order-flow descriptions identical across related docs.
- Do obtain explicit user permission before initiating any code modification or new implementation. (코드 수정 및 구현을 시작하기 전에 반드시 사용자에게 명시적인 허락을 구해야 합니다.) ✨[추가됨]
- Do not document fields that no longer exist in Java entities.
- Do not leave auth-flow steps ambiguous when security rules depend on them.
- Do not describe tax, approval, or inventory behavior differently from implemented code.

## Agent Workflow

All non-trivial tasks should follow:

1. Researcher
2. Planner
3. Reviewer
4. Main implementation

Subagents must not directly modify files unless explicitly requested.
