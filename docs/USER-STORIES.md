# Meteoris Insight — User stories

Agile-style **user stories** for Meteoris Insight, aligned with [PRD.md](PRD.md) (**FR-xx** / **NFR-xx**), [VISION.md](VISION.md), [IMPLEMENTATION-PLAN-WBS.md](IMPLEMENTATION-PLAN-WBS.md) (delivery sequencing), [USE-CASES.md](USE-CASES.md), [WORK-SCENARIOS.md](WORK-SCENARIOS.md), [FORMS-AND-FLOWS.md](FORMS-AND-FLOWS.md), [WIREFRAMES.md](WIREFRAMES.md), [EVALUATION-METHODOLOGY.md](EVALUATION-METHODOLOGY.md), and [ARCHITECTURE.md](ARCHITECTURE.md).

**Primary personas**

- **End user** — uses the **browser demo UI** for chat-style weather and news (English UI unless otherwise specified).
- **Integrator** — calls **`/api/**`** using a client built from the **published HTTP contract** (see [ARCHITECTURE.md](ARCHITECTURE.md)).
- **Operator** — developer or course evaluator running the app locally, maintaining the **API contract**, skills, and prompts; runs **CI** and **end-to-end checks** with stubbed AI (see the [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md)).

**Story ID prefix:** `US-xx` — traceability to use case IDs (`U.xx`, `A.xx`, `O.xx`, …) in [USE-CASES.md](USE-CASES.md).

---

## Summary backlog

| ID | Epic | Short title |
|----|------|-------------|
| US-01 | Shell | Landing / home |
| US-02 | Shell | Navigate chat, weather, and news areas |
| US-03 | Chat UI | First-time weather with clarification |
| US-04 | Chat UI | First-time news with clarification |
| US-05 | Chat UI | Explicit weather query |
| US-06 | Chat UI | Explicit news query |
| US-07 | Chat UI | Mixed weather and news in one turn |
| US-08 | Chat UI | Compare two locations |
| US-09 | Chat UI | Follow-up in the same session |
| US-10 | Chat UI | Persist units and topic preferences |
| US-11 | Chat UI | Visible todo progress (optional) |
| US-12 | Chat UI | Skill-guided briefing style |
| US-13 | Chat UI | Recall earlier turns in session |
| US-14 | Chat UI | Graceful MCP errors |
| US-15 | Chat UI | Start a new chat session |
| US-16 | REST | HTTP API contract-first workflow |
| US-17 | REST | Send chat message (POST) |
| US-18 | REST | Stream assistant reply (if in contract) |
| US-19 | REST | Complete AskUser flow asynchronously |
| US-20 | REST | Trigger evaluation run |
| US-21 | REST | Validation, versioning, idempotency, Problem+JSON |
| US-22 | Agents | Direct orchestrator reply without tools |
| US-23 | Agents | Delegate weather to subagent + MCP |
| US-24 | Agents | Delegate news to subagent + MCP |
| US-25 | Agents | Multi-step compound requests |
| US-26 | Agents | Guardrails and safe refusal |
| US-27 | Agents | Recover from wrong branch or tool failure |
| US-28 | Agents | Subagents do not nest Task |
| US-29 | Memory | Session history and compaction |
| US-30 | Memory | Branch isolation for weather vs news |
| US-31 | Memory | AutoMemory for durable preferences |
| US-32 | Memory | Forget or purge stored memory (policy) |
| US-33 | Memory | Semantic recall over news |
| US-34 | Memory | Isolated session for evaluation runs |
| US-35 | Eval | Single-case weather assertion |
| US-36 | Eval | Single-case news assertion |
| US-37 | Eval | Full suite and aggregate pass rate |
| US-38 | Eval | Regression gate in CI |
| US-39 | Eval | Optional safety check on cited URLs |
| US-40 | Eval | Export failed cases for review |
| US-41 | Engineering | Local run with Postgres |
| US-42 | Engineering | Unit tests with stub AI |
| US-43 | Engineering | Full CI build and black-box E2E |
| US-44 | Engineering | Strict documentation build |
| US-45 | Engineering | HTTP contract drift detection in CI |
| US-46 | Engineering | Versioned database migrations |
| US-47 | Identity | ObjectId-layout string ids |
| US-48 | Persistence | Relational access without ORM (NFR-3) |
| US-49 | A2A (optional) | Publish AgentCard |
| US-50 | A2A (optional) | Serve remote A2A clients |
| US-51 | A2A (optional) | Delegate to external A2A agent |
| US-52 | Reliability | MCP rate limits, empty results, bad coordinates |
| US-53 | Reliability | LLM provider outage surfaces as 503 |
| US-54 | Security / NFR | Large input, concurrency, secrets handling |
| US-55 | Ops | Health checks and structured logs |

