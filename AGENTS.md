# Project Context

This repository is a Spring Boot 3.5.11 B2B platform on Java 21.
Current implemented focus is enterprise SSO, account linking, and core domain modeling for company, user, category, and product.
Persistence uses Spring Data JPA with MySQL.
Views use Thymeleaf.
Security uses Spring Security OAuth2/OIDC.

# Project Operations

## Operational Commands

- `./gradlew bootRun`
- `./gradlew test`
- `./gradlew clean test`
- `./gradlew build`

## Working Assumptions

- Default local database is MySQL from `src/main/resources/application.properties`.
- JPA schema mode is `update`; do not rely on it as a migration strategy for production-grade changes.
- OAuth/OIDC flows must remain compatible with Spring Security defaults unless a change is deliberate and reviewed.
- Repository text files and documents should be maintained in UTF-8.

# Golden Rules

## Immutable

- Never commit real OAuth client secrets, database passwords, or tenant-specific credentials.
- Never bypass Spring Security login or account-linking checks to make flows work locally.
- Never change company-account mapping rules without updating related docs in `docs/auth-flow.md`.
- Never introduce destructive database changes without an explicit migration plan.
- If monetary domain logic is introduced, all monetary values must use BigDecimal.
- If order pricing is implemented, price, tax, and totals must be calculated and stored at order creation time.
- If historical order data is persisted, it must not be recalculated after creation.

## Do

- Use constructor or builder signatures that match the current entity model exactly.
- Keep domain terminology consistent across code and docs.
- Treat docs as the source of truth when code and docs diverge, unless explicitly re-decided.
- Prefer Spring Boot conventions over custom framework code.
- Update `docs/erd.md` and `docs/schema.md` when entity structure changes.
- Update tests for authentication rules, entity constraints, and service behavior when logic changes.

## Don't

- Do not hardcode provider-specific logic outside the auth/security layer.
- Do not place business logic in controllers.
- Do not add new dependencies when Spring Boot built-ins already solve the problem.
- Do not let docs drift from code after changing entities or auth flow.
- Do not mix presentation concerns into JPA entities.
- Do not expose JPA entities directly to API or template layers.

# Architecture Rules

- Controller: request/response only.
- Service: business logic and transaction boundaries.
- Repository: persistence only.
- Entity: persistence model only.

- Controllers must never access repositories directly.
- Business rules must be implemented in services, not in controllers, templates, or entities.

# Domain Rules

## Future Commerce Scope

- If `cart`, `purchase_order`, `order_item`, `approval`, or `inventory` are implemented later, document the intended lifecycle explicitly before adding cross-module rules.
- Future monetary, tax, approval, and inventory rules must be described in both code and docs as implemented behavior only after the supporting code exists.
- Planned commerce rules must be labeled as future scope until entities, services, and tests exist in the codebase.

# JPA Rules

- Use LAZY loading for relationships by default.
- Avoid bidirectional relationships unless required by the use case.
- Use a shared base auditing model where applicable.
- Verify startup and repository behavior after entity relationship changes.

# Standards And References

## Coding Standards

- Follow Spring Boot package boundaries already present in `domain` and `global`.
- Keep entities persistence-focused; put orchestration in services.
- Prefer repository interfaces for persistence and service classes for use-case logic.
- Use clear, business-oriented method names.
- Keep files compact; split classes when responsibilities diverge.

## Git Standards

- Use focused commits with one logical change per commit.
- Commit message style: short imperative summary, for example `Remove supplier from product model`.
- Avoid unrelated formatting-only edits while changing behavior.

## Testing Standards

- Service-level business logic changes require tests.
- Authentication and account-linking changes require focused integration coverage.
- Entity and repository changes should be validated with persistence-focused tests.

## Maintenance Policy

- If code, docs, and configuration disagree, align the code and configuration to the docs in the same task where practical.
- If a new high-context area appears, add a nested `AGENTS.md` at that boundary.
- If security-sensitive config is found in tracked files, propose moving it to environment-specific configuration.
- If business rules change, update both implementation guidance and documentation guidance in the same change.

# Context Map

- **[Documentation And Data Model](./docs/AGENTS.md)** : ERD, schema, auth-flow, status definitions, and domain-language updates
- **[Java Application Code](./src/main/java/AGENTS.md)** : Spring Boot domain, auth, controller, service, repository, and entity changes
- **[Runtime Configuration And Templates](./src/main/resources/AGENTS.md)** : `application.properties`, Thymeleaf templates, static assets, and local runtime behavior
- **`gstack/`** : Separate gstack helper area for agent workflow guidance, not part of the product application rules
