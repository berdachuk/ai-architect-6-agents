# Meteoris Insight — Project Vision

**Application name:** Meteoris Insight

The **Product Requirements Document** ([PRD.md](PRD.md)) defines **FR-xx** / **NFR-xx** requirements; this vision expands rationale, Spring AI pattern mapping, and implementation notes. **Thymeleaf routes, forms, and UI/REST workflows** are specified in [FORMS-AND-FLOWS.md](FORMS-AND-FLOWS.md). **Text wireframes** for each screen are in [WIREFRAMES.md](WIREFRAMES.md).

A structured catalogue of **possible work scenarios** (users, API, agents, memory, eval, CI) lives in [WORK-SCENARIOS.md](WORK-SCENARIOS.md). Every scenario ID in one document (including product assignment and identity/persistence cases) is in [USE-CASES.md](USE-CASES.md). User stories (`US-xx`) with acceptance criteria map to those IDs in [USER-STORIES.md](USER-STORIES.md). The **engineering architecture** (layers, Modulith, data, flows, testing) is detailed in [ARCHITECTURE.md](ARCHITECTURE.md).

## Practical Task: Agentic AI

*What should be done*

- **Meteoris Insight** shall answer questions about current weather and latest news, using Agent orchestrators and MCP-class integrations for **Open‑Meteo** and **Google News RSS** (keyless — no API key required).
- At least one evaluation metric is defined, evaluated, and demonstrated (quantitative or qualitative measure used to assess the performance, quality, and safety of generative model outputs). You need to have at least small evaluation data set and for at least one criterion.


**Meteoris Insight** is a modular “agentic” service: one orchestrator agent, subordinate weather/news sub‑agents, calls to MCP-class integrations for **Open‑Meteo** (public HTTP geocoding + forecast API) and **Google News RSS** (keyless — no API key required), plus a memory layer (Session + AutoMemory + PgVector) and a separate evaluation module.

For **implementation and manual testing simplicity**, the product UI will be built with **server-side Thymeleaf** pages and Spring MVC controllers. This avoids a separate SPA build pipeline while still allowing a clear **QuestionHandler** mapping (HTML forms or fragments for **AskUserQuestionTool**, chat-style pages for orchestrated Q&A, optional progress display for **TodoWriteTool** events).

The **HTTP API** will follow an **API-first** workflow: a single canonical **OpenAPI 3** document (e.g. `meteoris-insight/api/openapi.yaml`) describes `/api/**` before implementation; the application build **generates** Spring server API interfaces and DTOs (e.g. via `openapi-generator-maven-plugin`); REST controllers **implement** those generated interfaces and delegate to orchestration. **`meteoris-insight-e2e`** generates its HTTP client from the **same** `openapi.yaml`, keeping provider and consumer in lockstep and simplifying black-box tests.

