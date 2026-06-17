# Module Context

This directory owns runtime configuration, server-side templates, and static assets.
Changes here directly affect local startup behavior, rendered UI, and environment-dependent integration settings.

# Tech Stack And Constraints

- Spring Boot property binding
- Thymeleaf templates
- Static CSS assets

# Implementation Patterns

- Keep secrets out of committed property files whenever possible.
- Keep text assets and templates in UTF-8.
- Use `application.properties` for local defaults only; prefer environment variables or external config for credentials.
- Keep templates simple and consistent with controller-provided model data.
- Keep CSS changes aligned with the current UI structure rather than speculative redesigns.
- Keep configuration changes compatible with Spring Boot conventions and existing auth flow.

# Environment Rules

- `application.properties` is for local defaults only.
- Environment-specific overrides must use Spring-supported external configuration patterns.
- Sensitive values must be injected through environment variables or external configuration sources.
- Configuration changes must not silently alter business rules.

# OAuth Configuration Rules

- Registration and provider keys must match Spring Security conventions exactly.
- Redirect URI configuration must stay aligned with registered provider settings.
- Do not override default Spring Security OAuth behavior casually.
- Auth configuration changes must stay aligned with `docs/auth-flow.md`.

# Template Rules

- Templates must not contain business logic.
- Templates must render controller-provided model data only.
- Conditional rendering is allowed; domain calculations are not.
- Template names, IDs, and model attributes must remain synchronized with controller code.
- Use the header container as the horizontal layout baseline.
- New page bodies should match the header's left and right gutter by staying inside the shared page shell rather than introducing narrower independent outer widths.

# Testing Strategy

- After config changes, verify `./gradlew test` still passes.
- After template changes, run the app locally when possible and inspect the affected page.
- For auth config changes, verify registration and provider property names match Spring Security conventions.
- Re-check affected login, callback, and account-linking pages after auth-related template or config changes.

# Local Golden Rules

- Do treat OAuth client secrets and database credentials as sensitive.
- Do preserve property naming conventions already used by Spring Boot.
- Do keep template IDs, names, and model attributes synchronized with controller code.
- Do keep configuration intent explicit when a property changes runtime behavior.
- Do keep page body horizontal spacing aligned with the header gutter.
- Do not commit real production credentials.
- Do not rename config keys casually; Spring binding depends on exact keys.
- Do not move business rules into templates or configuration files.
