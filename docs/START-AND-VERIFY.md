# Meteoris Insight — Start & Verify Guide

Step-by-step instructions to start the full stack and **manually verify** every assignment requirement.

## Assignment Requirements (refresher)

> Create an application that answers questions about **current weather** and **latest news**, using **Agent orchestrators** and **MCP servers** for **Open-Meteo** and a **keyless news source**. At least one **evaluation metric** is defined, evaluated, and demonstrated on a small dataset for at least one criterion.

| Requirement | How it is satisfied |
|-------------|---------------------|
| Orchestrator agent | `OrchestrationService` / `StubOrchestrationService` — routes user messages to weather or news delegation |
| Open-Meteo weather | `OpenMeteoWeatherIntegration` — live HTTP calls to Open-Meteo geocoding + forecast APIs |
| Keyless news MCP | `KeylessNewsIntegration` — Google News RSS (no API key required) |
| Evaluation metric | **Task Fulfilment & Information Completeness** — weather: city + temperature + conditions + time; news: ≥3 numbered headlines (`EvalScorer`) |
| Evaluation dataset | `meteoris-eval-v1.yaml` — 6 weather + 4 news cases |
| Evaluation runner | `EvaluationService` — REST `POST /api/v1/evaluation/run` and Thymeleaf page `/evaluation` |

---

## Prerequisites

- **Java 21** on `PATH`
- **Maven 3.9+** on `PATH`
- **Docker** running (for PostgreSQL 16 + pgvector)
- **curl** (or a browser)
- (Optional) `psql` for direct DB inspection

---

## 1. Build

```bash
cd /path/to/ai-architect-6-agents
mvn verify
```

Expected: `BUILD SUCCESS`, 0 failures.

---

## 2. Start PostgreSQL

```bash
docker compose up -d
```

Wait until healthy:

```bash
docker ps --filter "name=meteoris-insight-pg" --format "{{.Status}}"
# Should show: Up … (healthy)
```

Default DB: host `localhost`, port **27432**, user/database `meteoris`, password `meteoris`.

---

## 3. Start the Application

### Option A — stub-ai profile (no live LLM, deterministic stubs)

```bash
cd meteoris-insight
mvn spring-boot:run -Dspring-boot.run.profiles=stub-ai,docker-db
```

### Option B — live LLM + real weather/news (requires OpenAI-compatible endpoint)

```bash
export METEORIS_CHAT_BASE_URL=https://api.openai.com   # or your Ollama endpoint
export METEORIS_CHAT_API_KEY=sk-…                     # omit if using Ollama
export METEORIS_CHAT_MODEL=gpt-4o                     # or gemma4:26b, etc.
cd meteoris-insight
mvn spring-boot:run -Dspring-boot.run.profiles=local,docker-db
```

Wait for:

```
Tomcat started on port 18443 (http)
Started MeteorisInsightApplication in … seconds
```

All URLs below assume port **18443** (default `SERVER_PORT`).

---

## 4. Verify — Health & Home

```bash
curl -s http://localhost:18443/actuator/health
```

Expected: `{"groups":["liveness","readiness"],"status":"UP"}`

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:18443/
```

Expected: `200`

Open in a browser: [http://localhost:18443/](http://localhost:18443/)

---

## 5. Verify — Agent Orchestrator (Weather)

### 5.1 Create a new chat session

```bash
curl -s -X POST http://localhost:18443/api/v1/chat/session
```

Expected response:

```json
{"sessionId":"<24-char-hex-id>"}
```

Note the `sessionId` for the next step (or skip — the API will create one if omitted).

### 5.2 Ask a weather question

```bash
curl -s -X POST http://localhost:18443/api/v1/chat/messages \
  -H 'Content-Type: application/json' \
  -d '{"message":"What is the weather in Brest?"}'
