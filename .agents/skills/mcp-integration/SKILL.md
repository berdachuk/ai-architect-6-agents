# MCP Integration (Meteoris Insight)

## Description

Covers **Model Context Protocol** usage for **Open‑Meteo** (weather) and a **keyless** news MCP, including Java client wiring, health/timeouts, and Spring `@Tool` facades consumed by agents.

## When to use

- Adding or changing MCP servers, tool schemas, or transport (stdio vs HTTP).
- Normalising MCP JSON into Java DTOs for the LLM.
- Debugging tool-call failures, latency, or schema drift.

## Instructions

- Keep **raw MCP types** inside `app-weather-agent` / `app-news-agent` (or shared `infra` if you add one); the orchestrator should call **stable `@Tool` method names** (e.g. `getWeatherForecast`, `findNews`) as described in `docs/VISION.md`.
- Document each MCP’s **tool list** and **parameter conventions** in module README or `docs/` — agents rely on this.
- Prefer **structured errors** back to the LLM (city not found, rate limit) instead of stack traces in user-visible text.
- For **news without API keys**, validate the chosen MCP’s terms and failure modes; add integration tests with **recorded fixtures** or testcontainers where feasible.

## Boundaries

- Does not define evaluation metrics (`evaluation` skill).
- Must not embed paid API secrets in repository files or skills.
- Must not bypass assignment constraint “news MCP without API key” without explicit human approval.