**Out of scope (backlog / future):** multi-tenant isolation; paid news APIs with keys; SPA replacement for the server-rendered demo UI — see [PRD.md](PRD.md) non-goals and [ARCHITECTURE.md](ARCHITECTURE.md) extension points.

---

## Epic: Application shell and navigation

### US-01 — Landing / home

**As an** end user,  
**I want** a clear home or landing page when I open the app root URL,  
**So that** I can see where to start a weather or news conversation.

**Acceptance criteria**

- Root URL renders a landing page with entry points to chat / weather / news areas as designed in [WIREFRAMES.md](WIREFRAMES.md) and [FORMS-AND-FLOWS.md](FORMS-AND-FLOWS.md).
- Page loads without requiring prior session state.

**Maps to:** U.01, P.01 (shell), P.02 (shell)

---

### US-02 — Global navigation

**As an** end user,  
**I want** consistent navigation between main areas of the demo UI,  
**So that** I am not stuck on a leaf page after one interaction.

**Acceptance criteria**

- Shared layout provides navigation links consistent with the implemented routes.
- Links remain usable after form posts that re-render the same page.

**Maps to:** U.01

---

## Epic: Conversational UI

### US-03 — First-time weather with clarification

**As an** end user,  
**I want** the assistant to ask which city or coordinates to use when my weather question is ambiguous,  
**So that** forecasts are not silently wrong.

**Acceptance criteria**

- **AskUserQuestionTool** (or equivalent **QuestionHandler**) can present structured choices before calling weather tools.
- After I answer, the orchestrator delegates to the weather path; the reply reflects **weather-skill** guidance (units, conditions).

**Maps to:** U.02, P.01, O.02

---

### US-04 — First-time news with clarification

**As an** end user,  
**I want** clarifying questions for topic, language, or time window when I ask for “latest news” vaguely,  
**So that** the digest matches my intent.

**Acceptance criteria**

- Model or tool flow asks for missing dimensions before heavy news retrieval.
- Final answer follows **news-skill** (recency, attribution where MCP provides sources).

**Maps to:** U.03, P.02, O.03

---

### US-05 — Explicit weather query

**As an** end user,  
**I want** to ask for weather in a named place in one message,  
**So that** I get a fast answer without extra questions.

**Acceptance criteria**

- When the city or coordinates are clear in natural language, the orchestrator may delegate via **Task** to the weather subagent and Open‑Meteo MCP-backed tools.
- Response is readable and includes relevant conditions.

**Maps to:** U.04, O.02

---

### US-06 — Explicit news query

**As an** end user,  
**I want** to ask for headlines on a clear topic,  
**So that** I see current items with sources or times when the MCP returns them.

**Acceptance criteria**

- News path uses the keyless news MCP integration (assignment constraint).
- Headlines do not invent URLs not returned by tools when safety checks are enabled (see US-39).

**Maps to:** U.05, O.03, P.02

---

### US-07 — Mixed weather and news

**As an** end user,  
**I want** to request weather for one place and news on a topic in a single message,  
**So that** I get one merged reply without switching pages.

