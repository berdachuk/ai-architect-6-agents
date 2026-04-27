# Meteoris Insight E2E — Agent Guide

End-to-end **contract** tests live in **`meteoris-insight-e2e`**: a **separate Maven module** from `meteoris-insight`, built from the **root `pom.xml`** reactor. The default harness is an **in-process `@SpringBootTest`** against **`MeteorisInsightApplication`** plus **Docker Testcontainers** PostgreSQL + pgvector (same classpath and Flyway migrations as production), not a separate JVM driving the `-exec` fat JAR.

## Purpose

- Exercise the running service (HTTP/WebSocket, optional browser flows) **without** importing application `internal` packages.
- Validate contracts shared with clients (e.g. OpenAPI-generated clients once you publish a spec).
- Keep **stub AI** / fixed ports / Docker Compose lifecycle aligned with `docs/VISION.md` and root `AGENTS.md` boundaries.

## Commands

```bash
mvn verify
```

**Docker:** in-process **`@SpringBootTest`** suites in this module start the same **PostgreSQL + pgvector** Testcontainers setup as **`meteoris-insight`** (see **`../application/AGENTS.md`** → *Database and automated tests*). CI and local **`mvn verify`** must have Docker available. The application does not use an embedded H2 database; production-like runs use **PostgreSQL** only.

Tune phases later (start fat JAR or Compose, run Cucumber/REST Assured, tear down) as the project evolves.

## Constraints

- **Language:** E2E test names, comments, Cucumber feature text, and README snippets must be in **English** (see root `AGENTS.md` → **Global boundaries**).
- No secrets in test resources; no live LLM calls in CI by default.
- **API-first:** Contract tests hit the same paths as **`../meteoris-insight/api/openapi.yaml`**. Any `/api/**` change must update that file **before** or **with** code; optional follow-on: generate a typed HTTP client from the spec.

## Skills

- `../.agents/skills/testing/SKILL.md`
- `../.agents/skills/api-design/SKILL.md`
