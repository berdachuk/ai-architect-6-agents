# Meteoris Insight — Product Requirements Document (PRD)

Normative **product and engineering requirements** for **Meteoris Insight**. This PRD is the primary requirements source for functional and non-functional expectations. Supporting artifacts:

| Artifact | Role |
|----------|------|
| [VISION.md](VISION.md) | Assignment alignment, Spring AI Parts 1–7 narrative, rough implementation plan |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Engineering structure, flows, quality attributes |
| [USE-CASES.md](USE-CASES.md) | Use case ID catalogue (`U.*`, `A.*`, …) |
| [USER-STORIES.md](USER-STORIES.md) | Agile backlog `US-xx` with acceptance criteria mapped to use cases |
| [WORK-SCENARIOS.md](WORK-SCENARIOS.md) | Operational scenario tables |
| [FORMS-AND-FLOWS.md](FORMS-AND-FLOWS.md) | Thymeleaf forms, page map, UI/REST workflows |
| [WIREFRAMES.md](WIREFRAMES.md) | Text wireframes per screen (labels, empty states) |
| [EVALUATION-METHODOLOGY.md](EVALUATION-METHODOLOGY.md) | Reproducible eval: dataset schema, profiles, scoring, reports |
| [IMPLEMENTATION-PLAN-WBS.md](IMPLEMENTATION-PLAN-WBS.md) | WBS, phases, milestones M1–M6, Definition of Done |

**Canonical REST contract:** `meteoris-insight/api/openapi.yaml` (when present). This PRD describes required **capabilities** and **example shapes**; path names and schemas must match the OpenAPI file once authored.

---

## Overview

Meteoris Insight is an **English-first** agentic assistant that answers questions about **current weather** and **latest news** using a **main orchestrator**, **weather** and **news** subagents (via the Spring AI **Task** tool), **MCP** integrations for **Open‑Meteo** and a **keyless** news source, **Session** short-term memory (with compaction and optional branch isolation), **AutoMemory** for durable preferences, optional **pgvector** for semantic news assistance, and a separate **evaluation** module with at least **one** demonstrated metric on a **small** dataset.

**v1 implementation note (weather and news transport):** The shipped service uses **public HTTP** integrations that remain **assignment-compatible**: **Open‑Meteo** geocoding and forecast APIs (the same data family as Open‑Meteo MCP servers) and **Google News RSS** as the **documented keyless** headline source. Both are exposed to the LLM through the same **Spring `@Tool`** facade used for MCP-backed tools, so a later revision can swap internals to **stdio or HTTP MCP clients** without changing the REST contract or Thymeleaf demo flows.

The product satisfies the **Practical Task: Agentic AI** with a **production-style** modular monolith (Spring Modulith logical modules inside one deployable JAR), **API-first** REST under `/api/**`, and a **Thymeleaf** demo UI with server-rendered pages alongside generated REST clients and **`meteoris-insight-e2e`** consuming the same OpenAPI spec.

**Course wording (Practical Task: Agentic AI — *What should be done*)**

> Create an application that can answer questions about **current weather** and **latest news**, using **agent orchestrators** and MCP-class tools for **Open‑Meteo** and a **keyless** news source (**Google News RSS** — no API key).
> At least **one evaluation metric** is defined, evaluated, and demonstrated (quantitative or qualitative for performance, quality, and safety of generative outputs), with a **small evaluation dataset** and at least **one** criterion.

### Alignment with Practical Task: Agentic AI