**Acceptance criteria**

- **TodoWriteTool** may plan steps for multi-part work; **Task** delegates weather and news as appropriate.
- Final synthesis addresses both parts.

**Maps to:** U.06, O.04

---

### US-08 — Compare two locations

**As an** end user,  
**I want** to compare weather (and optionally news) across two locations,  
**So that** I can decide between them in one conversation.

**Acceptance criteria**

- Multiple subagent or tool invocations are allowed; optional todo UI shows progress (see US-11).
- Answer contrasts the locations clearly.

**Maps to:** U.07, O.04

---

### US-09 — Follow-up in the same session

**As an** end user,  
**I want** follow-ups like “same for tomorrow” to reuse my prior city or topic without re-entering everything,  
**So that** multi-turn chat feels natural.

**Acceptance criteria**

- **Session** id is carried on cookie or header per architecture; prior turns inform the model within compaction policy.
- If a location was already bound, the system does not re-ask unless I change scope.

**Maps to:** U.08, M.01, M.02

---

### US-10 — Persist units and topic preferences

**As an** end user,  
**I want** stable preferences such as Celsius or preferred news categories to persist across sessions,  
**So that** I am not asked the same preference every time.

**Acceptance criteria**

- **AutoMemory** (or equivalent) can store curated preference facts under a configured root with index file per Spring AI Part 6 pattern.
- New sessions still apply stable prefs where appropriate (session boundary vs memory boundary documented in ops notes).

**Maps to:** U.09, M.04, M.06, P.01, P.02

---

### US-11 — Visible todo progress (optional)

**As an** end user,  
**I want** to see todo list state changes during long requests,  
**So that** I trust the system is working through multiple steps.

**Acceptance criteria**

- When enabled, UI subscribes to **TodoWriteTool** events or polls state exposed by the web layer.
- Feature may be optional for minimal demos but is documented.

**Maps to:** U.10

---

### US-12 — Skill-guided briefing style

**As an** end user,  
**I want** to ask for a shorter or different style of answer,  
**So that** demos show **SkillsTool** progressive disclosure.

**Acceptance criteria**

- **SkillsTool** registers **weather-skill** and **news-skill** catalogues at startup; model can load full skill markdown on demand.
- A request like “short briefing” results in visibly tighter formatting aligned with the skill text.

**Maps to:** U.11

---

### US-13 — Recall earlier turns

**As an** end user,  
**I want** to ask what I said a few turns ago,  
**So that** session recall works beyond the active context window when **SessionEventTools** are enabled.

**Acceptance criteria**

- `conversation_search` (or equivalent) can retrieve prior user or assistant content from stored session events.
- Behaviour respects branch filters so weather recall does not leak unrelated news branches when configured.

**Maps to:** U.12, M.03

---

### US-14 — Graceful MCP errors

**As an** end user,  
**I want** a friendly message when the weather MCP times out or fails,  
**So that** I never see a raw stack trace in the browser.

**Acceptance criteria**

- Errors from MCP layers are mapped to user-visible text; optional retry hint.
- Logs retain correlation data for operators without leaking secrets (see US-54).

**Maps to:** U.13, O.07, N.01

---

### US-15 — Start a new chat session

**As an** end user,  
**I want** to explicitly start a new chat,  
**So that** short-term context resets while durable preferences may remain.

**Acceptance criteria**

- “New chat” (or equivalent) issues a new **Session** id per product policy.
- **AutoMemory** preferences are not wiped unless the user requests forget behaviour (see US-32).

**Maps to:** U.14, M.04

---

## Epic: REST API (HTTP contract)

### US-16 — HTTP API contract-first workflow

**As an** operator,  
**I want** **`/api/**`** behaviour defined in the **published machine-readable contract** before server code diverges,  
**So that** automated tests and external clients stay aligned with the server.

**Acceptance criteria**