```

**With `stub-ai`** profile, expected:

```json
{
  "sessionId": "...",
  "status": "COMPLETE",
  "modelName": "stub-ai",
  "reply": "Weather in Brest: 12°C, partly cloudy, observation time 2026-04-18T12:00:00Z (stub profile)."
}
```

**With `local`** profile (live LLM + Open-Meteo), expected reply contains real data from Open-Meteo, e.g.:

```
Weather in Brest, Belarus: 9.3°C, mainly clear to cloudy, observation time 2026-04-26T10:00Z (Open-Meteo).
```

### 5.3 Verify the orchestrator routes to the weather sub-agent

The reply **must** include:

- The city name (`Brest`)
- A temperature value (e.g. `12°C` or `9.3°C`)
- A condition description (e.g. `partly cloudy`, `clear sky`)
- A time reference (e.g. `observation time …`)

### 5.4 (Browser) Thymeleaf chat page

Open [http://localhost:18443/chat](http://localhost:18443/chat), type "weather in Minsk", press Send.

Expected: the page shows a weather reply with city, temperature, conditions, and time.

---

## 6. Verify — Agent Orchestrator (News)

### 6.1 Ask a news question

```bash
curl -s -X POST http://localhost:18443/api/v1/chat/messages \
  -H 'Content-Type: application/json' \
  -d '{"message":"Latest AI news"}'
```

**With `stub-ai`**, expected reply:

```
News digest (AI):
1. Headline one about AI
2. Headline two — industry update
3. Headline three — policy note
Source: stub profile (keyless MCP not invoked in CI).
```

**With `local`** (live Google News RSS), expected reply:

```
News digest (AI, Google News RSS, keyless):
1. <real headline about AI>
2. <real headline about AI>
3. …
```

### 6.2 Verify the keyless news integration

- The news source requires **no API key** (`KeylessNewsIntegration` → Google News RSS).
- The reply must contain at least 3 numbered headlines.

### 6.3 (Browser) Ask in Thymeleaf

On [http://localhost:18443/chat](http://localhost:18443/chat), type "news about technology".

---

## 7. Verify — AskUserQuestionTool (interactive clarification)

```bash
curl -s -X POST http://localhost:18443/api/v1/chat/messages \
  -H 'Content-Type: application/json' \
  -d '{"message":"askuser"}'
```

Expected:

```json
{
  "status": "ASK_USER",
  "ticketId": "<24-char-hex-id>",
  "prompt": "Which city should we use for the forecast?",
  "options": [
    {"id": "brest", "label": "Brest"},
    {"id": "minsk", "label": "Minsk"}
  ]
}
```

Answer the question:

```bash
curl -s -X POST http://localhost:18443/api/v1/chat/questions/<ticketId>/answers \
  -H 'Content-Type: application/json' \
  -d '{"selectedOptionIds":["brest"]}'
```

Expected: `status: "COMPLETE"` with a weather reply for Brest.

### 7.1 (Browser) AskUser flow

On the chat page, type "askuser". The page presents a form with radio buttons (Brest / Minsk). Select one and submit.

---

## 8. Verify — Evaluation Metric & Dataset

### 8.1 Metric definition

**Task Fulfilment & Information Completeness** (`EvalScorer`):

| Type | Criterion | Check |
|------|-----------|-------|
| **Weather** | City mentioned | Answer contains the expected city name (case-insensitive) |
| **Weather** | Temperature present | Answer contains `°` or `temp` |
| **Weather** | Conditions present | Answer contains `cloud`, `clear`, `rain`, `snow`, etc. |
| **Weather** | Time reference | Answer contains `time` or ISO timestamp |
| **News** | Minimum headlines | Answer contains ≥ N numbered headline lines (`1. …`, `2. …`, …) |

### 8.2 Dataset (`meteoris-eval-v1.yaml`)

10 cases: 6 weather (w1–w6) + 4 news (n1–n4).

| ID | Type | Question | Key checks |
|----|------|----------|-------------|
| w1 | weather | "What is the current weather in Brest, Belarus?" | city=Brest, temp, conditions, time |
| w2 | weather | "Weather in Minsk today?" | city=Minsk, temp, conditions, time |
| w3 | weather | "Forecast for Grodno" | city=Grodno, temp, conditions, time |
| w4 | weather | "What is the weather in Vitebsk?" | city=Vitebsk, temp, conditions, time |
| w5 | weather | "Current conditions in Gomel" | city=Gomel, temp, conditions, time |
| w6 | weather | "Weather in Mogilev" | city=Mogilev, temp, conditions, time |
| n1 | news | "Latest AI news for today" | ≥3 headlines |
| n2 | news | "News about climate" | ≥3 headlines |
| n3 | news | "Headlines on space exploration" | ≥3 headlines |
| n4 | news | "Technology sector news" | ≥3 headlines |

### 8.3 Run evaluation via REST

```bash
curl -s -X POST http://localhost:18443/api/v1/evaluation/run \
  -H 'Content-Type: application/json' \
  -d '{"dataset":"meteoris-eval-v1","profile":"stub-ai"}' | python3 -m json.tool
