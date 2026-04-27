# Meteoris Insight — Work scenarios

This document lists **possible operational scenarios** for Meteoris Insight: user journeys, agent/tool behaviour, APIs, memory, evaluation, testing, and optional integrations. **Product requirements** (FR/NFR) are in [PRD.md](PRD.md). A **single catalogue of every use case ID** (master register plus product and identity cases) lives in [USE-CASES.md](USE-CASES.md). **User stories** (`US-xx`) with acceptance criteria are in [USER-STORIES.md](USER-STORIES.md). **Forms, page map, and workflow narratives** are in [FORMS-AND-FLOWS.md](FORMS-AND-FLOWS.md). **Text wireframes** per screen are in [WIREFRAMES.md](WIREFRAMES.md). This file complements [Project Vision](VISION.md); the **authoritative HTTP API description** is summarized in [ARCHITECTURE.md](ARCHITECTURE.md).

**Conventions**

- **Orchestrator** — main conversational agent exposed via the demo UI and **`/api/**`**.
- **Weather / News** — subagents or tools backed by MCP (Open‑Meteo + keyless news MCP).

---

## 1. Actors

| Actor | Description |
|--------|-------------|
| **End user** | Uses the **browser demo UI** and/or public **`/api/**`** clients. |
| **Integrator / script** | Calls **`/api/**`** using a client aligned with the **published HTTP contract**. |
| **Developer** | Runs app locally; maintains the **API contract**, skills, and prompts; uses stub AI profiles. |
| **CI pipeline** | Full reactor **verify** with stub stack; no live LLM unless explicitly opted in (commands in the [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md)). |
| **External A2A agent** (optional) | Discovers `/.well-known/agent-card.json` and sends messages if A2A is enabled. |

---

## 2. End-user scenarios (browser demo UI)

| ID | Scenario | Preconditions | Main flow | Expected outcome |
|----|-----------|-----------------|-----------|------------------|
| U.01 | Open home / landing | App running | User opens root URL | Navigation to chat / weather / news areas (as designed). |
| U.02 | First-time chat — weather | None | User asks for weather without naming a city | **AskUserQuestionTool** presents city (or coordinates) choice; user answers; orchestrator calls weather path; answer shows units and conditions per **weather-skill**. |
| U.03 | First-time chat — news | None | User asks for “latest news” without topic or window | Clarifying questions (topic, language, time range); then news MCP + **news-skill** digest. |
| U.04 | Explicit weather query | City clear in text | User asks “Weather in Brest now” | Task tool may delegate to **Weather** subagent; MCP Open‑Meteo; structured answer. |
| U.05 | Explicit news query | Topic clear | User asks “Top AI headlines today” | **News** path; headlines with sources/time where MCP provides them. |
| U.06 | Mixed question | None | User asks for weather in city A **and** news on topic B in one message | **TodoWriteTool** may plan steps; **Task** delegates weather + news; merged reply. |
| U.07 | Compare two cities | None | User compares weather (and optionally news) for two locations | Multiple subagent calls; todo list may show progress; final synthesis. |
| U.08 | Follow-up in same browser session | Session id cookie/header | User continues “same for tomorrow” | **Session** history supplies context; no re-ask if city already bound. |
| U.09 | User corrects preference | Prior turn stored | User says “always Celsius” or “prefer economy news” | **AutoMemory** may persist preference; later turns use it without re-asking. |
| U.10 | Todo progress visible | Todo events wired to UI | Long multi-step request | UI shows **TodoWriteTool** state changes (optional demo). |
| U.11 | Skill-only refinement | Skills registered | User asks for wording “short briefing style” | Model invokes **SkillsTool** → loads **news-skill** or **weather-skill** fragment. |
| U.12 | Session recall | SessionEventTools enabled | User asks “what did I ask three turns ago?” | `conversation_search` (or natural recall from compacted history) returns prior content. |
| U.13 | Error — MCP timeout | MCP slow/unavailable | User triggers weather | User-visible graceful error; no raw stack trace; optional retry hint. |
| U.14 | Logout / new session | Session boundary defined | User starts “new chat” | New **Session** id; AutoMemory may still apply stable prefs. |

---

## 3. REST / API-first scenarios (`/api/**`)

| ID | Scenario | Preconditions | Main flow | Expected outcome |
|----|-----------|-----------------|-----------|------------------|
| A.01 | Contract-first change | Published HTTP contract | Developer updates the authoritative API description, runs build | Generated stubs and consumer client stay aligned; server compiles. |
| A.02 | Chat POST | Valid session + request body per contract | Client POSTs user message | Assistant text (and optional structured metadata if in contract). |
| A.03 | Chat with streaming (if in contract) | SSE/WebSocket defined | Client opens stream | Token or chunk stream; clean close on completion. |
| A.04 | AskUser over REST | Async pattern implemented | POST returns `202` + question ticket; client polls or POSTs answers | Second call resumes the orchestrated conversation with answers. |
| A.05 | Eval trigger via API | `POST /eval/run` or similar in contract | CI or operator triggers batch eval | JSON report; HTTP 200 with job id if async. |
| A.06 | Invalid payload | Contract validation on ingress | Client sends missing required field | **Problem+JSON** (or contract error) with stable error type. |
| A.07 | Version negotiation | Multiple API versions in spec | Client sends `Accept-Version` or path version | Correct handler or `406`/`404` per policy. |
| A.08 | Idempotent retry | Safe retry idempotency key in contract | Client retries same message id | No duplicate side effects (if designed). |

---

## 4. Orchestrator and subagent scenarios

