# AI Context (Root) — Meteoris Insight

**Meteoris Insight** is an agentic AI service (course project): orchestrator + weather/news subagents, **MCP** (**Open‑Meteo** + **one** keyless news MCP from the assignment list: **TheNews API / GNews.io / NewsAPI**—choose one, no API key), **Session + AutoMemory + pgvector**, and a separate **evaluation** module (≥1 metric, small dataset, demonstrated). Canonical product/architecture intent lives in `docs/VISION.md` and `docs/PRD.md`.

**Target stack (planned):** Java 21, Maven, Spring Boot 4.x, Spring Modulith, Spring AI 2.x + `spring-ai-agent-utils` / Session community libs, PostgreSQL 16 + pgvector, MCP clients, **Thymeleaf** demo UI, **OpenAPI / API-first** REST (`api/openapi.yaml` — see `docs/VISION.md`). **LLM:** `spring-ai-starter-model-openai` + **`spring.ai.custom.chat.*`** (and optional **`spring.ai.custom.embedding.*`**) per `docs/PRD.md` **NFR-8**.

## Repo Map (today)

```text
.
├── pom.xml                    # Maven reactor (app + e2e)
├── AGENTS.md                  # canonical project rules (root); see nested AGENTS.md
├── .agents/skills/
├── application/AGENTS.md      # runtime conventions (Modulith); code in meteoris-insight/
├── meteoris-insight/          # Spring Boot app (Modulith packages: app-api, …)
├── meteoris-insight-e2e/      # black-box E2E (separate Maven module)
├── docs/
│   ├── VISION.md
│   └── …
├── mkdocs.yml
└── requirements-docs.txt
```

**Build:** `mvn verify` from project root — builds `meteoris-insight`, then `meteoris-insight-e2e`. Logical modules `app-*` from `docs/VISION.md` are **Spring Modulith boundaries inside** `meteoris-insight`, not separate Maven modules unless you split them later on purpose.

## Architecture (high level)

Conceptual layers (from vision):

```text
           ┌─────────────┐
           │  app-core   │  IdGenerator (ObjectId ids), tiny utils
           └──────┬──────┘
                  │ (OPEN — all modules may use)
           ┌──────▼──────┐
           │  app-api    │  Thymeleaf + OpenAPI REST
           └──────┬──────┘
                  │
           ┌──────▼──────┐
           │ app-agent   │  ChatClient, advisors, Task tool, tools
           │   -core     │
           └──────┬──────┘
      ┌────────────┼────────────┐
      ▼            ▼            ▼
 app-weather   app-news    app-memory
 (MCP+skill)   (MCP+RAG)   Session+AutoMemory
                  │
                  ▼
              app-eval
```

**Edge:** `app-api` (transport). **Core orchestration:** `app-agent-core`. **Domain/integration slices:** `app-weather-agent`, `app-news-agent`, `app-memory`, `app-eval`. Dependencies should flow **inward** (API → core → leaf modules); avoid core depending on API.

**Domain concepts (planned):** chat sessions & branches (Session API); durable user prefs (AutoMemory files); weather/news **tool results** as DTOs passed to the LLM; evaluation **examples** + **metric reports** (owned by `app-eval`).

## Commands

```bash
# Documentation (strict)
pip install -r requirements-docs.txt
mkdocs build -s

mvn verify
```

**Integration tests** use **Docker Testcontainers** (PostgreSQL 16 + pgvector); local **`spring-boot:run`** uses **PostgreSQL** via **`docker-db`** (see **`application/AGENTS.md`** → *Database and automated tests*).

## Where project rules live

- **All project rules** (coding standards, language policy, boundaries, workflows) are defined **only** in **`AGENTS.md`** at the repository root and in **nested `AGENTS.md`** files at major module boundaries (`docs/AGENTS.md`, `application/AGENTS.md`, `meteoris-insight-e2e/AGENTS.md`, etc.).
- **Do not** use **`.cursor/`** (including `.cursor/rules/`) for this repository. Do not add duplicate rule sources there. Agents and humans must read **`AGENTS.md`** + **`docs/ai-context-strategy.md`** + **`.agents/skills/**/SKILL.md`** for how-to detail.
- Supplementary product specs remain in `docs/` (e.g. `VISION.md`).

## Global boundaries

- **Language (English only):** Write **all project documentation** in **English**: Markdown under `docs/`, root and nested `AGENTS.md`, `.agents/skills/**/SKILL.md`, `README.md`, MkDocs pages, OpenAPI descriptions, ADRs, and commit/PR descriptions for this repository. Write **all code comments** in **English**: inline and block comments, JavaDoc, XML/HTML comments in shipped source and config. **Agent skills** are contributor-facing documentation — English only. Do **not** mix other natural languages in these artifacts; if you quote external non-English content, keep the surrounding explanation in English.
- ✅ OK: extend `docs/VISION.md`, MkDocs, skills, and (once present) Modulith modules per vision.
- ⚠️ Changing **evaluation criteria** or **`/api/**` behaviour** must stay aligned with `docs/VISION.md` and **`meteoris-insight/api/openapi.yaml`** (E2E client generation depends on it).
- ⚠️ **MCP / external APIs:** no secrets in repo; respect rate limits and keyless news MCP choice.
- 🚫 Do not call **live** LLM providers from automated tests without a stub profile (use **`stub-ai`** and stub beans as in this repository).
- 🚫 Do not bypass Modulith boundaries once `package-info.java` exists (no deep imports across internal packages).
- 🚫 Do **not** use **JPA / Hibernate** for Meteoris-owned data — **Spring JDBC** (`NamedParameterJdbcTemplate`) only.
- **Identifiers:** **MongoDB-compatible** 24-char lowercase hex ids (**ObjectId** byte layout); use **`app-core` `IdGenerator`** with **`extractCreationInstant`** — see `docs/VISION.md` and `docs/ARCHITECTURE.md`.

## Skills index (canonical)

| Skill | When to use |
|-------|-------------|
| `core-architecture` | module layout, Modulith edges, dependency direction |
| `agentic-patterns` | SkillsTool, Task tool, Session, AutoMemory, Todo/AskUser, A2A |
| `mcp-integration` | MCP servers, Java clients, `@Tool` wrappers for weather/news |
| `modulith-spring-ai-boundaries` | Prevent `@Tool` annotations from leaking Modulith module boundaries |
| `api-design` | REST/WebSocket chat, QuestionHandler, DTO versioning |
| `testing` | unit/integration tests, Testcontainers, stub AI |
| `evaluation` | eval datasets, metrics, `EvaluationRunner` style flows |
| `repository-design` | JDBC, Flyway, pgvector SQL, `IdGenerator`, named parameters |

## Module guidance

- Docs & MkDocs: `docs/AGENTS.md` (includes **WIREFRAMES.md** rule: after each text screen wireframe, add a **PlantUML Salt** `@startsalt` / `@endsalt` block)
- Spring runtime (Modulith, MCP, DB): `application/AGENTS.md` (conventions); code tree: `meteoris-insight/`
- E2E subproject: `meteoris-insight-e2e/AGENTS.md`

## Links

- Vision & assignment: `docs/VISION.md`
- Engineering architecture: `docs/ARCHITECTURE.md`
- Work scenarios catalogue: `docs/WORK-SCENARIOS.md`
- AI context strategy: `docs/ai-context-strategy.md`
- Supplementary patterns: `.agents/skills/` and `docs/ai-context-strategy.md`
