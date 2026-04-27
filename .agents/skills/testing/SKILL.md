# Testing (Meteoris Insight)

## Description

Testing strategy for agentic services: unit tests for pure logic, Spring tests with **stub AI**, Testcontainers for Postgres/pgvector, and optional contract tests for MCP fixtures.

## When to use

- Creating `src/test/java` layouts, profiles, or CI commands.
- Adding tests for tools, advisors, or evaluation runner.
- Ensuring automated builds do not hit live LLM endpoints.

## Instructions

- Use **English** for test names, comments, and Cucumber/Gherkin text (project-wide language rule).
- Use **stub AI** configuration: dedicated **`stub-ai`** / `test` (and `e2e` if added) Spring profiles with `@TestConfiguration` or `@Configuration` replacing `ChatModel` / embedding clients where needed.
- **Database:** use **Docker Testcontainers** with **`pgvector/pgvector:pg16`**, **`@ServiceConnection`** on a **`PostgreSQLContainer`**, profiles **`stub-ai`** + **`test-pgvector`**, **`@AutoConfigureTestDatabase(replace = Replace.NONE)`**, and Flyway locations **`db/migration`** + **`db/migration-postgresql`**. Integration tests must use **PostgreSQL** (same family as production); the app does not use H2. Canonical detail: **`application/AGENTS.md`** → *Database and automated tests*.
- **OpenAPI:** validate **`api/openapi.yaml`** with **swagger-parser** (OpenAPI 3) in tests, not only YAML load.
- For MCP, prefer **wiremock** or recorded JSON fixtures over live external services in CI.
- Add at least one test that proves **AskUserQuestionTool** pauses until answers are supplied (where you implement that flow).

## Boundaries

- Must not require network access to LLM vendors in default `mvn test`.
- Must not store real user prompts or PII from production in test resources.