| Program requirement | How Meteoris Insight addresses it |
|---------------------|----------------------------------|
| Weather and news via agents + MCP-class tools | **FR-1**, **FR-2**, **FR-3**: orchestrator, **Task** delegation, **Open‑Meteo‑family HTTP** tools, and **Google News RSS** as the **documented keyless** headline path (README); normalized `@Tool` surface to the model (MCP client transport optional follow-on). |
| Spring AI agentic patterns | **FR-4** Skills; **FR-5** AskUser + Todo; **FR-6** Session; **FR-7** AutoMemory; optional **pgvector** semantic news under **FR-3** / **`app-news-agent`**; see [VISION.md](VISION.md) Parts 1–7. |
| Thymeleaf + manual testing | **FR-9**: forms for AskUser, chat-style pages, optional todo progress. |
| OpenAPI / API-first | **FR-8**: single `openapi.yaml`; generated server interfaces; E2E client from same file. |
| Evaluation: ≥1 metric, small dataset, demonstrated | **FR-10** + [Evaluation](#evaluation-recommendations). |
| Identifiers and JDBC | **NFR-2**, **NFR-3**, **FR-11**: 24 hex ObjectId-layout string ids with `extractCreationInstant`; **NamedParameterJdbcTemplate** only; Flyway; **no JPA** for Meteoris-owned tables. |
| LLM wiring (OpenAI-compatible) | **NFR-8**: **programmatic Spring AI** beans (`spring.ai.custom.chat.*`, `@Primary` `ChatModel` / `ChatClient`); see **`LiveAgentConfiguration`** and **`StubAiConfiguration`** in this repository. |

**Quantitative vs qualitative:** The task allows either. This PRD standardizes on **quantitative** checks where possible (field presence, minimum headline counts) and allows a **documented qualitative** rubric on a small sample as a supplement.

### Optional extensions (not required for baseline)

| Item | Status |
|------|--------|
| **A2A server** (AgentCard, JSON-RPC) | **Optional** strong demo; feature-flagged. |
| **Streaming** (SSE/WebSocket) for `/api/**` | **Optional**; only if reflected in OpenAPI. |
| **Outbound A2A** client to remote agents | **Optional** ninja direction. |
| Multi-tenant auth, paid news APIs with keys | **Non-goals** ([Non-goals](#non-goals)). |

---

## Product goals

### Primary goals

- Deliver **accurate, attributable** weather answers using **Open‑Meteo** via MCP and a dedicated **weather** subagent path.
- Deliver **latest news** digests using a **keyless** news MCP and a **news** subagent path.
- Use **AskUserQuestionTool** when city, coordinates, topic, language, or time window are missing or ambiguous.
- Use **TodoWriteTool** for **multi-step** user requests (for example weather plus news, or compare-two-cities flows).
- Persist conversation state with the **Session API** (JDBC, append-only events, **turn-safe compaction**).
- Persist stable preferences with **AutoMemory** where appropriate.
- Expose **OpenAPI-first** `/api/**` and a **Thymeleaf** UI for the same orchestration.
- Run **evaluation** with **at least one** defined metric on a **small** versioned dataset; store or export results suitable for demo and CI.

### Secondary goals

- Optional **pgvector** storage for news embeddings and similarity-style recall.
- **Spring Modulith** with enforced `allowedDependencies` and `*.api` facades.
- **`mvn verify`** from project root with **stub AI** (no live LLM in default CI).
- Optional **A2A** exposure of the orchestrator for interoperability demos.

### Non-goals

- Multi-tenant authorization and per-tenant MCP credentials in v1.
- Full SPA frontend; UI remains server-rendered Thymeleaf for the course demo.
- **JPA / Hibernate** or Spring Data JPA for first-party tables.
- **UUID** primary keys for new Meteoris-owned rows (use ObjectId-layout string ids per **NFR-2**).
- Paid news vendor APIs that require per-operator API keys for the **baseline** assignment path (baseline is **keyless** news MCP only).

---

## Scope

### In scope

- Maven reactor: **root `pom.xml`** → **`meteoris-insight`** (Spring Boot) + **`meteoris-insight-e2e`**.
- Modulith packages: at minimum **`app-core`**, **`app-api`**, **`app-agent-core`**, **`app-weather-agent`**, **`app-news-agent`**, **`app-memory`**, **`app-eval`** (exact DAG in [ARCHITECTURE.md](ARCHITECTURE.md)).
- MCP client adapters and Spring AI `@Tool` methods (`getWeatherForecast`, `findNews`, or names aligned with OpenAPI).
- PostgreSQL **16** with **pgvector** extension; Flyway migrations; Session JDBC schema when using `spring-ai-starter-session-jdbc`.
- Documentation in English (repo docs and code comments per root `AGENTS.md`).

### Out of scope

- Message brokers, Kubernetes-specific deployment manifests (unless added later as samples).
- Legal certification of weather or news data for production consumer use (demo / educational context).

---

## Users and usage scenarios

### Primary users

- **End user:** uses Thymeleaf pages to chat about weather and news.
- **Integrator:** uses REST clients generated from `openapi.yaml`.
- **Operator:** developer or evaluator runs locally, edits skills and prompts, runs `mvn verify`, triggers eval.

### Key demo scenarios

1. Ask for weather without a city; complete **AskUser** flow; receive forecast with units per **weather-skill**.
2. Ask for news with clarifying topic or window; receive digest per **news-skill**.
3. Ask for **both** weather and news in one message; observe optional todos and merged answer.
4. Call **`POST`** (or contract-equivalent) chat API from a script; same behaviour as UI where applicable.
5. Run **evaluation** suite; inspect pass rate and failed-case export.

Detailed scenario IDs: [USE-CASES.md](USE-CASES.md).

---

## Technology stack

| Area | Choice | Notes |
|------|--------|-------|
| Language | **Java 21** | |
| Runtime | **Spring Boot 4.x** | Pin BOMs with Spring AI / Modulith compatibility at scaffold time. |
| Modularity | **Spring Modulith** | Logical modules as packages + `package-info.java`; architecture tests in CI. |
| Generative AI | **Spring AI** 2.x + **spring-ai-agent-utils** | SkillsTool, AskUserQuestionTool, TodoWriteTool, Task tool. |
| LLM transport | **`spring-ai-starter-model-openai`** + explicit Java beans | **NFR-8**: OpenAI-**compatible** HTTP only; **`spring.ai.custom.chat.*`** for chat endpoint; disable conflicting Spring AI OpenAI auto-config; **`@Primary` `ChatModel`** + **`ChatClient`**. |
| Embeddings (optional) | Programmatic **`OpenAiEmbeddingModel`** when pgvector is used | **`spring.ai.custom.embedding.*`**, **`@Primary` `EmbeddingModel`**; omit if v1 skips semantic embeddings. |
| Session | **spring-ai-session** + JDBC starter | `AI_SESSION`, `AI_SESSION_EVENT`; compaction advisors. |
| Web | **Spring Web MVC** + **Thymeleaf** | |
| API | **OpenAPI 3** + generator | Server stubs + **E2E** client from same YAML. |
| Database | **PostgreSQL 16** + **pgvector** | Session, app tables, optional embeddings. **No embedded H2** — local runs use **Docker Compose** (profile **`docker-db`**) or **Testcontainers** in automated tests. |
| Persistence access | **Spring JDBC** (`NamedParameterJdbcTemplate`) | **No JPA** for Meteoris-owned tables (**NFR-3**). |
| Migrations | **Flyway** | Append-only; never rewrite applied files. |
| MCP | Community / Tools4AI stack (finalize at implementation) | Stdio or HTTP per server. |
| Optional A2A | **spring-ai-a2a** (community) | Server autoconfigure when enabled. |
| Testing | JUnit 5; optional Testcontainers; **stub AI** in default `verify` | No live LLM in canonical CI path. |

---

## External integrations

### Weather MCP (Open‑Meteo)

- The application connects to an MCP server exposing Open‑Meteo-backed tools (for example forecast by coordinates or geocoded city).
- Java layer: **`WeatherMcpClient`** (or equivalent) + **`WeatherMcpTool`** / `@Tool` wrapping MCP; orchestrator does not embed raw HTTP to Open‑Meteo in prompts.

### News MCP (keyless)

- Baseline integration **must not** require an operator API key for the news source (assignment constraint). The implementation uses **Google News RSS** as the keyless headline source.
- Java layer: **`NewsMcpClient`** + **`NewsMcpTool`**; optional pgvector cache in **`app-news-agent`**.

---

## System architecture

Meteoris Insight is a **single deployable Spring Boot application** with **Spring Modulith** boundaries. High-level layers: **API/UI** (Thymeleaf + REST implementing generated interfaces), **Agent orchestration** (orchestrator + subagents), **MCP/A2A**, **Persistence & memory** (Postgres, Session JDBC, AutoMemory filesystem, optional vectors), **Evaluation**.

Authoritative module list, dependency rules, and runtime diagrams: **[ARCHITECTURE.md](ARCHITECTURE.md)**.

---

## Functional requirements

### FR-1 — Orchestrator and subagents

The system shall provide a **main orchestrator** `ChatClient` that can delegate to **weather** and **news** subagents using the **Task** tool from `spring-ai-agent-utils`. Subagents run with **isolated** prompts and **tool allow-lists**. Subagents **shall not** register a nested **Task** tool.

**Acceptance criteria**

- Weather and news intents are routed to the correct subagent in representative manual and automated tests.
- **Maps to:** `O.02`, `O.03`, `O.08`, `P.01`, `P.02`; **US-23**, **US-24**, **US-28**.

---

### FR-2 — Weather via MCP

The system shall answer current and near-term **weather** questions using **Open‑Meteo** data obtained through an **MCP** server and the weather subagent tools.

**Acceptance criteria**

- Forecasts include interpretable conditions (for example temperature, conditions, effective time window) when data is available.
- Invalid coordinates or MCP errors surface as user-safe messages (**NFR-7** alignment).
- **Maps to:** `P.01`, `U.02`, `U.04`, `U.13`; **US-03**, **US-05**, **US-14**, **US-52**.

---

### FR-3 — News via keyless MCP

The system shall answer **latest news** questions using a **keyless** news MCP and the news subagent tools.

**Acceptance criteria**

- Headlines or items reflect MCP payloads; optional eval check that URLs are not invented when **FR-10** safety flag is enabled.
- **Maps to:** `P.02`, `U.03`, `U.05`, `U.13`; **US-04**, **US-06**, **US-39**.

---

### FR-4 — Agent skills and guardrails

The system shall register **weather-skill** and **news-skill** via **SkillsTool** (progressive disclosure). The orchestrator shall **refuse** harmful or unsupported requests without calling MCP where policy defines a block.

**Acceptance criteria**

- Each skill has `SKILL.md` with YAML front matter (`name`, `description`).
- **Maps to:** `U.11`, `O.05`; **US-12**, **US-26**.

---

### FR-5 — AskUser and Todo patterns

The system shall integrate **AskUserQuestionTool** with a **`QuestionHandler`** for Thymeleaf and shall support **TodoWriteTool** for multi-step tasks, with optional UI subscription to todo events.

**Acceptance criteria**

- Ambiguous weather or news inputs trigger structured questions before expensive tool calls when appropriate.
- **Maps to:** `U.02`, `U.03`, `U.06`, `U.10`, `A.04`; **US-03**, **US-04**, **US-07**, **US-11**, **US-19**.

---

### FR-6 — Session memory

The system shall persist **Session** / **SessionEvent** history with **compaction** that respects **turn boundaries**; optional **branch** labels (`orch`, `orch.weather`, `orch.news`) with filters for subagent isolation; optional **SessionEventTools** for `conversation_search`.

**Acceptance criteria**

- Session id is supplied per HTTP request via advisor context keys as per Spring AI Session documentation.
- **Maps to:** `M.01`–`M.03`, `U.08`, `U.12`, `U.14`; **US-09**, **US-13**, **US-29**, **US-30**.

---

### FR-7 — AutoMemory

The system shall configure **AutoMemoryTools** with a filesystem root and **`MEMORY.md`** index for curated long-term facts (default city, units, preferred topics).

**Acceptance criteria**

- **AutoMemoryToolsAdvisor** (or equivalent wiring) registers tools and guidance.
- **Maps to:** `M.04`, `M.06`, `U.09`; **US-10**, **US-31**.

---

### FR-8 — OpenAPI-first REST

All machine-facing **`/api/**`** behaviour shall be described in **`meteoris-insight/api/openapi.yaml`** before implementation drift; generated server API interfaces are implemented by controllers; **`meteoris-insight-e2e`** generates its HTTP client from the **same** file.

**Acceptance criteria**

- Invalid requests return contract-defined error bodies (for example Problem+JSON) with stable problem types.
- Version negotiation and idempotency follow the spec when those features are declared.
- **Maps to:** `A.01`–`A.08`; **US-16**, **US-17**, **US-21**.

---

### FR-9 — Thymeleaf UI

The system shall provide server-rendered pages for **landing**, **chat** (or equivalent), and navigation sufficient to demo **FR-1**–**FR-7** without a SPA build.

**Acceptance criteria**

- AskUser flows work through HTML forms or fragments bound to **QuestionHandler**.
- **Maps to:** `U.01`–`U.15`; **US-01**–**US-15**.

---

### FR-10 — Evaluation module

The system shall include an **`app-eval`** (or equivalent) module that loads a **versioned** small dataset (YAML/JSON), runs cases through the orchestrator (typically **fresh session**, **AutoMemory disabled** for isolation), computes **at least one** metric, and emits a **report** (JSON log, file, and/or REST response).

**Acceptance criteria**

- **Weather** cases: configurable `required_fields` (for example city, temperature, conditions, time period) with pass/fail rules documented in README.
- **News** cases: `min_headlines` and optional source/time checks.
- Operator can trigger eval via **REST** and/or **CLI** (`ApplicationRunner` or documented command).
- Optional **safety** check: headlines cite URLs present in MCP payload (**E.05**).
- Failed-case **export** for review (**E.06**).
- **Maps to:** `P.03`, `E.01`–`E.06`, `M.08`; **US-34**–**US-40**, **US-20**.

---

### FR-11 — Core identifiers and JDBC data

The system shall implement **`IdGenerator`** in **`app-core`**: `generateId()`, `isValidId(String)`, `extractCreationInstant(String)` for **BSON ObjectId-layout** 24 lowercase hex string ids. Application tables shall use **`varchar(24)`** / **`char(24)`** for these keys where applicable; repositories use **named** JDBC parameters.

**Acceptance criteria**

- Unit tests cover generation, validation, and instant extraction.
- **Maps to:** `I.01`–`I.04`; **US-47**, **US-48**.

---

## Non-functional requirements

### NFR-1 — Modularity and maintainability

Spring Modulith **`@ApplicationModule`** and **`allowedDependencies`** shall be enforced; **`mvn verify`** runs architecture tests. Cross-module consumers depend on **`*.api`** types where facades exist.

### NFR-2 — Identifiers

All application-generated primary keys for persisted Meteoris-owned entities shall be **24-character lowercase hex** strings with **ObjectId** byte layout; wire and JSON use lowercase hex. **`extractCreationInstant`** must decode the leading **4-byte Unix timestamp** to UTC.

### NFR-3 — Persistence technology

**No JPA or Spring Data JPA** for Meteoris-owned tables. Use **`NamedParameterJdbcTemplate`** and Flyway.

### NFR-4 — Testing and CI

Default **`mvn verify`** shall **not** require live LLM credentials. Use **stub** or **mock** `ChatClient` / `ChatModel` in `test` and e2e profiles. E2E exercises **`/api/**`** against a running instance or test harness per module README.

### NFR-5 — Observability

Enable Spring Boot **Actuator** health. Log chat, tool, MCP, and eval phases with **correlation** and **session** ids where available (**US-55**).

### NFR-6 — Safety and transparency

UI (and README) shall state that outputs are **demo / educational**, that **news** is not financial or professional advice, and that **weather** may differ from official warnings—wording to be finalized with legal course guidance.

### NFR-7 — Security hygiene

Reject or strip **secrets** pasted into chat where feasible; **never** log raw API keys or Authorization headers (**N.07**, **US-54**).

### NFR-8 — LLM configuration (programmatic Spring AI)

Meteoris Insight shall configure **chat** (and **optional embeddings** for pgvector) using **explicit** Spring AI beans and documented **`spring.ai.custom.*`** properties, as implemented in **`LiveAgentConfiguration`**, **`StubAiConfiguration`**, and **`application.yml`** in this repository.

**Shall**

- Depend on **`spring-ai-starter-model-openai`** and build **`OpenAiApi`** + **`OpenAiChatModel`** (and **`OpenAiEmbeddingModel`** when embeddings are required) in **application code**, not from conflicting auto-configuration alone.
- Keep **`spring.ai.openai.enabled: false`** (or equivalent) so duplicate default beans are not created; document **`spring.ai.custom.chat.*`** (base URL, API key, model, temperature, max tokens, timeouts) in README and config samples.
- Expose **`@Primary` `ChatModel`** and a **`ChatClient`** used by orchestrator and subagents (multi-`ChatClient` beans may use **`@Qualifier`** where Spring AI agent wiring requires it).
- For **optional** news/pgvector embeddings, use **`spring.ai.custom.embedding.*`** with **`@Primary` `EmbeddingModel`** and dimensions consistent with the chosen embedder and DB `vector(n)` column.

**Shall (tests)**

- Under the **`test`** (and default CI) profile, provide **`@Primary`** mock or stub **`ChatModel`** / **`ChatClient`** (and **`EmbeddingModel`** when used) so **`mvn verify`** does **not** call a live LLM or embedding endpoint — for example stub beans under **`stub-ai`** (`@Profile("!stub-ai")` or equivalent for real beans).

**Should**

- README documents env vars / property examples (e.g. chat base URL and model id for Ollama or cloud OpenAI-compatible endpoints).

---

## Database design

### Identifier strategy

Same as [VISION.md](VISION.md): MongoDB-compatible **ObjectId-layout** string ids; **`IdGenerator`** in **`app-core`**; SQL `varchar(24)` / `char(24)`; OpenAPI `pattern` for id fields when exposed.

### Session tables

When using **`spring-ai-starter-session-jdbc`**, apply vendor schema for **`AI_SESSION`** and **`AI_SESSION_EVENT`** (append-only) via Flyway or starter init per Spring AI Session documentation.

### Optional news vectors

Table(s) for cached headlines/snippets with **`vector(d)`** column when optional **pgvector** support is implemented (**FR-3** / **`app-news-agent`**); dimension **d** must match the chosen embedding model.

---

## API requirements

Concrete paths, operationIds, and schemas live in **`meteoris-insight/api/openapi.yaml`**. The following are **normative capability** expectations; names may differ if the OpenAPI file uses equivalent operations.

### Required capability groups

| Group | Examples (illustrative) | FR |
|-------|-------------------------|-----|
| Chat | `POST /api/...` send user message; receive assistant reply | FR-8, FR-1 |
| AskUser resume | Second call or ticket flow to supply answers | FR-5, FR-8 |
| Evaluation | `POST /api/.../evaluation/run` (or CLI-only if documented as exception) | FR-10 |
| Health | Spring Boot Actuator | NFR-5 |

### Example request/response shapes (illustrative)

Until OpenAPI is authoritative, treat these as **shape guidance** for implementors.

#### Chat message

Request:

```json
{
  "sessionId": "674a1b2c3d4e5f6789012345",
  "message": "What is the weather in Brest, Belarus right now?"
}
```

Response:

```json
{
  "sessionId": "674a1b2c3d4e5f6789012345",
  "reply": "In Brest, it is currently about 12°C and partly cloudy …",
  "metadata": {
    "model": "stub-or-live-model-id"
  }
}
```

#### Evaluation run

Request:

```json
{
  "dataset": "meteoris-eval-v1",
  "profile": "stub-ai"
}
```

Response:

```json
{
  "runId": "674a1b2c3d4e5f6789012346",
  "total": 12,
  "passed": 10,
  "failed": 2,
  "weatherPassRate": 0.83,
  "newsPassRate": 0.84
}
```

---

## UI requirements

The Thymeleaf UI is for **demonstration** and **manual testing**, not full product UX.

### Required pages (minimum)

- **`/`** — landing with links to chat / weather / news areas (**FR-9**).
- **Chat (or combined)** — message input, assistant reply area, rendering of **AskUser** forms when the tool activates.
- **Navigation** — consistent shell between pages (**US-02**).

### Optional

- Live **todo** progress panel bound to **TodoWriteTool** events (**US-11**).

Form fields, routes, and step-by-step flows: **[FORMS-AND-FLOWS.md](FORMS-AND-FLOWS.md)**. Screen-level layout and copy: **[WIREFRAMES.md](WIREFRAMES.md)**.

---

## Evaluation recommendations

**Normative methodology** (dataset schema, environment profiles, scoring algorithms, JSON report shape, regression rules): **[EVALUATION-METHODOLOGY.md](EVALUATION-METHODOLOGY.md)**.

Summary (aligns with [VISION.md](VISION.md)):

- **Weather metric:** share of eval cases whose answers mention the expected **city**, include **temperature** and **conditions**, and reference a **time frame** (now / today / tomorrow).
- **News metric:** share of cases with at least **N** headlines and short descriptions; optional source/time presence when MCP supplies them.

Dataset size: **about 10–20** cases is acceptable for a minimal course demo if documented; larger sets improve regression confidence.

Runner behaviour: **new Session** per case; **AutoMemory off** for eval isolation unless testing memory-specific cases (**M.08**).

---

## Security and compliance notes

- Demo system, not a certified weather or news service.
- Disclaimers visible per **NFR-6**.
- LLM and MCP endpoints configured via **environment** or Spring configuration; secrets not committed.

---

## Milestones

Step-by-step **work breakdown**, dependencies, and Definition of Done: **[IMPLEMENTATION-PLAN-WBS.md](IMPLEMENTATION-PLAN-WBS.md)**.

### Milestone 1 — Project bootstrap

- Reactor **`pom.xml` (root)** with **`meteoris-insight`** + **`meteoris-insight-e2e`**.
- Spring Boot, Modulith, Spring AI, Thymeleaf, JDBC, Flyway, PostgreSQL driver; **`app-core`** **`IdGenerator`** with tests.
- OpenAPI generator wired; placeholder **`api/openapi.yaml`** checked in.
- Local Postgres + pgvector (Docker Compose documented).

### Milestone 2 — MCP and tools

- Open‑Meteo MCP client + **`getWeatherForecast`** (or equivalent).
- Keyless news MCP client + **`findNews`** (or equivalent).
- Subagent **Task** registrations and smoke tests.

### Milestone 3 — Memory and skills

- **Session** JDBC + **SessionMemoryAdvisor** + compaction.
- **AutoMemory** root + advisor.
- **weather-skill** and **news-skill** registered via **SkillsTool**.

### Milestone 4 — UI and AskUser

- Thymeleaf landing + chat flows.
- **QuestionHandler** wired for AskUser forms (and REST resume when **FR-8** defines it).

### Milestone 5 — REST parity and E2E

- Controllers implement generated API interfaces.
- **`meteoris-insight-e2e`** black-box tests against **`openapi.yaml`** contract.

### Milestone 6 — Evaluation, polish, delivery

- **`app-eval`** dataset + runner + report export.
- README: run, demo script, eval instructions, metric definition.
- **`mvn verify`** green with **stub AI**; screenshots or logs for submission evidence.

---

## Deliverables

- Source repository with Modulith modules and architecture tests.
- **`api/openapi.yaml`** + generated interfaces; E2E client generation.
- Flyway migrations for application and Session tables.
- Docker Compose (or equivalent) for Postgres + pgvector.
- Versioned **eval dataset** (YAML/JSON) and **report** sample output.
- README with setup, **LLM / embedding env and `spring.ai.custom.*` properties**, **keyless news source** (Google News RSS), **metric** definition, and **demo** steps.
- This PRD and linked docs kept consistent on scope changes.

---

## Cursor AI implementation guidance

Use bounded tasks aligned to milestones. Prefer: scaffold Modulith packages first, then Flyway + JDBC, then MCP tools, then orchestration, then UI, then eval, then E2E.

### Suggested prompts (examples)

- “Add `package-info.java` with `@ApplicationModule` for `app-core`, `app-api`, `app-agent-core`, `app-weather-agent`, `app-news-agent`, `app-memory`, `app-eval` per [ARCHITECTURE.md](ARCHITECTURE.md).”
- “Implement `IdGenerator` in `app-core` with `generateId`, `isValidId`, `extractCreationInstant` and unit tests per **NFR-2**.”
- “Create Flyway V1__…sql for Session tables and news cache table with `varchar(24)` PKs.”
- “Wire `SessionMemoryAdvisor` + compaction triggers; add branch labels for weather vs news.”
- “Add openapi.yaml operations for chat + eval run; regenerate sources; implement delegate to orchestrator.”
- “Implement LLM wiring in **`app-core`** or **`app-agent-core`** (`@Profile("!stub-ai")` for live beans): disable conflicting OpenAI auto-config, read **`spring.ai.custom.chat`** / **`spring.ai.custom.embedding`**, build **`OpenAiChatModel`** (and **`OpenAiEmbeddingModel`** if needed), **`@Primary`** beans and **`ChatClient`**; keep **`StubAiConfiguration`** for **`stub-ai`** / CI.”

---

## Traceability (FR → user stories)

| FR | Primary user stories |
|----|------------------------|
| FR-1 | US-22–US-28 |
| FR-2 | US-03, US-05, US-14, US-52 |
| FR-3 | US-04, US-06, US-39 |
| FR-4 | US-12, US-26 |
| FR-5 | US-03, US-04, US-07, US-11, US-19 |
| FR-6 | US-09, US-13, US-29, US-30 |
| FR-7 | US-10, US-31, US-32 |
| FR-8 | US-16–US-18, US-21 |
| FR-9 | US-01–US-15 |
| FR-10 | US-20, US-34–US-40 |
| FR-11 | US-47, US-48, US-46 |

---

## Document version

**1.0** — Initial PRD aligned with [VISION.md](VISION.md) and Meteoris Insight architecture and backlog docs. Update this file when FR/NFR or API capabilities change; update **OpenAPI** and **USER-STORIES** in the same change set when feasible.
