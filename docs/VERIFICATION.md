# Meteoris Insight — Start & Verification Guide

This document describes how to build, start, and verify that **Meteoris Insight** satisfies the **Practical Task: Agentic AI** requirements.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | `javac` and `java` on PATH |
| Maven | 3.9+ | `mvn -version` |
| Docker | 24+ | For PostgreSQL 16 + pgvector |
| (Optional) LLM endpoint | — | Ollama or OpenAI-compatible HTTP server for live profile |

---

## 1. Build and automated verification

The canonical CI command is `mvn verify` from the **project root**. It compiles `meteoris-insight`, runs tests against Testcontainers PostgreSQL, then runs `meteoris-insight-e2e` contract tests.

```bash
cd /path/to/ai-architect-6-agents
mvn verify
```

**What it verifies**

| Check | How observed |
|-------|--------------|
| Compiles and packages | `BUILD SUCCESS` |
| Modulith structure | `ModulithStructureTest` passes (enforced module boundaries) |
| Database migrations | Flyway V1–V3 applied on Testcontainers `pgvector/pgvector:pg16` |
| Weather tool | `StubAiConfiguration` + `WeatherToolsTest` (stub path) |
| News tool | `StubAiConfiguration` + `NewsToolsTest` (stub path) |
| Session memory | `SessionServiceTest` — JDBC session table round-trip |
| Evaluation runner | `EvalCliIT` runs through `meteoris-eval-v1.yaml` (stub profile) |
| Evaluation module | `EvaluationModuleTest` — 10 eval cases via stub orchestration |
| E2E API contract | `MeteorisInsightE2EIT` — health, chat session, weather flow, eval run, 422 error |

**Expected result**

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

The single skipped test is `MeteorisInsightApplicationTest` under the `!stub-ai` condition (live profile — optional). All other tests pass deterministically with no live LLM or live MCP.

---

## 2. Start the application (web mode)

### 2.1 Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL 16 with pgvector on `localhost:27432` (credentials in `docker-compose.yml`).

**Verify**

```bash
docker compose ps
```
Status should be `running (healthy)`.

### 2.2 Run with stub AI (default, deterministic)

```bash
mvn -pl meteoris-insight spring-boot:run
```

Active profiles: `stub-ai,docker-db`.

Open:
- **Thymeleaf UI** — `http://localhost:18443/`
- **Swagger UI** — `http://localhost:18443/swagger-ui.html`
- **Actuator** — `http://localhost:18443/actuator/health`

### 2.3 Live LLM (optional)

If you have a local Ollama instance or an OpenAI-compatible endpoint:

```bash
export METEORIS_CHAT_BASE_URL=http://localhost:11434
export METEORIS_CHAT_MODEL=gemma4:26b
export METEORIS_EMBEDDING_BASE_URL=http://localhost:11434
mvn -pl meteoris-insight spring-boot:run -Dspring-boot.run.profiles=local,docker-db
```

Profiles active: `local,docker-db` (no `stub-ai`).

---

## 3. Verify task requirements

### Requirement 1 — Answer weather questions using MCP-class tools

**Approach:** The orchestrator delegates weather queries to a **Weather specialist** `ChatClient` that uses `@Tool` methods calling the **Open-Meteo** HTTP API. The tool schema matches MCP-style `get_weather_forecast(lat,lon)` and `geocode_city(city)`.

**Verify via UI**

1. Open `http://localhost:18443/chat`
2. Type: `What is the current weather in Brest, Belarus?`
3. Expected: assistant reply contains temperature, conditions, and city name.

**Verify via REST**

```bash
curl -X POST http://localhost:18443/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d '{"message":"What is the current weather in Brest, Belarus?"}'
```

Expected JSON:
```json
{
  "status": "COMPLETE",
  "reply": "...temperature...conditions...Brest...",
  "sessionId": "<24-char hex>",
  ...
}
```

**Verify via E2E test (stub)**

```bash
mvn verify -pl meteoris-insight-e2e
```
Test `chatMessageStubWeatherFlow` asserts `COMPLETE` status with weather content in the response body.

---

### Requirement 2 — Answer news questions using keyless MCP

**Approach:** The orchestrator delegates news queries to a **News specialist** that uses `@Tool` methods parsing **Google News RSS** (no API key required). Results are stored in PostgreSQL with pgvector embeddings for optional similarity recall.

**Verify via UI**

1. Open `http://localhost:18443/chat`
2. Type: `Latest AI news`
3. Expected: assistant reply contains headline-like items.

**Verify via REST**

```bash
curl -X POST http://localhost:18443/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d '{"message":"Latest AI news"}'
```

Expected JSON:
```json
{
  "status": "COMPLETE",
  "reply": "Here are the latest headlines: ..."
}
```

---

### Requirement 3 — Agent orchestration

**Approach:** The **Main Orchestrator** `ChatClient` (profile `!stub-ai`) or the **Stub Orchestration Service** (profile `stub-ai`) routes requests to weather/news specialists using `DelegationTools` (`delegate_weather`, `delegate_news`). Session memory, todo lists, skills, and ask-user flows are orchestrated alongside.