```

The response contains:

- `runId` — unique run identifier
- `passCount` / `failCount` — aggregate scores
- `reportJson` — full JSON with per-case pass/fail and reason codes

With **`stub-ai`**, expect `passCount: 0, failCount: 10` (stubs don't produce real data, but the metric pipeline and infrastructure are exercised).

With **`local`** (live LLM + real Open-Meteo + real news), expect 10/10 pass when theLLM and integrations are healthy.

### 8.4 Run evaluation via Thymeleaf

Open [http://localhost:18443/evaluation](http://localhost:18443/evaluation), select dataset `meteoris-eval-v1`, click **Run**.

The result page shows the full JSON report with per-case pass/fail breakdown.

---

## 9. Verify — Database & Migrations

```bash
PGPASSWORD=meteoris psql -h localhost -p 27432 -U meteoris -d meteoris -c '\dt'
```

Expected tables (Flyway migrations V1–V3 + pgvector):

| Table | Owner |
|-------|-------|
| `ai_session` | meteoris |
| `ai_session_event` | meteoris |
| `spring_ai_chat_memory` | meteoris |
| `news_article_embedding` | meteoris |
| `flyway_schema_history` | meteoris |

---

## 10. Verify — OpenAPI & Swagger

| URL | Purpose |
|-----|---------|
| [http://localhost:18443/v3/api-docs](http://localhost:18443/v3/api-docs) | OpenAPI 3 JSON |
| [http://localhost:18443/swagger-ui/index.html](http://localhost:18443/swagger-ui/index.html) | Swagger UI (interactive) |

---

## 11. Stop Everything

```bash
# Stop Spring Boot (Ctrl+C in the terminal, or:)

# Stop Docker
docker compose down
```

---

## Quick Checklist

| # | What to verify | How | Pass? |
|---|----------------|-----|-------|
| 1 | Build passes | `mvn verify` → BUILD SUCCESS | ☐ |
| 2 | PostgreSQL healthy | `docker ps` → healthy | ☐ |
| 3 | App starts | `actuator/health` → UP | ☐ |
| 4 | Home page renders | `GET /` → 200 | ☐ |
| 5 | **Weather question** (Open-Meteo) | `POST /api/v1/chat/messages` with "weather in Brest" → reply includes city, temp, conditions, time | ☐ |
| 6 | **News question** (keyless MCP) | `POST /api/v1/chat/messages` with "Latest AI news" → reply includes ≥3 numbered headlines, no API key | ☐ |
| 7 | **AskUserQuestionTool** | Send "askuser" → ASK_USER status with options; answer → COMPLETE | ☐ |
| 8 | **Evaluation metric defined** | `EvalScorer` checks city/temp/conditions/time (weather) and ≥N headlines (news) | ☐ |
| 9 | **Evaluation dataset exists** | `meteoris-eval-v1.yaml` — 10 cases | ☐ |
| 10 | **Evaluation demonstrated** | `POST /api/v1/evaluation/run` → JSON report with passCount/failCount per case | ☐ |
| 11 | DB tables created | `\dt` shows 5 tables | ☐ |
| 12 | OpenAPI & Swagger | `/v3/api-docs` → 200; `/swagger-ui/index.html` → 200 | ☐ |