- Contract updates precede incompatible behaviour changes; build regenerates server stubs and consumer clients per [ARCHITECTURE.md](ARCHITECTURE.md).
- End-to-end tests validate the same contract (see engineering docs).

**Maps to:** A.01, D.06

---

### US-17 — Send chat message over REST

**As an** integrator,  
**I want** to `POST` a user message per the published contract and receive the assistant reply,  
**So that** I can script conversations without the browser.

**Acceptance criteria**

- Request and response bodies match the published contract; invalid payloads are rejected with stable error shapes (**FR-8**, **A.06**).
- Server implementation follows the contract-first layering described in [ARCHITECTURE.md](ARCHITECTURE.md).

**Maps to:** A.02

---

### US-18 — Stream assistant reply (if in contract)

**As an** integrator,  
**I want** optional streaming (for example SSE or WebSocket) when the published API contract defines it,  
**So that** long answers appear progressively.

**Acceptance criteria**

- If streaming operations exist in the contract, tokens or chunks stream until completion with clean termination.
- If not in v1 contract, story is deferred without breaking US-17.

**Maps to:** A.03

---

### US-19 — AskUser asynchronous completion over REST

**As an** integrator,  
**I want** a documented async pattern when the model must ask structured questions mid-flight,  
**So that** I can resume the same orchestrated conversation after I submit answers.

**Acceptance criteria**

- Contract describes question ticket and resume operation (for example `202` + poll or second POST) per architecture.
- **QuestionHandler** integration works for HTML and REST without deadlocking the client.

**Maps to:** A.04

---

### US-20 — Trigger evaluation over REST

**As an** operator or CI job,  
**I want** to trigger a batch evaluation via `/api/**` as specified,  
**So that** metrics are reproducible outside the UI.

**Acceptance criteria**

- Response returns report JSON and, if async, a job id per contract.
- Stub AI profile can satisfy the call in CI without live LLM keys.

**Maps to:** A.05, E.03, P.03

---

### US-21 — Validation, versioning, idempotency, errors

**As an** integrator,  
**I want** predictable HTTP status codes, Problem+JSON (or contract errors), optional API version negotiation, and safe retries,  
**So that** my client handles failures and upgrades cleanly.

**Acceptance criteria**

- Invalid payloads return stable machine-readable error types (A.06).
- Version policy documented with the API contract or gateway config (A.07).
- Idempotency keys or message ids prevent duplicate side effects when specified (A.08).

**Maps to:** A.06, A.07, A.08

---

## Epic: Orchestrator and subagents

### US-22 — Direct orchestrator reply

**As an** end user,  
**I want** simple greetings or out-of-scope requests answered without calling external tools,  
**So that** latency and cost stay low for trivial turns.

**Acceptance criteria**

- Orchestrator can answer without MCP for clearly non-weather/non-news intents.
- Polite decline or redirect when the product does not support the request.

**Maps to:** O.01

---

### US-23 — Weather delegation path

**As an** end user,  
**I want** weather questions routed to a dedicated weather context with Open‑Meteo tools,  
**So that** prompts and tool allow-lists stay focused.

**Acceptance criteria**

- **Task** tool delegates to a weather subagent configuration; subagent uses MCP-backed `@Tool` methods, not raw MCP in prompts.
- Subagent does **not** register a nested **Task** tool (see US-28).

**Maps to:** O.02, O.08, P.01

---

### US-24 — News delegation path

**As an** end user,  
**I want** news questions routed to a news subagent with the keyless news MCP,  
**So that** retrieval stays isolated from weather logic.

**Acceptance criteria**

- News subagent may combine MCP results with optional **semantic recall** helpers per [ARCHITECTURE.md](ARCHITECTURE.md).
- Same nested Task prohibition as US-23.

**Maps to:** O.03, O.08, P.02

---

### US-25 — Multi-step compound requests

**As an** end user,  
**I want** independent facts gathered in a controlled order when the runtime requires sequential tool use,  
**So that** the model does not skip steps.

**Acceptance criteria**