**Verify via REST**

```bash
curl -X POST http://localhost:18443/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d '{"message":"weather"}'
```

Expected `status: COMPLETE` with assistant reply (with `stub-ai`, AskUser is not triggered and a deterministic orchestrated response is returned).

**Verify via Thymeleaf UI**
- `/chat` — send a message, see reply.
- `/chat/answer` — if AskUser triggers (live profile with `local`), fill the answer form and submit.
- `/evaluation` — run evaluation from the browser.

---

### Requirement 4 — Evaluation metric demonstrated

**Approach:** A small versioned evaluation dataset + metric runner is built into the app.

| Item | Detail |
|------|--------|
| Dataset | `meteoris-insight/src/main/resources/eval/meteoris-eval-v1.yaml` |
| Cases | 10 (6 weather + 4 news) |
| Metrics | Weather: `city_presence_pass`, `required_fields_pass`; News: `headline_count_pass`, `source_or_time_pass` |
| Scorer | `EvalScorer.evaluate(answer, evalCase)` — deterministic string checks |

**Verify via REST**

```bash
curl -X POST http://localhost:18443/api/v1/evaluation/run \
  -H "Content-Type: application/json" \
  -d '{"dataset":"meteoris-eval-v1","profile":"stub-ai"}'
```

Expected JSON (example):
```json
{
  "datasetId": "meteoris-eval",
  "version": "1.0.0",
  "passCount": 10,
  "totalCount": 10,
  "metrics": {
    "weather_pass_rate": 1.0,
    "news_pass_rate": 1.0
  },
  "reportJson": "...",
  "failedCases": []
}
```

**Verify via UI**

1. Open `http://localhost:18443/evaluation`
2. Enter dataset: `meteoris-eval-v1`
3. Enter profile: `stub-ai`
4. Click **Run**
5. Page shows total cases, pass count, and aggregate pass rate.

**Verify via CLI (no web server)**

```bash
mvn -q -pl meteoris-insight spring-boot:run \
  -Dspring-boot.run.profiles=eval-cli,stub-ai,docker-db \
  -Dspring-boot.run.arguments="--meteoris.eval.dataset=meteoris-eval-v1,--meteoris.eval.profile=stub-ai"
```

Output is printed to stdout and the JVM exits.

**Verify via automated test**

```bash
mvn -pl meteoris-insight test -Dtest=EvalCliIT
```

Assertions:
- Captured output contains `"dataset_id": "meteoris-eval"`
- Captured output contains `"pass_count":`

---

## 4. Quick smoke check

If you only want to confirm the app boots and can handle a single weather request:

```bash
docker compose up -d
mvn -pl meteoris-insight spring-boot:run &
sleep 10
curl -s http://localhost:18443/actuator/health | grep UP
curl -s -X POST http://localhost:18443/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d '{"message":"weather in Brest, Belarus"}' | grep COMPLETE
```

Both `curl` calls should succeed.

---

## 5. Profiles quick reference

| Profile | Purpose |
|---------|---------|
| `stub-ai` | Deterministic mock ChatClient (CI default). |
| `docker-db` | Connect to PostgreSQL from `docker compose up -d`. |
| `test-pgvector` | Testcontainers PostgreSQL + pgvector (used in tests). |
| `local` | Live OpenAI-compatible endpoint (requires `METEORIS_CHAT_BASE_URL` and optionally `METEORIS_CHAT_API_KEY`). |
| `eval-cli` | Non-web Spring Boot context for CLI eval runner. |

---

## 6. Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `mvn verify` fails with Docker error | Docker not running or Testcontainers cannot pull image | `docker info` should succeed; check internet/Docker daemon. |
| `UnsatisfiedDependencyException: HttpServletRequest` | `eval-cli` profile without `@ConditionalOnWebApplication` | Already fixed — pull latest; check you're on the right branch. |
| E2E client compilation errors | `openapi.yaml` changed without regenerating | `mvn clean verify` from root to regenerate sources. |
| Live profile gives no weather/news | No LLM endpoint reachable | Verify `METEORIS_CHAT_BASE_URL` and model availability (`ollama list`). |

---

## 7. Project structure (relevant paths)

```
meteoris-insight/src/main/java/com/berdachuk/meteoris/insight/
├── agent/               # Orchestrator + stub/live configuration
├── api/                 # REST controllers (OpenAPI-first)
├── evaluation/          # EvalDataset, EvalScorer, EvaluationService
├── memory/              # Session, AutoMemory, TodoStateStore
├── news/                # Keyless news integration (Google News RSS)
├── weather/             # Open-Meteo HTTP integration
meteoris-insight/src/main/resources/
├── eval/meteoris-eval-v1.yaml   # Evaluation dataset
├── db/migration/        # Flyway V1–V3
meteoris-insight/api/
├── openapi.yaml         # Canonical OpenAPI contract
```
