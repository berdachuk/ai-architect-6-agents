# Meteoris Insight

Agentic Spring Boot service: weather + news orchestration, MCP integrations, JDBC session memory, Thymeleaf UI, OpenAPI-first REST, and a reproducible evaluation runner.

## Build

```bash
mvn verify
```

`mvn verify` requires **Docker**: integration tests start **PostgreSQL 16 + pgvector** via **Testcontainers** (`pgvector/pgvector:pg16`) with profiles **`stub-ai`** and **`test-pgvector`** (no live LLM, deterministic tool integrations). Local `spring-boot:run` defaults to **`stub-ai,docker-db`** (PostgreSQL on `localhost:27432` — start **`docker compose up -d`** first). Canonical conventions: **`application/AGENTS.md`** (*Database and automated tests*).

After `mvn package`, the runnable Spring Boot fat JAR is:

`meteoris-insight/target/meteoris-insight-*-exec.jar`

The plain JAR (without `exec` classifier) is the Maven compile dependency for `meteoris-insight-e2e`.

## Run locally

The runtime uses **PostgreSQL only** (see `docker-compose.yml` and `meteoris-insight/src/main/resources/application-docker-db.yml`). There is no embedded H2 profile.

### Database environment variables

| Variable               | Default     | Description                             |
|------------------------|-------------|-----------------------------------------|
| `METEORIS_DB_NAME`     | `meteoris`  | PostgreSQL database name                |
| `METEORIS_DB_USER`     | `meteoris`  | Database user                           |
| `METEORIS_DB_PASSWORD` | `meteoris`  | Database password                       |
| `METEORIS_DB_HOST`     | `localhost` | Database host (for `docker-db` profile) |
| `METEORIS_DB_PORT`     | `27432`     | Database port                           |

Set these before running `docker compose up -d` or when starting the application.

```bash
export METEORIS_DB_NAME=mydb METEORIS_DB_USER=myuser METEORIS_DB_PASSWORD=secret
docker compose up -d
mvn -pl meteoris-insight spring-boot:run
```

```bash
docker compose up -d
mvn -pl meteoris-insight spring-boot:run
```

Active defaults are **`stub-ai,docker-db`**. Open `http://localhost:18443/` once Postgres is healthy (`docker compose ps`).

### Live LLM (OpenAI-compatible)

1. Export `OPENAI_API_KEY`.
2. With **`docker compose up -d`**, run **`local,docker-db`** and **without** `stub-ai`, for example:

```bash
export OPENAI_API_KEY=sk-...
mvn -pl ../meteoris-insight spring-boot:run -Dspring-boot.run.profiles=local,docker-db
```

`spring.ai.custom.chat.*` follows the programmatic wiring described in `docs/PRD.md` (**NFR-8**).

### Keyless news MCP

Baseline CI uses stub news/weather. For coursework demos, configure your chosen **keyless** news MCP client under the `local` profile (see `docs/PRD.md` and `application/AGENTS.md`).

### Live integrations (no API keys)

With **`local`** (and **without** `stub-ai`), the app uses:

- **Weather:** public [Open-Meteo](https://open-meteo.com/) HTTP APIs (geocoding + forecast) — same data family as Open-Meteo MCP servers, without running a separate MCP process.
- **News:** [Google News RSS](https://news.google.com/) search feed (keyless).

The orchestrator delegates to **weather** and **news specialist** `ChatClient` instances via `delegate_weather` / `delegate_news` tools. Additional tools: **`list_skills` / `load_skill`** (classpath `skills/*/SKILL.md`), **`todo_write` / `todo_list`**, and **`automemory_append` / `automemory_read`** (files under `meteoris.automemory.root`).

## Evaluation

- Dataset: `meteoris-insight/src/main/resources/eval/meteoris-eval-v1.yaml`
- UI: `POST /evaluation/run` from `/evaluation`
- REST: `POST /api/v1/evaluation/run` with JSON body `{"dataset":"meteoris-eval-v1","profile":"stub-ai"}`

Methodology: `docs/EVALUATION-METHODOLOGY.md`.

### Evaluation CLI (no HTTP server)

Runs the same `EvaluationService` as the REST endpoint, then exits (non-web Spring Boot). Start Postgres first (`docker compose up -d`).

```bash
mvn -q -pl meteoris-insight spring-boot:run \

## Submission evidence

Capture screenshots and logs under [`docs/submission/`](docs/submission/README.md) (see that folder’s checklist).

## User guide

Browser chat, AskUser, evaluation page, and REST overview: [`docs/USER-GUIDE.md`](docs/USER-GUIDE.md).

## Documentation site

```bash
pip install -r requirements-docs.txt
mkdocs build -s
```

**Preview locally:** `mkdocs serve` (default `http://127.0.0.1:8000/`).

**Preview on the LAN:** bind to all interfaces so other devices can connect (firewall must allow the port).

```bash
cd /path/to/ai-architect-6-agents
mkdocs serve -a 0.0.0.0:8000
```

Then open `http://<server-lan-ip>:8000/` plus the path shown in the MkDocs log (this repo uses `site_url` with a project subpath, e.g. `…/ai-architect-6-agents/`).

**Preview over SSH:** on the server run `mkdocs serve` (default `127.0.0.1:8000`), then from your laptop:

```bash
ssh -L 8000:127.0.0.1:8000 user@remote-host
```

Browse `http://127.0.0.1:8000/` on the laptop (same subpath as in the server log).

**Public hosting:** run `mkdocs build` and deploy the `site/` directory (e.g. GitHub Pages per `mkdocs.yml` `site_url`).

## WBS / milestones


See `docs/IMPLEMENTATION-PLAN-WBS.md`.