- **TodoWriteTool** guidance respected for about three or more distinct steps where applicable.
- Parallel **Task** usage only if explicitly allowed by configuration and safe for the use case.

**Maps to:** O.04

---

### US-26 — Guardrails and refusal

**As an** end user (and stakeholder),  
**I want** harmful or unsupported requests refused without calling external news or weather APIs,  
**So that** abuse surface stays small.

**Acceptance criteria**

- Orchestrator declines or redirects without MCP for blocked categories defined in policy.
- Refusal messages are safe to show in UI and logs (no prompt injection echo).

**Maps to:** O.05

---

### US-27 — Wrong branch or tool failure recovery

**As an** end user,  
**I want** ambiguous queries clarified and tool failures explained in natural language,  
**So that** I can correct my request or retry.

**Acceptance criteria**

- Wrong subagent choice can be corrected via AskUser or a clarification turn (O.06).
- MCP 4xx/5xx bubbles to a user-facing summary without raw vendor payloads (O.07).

**Maps to:** O.06, O.07

---

### US-28 — Subagent configuration safety

**As an** operator,  
**I want** weather and news subagents configured **without** a nested **Task** tool,  
**So that** the stock Spring AI pattern avoids recursive agent trees.

**Acceptance criteria**

- Automated test or Modulith smoke configuration asserts subagent tool allow-lists exclude nested Task.
- Misconfiguration is caught at startup or in CI where feasible.

**Maps to:** O.08

---

## Epic: Memory (Session, AutoMemory, vector)

### US-29 — Session history and compaction

**As an** end user,  
**I want** long conversations to remain coherent after compaction triggers,  
**So that** token limits do not orphan tool results mid-turn.

**Acceptance criteria**

- **SessionMemoryAdvisor** (or equivalent) persists append-only events; compaction snaps to **turn boundaries** per Part 7 guidance.
- Short conversations retain verbatim history under threshold (M.01); longer runs invoke compaction policy (M.02).

**Maps to:** M.01, M.02

---

### US-30 — Branch isolation

**As an** operator,  
**I want** `orch.weather` and `orch.news` branches isolated in session filters,  
**So that** advisors do not mix unrelated tool traffic.

**Acceptance criteria**

- Session events carry branch labels; **EventFilter.forBranch** (or equivalent) used in advisor wiring.
- Documented mapping matches architecture diagrams.

**Maps to:** M.03

---

### US-31 — AutoMemory for durable preferences

**As an** end user,  
**I want** curated long-term facts stored outside the rolling chat window,  
**So that** preferences survive compaction and new sessions.

**Acceptance criteria**

- **AutoMemoryTools** root configured; **MEMORY.md** index maintained per Part 6.
- **AutoMemoryToolsAdvisor** (or manual wiring) registers tools and system guidance.

**Maps to:** M.04, M.06

---

### US-32 — Forget or purge memory

**As an** end user,  
**I want** to request deletion of stored memory or session purge when policy allows,  
**So that** demos respect privacy expectations.

**Acceptance criteria**

- Behaviour documented (manual delete vs tool-driven); aligns with assignment policy TBD in use cases.
- No silent retention after explicit “forget me” if implemented.

**Maps to:** M.05

---

### US-33 — Semantic news recall

**As an** end user,  
**I want** to refer to “similar articles to what I saw yesterday” using embeddings,  
**So that** **semantic recall** augments keyword history.

**Acceptance criteria**

- Embeddings stored for news snippets or headlines per [ARCHITECTURE.md](ARCHITECTURE.md) (relational DB with vector extension where used).
- Query path documented; falls back gracefully if vectors missing.

**Maps to:** M.07

---

### US-34 — Evaluation session isolation

**As an** operator,  
**I want** evaluation runs to use fresh sessions and disabled AutoMemory when configured,  
**So that** metrics are not biased by hidden preferences.

**Acceptance criteria**

- Eval runner can start with new session id and memory flags per eval module design.
- Documented in eval README or architecture.

