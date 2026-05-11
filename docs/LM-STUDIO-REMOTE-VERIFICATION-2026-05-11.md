# LM Studio Remote Verification Report — 2026-05-11

This report records an **end-to-end verification** of Meteoris Insight against a **remote LM Studio** OpenAI-compatible API, plus **`mvn verify`** from the repository root.

| Field | Value |
|-------|--------|
| **Date / time (local)** | 2026-05-11 |
| **Git branch** | `develop` |
| **Git commit** | `b7c86c0` |
| **App version** | `0.1.0-SNAPSHOT` |
| **LM Studio base URL** | `http://192.168.0.73:1234` |
| **Spring profiles** | `docker-db`, **`lm-studio`** (`application-lm-studio.yml`) |
| **Application URL** | `http://localhost:18443` |
| **PostgreSQL** | Docker Compose — `localhost:27432`, container **healthy** |

---

## 1. Automated build (`mvn verify`)

Executed from the **repository root**:

```bash
mvn verify
```

| Result | Detail |
|--------|--------|
| **Outcome** | **BUILD SUCCESS** |
| **Scope** | Reactor: `meteoris-insight` (unit/integration incl. Testcontainers PostgreSQL + pgvector) + `meteoris-insight-e2e` |

---

## 2. LM Studio API (`GET /v1/models`)

| Check | Result |
|-------|--------|
| **HTTP** | **200** |
| **Requested chat model present** | `google/gemma-4-26b-a4b` |
| **Requested embedding model present** | `text-embedding-nomic-embed-text-v1.5@q8_0` |

Additional model ids observed (subset): `google/gemma-4-31b`, `google/gemma-4-e4b`, `text-embedding-nomic-embed-text-v1.5@q4_k_m`.

---

## 3. Application runtime

Startup command:

```bash
docker-compose up -d   # PostgreSQL (if not already running)
mvn -pl meteoris-insight spring-boot:run "-Dspring-boot.run.profiles=docker-db,lm-studio"
```

Boot aligns chat and embeddings with **`http://192.168.0.73:1234`** per `application-lm-studio.yml`.

---

## 4. Health (`GET /actuator/health`)

| Component | Status |
|-----------|--------|
| **Aggregate root `status`** | **UP** |
| **`db`** | **UP** (PostgreSQL validation) |
| **`meteoris`** | **UP** |
| **`meteoris.details.chatModel`** | **UP** — `google/gemma-4-26b-a4b`, base URL LM Studio |
| **`meteoris.details.embeddingModel`** | **UP** — `ConfiguredDimensionsOpenAiEmbeddingModel`, model `text-embedding-nomic-embed-text-v1.5@q8_0`, dimensions **768** |

Embeddings health reflects **`ConfiguredDimensionsOpenAiEmbeddingModel`** (configured dimensions + LM Studio client); aggregate health was **UP** for this run.

---

## 5. Weather (Open-Meteo via delegated tools)

**Request:** `POST /api/v1/chat/messages`  
**Body (JSON file recommended on Windows):** `{"message":"What is the current weather in Brest, Belarus?"}`

| Field | Result |
|-------|--------|
| **HTTP** | **200** |
| **`status`** | **`COMPLETE`** |
| **`modelName`** | `live` |
| **Reply (evidence)** | Contains **Brest**, **Belarus**, temperature, **Open-Meteo** |

Sample session id from run: `6a01e8360a4e8387d094bac2`.

---

## 6. News (Google News RSS, keyless)

**Request:** `POST /api/v1/chat/messages`  
**Body:** `{"message":"Latest AI news headlines"}`

| Field | Result |
|-------|--------|
| **HTTP** | **200** |
| **`status`** | **`COMPLETE`** |
| **Digest** | Prefix `News digest (general, Google News RSS, keyless):`, **8** numbered headlines |

Sample session id: `6a01e83860356a998394bac3`.

---

## 7. Evaluation (`POST /api/v1/evaluation/run`)

**Body:** `{"dataset":"meteoris-eval-v1","profile":"lm-studio"}`

| Field | Result |
|-------|--------|
| **HTTP** | **200** |
| **`passCount`** | **10** |
| **`failCount`** | **0** |
| **`profile`** | **`lm-studio`** |

Sample `runId`: `6a01e83fec9e6c1b4294bace`.

---

## 8. Conclusion

| Requirement | Status |
|-------------|--------|
| **`mvn verify`** | ✅ **PASS** |
| **Docker PostgreSQL** | ✅ Running (**healthy**) |
| **LM Studio `/v1/models`** | ✅ Required models listed |
| **`docker-db` + `lm-studio` boot** | ✅ Tomcat **18443** |
| **`/actuator/health` aggregate** | ✅ **UP** |
| **Weather + news chat** | ✅ **`COMPLETE`** |
| **Evaluation dataset `meteoris-eval-v1`** | ✅ **10 / 10** |

---

## 9. Reproduction notes

- Use **`curl.exe`** with **`--data-binary @path\to\body.json`** on Windows to avoid PowerShell mangling JSON.
- Example bodies are convenient under `meteoris-insight/target/lm-verify-*.json` (regenerated for this run).
- Stop the dev server when finished (`spring-boot:run` uses port **18443**).

---

## 10. References

- Profile YAML: `meteoris-insight/src/main/resources/application-lm-studio.yml`
- Broader verification catalogue: [VERIFICATION-REPORT.md](VERIFICATION-REPORT.md)
