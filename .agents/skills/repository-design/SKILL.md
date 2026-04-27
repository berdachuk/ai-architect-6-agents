# Repository design (Meteoris Insight)

## Description

JDBC, SQL, Flyway, and **MongoDB-compatible string id** conventions for Meteoris Insight: `NamedParameterJdbcTemplate`, named parameters, no JPA, and **`IdGenerator`** with ObjectId layout plus **`extractCreationInstant`**.

## When to use

- Writing or refactoring SQL repositories / JDBC services.
- Adding or changing Flyway migrations.
- Touching pgvector columns, casts, or similarity queries.
- Defining new primary keys or OpenAPI id patterns.

## Instructions

- Prefer **`NamedParameterJdbcTemplate`** and **named parameters** (`:articleId`, `:sessionId`). Avoid positional `?` except where drivers force it.
- Keep SQL explicit (Java text blocks or `.sql` classpath resources). Do not build SQL by concatenating untrusted user input.
- **Primary keys:** use **`IdGenerator.generateId()`** from **`app-core`**; store as **`varchar(24)`** or **`char(24)`**; validate inbound ids with **`IdGenerator.isValidId`** before queries.
- **Time from id:** use **`IdGenerator.extractCreationInstant(id)`** for logging, sorting, or UI “created at” when no separate column exists (per `docs/VISION.md` — BSON ObjectId first four bytes = Unix seconds UTC).
- **Vector casts:** use explicit casts compatible with pgvector (e.g. `CAST(:vectorLiteral AS vector)`).
- **Migrations:** all schema changes under `meteoris-insight/src/main/resources/db/migration/` as new `V<n>__description.sql` files. **Never** edit already-applied migrations.

## Boundaries

- Do **not** introduce **JPA / Hibernate** or Spring Data JPA.
- Do not change embedding dimensionality without coordinating Flyway + Spring AI embedding config + fixtures.
- Session API tables owned by **`spring-ai-session`** follow that library’s schema; do not hand-edit their migrations unless the upstream docs require it.