Below is a vision and rough plan tailored to the stack (Java 21, Spring Boot 4.x, Spring AI 2.0.0‑M4+, Spring Modulith 2.0.5, PgVector 16, **Thymeleaf**, **OpenAPI / API-first**), aligned with the **Spring AI Agentic Patterns** blog series ([Part 1 — Agent Skills](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills) through [Part 7 — Session API](https://spring.io/blog/2026/04/15/spring-ai-session-management)). Concrete dependency versions should follow the current `spring-ai-agent-utils`, `spring-ai-session`, and Spring AI BOMs when you implement.

### LLM and embedding settings

Configure **chat** (and **optional embeddings** if pgvector news features are enabled) using **programmatic Spring AI** beans: **`spring-ai-starter-model-openai`**, **`OpenAiChatModel`** / optional **`OpenAiEmbeddingModel`** from **`spring.ai.custom.chat.*`** and **`spring.ai.custom.embedding.*`**, **`spring.ai.openai.enabled: false`**, **`@Primary`** beans, and **stub-profile mocks** (e.g. **`stub-ai`**) so CI never calls a live endpoint. Normative requirement: **[PRD.md](PRD.md) NFR-8**.

### Identifiers and database access

- **Unified identifiers:** use **MongoDB-compatible string IDs** everywhere a primary key or stable reference is needed (REST path ids, DB primary keys, correlation ids in logs): exactly **24 hexadecimal characters** (12 bytes), **lowercase** hex in wire and JSON for consistency.
- **Layout (BSON ObjectId):** same as MongoDB **ObjectId**: **4-byte big-endian Unix time** (seconds since epoch), then **5** random bytes, then **3-byte** monotonic counter. This embeds **creation time** in the id.
- **Generator API:** implement **`IdGenerator`** (or equivalent) in a small **OPEN** Modulith module (e.g. **`app-core`**) with at least:
  - `String generateId()` — new id using current time;
  - `boolean isValidId(String id)` — rejects wrong length or non-hex;
  - **`Instant extractCreationInstant(String id)`** (or `extractInstant`) — decodes the **leading 4 bytes** (first **8** hex chars) to **UTC** `Instant`, equivalent in intent to **`ObjectId.getDate()`** in MongoDB drivers — so support and analytics can recover **date and time** without a separate column (you may still add `created_at` for SQL convenience if desired, but ids alone must be decodable).
- **Database access:** use **Spring JDBC only** — primarily **`NamedParameterJdbcTemplate`** with **named** parameters (`:sessionId`, `:articleId`). **Do not** introduce **JPA / Hibernate** or Spring Data JPA repositories for Meteoris-owned tables (**NFR-3**).
- **Migrations:** **Flyway** scripts under `meteoris-insight/src/main/resources/db/migration/`; append-only versioning — **never** edit applied migrations in place.
- **Column types:** store generated ids as **`varchar(24)`** or **`char(24)`** (fixed width) for application tables; match OpenAPI `pattern` for id fields where applicable.

***

## Spring AI Agentic Patterns (Parts 1–7) and Meteoris Insight

Official series (Spring blog):

| Part | Topic | Article |
|------|--------|---------|
| 1 | Agent Skills — modular, reusable capabilities | [spring.io/…/spring-ai-generic-agent-skills](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills) |
| 2 | AskUserQuestionTool — interactive workflows | [spring.io/…/spring-ai-ask-user-question-tool](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool) |
| 3 | TodoWriteTool — structured task management | [spring.io/…/spring-ai-agentic-patterns-3-todowrite](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/) |
| 4 | Subagent orchestration — hierarchical multi‑agent architectures | [spring.io/…/spring-ai-agentic-patterns-4-task-subagents](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents) |
| 5 | A2A integration — interoperable agents | [spring.io/…/spring-ai-agentic-patterns-a2a-integration](https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration) |
| 6 | AutoMemoryTools — persistent long‑term memory | [spring.io/…/spring-ai-agentic-patterns-6-memory-tools](https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools) |
| 7 | Session API — short‑term memory with turn‑safe compaction | [spring.io/…/spring-ai-session-management](https://spring.io/blog/2026/04/15/spring-ai-session-management) |

### Part 1 — Agent Skills

- Skills are directories with a required `SKILL.md` (YAML front matter with at least `name` and `description`) plus optional `scripts/`, `references/`, and `assets/`. At startup, **SkillsTool** registers a lightweight catalogue (names and descriptions only); the model loads full instructions **on demand** when it invokes the skill — **progressive disclosure** that keeps context small even with many skills.
- Optional companions from the same toolkit: **FileSystemTools** (read bundled references) and **ShellTools** (run helper scripts); scripts run on the host, so treat them as trusted code or isolate the process (see the series security notes).
- Meteoris Insight should ship at least **`weather-skill`** (units, phrasing, how to present forecasts) and **`news-skill`** (digests, recency, source attribution). Wire **SkillsTool** via `spring-ai-agent-utils` so behaviour can evolve by editing Markdown rather than Java. [Part 1](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills)

### Part 2 — AskUserQuestionTool

- Lets the model ask structured questions (options with descriptions, single or multi‑select, plus free text) **before** answering, so assumptions about city, topic, language, or time window are not silently wrong.
- You implement a **`QuestionHandler`** (console, REST, or WebSocket/SSE + `CompletableFuture` for async UIs); the tool delivers questions, your code returns answers, execution continues in the same `ChatClient` flow. Conceptually related to **MCP Elicitation** for server‑driven forms; AskUserQuestionTool keeps the pattern inside the agent. [Part 2](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool)
- Meteoris Insight: use when **weather** inputs (city / coordinates) or **news** inputs (topic, language, horizon) are missing or ambiguous.

### Part 3 — TodoWriteTool

- The model maintains an explicit todo list (`pending`, `in_progress`, `completed`). **Only one item may be `in_progress`** at a time, nudging sequential, observable execution instead of “lost in the middle” skipped steps.
- Tool guidance recommends use when a task needs **about three or more distinct steps**; combine with **ToolCallAdvisor** and durable conversation state so list updates are visible to the model across turns (see the Part 3 configuration example).
- Optional: subscribe to todo update **events** to drive a live UI progress bar.
- Meteoris Insight: ideal for compound requests (e.g. compare weather and news for two cities, then synthesise) — same demonstration value as core product value. [Part 3](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/)

### Part 4 — Subagent orchestration (Task tool)

- The **orchestrator** talks to the user and delegates to **subagents** through the **Task** tool from `spring-ai-agent-utils`. Each subagent runs in an **isolated context** with its own system prompt, tool allow‑list, and optional **model preference** (multi‑model routing). Subagents are described at startup (e.g. Markdown definitions with `name`, `description`, optional `tools`, `model`); the parent’s LLM chooses **when** to delegate from natural‑language descriptions — an **agent registry** pattern.
- Subagents must **not** nest another Task tool (no recursive subagent trees in the stock pattern).
- Meteoris Insight mapping:
  - **Main orchestrator** — SkillsTool, AskUserQuestionTool, TodoWriteTool, Task tool, Session/AutoMemory as configured.
  - **Weather subagent** — Open‑Meteo–backed tools only.
  - **News subagent** — news MCP tools (+ optional vector/RAG helpers).
- Further API details: community [TaskTools / Task tool documentation](https://github.com/spring-ai-community/spring-ai-agent-utils) alongside [Part 4](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents).

### Part 5 — A2A integration

- **Agent2Agent (A2A)** standardises discovery and messaging between agents. Peers discover capabilities via an **AgentCard** (JSON), exposed under **`/.well-known/agent-card.json`** (and related HTTP JSON‑RPC message endpoints as described in the post).
- **Spring AI A2A** (community) focuses on **server** autoconfiguration: expose a `ChatClient`‑backed agent as an A2A server (`DefaultAgentExecutor`, agent card bean, etc.). A separate **A2A Java SDK client** dependency supports a **host** agent that delegates to remote A2A agents via tools.
- Meteoris Insight (assignment scope): exposing the **main orchestrator** as an A2A server is a strong demo; splitting weather/news into remote A2A services is optional. [Part 5](https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration)

### Part 6 — AutoMemoryTools (long‑term memory)

- **Complements** sliding chat history: **AutoMemoryTools** persist **curated** facts (preferences, stable domain choices) as **Markdown files** under a configured root, with a **`MEMORY.md` index** and typed entries (`user`, `feedback`, `project`, `reference`). Six sandboxed operations (view, create, str‑replace, insert, delete, rename) implement the Claude‑style memory workflow; **AutoMemoryToolsAdvisor** can register tools + system prompt in one step.
- **Not a substitute** for Session (Part 7): Session holds the structured **conversation**; AutoMemory holds **facts that should survive** across sessions and compaction.
- Meteoris Insight: default city, units, preferred news topics, evaluation preferences. [Part 6](https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools)

### Part 7 — Session API (short‑term memory)

- Replaces “flat **ChatMemory** list” with **event‑sourced** **`Session` / `SessionEvent`** storage: every user turn, assistant reply, tool call, and tool result is recorded with identity and timestamps. **Compaction** (turn count, token budget, sliding window, or **recursive summarisation**) always snaps to **turn boundaries** so the model never sees orphaned tool results.
- **`SessionMemoryAdvisor`** loads history, appends new messages, runs compaction when triggers fire; pass **session id** (and user id) through advisor context per request (see `SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY` in the Part 7 examples).
- **Multi‑agent**: **`branch`** labels (e.g. `orch`, `orch.weather`, `orch.news`) plus **`EventFilter.forBranch`** isolate sibling subagent traffic while sharing one session when desired.
- **Recall**: **SessionEventTools** exposes keyword **conversation_search** over the full log even after events drop out of the active window.
- **JDBC**: `spring-ai-starter-session-jdbc` with schema for **`AI_SESSION`** and **`AI_SESSION_EVENT`** (append‑only); enable schema init for Postgres in dev as documented. The Session module is **incubating** in spring‑ai‑community; the Part 7 article discusses roadmap alignment with future Spring AI releases — pin BOMs to supported combinations when building.
- Meteoris Insight: orchestrator + weather/news branches for clean traces; safe long tool chains for weather + news + eval. [Part 7](https://spring.io/blog/2026/04/15/spring-ai-session-management)

***

## Meteoris Insight — Application Architecture (Vision)

### Logical Layers

- **API/UI layer**
  - **Thymeleaf** views + Spring MVC for the primary **demo and manual test** surface (simple pages, minimal JS, easy to run locally).
  - **API-first REST** under `/api/**`: **`meteoris-insight/api/openapi.yaml`** is the **source of truth**; change the contract first, regenerate code, then align controllers and E2E. Validation and error responses should follow the contract (e.g. RFC 7807-style problem JSON where specified).
  - REST (and optionally WebSocket/SSE later) for **machine-facing** chat or automation; **`meteoris-insight-e2e`** consumes the same OpenAPI definition for generated clients or contract checks.
  - Maps to a single `MainOrchestratorAgent`.

- **Agent Orchestration layer**
  - `ChatClient` configs for: orchestrator, WeatherAgent, NewsAgent — separate system prompts, possibly different model providers/models ([Part 4](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents)).
  - Tools wired into the orchestrator: **Task tool** (subagent delegation), `TodoWriteTool`, `AskUserQuestionTool`, `SkillsTool` (+ optional `FileSystemTools` / `ShellTools` for skills), MCP‑backed `@Tool` beans, **SessionEventTools** if you enable recall search ([Parts 1–4](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills), [Part 7](https://spring.io/blog/2026/04/15/spring-ai-session-management)).
  - Advisors: **`SessionMemoryAdvisor`** (Part 7), **`AutoMemoryToolsAdvisor`** or manual AutoMemory wiring (Part 6), **`ToolCallAdvisor`** where required for tool/todo visibility ([Parts 3, 6, 7](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/)).

- **MCP/A2A layer**
  - MCP client beans (from Tools4AI or another Java MCP library) for Open‑Meteo and the chosen news MCP. [dev](https://dev.to/vishalmysore/building-ai-agents-with-a2a-and-mcp-protocol-a-hands-on-implementation-guide-4fbl)
  - Optionally, A2A wrapper around the orchestrator.

- **Persistence & Memory**
  - PostgreSQL 16 + pgvector for:
    - storing vector embeddings of news (cache or simple RAG over recent articles);
    - storing vectorized history/eval examples for quality analysis.
  - **Session API (JDBC)**: append‑only event log (`AI_SESSION`, `AI_SESSION_EVENT` when using `spring-ai-starter-session-jdbc` — see [Part 7](https://spring.io/blog/2026/04/15/spring-ai-session-management)).
  - Filesystem directory for **AutoMemoryTools** (Part 6), separate from Session storage.

- **Evaluation layer**
  - Separate Spring Modulith module that reads a small eval dataset, runs it through the orchestrator and computes quality metrics.

### API-first workflow

1. Edit **`meteoris-insight/api/openapi.yaml`** when adding or changing `/api/**` paths, operations, request/response schemas, or security schemes.
2. Run the Maven lifecycle so **OpenAPI Generator** produces server stubs (interfaces + models) under `target/generated-sources/openapi` (exact package layout to be chosen when scaffolding).
3. Implement REST controllers in **`app-api`** (or the `web` facade package) that **implement** the generated API interfaces and call application/orchestrator services.
4. Regenerate the **E2E client** from the same file in **`meteoris-insight-e2e`** so black-box tests never drift from the published contract.

Any change to live REST behaviour without updating **`openapi.yaml`** is a process violation (E2E and external consumers depend on it).

***

## Spring Modulith Module Breakdown

Proposed module structure:

- `app-core` (OPEN — shared kernel)
  - **`IdGenerator`** (ObjectId-layout string ids + `extractCreationInstant`); tiny cross-cutting utilities used by all modules.

- `app-api`
  - Thymeleaf templates (`templates/**`) and MVC controllers for user-facing flows.
  - **OpenAPI-first REST:** maintain **`api/openapi.yaml`** at the application module root; REST controllers implement **generated** API interfaces; share the same file with **`meteoris-insight-e2e`** for client generation.

- `app-agent-core`
  - `ChatClient` config for orchestrator and sub‑agents; advisors: `SessionMemoryAdvisor`, `AutoMemoryToolsAdvisor` (or explicit AutoMemory prompt + tools), `ToolCallAdvisor` as required ([Parts 3, 6, 7](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/)).
  - System prompts and wiring: `AskUserQuestionTool`, `TodoWriteTool`, **Task tool** / `TaskToolCallbackProvider`, `SkillsTool`.

- `app-weather-agent`
  - `weather-skill`;
  - MCP tool `WeatherMcpTool` (wrapper around MCP client for Open‑Meteo);
  - optional dedicated `ChatClient` with a lightweight model.

- `app-news-agent`
  - `news-skill`;
  - MCP tool `NewsMcpTool` (wrapper around MCP client for the chosen news MCP without API key);
  - PgVector integration for caching news.

- `app-memory`
  - Session API: `SessionService` + JDBC starter, compaction trigger/strategy, optional `SessionEventTools`; AutoMemoryTools root directory and advisor ([Part 6](https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools), [Part 7](https://spring.io/blog/2026/04/15/spring-ai-session-management)).

- `app-eval`
  - Eval example model, evaluation service, CLI/REST endpoint to run evaluations.

***

## Maven reactor and E2E subproject

The codebase is a **multi-module Maven** build (reactor POM + application module + E2E module):

| Maven module | Role |
|----------------|------|
| `pom.xml` (root) | Reactor POM only (`packaging` `pom`): lists `meteoris-insight` and `meteoris-insight-e2e`. Run builds from the project root (`mvn verify`). |

**Convention:** `mvn verify` from the project root builds the application module first, then executes E2E (in-process Spring tests and optional JAR smoke tests in `meteoris-insight-e2e`). Unit and integration tests for Modulith internals remain under `meteoris-insight/src/test/java`.

***

## MCP Integration: Open‑Meteo and News

- Use MCP as a “tool layer”: the MCP server exposes tool schemas (e.g. `get_weather(lat,lon)` or `search_news(query,language,from,to)`), and a Java client talks to it via HTTP/WebSocket. [dev](https://dev.to/vishalmysore/building-ai-agents-with-a2a-and-mcp-protocol-a-hands-on-implementation-guide-4fbl)
- In `app-weather-agent`:
  - define `WeatherMcpClient` Java interface + adapter to the MCP client;
  - on top of that create a Spring AI `@Tool` method `getWeatherForecast` that calls MCP and returns a normalized DTO.
  - Similar approach in `app-news-agent` for Google News RSS.
- The orchestrator only sees Spring AI tools `getWeatherForecast` and `findNews`, not raw MCP details — this keeps the design flexible.

***

## Memory: Session API, AutoMemory, PgVector

- **Short‑term (Part 7 — Session):** event‑sourced history with **turn‑safe compaction** and optional **branch isolation** for Weather vs News subagents; add **SessionEventTools** if the model should **search** pruned history by keyword.
- **Long‑term (Part 6 — AutoMemory):** curated Markdown memories + `MEMORY.md` index for preferences and stable facts (default city, units, topics). Session and AutoMemory are **complementary**, not interchangeable ([Part 6](https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools), [Part 7](https://spring.io/blog/2026/04/15/spring-ai-session-management)).

- **Semantic / vector (PgVector):**
  - Table `news_articles` with `embedding vector(1536)` (or the dimension your embedding model uses) and Spring AI `VectorStore` for similarity over ingested headlines/snippets.
  - Optionally embed final answers for offline quality clustering (“good” vs “bad” response styles).

***

## Evaluation: Metric and Small Dataset

**Reproducible procedure, dataset schema, and report format:** [EVALUATION-METHODOLOGY.md](EVALUATION-METHODOLOGY.md).

### What the assignment needs

- “At least one evaluation metric is defined, evaluated, and demonstrated” plus a small dataset for at least one criterion (quality/safety/performance).

### Suggested metric

Use a simple automatically checkable “Task Fulfilment & Information Completeness” metric:

- **For weather:**
  - For each eval question, store `city` and `expected_fields = [temperature, conditions, time_period]`.
  - Metric: share of questions whose answers:
    - explicitly mention the city;
    - contain temperature and condition values;
    - mention a time frame (now/today/tomorrow).

- **For news** (if you want a second metric):
  - For each eval query, store `min_headlines=3`.
  - Metric: share of answers that output ≥ N headlines/items with short descriptions and include source/time (from MCP response).

This gives a clear quantitative metric “percentage of complete answers” on a small set (~10–20 queries).

### How to implement the evaluation module

- In `app-eval` define a YAML/JSON like:

```yaml
- id: w1
  type: weather
  question: "What is the current weather in Brest?"
  required_fields: ["city", "temperature", "conditions", "time"]
- id: n1
  type: news
  question: "Latest AI news for today"
  min_headlines: 3
```

- `EvaluationRunner` service:
  - for each example, creates a new Session (without AutoMemory to avoid hints);
  - sends the question to `MainOrchestratorAgent`;
  - parses the answer with simple handcrafted checks (regex/heuristics) and decides whether conditions are met.
- Outputs a report:
  - overall metric per type (weather/news);
  - list of failed cases (for demo).

If you want a safety criterion, you can also check that answers do not invent sources (require each news item to reference a source actually present in the MCP response; a bit more complex, but doable).

***

## Rough Implementation Plan

A detailed **WBS** with work packages, dependencies, and exit criteria: **[IMPLEMENTATION-PLAN-WBS.md](IMPLEMENTATION-PLAN-WBS.md)**.

1. **Project skeleton**
    - Keep the **Maven reactor**: root `pom.xml` → `meteoris-insight` (application) + `meteoris-insight-e2e` (black-box tests). Evolve `meteoris-insight` into Spring Boot 4.0.5, Java 21, Modulith 2.0.5, Spring AI 2.0.0‑M4.
   - Add dependencies: `spring-ai-spring-boot-starter`, `spring-ai-agent-utils`, **Thymeleaf** (`spring-boot-starter-thymeleaf`), PgVector + Spring Data JDBC/JPA, MCP/A2A client libraries (Tools4AI). [dev](https://dev.to/vishalmysore/building-cross-protocol-ai-agents-with-spring-boot-a2a-and-mcp-server-guide-2d71)
   - Add **OpenAPI Generator** (or equivalent) so **`api/openapi.yaml`** drives generated server interfaces and models; wire **`meteoris-insight-e2e`** to generate its client from the same spec (**API-first** workflow).
   - Scaffold **Thymeleaf** pages for chat / weather / news smoke flows early (`templates/**`, MVC controllers) so **AskUserQuestionTool** and orchestration can be exercised without a SPA.

2. **PostgreSQL + pgvector**
   - Run Postgres 16, enable pgvector extension.
   - Create tables for Session API (if using JDBC repo) and `news_articles` with vector column; primary keys as **24-char hex** strings from **`IdGenerator`**.
   - Implement **`app-core.IdGenerator`** + tests (format, uniqueness, **`extractCreationInstant`** round-trip on leading timestamp bytes).

3. **ChatClient and advisors config**
   - Orchestrator `ChatClient`: `SessionMemoryAdvisor` + compaction, `AutoMemoryToolsAdvisor` (or manual AutoMemory), `ToolCallAdvisor` as required; tools: `AskUserQuestionTool`, `TodoWriteTool`, `SkillsTool`, **Task tool** provider, MCP `@Tool` beans ([Parts 1–4, 6–7](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills)).
   - Optionally add separate `ChatClient` builders for Weather/News subagents (e.g. smaller/cheaper models) per [Part 4](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents).

4. **Skills and system prompts**
   - Create `weather-skill` and `news-skill` with `SKILL.md` (YAML front matter, instructions, optional references/scripts). [Part 1](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills)

5. **MCP clients and Spring AI tools**
   - Spin up/connect MCP servers for Open‑Meteo and your chosen news provider without API keys.
   - Implement Java wrappers for MCP clients and matching `@Tool` methods `getWeatherForecast` and `findNews`.

6. **Subagent orchestration**
   - Configure the **Task tool** (e.g. `TaskToolCallbackProvider` + subagent definitions): register **Weather** and **News** subagents with clear **description** fields so the orchestrator delegates reliably ([Part 4](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents)).

7. **AskUserQuestionTool + TodoWriteTool**
   - Implement `QuestionHandler` and connect it to REST/Web UI ([Part 2](https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool)).
   - Align **TodoWriteTool** with **ToolCallAdvisor** and conversation retention as in [Part 3](https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/); system prompt may instruct use for multi‑step tasks.

8. **Session API and AutoMemoryTools**
   - Import **`spring-ai-session`** BOM/starter (JDBC for Postgres), set **compaction trigger + strategy**, initialise schema; wire **`SessionMemoryAdvisor`** and per‑request session id ([Part 7](https://spring.io/blog/2026/04/15/spring-ai-session-management)).
   - Configure **AutoMemoryTools** root path and **`AutoMemoryToolsAdvisor`** (or manual prompt + tools) ([Part 6](https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools)).

9. **VectorStore/PgVector integration**
   - Configure Spring AI VectorStore (PgVector) and hook it into `NewsAgent` for news caching and semantic search.

10. **Evaluation module**
    - Add `app-eval` with the dataset and metrics; implement `@CommandLineRunner` and/or the **evaluation trigger** operation defined in **`api/openapi.yaml`** (illustrative id: `POST /api/v1/evaluation/run`—exact path must match the published contract).
    - Generate a report (JSON/log/table) and use it in your presentation.

11. **Tests and demo scenarios**
    - **In `meteoris-insight`:** integration tests (weather, news, mixed, AskUserQuestionTool) with Testcontainers / stub AI as appropriate.
    - **In `meteoris-insight-e2e`:** black-box scenarios against the running service using the **generated OpenAPI client** (or REST Assured against `/api/**`) so tests always track **`api/openapi.yaml`**.
    - Prepare a few screenshots/logs for the final demo.