**Maps to:** M.08, E.03

---

## Epic: Evaluation module

### US-35 — Single-case weather evaluation

**As an** operator,  
**I want** a YAML or JSON case that asserts required substrings or structured fields in a weather answer,  
**So that** I can demonstrate at least one quantitative or qualitative metric.

**Acceptance criteria**

- Runner invokes orchestrator (stub or live profile) and records pass or fail with reason.
- Example dataset committed under version control.

**Maps to:** E.01, P.03

---

### US-36 — Single-case news evaluation

**As an** operator,  
**I want** assertions on minimum headlines and optional source or time checks,  
**So that** news quality is measurable.

**Acceptance criteria**

- Cases tolerate MCP variability within defined bounds (for example minimum headline count).
- Clear skip or fail semantics when MCP returns empty (links to US-52).

**Maps to:** E.02, P.03

---

### US-37 — Full evaluation suite

**As an** operator,  
**I want** to run the full dataset and see aggregate pass rate,  
**So that** I can report overall behaviour for the assignment.

**Acceptance criteria**

- Batch execution produces JSON or HTML summary with counts and pass rate.
- Runnable via REST (US-20) and/or CLI documented alongside.

**Maps to:** E.03, P.03

---

### US-38 — Regression in CI

**As an** operator,  
**I want** the same dataset re-run after prompt or skill edits to catch regressions,  
**So that** CI fails when quality drops beyond a threshold.

**Acceptance criteria**

- The default **CI** pipeline or a dedicated job can execute eval with stub AI.
- Baseline comparison or exit codes documented.

**Maps to:** E.04, D.03

---

### US-39 — Optional safety check on URLs

**As an** stakeholder,  
**I want** optional checks that headlines only cite URLs returned by MCP,  
**So that** hallucinated links are caught in eval.

**Acceptance criteria**

- Flag-gated assertion in eval runner; does not break default suite when MCP omits URLs.
- Documented as optional (E.05).

**Maps to:** E.05

---

### US-40 — Export failed evaluation cases

**As an** operator,  
**I want** a JSON export of failing cases,  
**So that** I can attach evidence to demo slides or issues.

**Acceptance criteria**

- Export includes case id, input, expected hints, actual output snippet, failure reason.
- File path or API documented.

**Maps to:** E.06

---

## Epic: Engineering, build, and persistence

### US-41 — Local run with Postgres

**As an** operator,  
**I want** to start the app against a **documented local Postgres** (Compose or equivalent) with **session and vector-backed features** enabled where required,  
**So that** behaviour matches what CI and demos expect.

**Acceptance criteria**

- The [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md) documents ports, env vars, and profile names.
- Documented startup succeeds against an empty database with **schema migrations** applied automatically.

**Maps to:** D.01

---

### US-42 — Unit tests with stub AI

**As an** operator,  
**I want** fast tests that do not call live LLM providers by default,  
**So that** **unit tests** are reliable in CI without secrets.

**Acceptance criteria**

- Stubbed model client or test doubles used under the **`test`** profile per [ARCHITECTURE.md](ARCHITECTURE.md).
- At least one test covers **IdGenerator** rules (US-47).

**Maps to:** D.02

---

### US-43 — Full CI build and E2E

**As an** operator,  
**I want** the **parent reactor** build to compile the app then run **black-box** end-to-end tests,  
**So that** integration regressions are caught automatically.

**Acceptance criteria**

- E2E module uses a client generated from the **same HTTP contract** as the server, against a running instance or harness where configured (see [ARCHITECTURE.md](ARCHITECTURE.md)).
- No live LLM in default E2E path.

**Maps to:** D.03, D.04, A.01

---

### US-44 — Strict documentation build

**As an** operator,  
**I want** the **documentation site** to build in **strict** mode in CI (broken links fail the build),  
**So that** broken internal links are not merged.

**Acceptance criteria**