| ID | Scenario | Trigger | Tools / agents involved |
|----|-----------|---------|-------------------------|
| O.01 | Direct answer (no tools) | Trivial greeting / out-of-scope polite decline | Orchestrator only. |
| O.02 | Weather delegation | Model infers weather intent | **Task** → Weather subagent → Open‑Meteo MCP tool. |
| O.03 | News delegation | Model infers news intent | **Task** → News subagent → news MCP (+ optional vector search). |
| O.04 | Parallel mental model (sequential tool constraint) | Two independent facts | Orchestrator may call Task twice in sequence (or policy allows parallel Task if configured). |
| O.05 | Refusal / guardrail | Harmful or unsupported request | Orchestrator declines without calling MCP. |
| O.06 | Model chooses wrong subagent | Ambiguous text | Correction via AskUser or user clarification turn. |
| O.07 | Subagent tool failure | MCP 4xx/5xx | Error propagated; orchestrator summarises failure to user. |
| O.08 | Nested subagent attempt | Misconfiguration | **Must not** occur: subagents must not register Task tool. |

---

## 5. Memory scenarios (Session + AutoMemory + vector)

| ID | Scenario | Mechanism |
|----|-----------|-----------|
| M.01 | Short conversation under compaction threshold | Full verbatim events in context. |
| M.02 | Long conversation crosses token/turn trigger | **Compaction** runs at turn boundary; sliding window or summarisation. |
| M.03 | Weather branch isolation | Session events tagged `orch.weather`; sibling news hidden from weather advisor filter. |
| M.04 | User returns next day | Same **Session** id optional; **AutoMemory** supplies default city/topics. |
| M.05 | User asks to forget | Manual or prompted deletion of memory files / session purge (policy TBD). |
| M.06 | AutoMemory consolidation | Time-based or “goodbye” trigger | Model merges `MEMORY.md` pointers. |
| M.07 | Semantic news recall | Vector-backed embeddings (see [ARCHITECTURE.md](ARCHITECTURE.md)) | “Similar to what I read yesterday” uses an embeddings store. |
| M.08 | Eval run isolation | Fresh session, AutoMemory disabled | Deterministic metric without hidden hints. |

---

## 6. Evaluation module scenarios

| ID | Scenario | Description |
|----|-----------|-------------|
| E.01 | Single weather example | YAML/JSON case with `required_fields`; runner asserts presence in answer. |
| E.02 | Single news example | `min_headlines` and source/time checks. |
| E.03 | Full suite run | Batch through orchestrator; aggregate pass rate. |
| E.04 | Regression after prompt change | Same dataset; compare report diff in CI. |
| E.05 | Safety check (optional) | Headlines must cite URLs returned by MCP only. |
| E.06 | Failed case export | JSON list of failures for demo slides. |

---

## 7. Developer and CI scenarios

| ID | Scenario | Command / location |
|----|-----------|-------------------|
| D.01 | Local run | Documented startup (e.g. `local` profile) with Postgres via Compose or equivalent — [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md). |
| D.02 | Unit tests only | Fast `test` phase with stub AI; no live model keys by default — [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md). |
| D.03 | Full reactor | Parent **verify**: compile, tests, and integration gates — [repository README](https://github.com/berdachuk/ai-architect-6-agents/blob/main/README.md). |
| D.04 | E2E only (after app built) | Black-box suite wired after the application module — [ARCHITECTURE.md](ARCHITECTURE.md). |
| D.05 | Docs build | Strict documentation site build (broken links fail) — docs tooling in repo. |
| D.06 | HTTP API contract drift check | CI fails if generated stubs drift from the authoritative API description (policy: commit generated sources **or** fail on dirty tree after regenerate). |
| D.07 | Database migration add | New **append-only** SQL migration; session schema if relational sessions — [ARCHITECTURE.md](ARCHITECTURE.md). |

---

## 8. Optional A2A scenarios (bonus)

| ID | Scenario | Description |
|----|-----------|-------------|
| AA.01 | Publish AgentCard | `GET /.well-known/agent-card.json` lists skills and endpoint. |
| AA.02 | Remote agent calls Meteoris Insight | External client sends JSON-RPC message per A2A spec. |
| AA.03 | Meteoris Insight delegates outward | Host tool uses A2A Java client to another agent (if implemented). |

---

## 9. Negative, edge, and non-functional scenarios

| ID | Scenario | Expected handling |
|----|-----------|-------------------|
| N.01 | Rate limit from news MCP | Backoff message; no infinite retry loop. |
| N.02 | Open‑Meteo invalid coordinates | Structured error to model; user-friendly text. |
| N.03 | Empty MCP result set | Honest “no articles found” answer. |
| N.04 | LLM provider outage | HTTP 503 from API; UI error banner. |
| N.05 | Very large user paste | Token limit / truncation policy; optional warning. |
| N.06 | Concurrent requests same session | Last-write-wins or serialisation per session policy (define at implementation). |
| N.07 | Secret in request | Rejected at gateway or validation; never logged. |

---

## 10. Traceability

| Area | Canonical document |
|------|--------------------|
| Product goals + assignment | [VISION.md](VISION.md) |
| Engineering architecture | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Agent patterns Parts 1–7 | Links in VISION.md |
| REST shapes | [ARCHITECTURE.md](ARCHITECTURE.md) + machine-readable contract in the repo |
| AI agent rules for repo | Repository root `AGENTS.md` (not published inside this MkDocs site) |

When implementation choices **add** or **remove** a scenario, update this file and, if user-visible, **VISION.md** or the **HTTP API contract** artifact in the same change set.
