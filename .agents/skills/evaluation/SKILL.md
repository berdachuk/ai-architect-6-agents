# Evaluation (Meteoris Insight)

## Description

Defines how to implement the **evaluation module** (`app-eval`): small labelled dataset, deterministic or heuristic checks, metric reporting, and isolation from AutoMemory hints.

## When to use

- Adding YAML/JSON eval cases, parsers, or metric definitions.
- Running batch eval against the orchestrator.
- Extending quality or safety checks (e.g. source attribution vs MCP payload).

## Instructions

- Follow the **structure sketched in `docs/VISION.md`** (weather completeness, news headline counts, etc.).
- Each run should use a **fresh Session** and disable or reset **AutoMemory** so prior user facts do not contaminate metrics.
- Store datasets under a clear resource path (e.g. `app-eval/src/main/resources/eval/`) and version them with code changes.
- Emit a **machine-readable report** (JSON) plus a short human summary for demos.
- Prefer **explicit expected fields** over LLM-as-judge unless you add a separate documented process.

## Boundaries

- Must not redefine course assignment requirements without updating `docs/VISION.md`.
- Must not silently change metric meaning mid-term — document schema changes in eval README or docs.