- Docs pipeline documented in the [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md) or a contributing guide.
- New docs pages are registered in the **site navigation** when published (see docs tooling in the repo).

**Maps to:** D.05

---

### US-45 — HTTP contract drift detection

**As an** operator,  
**I want** CI to fail when **generated contract stubs** drift from the **authoritative API description**,  
**So that** implementors cannot skip contract updates.

**Acceptance criteria**

- Policy documented: either commit generated sources or fail `git diff` after regenerate (pick one and enforce).
- Enforcement details live in [ARCHITECTURE.md](ARCHITECTURE.md) / build configuration.

**Maps to:** D.06

---

### US-46 — Versioned database migrations

**As an** operator,  
**I want** **append-only** SQL migrations for session and application tables,  
**So that** schema history is auditable.

**Acceptance criteria**

- New migrations live under the path documented in [ARCHITECTURE.md](ARCHITECTURE.md).
- Never rewrite applied migration files in place.

**Maps to:** D.07, I.04

---

### US-47 — ObjectId-layout string identifiers

**As an** operator,  
**I want** a shared **IdGenerator** that generates 24-character lowercase hex ids and can decode creation time,  
**So that** API and persistence layers stay consistent with project conventions.

**Acceptance criteria**

- `generateId()`, `isValidId(String)`, `extractCreationInstant(String)` behave per [VISION.md](VISION.md) (BSON ObjectId byte layout).
- Unit tests cover valid, invalid, and time decode examples.

**Maps to:** I.01, I.02, I.03

---

### US-48 — Relational persistence without ORM (NFR-3)

**As an** operator,  
**I want** first-party tables accessed via **explicit SQL** and named parameters (no ORM for those tables),  
**So that** persistence matches **NFR-3** and stays easy to audit.

**Acceptance criteria**

- No JPA entities or Spring Data JPA repositories for first-party tables (see [ARCHITECTURE.md](ARCHITECTURE.md)).
- Primary keys use fixed-length string ids where applicable, matching the **published id format** when ids are exposed on the API.

**Maps to:** I.04

---

## Epic: Optional A2A

### US-49 — Publish AgentCard

**As an** external agent developer,  
**I want** `GET /.well-known/agent-card.json` to describe capabilities when A2A is enabled,  
**So that** peers can discover Meteoris Insight.

**Acceptance criteria**

- Agent card lists exposed skills and message endpoint per Spring AI A2A server autoconfigure docs.
- Feature flag disables endpoint in environments that should not expose agent surfaces.

**Maps to:** AA.01

---

### US-50 — Serve A2A JSON-RPC clients

**As an** external agent,  
**I want** to send standards-compliant messages to Meteoris Insight,  
**So that** cross-agent demos work end-to-end.

**Acceptance criteria**

- Integration test or manual script documented for happy path message round-trip.
- Errors map to spec-appropriate responses.

**Maps to:** AA.02

---

### US-51 — Delegate to remote A2A agent (optional)

**As an** operator,  
**I want** an optional host tool that calls another A2A agent via Java client,  
**So that** Meteoris Insight can participate in multi-agent topologies.

**Acceptance criteria**

- Clearly marked optional; build passes when dependency disabled.
- Configuration documented (endpoint, timeouts).

**Maps to:** AA.03

---

## Epic: Reliability, security, and operations

### US-52 — MCP edge cases

**As an** end user,  
**I want** honest handling of rate limits, empty result sets, and invalid coordinates,  
**So that** the assistant never spins forever or fabricates articles.

**Acceptance criteria**

- Backoff or user-visible message on news MCP rate limit (N.01); no infinite retry loop.
- User-friendly text for invalid coordinates (N.02) and “no articles found” when empty (N.03).

**Maps to:** N.01, N.02, N.03

---

### US-53 — LLM provider outage

**As an** integrator,  
**I want** HTTP 503 (or contract equivalent) when the LLM provider is unavailable,  
**So that** clients can retry with backoff.

**Acceptance criteria**

