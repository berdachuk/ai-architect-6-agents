# Meteoris Insight — Evaluation methodology

**Purpose:** Define a **clean, reproducible** way to measure orchestrated weather and news answers against a **small, versioned** dataset. Normative product rules: [PRD.md](PRD.md) **FR-10**; dataset shape and metric intent: [VISION.md](VISION.md) (Evaluation section).

**Out of scope for this document:** UI copy, OpenAPI path names, and implementation class names — those follow the codebase and **`api/openapi.yaml`**.

---

## 1. Principles

| Principle | Meaning |
|-----------|---------|
| **Versioned inputs** | Every report names **`dataset_id`**, **`dataset_version`**, and **`case_id`** so results can be tied to an exact file commit. |
| **Isolated memory** | Each case runs with a **new Session** and **AutoMemory disabled** unless the case explicitly tests memory (**M.08**). |
| **Deterministic CI** | Default **`mvn verify`** uses **`test`** / **`stub-ai`** profile: **no live LLM**, **no live MCP** (or MCP stubbed at the tool boundary). Same seed/configuration produces the same pass/fail for stubbed outputs. |
| **Transparent rules** | Pass/fail is computed from **documented** string checks (substring, regex, or structured parser), not ad-hoc human judgment in CI. |
| **Comparable reports** | Machine-readable **JSON** report with aggregate metrics and per-case rows; optional HTML for demos. |

---

## 2. Definitions

| Term | Definition |
|------|------------|
| **Dataset** | A single YAML or JSON file listing **cases** plus metadata (`id`, `version`). |
| **Case** | One user `question`, a `type` (`weather` \| `news`), and scoring parameters (`required_fields`, `min_headlines`, flags). |
| **Run** | One full execution of all cases in a dataset under a fixed **environment profile** (e.g. `stub-ai`). |
| **Pass** | Case satisfies **all** enabled criteria for its `type` (Section 6). |
| **Environment profile** | Spring profile (or equivalent) fixing: stub vs live `ChatModel`, stub vs live MCP adapters, timeouts. |

---

## 3. Environment matrix

Document the active profile in every report (`profile` field).

| Profile | LLM | MCP | Use |
|---------|-----|-----|-----|
| **`stub-ai`** (CI default) | Stub / mock `ChatModel` or `ChatClient` | Stub tool results or fixed fixtures | Reproducible **`mvn verify`**; regression gates. |
| **`local`** (optional) | Live OpenAI-compatible endpoint per [PRD.md](PRD.md) **NFR-8** | Live Open‑Meteo + keyless news MCP | Manual exploration; not required for CI. |

**Reproducibility rule:** Published scores for coursework or regression **must** state **`dataset_version` + `git_commit` (or tag) + `profile`**. CI must use **`stub-ai`** unless the project explicitly documents an opt-in live eval job.

---

## 4. Dataset specification

### 4.1 Location and naming

- Store under a path committed to git, for example:  
  `meteoris-insight/src/main/resources/eval/meteoris-eval-v1.yaml`  
  (exact path is an implementation choice; keep it stable and documented in README.)

### 4.2 Required metadata (file-level)

| Field | Required | Description |
|-------|----------|-------------|
| `dataset_id` | Yes | Stable slug, e.g. `meteoris-eval`. |
| `version` | Yes | Semver or date string, e.g. `1.0.0`. Bump when cases or rules change. |

### 4.3 Case schema (minimal)

**Weather**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Stable case id, e.g. `w1`. |
| `type` | `weather` | Yes | |
| `question` | string | Yes | Exact or normalised input sent to the orchestrator. |
| `expected_city` | string | Yes | City name that **must** appear in the answer (case-insensitive unless documented). |
| `required_fields` | list | Yes | Subset of: `city`, `temperature`, `conditions`, `time` — see Section 6.1. |

**News**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | e.g. `n1`. |
| `type` | `news` | Yes | |
| `question` | string | Yes | |
| `min_headlines` | integer | Yes | Minimum distinct headline-like lines or list items in the answer. |
| `require_source_or_time` | boolean | No | If true, answer must contain a URL pattern or a time expression per documented regex. |

### 4.4 Example (illustrative)

```yaml
dataset_id: meteoris-eval
version: "1.0.0"
cases:
  - id: w1
    type: weather
    question: "What is the current weather in Brest, Belarus?"
    expected_city: "Brest"
    required_fields: ["city", "temperature", "conditions", "time"]
  - id: n1
    type: news
    question: "Latest AI news for today"
    min_headlines: 3
    require_source_or_time: false
```

---

## 5. Execution procedure

### 5.1 Preconditions

1. Dataset file present and valid YAML/JSON schema (fail fast on parse errors).
2. Application built with eval module on classpath.
3. Profile set to **`stub-ai`** for CI (or documented alternative).

