# Core Architecture (Meteoris Insight)

## Description

Defines the **intended modular monolith** for Meteoris Insight: **Maven** modules `meteoris-insight` (application) + `meteoris-insight-e2e` (tests) under reactor **`pom.xml` (root)**; **inside** `meteoris-insight`, Spring Modulith-style boundaries (**`app-core`** OPEN for **`IdGenerator`**, then `app-api`, `app-agent-core`, `app-weather-agent`, `app-news-agent`, `app-memory`, `app-eval`), dependency direction, and mapping to `docs/VISION.md`. Deep structure, diagrams, and data design: **`docs/ARCHITECTURE.md`**. **LLM integration** (OpenAI-compatible beans, `spring.ai.custom.chat.*`, stub mocks) must follow **`docs/PRD.md` NFR-8**. JDBC + id rules: **`repository-design`** skill.

## When to use

- Adding or renaming Maven modules / Java packages.
- Deciding where a new class belongs (API vs orchestration vs MCP adapter).
- Refactoring that might cross module boundaries.
- Resolving “who owns this domain concept?” (session vs eval vs news cache).

## Instructions

- Treat `docs/VISION.md` as the **source of truth** until code exists; then keep code and vision in sync.
- **Dependency rule:** `app-api` → `app-agent-core` → feature modules; **avoid** core importing web controllers or transport DTOs from `app-api`.
- Prefer cross-module calls via **published API packages** (e.g. `*.api` types) once you introduce Modulith `package-info.java`.
- Keep **MCP protocol details** inside weather/news integration modules; expose only normalised DTOs or Spring `@Tool` interfaces upward.
- **Pgvector** tables for news belong with news retrieval logic (`app-news-agent` or shared infra module as you decide — document the choice in vision if you split).

## Boundaries

- Does not specify LLM prompts or full `ChatClient` bean code (use `agentic-patterns` + product docs); **where** to wire `ChatModel` / `ChatClient` and **which** configuration pattern is normative: **`docs/PRD.md` NFR-8**.
- Does not define REST path shapes in detail (use `api-design`).
- Must not author evaluation metrics without aligning with `evaluation` skill and `docs/VISION.md`.
