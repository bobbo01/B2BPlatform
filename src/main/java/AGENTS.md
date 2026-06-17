# Module Context

This tree contains the Spring Boot application code.
`domain` owns business modules and persistence models.
`global` owns cross-cutting concerns such as authentication, auditing, and shared infrastructure rules.

# Tech Stack And Constraints

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- Spring Security OAuth2 Client and OIDC
- Lombok is available; use it selectively and keep generated APIs clear

# Implementation Patterns

- Treat product docs as the source of truth when implementation and docs diverge.
- Keep controllers thin and delegate to services.
- Keep authentication provider mapping inside `global/auth` and user-account linking services.
- Model persistence relationships explicitly with JPA annotations.
- Prefer adding methods to the relevant domain service over scattering logic across controllers and entities.
- Use package boundaries intentionally: `entity`, `repository`, `service`, `controller`, `dto`.
- Keep DTOs at the web boundary; do not expose entities directly to controllers or templates.

# Architecture Rules

- Controllers handle request mapping, validation entry points, and response construction only.
- Services own business logic, transaction boundaries, and orchestration.
- Repositories own persistence access only.
- Entities own persistence state and relationship mapping only.

# Future Domain Logic Rules

- If order modules are added later, define the `cart`, `purchase_order`, `order_item`, `approval`, and `inventory` lifecycle in service code before documenting it as implemented behavior.
- Future approval, inventory, and order status rules must be enforced in services and backed by tests once those modules exist.

# Future Monetary And Transaction Rules

- If monetary calculations are introduced, use BigDecimal.
- Future `subtotal_amount`, `tax_amount`, and `total_amount` calculations belong in the service layer.
- If historical order amounts are persisted, they must not be recalculated after persistence.
- Future order creation and related inventory updates should run inside transactional boundaries.
- Avoid partial writes when future order workflows are implemented.

# JPA Rules

- Use LAZY loading for associations by default.
- Avoid bidirectional relationships unless a concrete use case requires them.
- Keep entity constructors, builders, and factory methods aligned with the current schema.
- Verify repository interaction and application startup expectations after entity changes.

# Security And Auth Rules

- Keep auth policy decisions in one place.
- Keep provider-specific mapping inside the auth/security layer.
- Do not hardcode provider IDs, default company IDs, or default role IDs in service logic without config indirection.
- Do not change security flow behavior without updating docs.

# Testing Strategy

- Run `./gradlew test` after behavior changes.
- Add focused tests for auth and account-linking changes.
- Add service tests for order, approval, inventory, and monetary logic changes when those modules are introduced.
- If changing entities, verify repository interaction and application startup expectations.
- Add persistence-focused tests for entity constraints when schema-relevant fields change.

# Local Golden Rules

- Do keep auth policy decisions in one place.
- Do update builders and constructors when entity fields change.
- Do favor explicit names over abbreviations in service and domain logic.
- Do keep business rules in services, not controllers, templates, or entities.
- Do not put request parsing or view concerns into entities.
- Do not expose entities directly across web boundaries.
- Do not change security flow behavior without updating docs.

## Routing Rules

# Use domain base path at class level and sub-paths at method level (root "/" is excluded)
- Define domain base paths using @RequestMapping at the controller class level.
- Define only sub-paths at the method level.
- Ensure method names match their sub-paths.
- Handle "/" in a separate controller without domain prefix.
- Use `/company/first-login` as the company first-login path. Do not use `/first-login/company`.