### 5.2 Per-case algorithm

1. Create **new** `session_id` (or equivalent) for the case.
2. Ensure **AutoMemory** is not read for scoring path (disabled or empty root for eval).
3. Send **`question`** to the **same** orchestration entrypoint used in production (REST or in-process service).
4. Collect **final assistant text** (last assistant message in the turn; if streaming, concatenate chunks in order).
5. Run **scoring** (Section 6); record `pass`, `fail`, and `reason_codes[]`.

### 5.3 Run-level aggregation

- **weather_pass_rate** = passed weather cases / total weather cases (0 if none).
- **news_pass_rate** = passed news cases / total news cases (0 if none).
- **overall_pass_rate** = all passed / all cases.

Handle zero division explicitly in the report (`null` or `"n/a"` with documented meaning).

---

## 6. Scoring rules (normative)

All checks use **normalized answer text** unless noted: trim whitespace, collapse repeated spaces, optional lowercasing for **city** match only.

### 6.1 Weather — field completeness

For each token in `required_fields`:

| Token | Pass condition (all must hold for that token) |
|-------|-----------------------------------------------|
| `city` | `expected_city` appears as a substring (case-insensitive). |
| `temperature` | Answer matches a **temperature pattern** documented in the runner (e.g. number + `°C`/`°F`/`C`/`F` or digit with optional decimal). |
| `conditions` | Answer contains at least one **weather condition token** from an allow-list **or** matches a generic “not only temperature” heuristic documented in code (prefer allow-list for reproducibility). |
| `time` | Answer contains a **time frame** substring from allow-list: e.g. `now`, `today`, `tomorrow`, `current`, `this afternoon` (document exact list in `README` and keep stable across versions). |

**Case pass:** every listed `required_field` passes.

### 6.2 News — headline count

- **min_headlines:** Count lines or bullet items that look like headlines (implementation: documented regex or markdown list item count). Must be **≥** `min_headlines`.
- **require_source_or_time:** If true, require URL-like pattern `\https?://` or time/date regex documented next to the flag.

**Case pass:** headline count OK and optional flag satisfied.

### 6.3 Optional safety check (E.05)

When enabled for a run:

- Parse **URLs** returned by the news MCP tool in that case’s trace (if captured); each URL cited in the answer must appear in that set.  
- **Reproducibility:** Requires logging or capturing MCP tool output during eval; document whether **stub** MCP returns fixed URLs for CI.

---

## 7. Report format (reproducible artifact)

Emit one JSON object per **run** (pretty-print for git diff friendly reviews):

| Field | Type | Description |
|-------|------|-------------|
| `run_id` | string | Unique id (e.g. 24-hex from `IdGenerator`). |
| `timestamp` | ISO-8601 UTC | When the run finished. |
| `git_commit` | string | From env `CI_COMMIT_SHA` or local `git rev-parse HEAD`; `"unknown"` if unavailable. |
| `dataset_id` | string | |
| `dataset_version` | string | |
| `profile` | string | e.g. `stub-ai`. |
| `model_id` | string | Stub name or live model id. |
| `metrics` | object | `weather_pass_rate`, `news_pass_rate`, `overall_pass_rate` (numbers 0–1). |
| `cases` | array | Each: `case_id`, `type`, `pass`, `reason_codes`, `answer_excerpt` (truncated). |

**Failed-case export (E.06):** same `cases` array filtered to `pass: false`, written to `eval-failures-{run_id}.json`.

---

## 8. Regression policy

1. **Baseline:** Tag a commit where **`stub-ai`** run achieves **overall_pass_rate = 1.0** on a frozen **`dataset_version`**.
2. **CI gate:** Subsequent PRs must keep **≥** agreed threshold (default **1.0** on stub profile for small v1 sets) or require explicit **dataset_version** bump + justification in PR description.
3. **Prompt or skill changes:** Re-run the same **`dataset_version`**; attach report JSON or summary table to PR when eval behaviour is affected.

---

## 9. Limitations (explicit)

- Heuristic checks do **not** prove factual correctness of weather or news against ground truth APIs.
- Stubbed LLM/MCP validates **wiring and formatting**, not real-world quality.
- Live eval is **noisy** (MCP and model variance); treat as exploratory unless repeated with fixed seeds and cached MCP fixtures.

---

## 10. Traceability

| This doc | PRD | Use cases | User stories |
|----------|-----|-----------|--------------|
| Sections 1–8 | **FR-10**, Evaluation recommendations | `E.01`–`E.06`, `P.03`, `M.08` | US-34–US-40, US-20 |

---

**Document version:** 1.0. Update when scoring rules or dataset schema change; bump **`dataset_version`** when cases change.
