# API Design (Meteoris Insight)

## Description

**Edge layer** for Meteoris Insight: **API-first** REST — canonical **`api/openapi.yaml`**, generated server interfaces + models, controllers that implement those interfaces; **Thymeleaf** + Spring MVC for the main demo UI; **AskUserQuestionTool** (`QuestionHandler`) wired through HTML or JSON flows as appropriate.

## When to use

- Adding `POST /chat`, streaming endpoints, or session-id headers.
- Designing request/response bodies for multi-turn + tool interactions.
- Exposing OpenAPI / AsyncAPI if you publish a contract for UI or E2E.

## Instructions

- Treat **session identity** as a first-class concern: align with `SessionMemoryAdvisor` context keys (see Part 7 in `docs/VISION.md`).
- For **AskUserQuestionTool**, prefer **Thymeleaf** pages or fragments (forms, option lists) for clarifying questions; for REST-only clients, use JSON/SSE patterns as needed. Answers must resume the same `ChatClient` invocation model.
- Use **problem+json** or a consistent JSON error envelope across endpoints.
- **Always** edit **`meteoris-insight/api/openapi.yaml` first**, regenerate, then update controller implementations; keep **`meteoris-insight-e2e`** client generation on the same file.
- **OpenAPI enum fields** generate an inner `*Enum` class (e.g. `StatusEnum`). Controllers must use `StatusEnum.fromValue()` when mapping between API models and domain values.
- **E2E client generation** — use **`library=native`**, not `resttemplate` (`HttpHeaders.containsKey()` compilation error with the REST template generator).
- **`TestRestTemplate`** is the correct HTTP client for **servlet-based `@SpringBootTest`** (not `WebTestClient`, which is for reactive stacks).

## Boundaries

- Does not own database schema (`application` + Flyway ownership).
- Must not put orchestration business rules in controllers — delegate to application services / `ChatClient` facade.
