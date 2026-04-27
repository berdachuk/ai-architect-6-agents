# ai-architect-6-agents Documentation

Welcome to the documentation for the **Practical Task: Agentic AI** project.

## Overview

This repository targets a modular agentic service: an orchestrator with weather and news sub-agents, MCP integrations (**Open‑Meteo** for weather and **one** keyless news MCP), memory (session, auto-memory, pgvector), and a separate evaluation module. The runnable service persists data in **PostgreSQL 16 + pgvector** only (Docker Compose or Testcontainers); there is **no H2** profile.

**Practical Task: Agentic AI (summary)** — The product shall answer questions about **current weather** and **latest news** using **agent orchestrators** and MCP-class integrations for **Open‑Meteo** and **Google News RSS** (both keyless — no API key). It shall define, run, and **demonstrate** at least **one** evaluation **metric** on a **small dataset** for at least one criterion (quality, safety, or performance). Full wording: [Project Vision](VISION.md) (*What should be done*).

Normative requirements are in the [Product Requirements Document](PRD.md). For **end users** (browser chat, AskUser, evaluation page, REST overview), see the [User guide](USER-GUIDE.md). For **reproducible evaluation** (dataset, profiles, scoring, reports), see [Evaluation methodology](EVALUATION-METHODOLOGY.md). For **implementation sequencing and WBS** (M1–M6), see [Implementation plan (WBS)](IMPLEMENTATION-PLAN-WBS.md). See the [Project Vision](VISION.md) for the full brief and assignment alignment. For **system architecture** (modules, data, integrations, runtime flows), see [Architecture](ARCHITECTURE.md). For **operational and user-journey scenarios**, see [Work scenarios](WORK-SCENARIOS.md). For **all use case IDs in one place** (master register plus detail tables), see [Use case catalogue](USE-CASES.md). For **agile user stories** with acceptance criteria, see [User stories](USER-STORIES.md). For **UI forms, pages, and end-to-end workflows**, see [Forms and workflows](FORMS-AND-FLOWS.md). For **text wireframes** (layout and copy per screen), see [Wireframes](WIREFRAMES.md). For how AI coding agents should use this repo’s context files, see [AI context strategy](ai-context-strategy.md).

## Local docs build

Install dependencies:

```bash
pip install -r requirements-docs.txt
```

Serve locally from the repository root:

```bash
mkdocs serve
```

Build the static site:

```bash
mkdocs build
```

Strict build (recommended in CI):

```bash
mkdocs build -s
```