- UI shows an error banner for the same condition (N.04).
- Logs include correlation id without body secrets.

**Maps to:** N.04

---

### US-54 — Abuse limits and secret handling

**As a** stakeholder,  
**I want** limits on oversized prompts, defined concurrency per session, and rejection of secrets in requests,  
**So that** demos remain safe and stable.

**Acceptance criteria**

- Very large pastes truncated or rejected with policy message (N.05).
- Concurrency policy documented (N.06): last-write-wins or serialisation per session.
- Secrets in input rejected at validation or gateway; never logged (N.07).

**Maps to:** N.05, N.06, N.07

---

### US-55 — Health checks and structured logs

**As an** operator,  
**I want** **HTTP health endpoints** (liveness/readiness) and **structured logs** for chat, tools, MCP, and eval,  
**So that** I can diagnose failures quickly.

**Acceptance criteria**

- Health checks appropriate for container deployment (see [ARCHITECTURE.md](ARCHITECTURE.md)).
- Log fields include request id and session id where available.

**Maps to:** Engineering NFR in [ARCHITECTURE.md](ARCHITECTURE.md) quality attributes

---

## Future backlog (not v1)

| ID | Story (summary) | Notes |
|----|-----------------|-------|
| US-F01 | As a tenant admin, I want per-tenant MCP credentials and quotas | Multi-tenant — non-goal for course demo |
| US-F02 | As an end user, I want voice input and read-aloud | Multimodal — future |
| US-F03 | As an evaluator, I want LLM-as-judge and faithfulness scores | Advanced eval — optional extension |

---

## Traceability matrix

| US | Use cases (IDs) |
|----|-----------------|
| US-01 | U.01 |
| US-02 | U.01 |
| US-03 | U.02, P.01, O.02 |
| US-04 | U.03, P.02, O.03 |
| US-05 | U.04, O.02 |
| US-06 | U.05, O.03, P.02 |
| US-07 | U.06, O.04 |
| US-08 | U.07, O.04 |
| US-09 | U.08, M.01, M.02 |
| US-10 | U.09, M.04, M.06, P.01, P.02 |
| US-11 | U.10 |
| US-12 | U.11 |
| US-13 | U.12, M.03 |
| US-14 | U.13, O.07, N.01 |
| US-15 | U.14, M.04 |
| US-16 | A.01, D.06 |
| US-17 | A.02 |
| US-18 | A.03 |
| US-19 | A.04 |
| US-20 | A.05, E.03, P.03 |
| US-21 | A.06, A.07, A.08 |
| US-22 | O.01 |
| US-23 | O.02, O.08, P.01 |
| US-24 | O.03, O.08, P.02 |
| US-25 | O.04 |
| US-26 | O.05 |
| US-27 | O.06, O.07 |
| US-28 | O.08 |
| US-29 | M.01, M.02 |
| US-30 | M.03 |
| US-31 | M.04, M.06 |
| US-32 | M.05 |
| US-33 | M.07 |
| US-34 | M.08, E.03 |
| US-35 | E.01, P.03 |
| US-36 | E.02, P.03 |
| US-37 | E.03, P.03 |
| US-38 | E.04, D.03 |
| US-39 | E.05 |
| US-40 | E.06 |
| US-41 | D.01 |
| US-42 | D.02 |
| US-43 | D.03, D.04, A.01 |
| US-44 | D.05 |
| US-45 | D.06 |
| US-46 | D.07, I.04 |
| US-47 | I.01, I.02, I.03 |
| US-48 | I.04 |
| US-49 | AA.01 |
| US-50 | AA.02 |
| US-51 | AA.03 |
| US-52 | N.01, N.02, N.03 |
| US-53 | N.04 |
| US-54 | N.05, N.06, N.07 |
| US-55 | Architecture quality / ops |

---

**Document version:** 1.0. Update when [USE-CASES.md](USE-CASES.md) or [VISION.md](VISION.md) change.
