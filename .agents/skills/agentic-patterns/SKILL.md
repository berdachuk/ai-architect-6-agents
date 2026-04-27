# Agentic Patterns (Meteoris Insight)

## Description

Guides use of **Spring AI Agentic Patterns** (Parts 1–7) and `spring-ai-agent-utils` for Meteoris Insight: SkillsTool, AskUserQuestionTool, TodoWriteTool, Task tool / subagents, optional A2A, AutoMemoryTools, Session API.

## When to use

- Configuring orchestrator vs weather vs news `ChatClient` instances.
- Choosing advisors: `SessionMemoryAdvisor`, `AutoMemoryToolsAdvisor`, `ToolCallAdvisor`.
- Branching Session events for subagents; enabling `SessionEventTools` recall.
- Deciding whether a behaviour belongs in a **skill** (`SKILL.md`) vs Java prompt vs tool.

## Instructions

- Follow the **official Spring blog order** (Parts 1–7):

| Part | Topic | URL |
|------|-------|-----|
| 1 | Agent Skills | https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills |
| 2 | AskUserQuestionTool | https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool |
| 3 | TodoWriteTool | https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/ |
| 4 | Task tool / Subagents | https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents |
| 5 | A2A integration | https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration |
| 6 | AutoMemoryTools | https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools |
| 7 | Session API | https://spring.io/blog/2026/04/15/spring-ai-session-management |
- **Part 1 — Skills:** ship `weather-skill` and `news-skill` under a directory scanned by `SkillsTool`; keep instructions in Markdown, not hard-coded Java strings where possible.
- **Part 2 — AskUser:** implement `QuestionHandler` for missing city, ambiguous news topic, etc.; bridge async UI with blocking contract as in Spring examples.
- **Part 3 — TodoWrite:** enable for multi-step user requests; respect **single `in_progress`**; pair with `ToolCallAdvisor` + durable history per Part 3 guidance.
- **Part 4 — Task tool:** orchestrator delegates to weather/news subagents; **no nested Task** inside subagents.
- **Part 5 — A2A:** optional; expose AgentCard at `/.well-known/agent-card.json` if you add `spring-ai-a2a-server-autoconfigure`.
- **Part 6 vs 7:** use **Session** for conversation + tool traces; use **AutoMemory** for stable user facts (`MEMORY.md` pattern). Neither replaces pgvector news cache.

## Boundaries

- Does not replace MCP transport documentation (`mcp-integration`).
- Must not silently upgrade Spring AI / community BOM versions without noting compatibility in `docs/VISION.md` or release notes.
- Must not disable session turn-safety to “save tokens” without an explicit documented trade-off.

- Do not use negative port numbers.
