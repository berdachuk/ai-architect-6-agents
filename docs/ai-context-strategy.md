# AI Context Strategy — Meteoris Insight

This repository uses a **layered, tool-agnostic** context layout so Cursor, Claude Code, Codex, Copilot Agents, and others share the same boundaries: root rules, nested `AGENTS.md` at module boundaries, and `.agents/skills/**` for repeatable how-to detail.

## Layer model

```text
AGENTS.md (root)                    ← canonical project rules
  → module-level AGENTS.md          ← boundary-specific rules only
    → .agents/skills/**/SKILL.md    ← detailed how-to (English); not a second rules layer
```

**Project rules** (must / must-not, language, review boundaries) live **only** in `AGENTS.md` files. Do **not** maintain a parallel rules tree under **`.cursor/`** in this repository.

### 1) Root `AGENTS.md`

Compact index: Meteoris Insight purpose, planned stack, repo map, high-level module diagram, global commands/boundaries, **skills table**, links to nested guides and `docs/VISION.md`.

### 2) Nested `AGENTS.md`

Only where workflows differ materially:

| Path | Role |
|------|------|
| `docs/AGENTS.md` | MkDocs, vision doc maintenance, doc consistency |
| `application/AGENTS.md` | Spring Boot / Modulith runtime, MCP, DB, profiles |
| `meteoris-insight-e2e/AGENTS.md` | Black-box E2E module (separate Maven project) |

Add more nested `AGENTS.md` files only when a **new** major boundary appears (e.g. a dedicated load-test harness).

### 3) Skills (`.agents/skills/**/SKILL.md`)

Canonical **how-to** for repeated or risky work: Modulith edges, Spring AI agentic patterns, MCP wiring, HTTP API, tests, evaluation. Each skill states **when to use**, **instructions**, and **boundaries**. Skills must be written in **English** (same as all repo documentation and code comments — see root `AGENTS.md` → **Global boundaries**).

## Feeding analysis into structure

When the Maven tree appears:

1. Re-read `docs/VISION.md` module table vs actual packages.
2. Update `core-architecture` skill and root repo map.
3. If a new surface (e.g. OpenAPI for chat) is introduced, extend `api-design` and document alignment rules in nested `AGENTS.md`.

## Adding a new skill

Create `.agents/skills/<name>/SKILL.md` with Description / When to use / Instructions / Boundaries. Register the skill in **root** `AGENTS.md` table and link it from any nested `AGENTS.md` that owns related workflows.

## Updating skills

- Vision or Spring AI dependency upgrades → `agentic-patterns`, `core-architecture`.
- New MCP server or tool contract → `mcp-integration`.
- Eval rubric or dataset layout → `evaluation`.

## Optional adapters (outside this repo’s canon)

If a tool needs its own config file on a developer machine, it should **mirror** content from **`AGENTS.md`** and `.agents/skills` — never the other way around. **This repository does not use `.cursor/` for project rules.**
